package net.minespree.dominion.lobby;

import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.minespree.myers.bungee.MyersPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class HubBalancer {
    @Getter
    private volatile Map<String, ServerPing> availableServers = new ConcurrentHashMap<>();

    public void refresh() {
        // find all possible servers
        MyersPlugin plugin = (MyersPlugin) ProxyServer.getInstance().getPluginManager().getPlugin("Myers");
        List<String> possibleServers = plugin.getServerManager().getAllServers().entrySet().stream()
                .filter(s -> s.getValue().getProperties().getOrDefault("playpen_package", "").equals("hub"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // prune old servers
        availableServers.keySet().retainAll(possibleServers);

        // identify servers by player count
        CountDownLatch l = new CountDownLatch(possibleServers.size());
        for (String possibleServer : possibleServers) {
            ServerInfo info = ProxyServer.getInstance().getServerInfo(possibleServer);
            info.ping((serverPing, throwable) -> {
                if (serverPing != null && serverPing.getPlayers().getOnline() < serverPing.getPlayers().getMax()) {
                    availableServers.put(possibleServer, serverPing);
                } else {
                    availableServers.remove(possibleServer);
                }
                l.countDown();
            });
        }

        try {
            l.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public ServerInfo pick() {
        if (availableServers.isEmpty()) {
            throw new NoSuchElementException();
        }

        Map.Entry<String, ServerPing> entry = Collections.min(availableServers.entrySet(), (o1, o2) ->
                Integer.compare(o2.getValue().getPlayers().getOnline(), o1.getValue().getPlayers().getOnline()));
        return ProxyServer.getInstance().getServerInfo(entry.getKey());
    }

    public boolean isHub(ServerInfo info) {
        return availableServers.containsKey(info.getName());
    }
}
