package net.minespree.dominion.player.loader;

import net.minespree.dominion.player.FeatherPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FeatherPlayerLoader {
    CompletableFuture<FeatherPlayer> loadPlayer(UUID id);
}
