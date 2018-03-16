package net.minespree.dominion.player.parties;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.minespree.dominion.DominionPlugin;
import net.minespree.dominion.babel.Babel;
import net.minespree.dominion.servers.data.GamePhase;
import net.minespree.dominion.servers.data.LobbyStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class PartyListener implements Listener {
    private final DominionPlugin plugin;
    private final PartyManager partyManager;
    @Getter
    private static final Map<UUID, String> allowMemberMove = new ConcurrentHashMap<>();
    @Getter
    private static final Set<UUID> moveWarningShown = Sets.newConcurrentHashSet();

    @EventHandler
    public void onChat(ChatEvent event) {
        if (event.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = ((ProxiedPlayer) event.getSender());
            Optional<Party> party = partyManager.getParty(player.getUniqueId());
            if (!party.isPresent()) {
                return;
            }

            if (event.getMessage().startsWith("@")) {
                event.setCancelled(true);

                // Make sure player doesn't have party chat off.
                if (partyManager.getPartyChatMuted().contains(player.getUniqueId())) {
                    Babel.translate("party_chat_is_toggled_off").sendMessage(player);
                    return;
                }

                String message = event.getMessage().substring(1);
                JsonObject object = party.get().getBase(player);
                object.addProperty("action", "chat");
                object.addProperty("message", message);
                partyManager.post(object);
            }
        }
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        Optional<Party> party = partyManager.getParty(event.getPlayer().getUniqueId());
        if (!party.isPresent()) {
            return;
        }

        JsonObject object = party.get().getBase(event.getPlayer());
        object.addProperty("action", "reconnect");
        partyManager.post(object);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        partyManager.getInviteManager().remove(event.getPlayer().getUniqueId());
        partyManager.getInviteManager().removeAllInvites(event.getPlayer().getUniqueId());
        partyManager.getPartyChatMuted().remove(event.getPlayer().getUniqueId());

        Optional<Party> party = partyManager.getParty(event.getPlayer().getUniqueId());
        if (!party.isPresent()) {
            return;
        }

        if (party.get().getOwner().equals(event.getPlayer().getUniqueId())) {
            // We currently can't change the party owner right now, so it's an instant disband for now.
            JsonObject object = party.get().getBase(event.getPlayer());
            object.addProperty("action", "disband");
            partyManager.post(object);
        } else {
            JsonObject object = party.get().getBase(event.getPlayer());
            object.addProperty("action", "disconnect");
            partyManager.post(object);

            // Allow 60 seconds to return to party.
            UUID p = event.getPlayer().getUniqueId();
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                if (!plugin.getPlayerTrackerStorage().isOnline(p)) {
                    JsonObject objectLeave = party.get().getBase(event.getPlayer());
                    objectLeave.addProperty("action", "leave");
                    partyManager.post(objectLeave);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerConnect(ServerConnectEvent event) {
        Optional<Party> party = partyManager.getParty(event.getPlayer().getUniqueId());
        if (!party.isPresent()) {
            return;
        }

        if (event.getPlayer().getUniqueId().equals(party.get().getOwner())) {
            if (allowMemberMove.remove(event.getPlayer().getUniqueId(), event.getTarget().getName())) {
                return;
            }

            // Check if we can safely move the party.
            if (isGameServer(event.getTarget().getName())) {
                Optional<LobbyStatus> targetStatus = plugin.getServerStatusUpdater().findLobbyStatus(event.getTarget().getName());
                if (!targetStatus.isPresent() || targetStatus.get().getPlayersOnline() + party.get().getMembers().size() > targetStatus.get().getPlayersMax()) {
                    event.setCancelled(true);
                    Babel.translate("parties_server_would_be_too_full").sendMessage(event.getPlayer());
                    return;
                }

                if (!moveWarningShown.remove(event.getPlayer().getUniqueId())) {
                    Map<UUID, String> memberServers = plugin.getPlayerTrackerStorage().getWhere(party.get().getMembers());
                    for (String s : memberServers.values()) {
                        Optional<LobbyStatus> ls = plugin.getServerStatusUpdater().findLobbyStatus(s);
                        if (ls.isPresent() && ls.get().getPhase() == GamePhase.PLAYING) {
                            // We can't let them move yet. Give them a warning and a chance to force a move.
                            moveWarningShown.add(event.getPlayer().getUniqueId());
                            event.setCancelled(true);
                            Babel.translate("party_confirm_move_to_server").sendMessage(event.getPlayer());
                            return;
                        }
                    }
                }

                event.setCancelled(true);
                JsonObject object = party.get().getBase(event.getPlayer());
                object.addProperty("action", "move");
                object.addProperty("server", event.getTarget().getName());
                partyManager.post(object);
            }
        } else {
            // Plebians can't move, unless it's to a non-game server.
            if (isGameServer(event.getTarget().getName())) {
                if (!allowMemberMove.remove(event.getPlayer().getUniqueId(), event.getTarget().getName())) {
                    event.setCancelled(true);
                    Babel.translate("party_member_cant_change_servers").sendMessage(event.getPlayer());
                }
            }
        }
    }

    private static boolean isGameServer(String name) {
        return name.startsWith("bw-") || name.startsWith("sw-") || name.startsWith("th-");
    }
}
