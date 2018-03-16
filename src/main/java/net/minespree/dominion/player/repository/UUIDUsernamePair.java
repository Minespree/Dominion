package net.minespree.dominion.player.repository;

import lombok.Value;

import java.util.UUID;

@Value
public class UUIDUsernamePair {
    private final String username;
    private final UUID uuid;
}
