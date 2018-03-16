package net.minespree.dominion.punishments;

import com.google.common.collect.Sets;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.minespree.dominion.DominionPlugin;
import org.bson.Document;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @since 12/09/2017
 */
public class PunishmentManager {

    private DominionPlugin plugin;
    private final MongoClient client;

    public PunishmentManager(DominionPlugin plugin, MongoClient client) {
        this.plugin = plugin;
        this.client = client;
    }

    public CompletableFuture<Set<Punishment>> fetchPunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Set<Punishment> punishments = Sets.newHashSet();
            MongoCollection<Document> collection = client.getDatabase(plugin.getConfiguration().getString("feather.mongodb.db")).getCollection("punishments");
            FindIterable<Document> iterable = collection.find(Filters.eq("target", uuid.toString()));
            for (Document document : iterable) {
                PunishmentType type = PunishmentType.valueOf(document.getString("type"));
                UUID target = UUID.fromString(document.getString("target"));
                UUID source = document.getString("source") == null ? null : UUID.fromString(document.getString("source"));
                long timeStamp = document.getLong("timestamp");
                long until = document.getLong("until");
                String reason = document.getString("reason");
                boolean undone = document.getBoolean("undone");
                String appealCode = document.getString("appealCode");

                if (((type == PunishmentType.TEMP_BAN || type == PunishmentType.E_TEMP_BAN || type == PunishmentType.H_TEMP_BAN) && System.currentTimeMillis() >= until) || undone) {
                    continue;
                }

                Punishment punishment = new Punishment(type, source, target, reason, timeStamp, until, appealCode);
                punishments.add(punishment);
            }
            return punishments;
        }, plugin.getExecutorService());
    }
}
