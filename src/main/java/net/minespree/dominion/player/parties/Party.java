package net.minespree.dominion.player.parties;

import com.google.gson.JsonObject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class Party {
    private final UUID owner;
    private final Set<UUID> members = new LinkedHashSet<>();

    public Party(UUID owner) {
        this.owner = owner;
        members.add(owner);
    }

    public Collection<UUID> getMembers() {
        return members;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean addMember(UUID uuid) {
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public void forEachPlayerOnThisServer(Consumer<ProxiedPlayer> consumer) {
        members.forEach(u -> {
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(u);
            if (p != null) consumer.accept(p);
        });
    }

    public JsonObject getBase(ProxiedPlayer player) {
        JsonObject object = new JsonObject();
        object.addProperty("actor", player.getUniqueId().toString());
        object.addProperty("actorName", player.getName());
        object.addProperty("partyOwner", owner.toString());
        object.addProperty("channel", "parties");
        return object;
    }
}
