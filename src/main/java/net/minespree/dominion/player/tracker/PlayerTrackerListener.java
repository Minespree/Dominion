package net.minespree.dominion.player.tracker;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.minespree.dominion.DominionPlugin;

public class PlayerTrackerListener implements Listener {
    private final DominionPlugin plugin;

    public PlayerTrackerListener(DominionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> plugin.getPlayerTrackerStorage().storeWhere(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> plugin.getPlayerTrackerStorage().removePlayer(event.getPlayer()));
    }
}
