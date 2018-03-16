package net.minespree.dominion.player.repository;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Optional;
import java.util.UUID;

public class LocalPlayerRepository implements PlayerRepository {
    @Override
    public Optional<UUIDUsernamePair> get(UUID id) {
        ProxiedPlayer pp = ProxyServer.getInstance().getPlayer(id);
        if (pp == null) {
            return Optional.empty();
        }
        return Optional.of(new UUIDUsernamePair(pp.getName(), id));
    }

    @Override
    public Optional<UUIDUsernamePair> get(String username) {
        ProxiedPlayer pp = ProxyServer.getInstance().getPlayer(username);
        if (pp == null) {
            return Optional.empty();
        }
        return Optional.of(new UUIDUsernamePair(pp.getName(), pp.getUniqueId()));
    }
}
