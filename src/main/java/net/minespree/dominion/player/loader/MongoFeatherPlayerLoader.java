package net.minespree.dominion.player.loader;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.minespree.dominion.DominionPlugin;
import net.minespree.dominion.player.FeatherPlayer;
import net.minespree.dominion.player.Rank;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MongoFeatherPlayerLoader implements FeatherPlayerLoader {
    private final DominionPlugin plugin;
    private final MongoClient client;

    public MongoFeatherPlayerLoader(DominionPlugin dominionPlugin, MongoClient client) {
        plugin = dominionPlugin;
        this.client = client;
    }

    @Override
    public CompletableFuture<FeatherPlayer> loadPlayer(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            Document projection = new Document("rank", 1);

            MongoCollection<Document> collection = client.getDatabase(plugin.getConfiguration().getString("feather.mongodb.db")).getCollection("players");
            Document playerDocument = collection.find(Filters.eq("_id", id.toString())).projection(projection).first();

            if (playerDocument != null) {
                return new MongoFeatherPlayer(id, playerDocument);
            } else {
                // Provide a stub document - we'll know this is just a regular Joe
                // Dominion is only interested in querying the Feather DB so it can retrieve data
                // from the server.
                return new MongoFeatherPlayer(id, new Document());
            }
        }, plugin.getExecutorService());
    }

    private static class MongoFeatherPlayer implements FeatherPlayer {
        private final UUID id;
        private final Document document;

        private MongoFeatherPlayer(UUID id, Document document) {
            this.id = id;
            this.document = document;
        }

        @Override
        public UUID getUuid() {
            return id;
        }

        @Override
        public Rank getRank() {
            String rankVal = document.getString("rank");
            if (rankVal != null) {
                return Rank.valueOf(rankVal);
            }
            return Rank.MEMBER;
        }
    }
}
