package net.minespree.dominion.player.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ChainedPlayerRepository implements PlayerRepository {
    private final List<PlayerRepository> repositories;

    public ChainedPlayerRepository(PlayerRepository... repositories) {
        this.repositories = Arrays.asList(repositories);
    }

    @Override
    public Optional<UUIDUsernamePair> get(UUID id) {
        for (PlayerRepository repository : repositories) {
            Optional<UUIDUsernamePair> p = repository.get(id);
            if (p.isPresent()) {
                return p;
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<UUIDUsernamePair> get(String username) {
        for (PlayerRepository repository : repositories) {
            Optional<UUIDUsernamePair> p = repository.get(username);
            if (p.isPresent()) {
                return p;
            }
        }
        return Optional.empty();
    }
}
