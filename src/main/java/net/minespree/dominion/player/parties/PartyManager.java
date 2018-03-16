package net.minespree.dominion.player.parties;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.minespree.dominion.DominionPlugin;
import net.minespree.dominion.babel.Babel;
import net.minespree.dominion.player.FeatherPlayer;
import net.minespree.dominion.player.Rank;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PartyManager {
    public static final int MEMBER_CAP = 16;

    private final Map<UUID, Party> partyMap = new ConcurrentHashMap<>();
    @Getter
    private final PartyInviteManager inviteManager = new PartyInviteManager();
    @Getter(AccessLevel.PACKAGE)
    private final Set<UUID> partyChatMuted = Sets.newConcurrentHashSet();
    @Getter(AccessLevel.PACKAGE)
    private final JedisPool pool;
    private final DominionPlugin plugin;

    public Party createParty(ProxiedPlayer owner) {
        if (partyMap.containsKey(owner.getUniqueId())) {
            throw new IllegalArgumentException("Player already is in a party");
        }

        Party party = new Party(owner.getUniqueId());
        try (Jedis jedis = pool.getResource()) {
            jedis.del("party-members:" + owner.getUniqueId());
            jedis.sadd("party-members:" + owner.getUniqueId(), owner.getUniqueId().toString());
        }

        partyMap.put(owner.getUniqueId(), party);
        return party;
    }

    public void removeParty(UUID owner) {
        Party party = partyMap.get(owner);
        if (party == null) {
            throw new IllegalArgumentException("Player is not in a party");
        }

        if (!party.getOwner().equals(owner)) {
            throw new IllegalArgumentException("This player doesn't own a party");
        }

        partyMap.values().removeIf(s -> s == party);
        try (Jedis jedis = pool.getResource()) {
            jedis.del("party-members:" + owner);
        }
    }

    private Party loadParty(UUID uuid) {
        Party party = new Party(uuid);
        try (Jedis jedis = pool.getResource()) {
            Collection<UUID> uuidizedMembers = jedis.smembers("party-members:" + uuid).stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
            party.getMembers().addAll(uuidizedMembers);
        }
        party.getMembers().forEach(m -> partyMap.put(m, party));
        return party;
    }

    public Optional<Party> getParty(UUID uuid) {
        return Optional.ofNullable(partyMap.get(uuid));
    }

    public void handleRedisMessage(JsonObject object) {
        String action = object.get("action").getAsString();
        UUID actor = UUID.fromString(object.get("actor").getAsString());
        String actorName = object.get("actorName").getAsString();
        UUID partyOwner = UUID.fromString(object.get("partyOwner").getAsString());

        // Does this event apply to us?
        if (!(partyMap.containsKey(actor) || partyMap.containsKey(partyOwner)) && !(action.equals("invite") ||
                action.equals("join") || action.equals("deny"))) {
            return;
        }

        Optional<Party> referencedParty = getParty(partyOwner);

        if (action.equals("chat")) {
            // Party chat
            String msg = object.get("message").getAsString();
            if (referencedParty.isPresent()) {
                Party ref = referencedParty.get();
                boolean isVipActor = plugin.getFeatherPlayerProvider().getIfLoaded(actor).map(FeatherPlayer::getRank).orElse(Rank.MEMBER).has(Rank.VIP);
                ref.forEachPlayerOnThisServer(p -> {
                    // Chat messages from VIP and above and party owner will always be shown.
                    if ((actor.equals(ref.getOwner()) || isVipActor) || !partyChatMuted.contains(p.getUniqueId())) {
                        Babel.translate("party_chat_message").sendMessage(p, actorName, msg);
                    }
                });
            }
        } else if (action.equals("join")) {
            // Join party
            if (!referencedParty.isPresent()) {
                referencedParty = Optional.of(loadParty(partyOwner));
            }

            referencedParty.get().addMember(actor);
            partyMap.put(actor, referencedParty.get());

            referencedParty.get().forEachPlayerOnThisServer(p -> Babel.translate("party_joined").sendMessage(p, actorName));
            inviteManager.removeAllInvites(actor);
        } else if (action.equals("leave")) {
            Party partyToLeave = partyMap.remove(actor);
            if (partyToLeave == null) {
                // Note: this isn't a typo. We don't want to remove any parties, just broadcast a message.
                partyToLeave = partyMap.get(partyOwner);
            }

            partyToLeave.forEachPlayerOnThisServer(p -> Babel.translate("party_left").sendMessage(p, actorName));
            partyToLeave.removeMember(actor);

            try (Jedis jedis = pool.getResource()) {
                jedis.srem("party-members:" + partyOwner, actor.toString());
            }
        } else if (action.equals("disband")) {
            if (referencedParty.isPresent()) {
                referencedParty.get().forEachPlayerOnThisServer(p -> Babel.translate("party_disbanded").sendMessage(p, actorName));
                removeParty(partyOwner);
                inviteManager.remove(partyOwner);
            }
        } else if (action.equals("invite")) {
            UUID inviting = UUID.fromString(object.get("inviting").getAsString());
            ProxiedPlayer toInvite = plugin.getProxy().getPlayer(inviting);

            if (toInvite != null) {
                inviteManager.addInvite(actor, inviting);

                ComponentBuilder builder = new ComponentBuilder(Babel.translate("invite_reply").toString(toInvite)).color(ChatColor.GRAY);
                builder.append(" " + Babel.translate("accept_button").toString(toInvite)).color(ChatColor.GREEN).bold(true);
                builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept " + actorName));
                builder.append(" " + Babel.translate("deny_button").toString(toInvite)).color(ChatColor.RED).bold(true);
                builder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party deny " + actorName));

                Babel.translate("party_invite_join").sendMessage(toInvite, actorName);
                toInvite.sendMessage(builder.create());
            }
        } else if (action.equals("cancel-invite")) {
            UUID invited = UUID.fromString(object.get("invited").getAsString());
            ProxiedPlayer toInvite = plugin.getProxy().getPlayer(invited);

            inviteManager.removeInvite(actor, invited);
            if (toInvite != null) {
                Babel.translate("party_invite_expired").sendMessage(toInvite, actorName);
            }
        } else if (action.equals("move")) {
            if (referencedParty.isPresent()) {
                Party ref = referencedParty.get();
                ServerInfo moveTo = plugin.getProxy().getServerInfo(object.get("server").getAsString());
                if (moveTo != null) {
                    final int[] i = {0};
                    ref.forEachPlayerOnThisServer(pp -> {
                        if (pp.getServer().getInfo().equals(moveTo)) {
                            return;
                        }

                        if (!ref.getOwner().equals(pp.getUniqueId())) {
                            Babel.translate("party_moving_server").sendMessage(pp, moveTo.getName());
                        }

                        plugin.getProxy().getScheduler().schedule(plugin, () -> {
                            PartyListener.getAllowMemberMove().put(pp.getUniqueId(), moveTo.getName());
                            pp.connect(moveTo);
                        }, i[0]++ * 500, TimeUnit.MILLISECONDS);
                    });
                }
            }
        } else if (action.equals("deny")) {
            ProxiedPlayer inviter = plugin.getProxy().getPlayer(partyOwner);
            if (inviter != null) {
                Babel.translate("party_invite_denied").sendMessage(inviter, actorName);
            }
        } else if (action.equals("disconnect")) {
            referencedParty.ifPresent(p -> p.forEachPlayerOnThisServer(pp -> Babel.translate("party_disconnected").sendMessage(pp, actorName)));
        } else if (action.equals("reconnect")) {
            referencedParty.ifPresent(p -> p.forEachPlayerOnThisServer(pp -> Babel.translate("party_reconnected").sendMessage(pp, actorName)));
        }
    }

    void post(JsonObject object) {
        // TODO: Add actual Redis support. For now, directly handle events.
        handleRedisMessage(object);
        //try (Jedis jedis = pool.getResource()) {
        //    jedis.publish("parties", object.toString());
        //}
    }
}
