package net.minespree.dominion.servers;

import lombok.Value;
import net.minespree.dominion.servers.data.LobbyStatus;

@Value
public class LobbyInfo implements Comparable<LobbyInfo> {
    private final String name;
    private final LobbyStatus status;

    @Override
    public int compareTo(LobbyInfo o) {
        int x = o.getStatus().compareTo(status);
        if (x != 0) {
            return x;
        }
        return o.getName().compareTo(name);
    }
}
