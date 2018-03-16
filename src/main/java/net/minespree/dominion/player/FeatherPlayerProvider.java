package net.minespree.dominion.player;

import net.minespree.dominion.player.loader.FeatherPlayerLoader;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FeatherPlayerProvider {
    private final Map<UUID, FeatherPlayer> loadedPlayers = new ConcurrentHashMap<>();
    private final FeatherPlayerLoader loader;

    public FeatherPlayerProvider(FeatherPlayerLoader loader) {
        this.loader = loader;
    }

    public Optional<FeatherPlayer> getIfLoaded(UUID uuid) {
        return Optional.ofNullable(loadedPlayers.get(uuid));
    }

    public CompletableFuture<FeatherPlayer> getOrLoad(UUID uuid) {
        Optional<FeatherPlayer> loaded = getIfLoaded(uuid);
        return loaded.map(CompletableFuture::completedFuture).orElseGet(() -> loader.loadPlayer(uuid).thenApply(p -> {
            loadedPlayers.put(uuid, p);
            return p;
        }));

    }

    public void unload(UUID id) {
        loadedPlayers.remove(id);
    }
}
