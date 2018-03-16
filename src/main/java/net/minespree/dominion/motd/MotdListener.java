package net.minespree.dominion.motd;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.minespree.dominion.DominionPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public class MotdListener implements Listener {
    private final DominionPlugin plugin;
    private final JedisPool pool;

    @EventHandler
    public void onServerPing(ProxyPingEvent event) {
        event.registerIntent(plugin);
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try (Jedis jedis = pool.getResource()) {
                String motd = jedis.get("motd");
                if (motd == null) {
                    motd = "Minespree";
                }
                motd = ChatColor.translateAlternateColorCodes('&', motd);
                event.getResponse().setDescription(motd);
            } finally {
                event.completeIntent(plugin);
            }
        });
    }
}
