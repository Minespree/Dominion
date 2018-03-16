package net.minespree.dominion.servers.data;

import lombok.Value;

@Value
public class LobbyStatus implements Comparable<LobbyStatus> {
    private final GamePhase phase;
    private final String mapName;
    private final int playersOnline;
    private final int playersMax;
    private final long timestamp;

    @Override
    public int compareTo(LobbyStatus o) {
        return Integer.compare(playersOnline, o.playersOnline);
    }
}