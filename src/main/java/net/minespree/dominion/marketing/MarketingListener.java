package net.minespree.dominion.marketing;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.minespree.dominion.DominionPlugin;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * @since 20/10/2017
 */
public class MarketingListener implements Listener {

    private DominionPlugin plugin;
    private MongoClient client;

    public MarketingListener(DominionPlugin plugin, MongoClient client) {
        this.plugin = plugin;
        this.client = client;
    }

    @EventHandler
    public void on(LoginEvent event) {
        String connection = event.getConnection().getVirtualHost().getHostName();
        if (connection.contains(".minespree.net")) {
            String[] split = connection.split("\\.");
            String market = split[0];
            if (!market.startsWith("dev") && !"play".equalsIgnoreCase(market) && !market.startsWith("game")) {
                plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                    MongoCollection<Document> tracker = client.getDatabase(plugin.getConfiguration().getString("feather.mongodb.db")).getCollection("marketing_tracking");
                    MongoCollection<Document> player = client.getDatabase(plugin.getConfiguration().getString("feather.mongodb.db")).getCollection("players");

                    Bson playerFilter = Filters.eq("_id", event.getConnection().getUniqueId().toString());
                    Document found = player.find(playerFilter).first();
                    if (found == null) {
                        Bson filter = Filters.eq("_id", market);
                        tracker.updateOne(filter, new Document(
                                "$addToSet",
                                new Document("uniqueJoins", event.getConnection().getUniqueId().toString())
                        ).append("$push", new Document("totalJoins",
                                new Document("uuid", event.getConnection().getUniqueId().toString()).
                                        append("name", event.getConnection().getName()).
                                        append("timeStamp", System.currentTimeMillis()))), new UpdateOptions().upsert(true));
                    }
                });
            }
        }
    }

}
