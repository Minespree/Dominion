package net.minespree.dominion.player.tracker;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import redis.clients.jedis.*;

import java.util.*;

public class RedisPlayerTrackerStorage implements PlayerTrackerStorage {
    private final JedisPool pool;

    public RedisPlayerTrackerStorage(JedisPool pool) {
        this.pool = pool;
    }

    @Override
    public void storeWhere(ProxiedPlayer player) {
        String shardAt = player.getUniqueId().toString().substring(0, 1);
        try (Jedis jedis = pool.getResource()) {
            String key = "player-server:" + shardAt;
            long res = jedis.hset(key, player.getUniqueId().toString(), player.getServer().getInfo().getName());
            if (res == 1) {
                JsonObject object = new JsonObject();
                object.addProperty("channel", "player-join");
                object.addProperty("player", player.getUniqueId().toString());

                jedis.publish("feather", object.toString());
            }
        }
    }

    @Override
    public void removePlayer(ProxiedPlayer player) {
        String shardAt = player.getUniqueId().toString().substring(0, 1);
        try (Jedis jedis = pool.getResource()) {
            jedis.hdel("player-server:" + shardAt, player.getUniqueId().toString());

            JsonObject object = new JsonObject();
            object.addProperty("channel", "player-quit");
            object.addProperty("player", player.getUniqueId().toString());

            jedis.publish("feather", object.toString());
        }
    }

    @Override
    public Optional<String> getWhere(UUID uuid) {
        String shardAt = uuid.toString().substring(0, 1);
        try (Jedis jedis = pool.getResource()) {
            return Optional.ofNullable(jedis.hget("player-server:" + shardAt, uuid.toString()));
        }
    }

    @Override
    public Map<UUID, String> getWhere(Collection<UUID> uuids) {
        List<UUID> currentOrder = ImmutableList.copyOf(uuids);
        Map<UUID, String> servers = new HashMap<>();
        try (Jedis jedis = pool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            List<Response<String>> where = new ArrayList<>();
            for (UUID uuid : currentOrder) {
                String uuidStr = uuid.toString();
                where.add(pipeline.hget("player-server:" + uuidStr.substring(0, 1), uuidStr));
            }
            pipeline.sync();
            for (int i = 0; i < currentOrder.size(); i++) {
                UUID u = currentOrder.get(i);
                Response<String> s = where.get(i);
                if (s.get() != null) {
                    servers.put(u, s.get());
                }
            }
        }
        return servers;
    }

    @Override
    public boolean isOnline(UUID uuid) {
        String shardAt = uuid.toString().substring(0, 1);
        try (Jedis jedis = pool.getResource()) {
            return jedis.hexists("player-server:" + shardAt, uuid.toString());
        }
    }
}
