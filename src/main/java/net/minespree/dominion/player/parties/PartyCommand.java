package net.minespree.dominion.player.parties;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.minespree.dominion.DominionPlugin;
import net.minespree.dominion.babel.Babel;
import net.minespree.dominion.player.repository.UUIDUsernamePair;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PartyCommand extends Command {

    private final PartyManager partyManager;
    private final DominionPlugin plugin;

    public PartyCommand(PartyManager partyManager, DominionPlugin plugin) {
        super("party", null, "p");
        this.partyManager = partyManager;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] fullArgs) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ChatColor.RED + "You have to be a player to use this command");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        if (fullArgs.length == 0) {
            Optional<Party> party = plugin.getPartyManager().getParty(player.getUniqueId());
            if (party.isPresent()) {
                ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
                    List<String> members = party.get().getMembers().stream()
                            .map(plugin.getPlayerRepository()::get)
                            .map(p -> p.map(UUIDUsernamePair::getUsername).orElse("UNKNOWN"))
                            .collect(Collectors.toList());
                    String ownerName = plugin.getPlayerRepository().get(party.get().getOwner())
                            .map(UUIDUsernamePair::getUsername).orElse("UNKNOWN");

                    Babel.translate("party_list_owner_username").sendMessage(player, ownerName);
                    Babel.translate("party_list_members").sendMessage(player, Joiner.on(", ").join(members));
                });
            } else {
                Babel.translate("party_how_to_create").sendMessage(player);
            }
            return;
        }
        
        String[] subcmdArgs;
        if (fullArgs.length == 1) {
            subcmdArgs = new String[0];
        } else {
            subcmdArgs = Arrays.copyOfRange(fullArgs, 1, fullArgs.length);
        }

        // need to run a sinks (thx wemux)
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            String cmd = fullArgs[0];
            if (cmd.equalsIgnoreCase("help")) {
                Babel.translate("party_help").sendMessage(player);
            } else if (cmd.equalsIgnoreCase("create")) {
                Babel.translate("party_create_command_instructions").sendMessage(player);
            } else if (cmd.equalsIgnoreCase("disband")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                if (!party.isPresent()) {
                    Babel.translate("not_in_a_party").sendMessage(player);
                    return;
                }

                if (!player.getUniqueId().equals(party.get().getOwner())) {
                    Babel.translate("not_party_owner").sendMessage(player);
                    return;
                }

                JsonObject object = party.get().getBase(player);
                object.addProperty("action", "disband");
                partyManager.post(object);
            } else if (cmd.equalsIgnoreCase("invite")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                int invitesLeft = partyManager.getInviteManager().getRemainingInvites(player.getUniqueId());
                if (party.isPresent()) {
                    if (!player.getUniqueId().equals(party.get().getOwner())) {
                        Babel.translate("not_party_owner").sendMessage(player);
                        return;
                    }

                    invitesLeft = partyManager.getInviteManager().getRemainingInvites(party.get());
                }

                if (subcmdArgs.length != 1) {
                    player.sendMessage("/party invite <Player>");
                    return;
                }

                if (invitesLeft <= 0) {
                    Babel.translate("party_invite_cap_exceeded").sendMessage(player);
                    return;
                }

                // TODO: repository lookup support?
                ProxiedPlayer toInvite = ProxyServer.getInstance().getPlayer(subcmdArgs[0]);
                if (toInvite == null) {
                    Babel.translate("party_invite_player_not_online").sendMessage(player);
                    return;
                }

                if (player.getUniqueId().equals(toInvite.getUniqueId())) {
                    Babel.translate("party_cant_invite_self").sendMessage(player);
                    return;
                }

                if (partyManager.getInviteManager().wasInvited(player.getUniqueId(), toInvite.getUniqueId())) {
                    Babel.translate("party_already_invited").sendMessage(player);
                    return;
                }

                if (partyManager.getParty(toInvite.getUniqueId()).isPresent()) {
                    Babel.translate("party_player_already_in_party").sendMessage(player);
                    return;
                }

                JsonObject inviteObj;
                if (party.isPresent()) {
                    inviteObj = party.get().getBase(player);
                    inviteObj.addProperty("action", "invite");
                    inviteObj.addProperty("inviting", toInvite.getUniqueId().toString());
                } else {
                    // manual invite object creation
                    inviteObj = new JsonObject();
                    inviteObj.addProperty("actor", player.getUniqueId().toString());
                    inviteObj.addProperty("actorName", player.getName());
                    inviteObj.addProperty("partyOwner", player.getUniqueId().toString());
                    inviteObj.addProperty("channel", "parties");
                    inviteObj.addProperty("action", "invite");
                    inviteObj.addProperty("inviting", toInvite.getUniqueId().toString());
                }

                partyManager.post(inviteObj);
                Babel.translate("party_invite_sent").sendMessage(player, toInvite.getName());

                // expiry handling
                JsonObject expireObj = new JsonObject();
                // I wish I could do new JsonObject(obj), but I'll have to settle for this
                for (Map.Entry<String, JsonElement> entry : inviteObj.entrySet()) {
                    expireObj.add(entry.getKey(), entry.getValue());
                }
                expireObj.addProperty("action", "cancel-invite");
                expireObj.addProperty("invited", toInvite.getUniqueId().toString());
                UUID runnerUuid = player.getUniqueId();
                ProxyServer.getInstance().getScheduler().schedule(plugin, () -> {
                    Optional<Party> checkAgain = partyManager.getParty(runnerUuid);
                    if (checkAgain.isPresent() && partyManager.getInviteManager().wasInvited(player.getUniqueId(), toInvite.getUniqueId())) {
                        partyManager.post(expireObj);
                    }
                }, 60, TimeUnit.SECONDS);
            } else if (cmd.equalsIgnoreCase("chat")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                if (!party.isPresent()) {
                    Babel.translate("not_in_a_party").sendMessage(player);
                    return;
                }

                if (subcmdArgs.length == 0) {
                    boolean toggled = partyManager.getPartyChatMuted().add(player.getUniqueId());
                    if (toggled) {
                        Babel.translate("party_chat_toggled_off").sendMessage(player);
                    } else {
                        partyManager.getPartyChatMuted().remove(player.getUniqueId());
                        Babel.translate("party_chat_toggled_on").sendMessage(player);
                    }
                    return;
                }

                // Make sure player doesn't have party chat off.
                if (partyManager.getPartyChatMuted().contains(player.getUniqueId())) {
                    Babel.translate("party_chat_is_toggled_off").sendMessage(player);
                    return;
                }

                JsonObject object = party.get().getBase(player);
                object.addProperty("action", "chat");
                object.addProperty("message", Joiner.on(' ').join(subcmdArgs));
                partyManager.post(object);
            } else if (cmd.equalsIgnoreCase("leave")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                if (!party.isPresent()) {
                    Babel.translate("not_in_a_party").sendMessage(player);
                    return;
                }

                if (player.getUniqueId().equals(party.get().getOwner())) {
                    Babel.translate("cant_leave_own_party").sendMessage(player);
                    return;
                }

                JsonObject object = party.get().getBase(player);
                object.addProperty("action", "leave");
                partyManager.post(object);

                try (Jedis jedis = partyManager.getPool().getResource()) {
                    jedis.srem("party-members:" + party.get().getOwner(), player.getUniqueId().toString());
                }
            } else if (cmd.equalsIgnoreCase("kick")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                if (!party.isPresent()) {
                    Babel.translate("not_in_a_party").sendMessage(player);
                    return;
                }

                if (!player.getUniqueId().equals(party.get().getOwner())) {
                    Babel.translate("not_party_owner").sendMessage(player);
                    return;
                }

                if (subcmdArgs.length != 1) {
                    player.sendMessage("/party kick <Player>");
                    return;
                }

                // TODO: repository lookup support?
                ProxiedPlayer toKick = ProxyServer.getInstance().getPlayer(subcmdArgs[0]);
                if (toKick == null) {
                    Babel.translate("party_invite_player_not_online").sendMessage(player);
                    return;
                }
                if (toKick.getUniqueId().equals(party.get().getOwner())) {
                    Babel.translate("parties_cant_kick_self").sendMessage(player);
                    return;
                }

                if (!party.get().getMembers().contains(toKick.getUniqueId())) {
                    Babel.translate("player_not_in_party").sendMessage(player);
                    return;
                }

                JsonObject object = party.get().getBase(player);
                object.addProperty("action", "leave");
                object.addProperty("actor", toKick.getUniqueId().toString());
                object.addProperty("actorName", toKick.getName());
                partyManager.post(object);

                try (Jedis jedis = partyManager.getPool().getResource()) {
                    jedis.srem("party-members:" + player.getUniqueId(), toKick.getUniqueId().toString());
                }

                Babel.translate("party_player_kicked").sendMessage(player, toKick.getName());
            } else if (cmd.equalsIgnoreCase("accept")) {
                Optional<Party> selfParty = partyManager.getParty(player.getUniqueId());
                if (selfParty.isPresent()) {
                    Babel.translate("already_in_party").sendMessage(player);
                    return;
                }

                if (subcmdArgs.length != 1) {
                    player.sendMessage("/party accept <Player>");
                    return;
                }

                // TODO: repository lookup support?
                ProxiedPlayer inviter = ProxyServer.getInstance().getPlayer(subcmdArgs[0]);
                if (inviter == null) {
                    Babel.translate("party_invite_player_not_online").sendMessage(player);
                    return;
                }

                if (partyManager.getInviteManager().removeInvite(inviter.getUniqueId(), player.getUniqueId())) {
                    Optional<Party> inviterParty = partyManager.getParty(inviter.getUniqueId());
                    if (!inviterParty.isPresent()) {
                        inviterParty = Optional.of(partyManager.createParty(inviter));
                    }

                    JsonObject object = inviterParty.get().getBase(player);
                    object.addProperty("action", "join");
                    partyManager.post(object);

                    try (Jedis jedis = partyManager.getPool().getResource()) {
                        jedis.sadd("party-members:" + inviter.getUniqueId(), player.getUniqueId().toString());
                    }
                } else {
                    Babel.translate("no_such_invite").sendMessage(player);
                }
            } else if (cmd.equalsIgnoreCase("deny")) {
                Optional<Party> selfParty = partyManager.getParty(player.getUniqueId());
                if (selfParty.isPresent()) {
                    Babel.translate("already_in_party").sendMessage(player);
                    return;
                }

                if (subcmdArgs.length != 1) {
                    player.sendMessage("/party deny <Player>");
                    return;
                }

                // TODO: repository lookup support?
                ProxiedPlayer inviter = ProxyServer.getInstance().getPlayer(subcmdArgs[0]);
                if (inviter == null) {
                    Babel.translate("party_invite_player_not_online").sendMessage(player);
                    return;
                }

                if (partyManager.getInviteManager().removeInvite(inviter.getUniqueId(), player.getUniqueId())) {
                    JsonObject inviteObj = new JsonObject();
                    inviteObj.addProperty("actor", player.getUniqueId().toString());
                    inviteObj.addProperty("actorName", player.getName());
                    inviteObj.addProperty("partyOwner",inviter.getUniqueId().toString());
                    inviteObj.addProperty("channel", "parties");
                    inviteObj.addProperty("action", "deny");

                    partyManager.post(inviteObj);
                } else {
                    Babel.translate("no_such_invite").sendMessage(player);
                }
            } else if (cmd.equalsIgnoreCase("warp")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                if (!party.isPresent()) {
                    Babel.translate("not_in_a_party").sendMessage(player);
                    return;
                }

                if (!player.getUniqueId().equals(party.get().getOwner())) {
                    Babel.translate("not_party_owner").sendMessage(player);
                    return;
                }

                Babel.translate("parties_warping_to_your_server").sendMessage(player);

                JsonObject object = party.get().getBase(player);
                object.addProperty("action", "move");
                object.addProperty("server", player.getServer().getInfo().getName());
                partyManager.post(object);
            } else if (cmd.equalsIgnoreCase("tp")) {
                Optional<Party> party = partyManager.getParty(player.getUniqueId());
                if (!party.isPresent()) {
                    Babel.translate("not_in_a_party").sendMessage(player);
                    return;
                }

                Optional<String> target = plugin.getPlayerTrackerStorage().getWhere(party.get().getOwner());
                if (target.isPresent()) {
                    PartyListener.getAllowMemberMove().put(player.getUniqueId(), target.get());
                    Babel.translate("parties_tping_to_owner").sendMessage(player);
                    player.connect(plugin.getProxy().getServerInfo(target.get()));
                } else {
                    Babel.translate("parties_owner_server_not_found").sendMessage(player);
                }
            } else if (cmd.equalsIgnoreCase("help")) {
                Babel.translateMulti("party_help").sendMessage(player);
            }
        });
    }
}
