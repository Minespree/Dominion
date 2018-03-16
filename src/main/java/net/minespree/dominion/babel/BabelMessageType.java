package net.minespree.dominion.babel;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

/**
 * @since 29/03/2017
 */
public abstract class BabelMessageType<T> {

    @SuppressWarnings("unchecked")
    public void sendMessage(ProxiedPlayer player, Object... params) {
        T msg = toString(player, params);
        if(msg instanceof String) {
            player.sendMessage((String) toString(player, params));
        } else if(msg instanceof List<?>) {
            for(String m : (List<String>) msg) {
                player.sendMessage(m);
            }
        }
    }

    public void broadcast(Object... params) {
        ProxyServer.getInstance().getPlayers().forEach(player -> sendMessage(player, params));
    }

    public T toString(ProxiedPlayer player, Object... params) {
        if(player == null)
            return toString(params);
        String locale = "en";
        if (player.getLocale() != null) {
            locale = player.getLocale().getLanguage();
        }
        return toString(SupportedLanguage.from(locale), params);
    }

    public T toString(Object... params) {
        return toString(SupportedLanguage.ENGLISH, params);
    }

    public String toString() {
        return (String) toString(SupportedLanguage.ENGLISH);
    }

    public abstract T toString(SupportedLanguage language, Object... params);
}
