package net.minespree.dominion.rejoin;

import java.util.Optional;
import java.util.UUID;

public interface ServerRejoinStorage {
    Optional<String> getStoredServer(UUID player);
}
