package net.minespree.dominion.player;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.minespree.dominion.DominionPlugin;

public class PlayerLoadListener implements Listener {
    private final DominionPlugin plugin;

    public PlayerLoadListener(DominionPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        // TODO: This _should_ be converted to a async task executing on LoginEvent, but BungeeCord only allows
        //       us to wage one battle at a time with split listeners. Sigh.
        plugin.getFeatherPlayerProvider().getOrLoad(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        plugin.getFeatherPlayerProvider().unload(event.getPlayer().getUniqueId());
    }
}
