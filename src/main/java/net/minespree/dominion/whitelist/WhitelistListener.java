package net.minespree.dominion.whitelist;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.LoginEvent;
import net.minespree.dominion.player.FeatherPlayer;
import net.minespree.dominion.player.FeatherPlayerProvider;
import net.minespree.dominion.player.Rank;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * @since 06/10/2017
 */
public class WhitelistListener {

    private JedisPool pool;
    private FeatherPlayerProvider provider;

    private JsonParser parser = new JsonParser();

    public WhitelistListener(JedisPool pool, FeatherPlayerProvider provider) {
        this.pool = pool;
        this.provider = provider;
    }

    public CompletableFuture<Void> on(LoginEvent event) {
        ComponentBuilder builder = new ComponentBuilder("You cannot join at this moment").color(ChatColor.RED);

        return CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                String s = jedis.get("whitelist");
                if (s != null) {
                    try {
                        JsonElement element = parser.parse(s);
                        JsonObject object = element.getAsJsonObject();
                        WhitelistMode mode = WhitelistMode.valueOf(object.get("mode").getAsString());

                        if (mode == WhitelistMode.ALL) return;
                        UUID uuid = event.getConnection().getUniqueId();
                        provider.getOrLoad(uuid).whenCompleteAsync((featherPlayer, throwable) -> {
                            if (throwable != null) {
                                event.setCancelled(true);
                                event.setCancelReason(builder.create());

                                provider.unload(uuid);
                                return;
                            }

                            if ((mode == WhitelistMode.STAFF && !featherPlayer.getRank().has(Rank.HELPER)) || (mode == WhitelistMode.VIP && !featherPlayer.getRank().has(Rank.VIP))) {
                                event.setCancelled(true);
                                event.setCancelReason(builder.create());

                                provider.unload(uuid);
                            } else {
                                event.setCancelled(false);
                            }
                        });
                    } catch (Exception e) {
                        event.setCancelled(true);
                        event.setCancelReason(builder.create());

                        provider.unload(event.getConnection().getUniqueId());

                        e.printStackTrace();
                    }
                }
            }
        }).thenApply(s -> null);
    }

    public enum WhitelistMode {
        ALL,
        STAFF,
        VIP
    }
}
