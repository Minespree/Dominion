package net.minespree.dominion.player.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CachedPlayerRepository implements PlayerRepository {
    private final Cache<String, UUIDUsernamePair> nameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();
    private final Cache<UUID, UUIDUsernamePair> uuidCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();
    private final PlayerRepository backing;

    public CachedPlayerRepository(PlayerRepository backing) {
        this.backing = backing;
    }

    @Override
    public Optional<UUIDUsernamePair> get(UUID id) {
        UUIDUsernamePair p = uuidCache.getIfPresent(id);
        if (p != null) {
            return Optional.of(p);
        }
        Optional<UUIDUsernamePair> po = backing.get(id);
        if (po.isPresent()) {
            nameCache.put(po.get().getUsername().toLowerCase(), po.get());
            uuidCache.put(id, po.get());
        }
        return po;
    }

    @Override
    public Optional<UUIDUsernamePair> get(String username) {
        UUIDUsernamePair p = uuidCache.getIfPresent(username.toLowerCase());
        if (p != null) {
            return Optional.of(p);
        }
        Optional<UUIDUsernamePair> po = backing.get(username);
        if (po.isPresent()) {
            nameCache.put(po.get().getUsername().toLowerCase(), po.get());
            uuidCache.put(po.get().getUuid(), po.get());
        }
        return po;
    }
}
