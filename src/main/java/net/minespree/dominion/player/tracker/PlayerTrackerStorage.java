package net.minespree.dominion.player.tracker;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PlayerTrackerStorage {
    void storeWhere(ProxiedPlayer player);

    void removePlayer(ProxiedPlayer player);

    Optional<String> getWhere(UUID uuid);

    Map<UUID, String> getWhere(Collection<UUID> uuids);

    boolean isOnline(UUID uuid);
}
