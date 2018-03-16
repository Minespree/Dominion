package net.minespree.dominion.player.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

public class MongoPlayerRepository implements PlayerRepository {
    private final MongoDatabase database;

    public MongoPlayerRepository(MongoDatabase database) {
        this.database = database;
    }

    @Override
    public Optional<UUIDUsernamePair> get(UUID id) {
        MongoCollection<Document> coll = database.getCollection("players");
        Document playerDocument = coll.find(Filters.eq("_id", id.toString())).projection(new Document("lastKnownName", 1)).first();
        if (playerDocument != null) {
            return Optional.of(new UUIDUsernamePair(playerDocument.getString("lastKnownName"), id));
        }
        return Optional.empty();
    }

    @Override
    public Optional<UUIDUsernamePair> get(String username) {
        Document projection = new Document();
        projection.put("lastKnownName", 1);
        projection.put("_id", 1);

        MongoCollection<Document> coll = database.getCollection("players");
        Document playerDocument = coll.find(Filters.eq("lastKnownName", username))
                .collation(Collation.builder().collationStrength(CollationStrength.SECONDARY).locale("en").build())
                .projection(projection)
                .first();
        if (playerDocument != null) {
            return Optional.of(new UUIDUsernamePair(playerDocument.getString("lastKnownName"), UUID.fromString(playerDocument.getString("_id"))));
        }
        return Optional.empty();
    }
}
