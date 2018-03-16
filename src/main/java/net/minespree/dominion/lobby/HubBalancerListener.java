package net.minespree.dominion.lobby;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.minespree.dominion.DominionPlugin;
import net.minespree.dominion.babel.Babel;
import net.minespree.dominion.player.parties.Party;
import net.minespree.dominion.servers.LobbyInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class HubBalancerListener implements Listener {
    private final Random random = new Random();
    private final Map<UUID, Integer> hubConnectionAttempts = new ConcurrentHashMap<>();
    private final DominionPlugin plugin;

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        if (event.getTarget().getName().equalsIgnoreCase("lobby")) {
            if (event.getPlayer().getServer() != null && plugin.getHubBalancer().isHub(event.getPlayer().getServer().getInfo())) {
                event.setTarget(event.getPlayer().getServer().getInfo());
            } else {
                event.setTarget(plugin.getHubBalancer().pick());
            }
        }
    }

    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        hubConnectionAttempts.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onServerKick(ServerKickEvent event) {
        if (plugin.getHubBalancer().isHub(event.getKickedFrom())) {
            if (plugin.getHubBalancer().getAvailableServers().size() > 1) {
                int currentAttempts = hubConnectionAttempts.compute(event.getPlayer().getUniqueId(), (k, val) -> val == null ? 1 : val + 1);
                if (currentAttempts > 3) {
                    // Tried too many times to connect to a hub, kick the player.
                    event.setCancelled(false);
                    event.setKickReason(Babel.translate("hub_not_available").toString(event.getPlayer()));
                    hubConnectionAttempts.remove(event.getPlayer().getUniqueId());
                }
                // Find a different (random) hub
                List<String> others = new ArrayList<>(plugin.getHubBalancer().getAvailableServers().keySet());
                others.remove(event.getKickedFrom().getName());
                ServerInfo otherHub = plugin.getProxy().getServerInfo(others.get(random.nextInt(others.size())));

                event.setCancelled(true);
                event.setCancelServer(otherHub);
            }
        } else {
            Babel.translate("sent_to_hub_server_downtime").sendMessage(event.getPlayer());
            event.setCancelled(true);
            event.setCancelServer(plugin.getHubBalancer().pick());
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.getReceiver() instanceof ProxiedPlayer && event.getTag().equals("Dominion")) {
            event.setCancelled(true);
            ProxiedPlayer pp = (ProxiedPlayer) event.getReceiver();

            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String action = in.readUTF();
            if (action.equals("Hub")) {
                pp.connect(plugin.getHubBalancer().pick());
            } else if (action.equals("JoinNext")) {
                String game = in.readUTF();

                // Party check
                Optional<Party> party = plugin.getPartyManager().getParty(pp.getUniqueId());
                if (!party.isPresent() || party.get().getOwner().equals(pp.getUniqueId())) {
                    Optional<LobbyInfo> li = plugin.getServerStatusUpdater().selectServer((ProxiedPlayer) event.getReceiver(), game);
                    if (li.isPresent()) {
                        Babel.translate("sending_player_to_lobby").sendMessage(pp, li.get().getName());
                        pp.connect(plugin.getProxy().getServerInfo(li.get().getName()));
                    } else {
                        Babel.translate("no_lobbies_available").sendMessage(pp, game);
                    }
                } else {
                    Babel.translate("party_member_cant_change_servers").sendMessage(pp);
                }
            }
        }
    }
}
