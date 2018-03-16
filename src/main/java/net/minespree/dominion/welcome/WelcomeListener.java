package net.minespree.dominion.welcome;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * @since 15/10/2017
 */
public class WelcomeListener implements Listener {
    @EventHandler
    public void onJoin(PostLoginEvent e) {
        WelcomeMessage.sendWelcome(e.getPlayer());
    }
}
