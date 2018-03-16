package net.minespree.dominion.babel;

import com.google.common.collect.Maps;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.Map;

/**
 * @since 26/03/2017
 */
public class Babel {

    private static MessageHolder holder;

    public static void initialize(MongoCollection<Document> langCollection) {
        holder = new BabelMessageHolder(langCollection);
    }

    public static BabelMessage translate(String key, String fallback) {
        Map<SupportedLanguage, String> possiblities = Maps.newHashMap();
        for (SupportedLanguage language : SupportedLanguage.values()) {
            if(holder.getMessageTable().contains(language, key)) {
                possiblities.put(language, holder.getMessageTable().get(language, key));
            } else {
                possiblities.put(language, fallback);
            }
        }
        return new BabelMessage(key, possiblities);
    }

    public static MultiBabelMessage translateMulti(String key, String fallback) {
        Map<SupportedLanguage, String> possiblities = Maps.newHashMap();
        for (SupportedLanguage language : SupportedLanguage.values()) {
            if(holder.getMessageTable().contains(language, key)) {
                possiblities.put(language, holder.getMessageTable().get(language, key));
            } else {
                possiblities.put(language, fallback);
            }
        }
        return new MultiBabelMessage(key, possiblities);
    }

    public static MultiBabelMessage translateMulti(String key) {
        return translateMulti(key, "<" + key + ">");
    }

    public static BabelMessage translate(String key) {
        return translate(key, "<" + key + ">");
    }

    public static StaticBabelMessage messageStatic(String message) {
        return new StaticBabelMessage(message);
    }

}
