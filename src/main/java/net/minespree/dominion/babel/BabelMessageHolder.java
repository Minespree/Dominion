package net.minespree.dominion.babel;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 01/09/2017
 */
@SuppressWarnings("unchecked")
public class BabelMessageHolder implements MessageHolder {

    private Table<SupportedLanguage, String, String> messageTable = HashBasedTable.create();

    BabelMessageHolder(MongoCollection<Document> messageCollection) {
        for (SupportedLanguage language : SupportedLanguage.values()) {
            Bson filter = Filters.eq("_id", language.getLocaleName());
            Document found = messageCollection.find(filter).first();
            if (found != null) {
                Document messages = (Document) found.get("messages");
                for (Map.Entry<String, Object> entry : messages.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof List) {
                        String message = ((List<Object>)value).stream().map(o -> (String) o).collect(Collectors.joining("\n"));
                        messageTable.put(language, entry.getKey(), message);
                    } else if (value instanceof String) {
                        String message = (String) value;
                        messageTable.put(language, entry.getKey(), message);
                    }
                }
            }
        }
    }

    @Override
    public Table<SupportedLanguage, String, String> getMessageTable() {
        return messageTable;
    }
}
