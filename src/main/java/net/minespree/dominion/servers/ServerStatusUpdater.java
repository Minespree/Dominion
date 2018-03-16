package net.minespree.dominion.servers;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import lombok.NonNull;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.minespree.dominion.player.parties.Party;
import net.minespree.dominion.player.parties.PartyManager;
import net.minespree.dominion.servers.data.GamePhase;
import net.minespree.dominion.servers.data.LobbyStatus;
import net.minespree.myers.bungee.MyersPlugin;
import net.minespree.myers.common.Server;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerStatusUpdater implements Runnable {
    private static final long SHORT_TIMEOUT = 15 * 1000; // 15s -> ms
    private final Map<String, List<LobbyInfo>> availableServers = new ConcurrentHashMap<>();
    private final Map<String, LobbyStatus> serversByName = new ConcurrentHashMap<>();
    private final JedisPool pool;
    private final Gson gson = new Gson();
    private final PartyManager partyManager;

    public ServerStatusUpdater(JedisPool pool, PartyManager partyManager) {
        this.pool = pool;
        this.partyManager = partyManager;
    }

    @Override
    public void run() {
        MyersPlugin mp = (MyersPlugin) ProxyServer.getInstance().getPluginManager().getPlugin("Myers");
        Map<String, Server> myersServers = mp.getServerManager().getAllServers();
        Set<String> packages = myersServers.values().stream()
                .map(s -> s.getProperties().get("playpen_package"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        try (Jedis jedis = pool.getResource()) {
            // get all enabled lobbies
            for (String game : packages) {
                Map<String, String> servers = jedis.hgetAll("instance-statuses:" + game);
                if (servers.isEmpty()) continue;

                List<LobbyInfo> lobbies = new ArrayList<>();
                for (Map.Entry<String, String> entry : servers.entrySet()) {
                    // If Myers doesn't know this server exists, we can't send players to it
                    if (!myersServers.containsKey(entry.getKey())) continue;

                    LobbyStatus status = gson.fromJson(entry.getValue(), LobbyStatus.class);
                    if (System.currentTimeMillis() >= status.getTimestamp() + SHORT_TIMEOUT) {
                        // DNR?
                        continue;
                    }
                    lobbies.add(new LobbyInfo(entry.getKey(), status));
                    serversByName.put(entry.getKey(), status);
                }

                lobbies.sort(null);
                availableServers.put(game, Collections.unmodifiableList(lobbies));
            }
        }
        serversByName.keySet().retainAll(myersServers.keySet());
    }

    public List<LobbyInfo> getLobbiesForGame(@NonNull String game) {
        return availableServers.getOrDefault(game, ImmutableList.of());
    }

    public Optional<LobbyInfo> selectServer(ProxiedPlayer player, String game) {
        List<LobbyInfo> lobbies = getLobbiesForGame(game);
        Optional<Party> party = partyManager.getParty(player.getUniqueId());

        int requiredRoom = party.isPresent() ? party.get().getMembers().size() : 1;
        return lobbies.stream()
                .filter(l -> l.getStatus().getPlayersOnline() + requiredRoom <= l.getStatus().getPlayersMax())
                .filter(l -> l.getStatus().getPhase() == GamePhase.WAITING)
                .findFirst();
    }

    public Optional<LobbyStatus> findLobbyStatus(String lobby) {
        return Optional.ofNullable(serversByName.get(lobby));
    }
}
