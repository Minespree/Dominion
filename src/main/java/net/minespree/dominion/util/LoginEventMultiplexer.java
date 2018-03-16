package net.minespree.dominion.util;

import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class LoginEventMultiplexer extends AsyncEventMultiplexer<LoginEvent> implements Listener {
    public LoginEventMultiplexer(Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void on(LoginEvent e) {
        handle(e);
    }
}
