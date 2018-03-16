package net.minespree.dominion.rejoin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;
import java.util.UUID;

public class RedisServerRejoinStorage implements ServerRejoinStorage {
    private final JedisPool pool;

    public RedisServerRejoinStorage(JedisPool pool) {
        this.pool = pool;
    }

    @Override
    public Optional<String> getStoredServer(UUID player) {
        try (Jedis jedis = pool.getResource()) {
            // retrieve and remove last game the player was in
            String gameId = jedis.get("game-last-left:" + player);
            if (gameId == null) {
                return Optional.empty();
            }

            jedis.del("game-last-left:" + player);

            String gameIdToGameServer = jedis.get("game-server-ids:" + gameId);
            return Optional.ofNullable(gameIdToGameServer);
        }
    }
}
