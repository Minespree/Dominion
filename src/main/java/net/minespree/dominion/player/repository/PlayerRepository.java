package net.minespree.dominion.player.repository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {
    Optional<UUIDUsernamePair> get(UUID id);

    Optional<UUIDUsernamePair> get(String username);
}
