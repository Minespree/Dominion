package net.minespree.dominion.babel;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.LinkedList;
import java.util.List;

public class ComplexBabelMessage extends BabelStringMessageType {

    private List<BabelMessageData> messages = new LinkedList<>();

    public ComplexBabelMessage append(BabelMessageType message, Object... params) {
        messages.add(new BabelMessageData(message, params));
        return this;
    }

    public ComplexBabelMessage append(String message, Object... params) {
        messages.add(new BabelMessageData(new StaticBabelMessage(message), params));
        return this;
    }

    public String toString(ProxiedPlayer player, Object... params) {
        return super.toString(player, params);
    }

    public String toString(Object... params) {
        return super.toString(params);
    }

    public String toString(SupportedLanguage language, Object... params) {
        StringBuilder builder = new StringBuilder();
        for (BabelMessageData data : messages) {
            builder.append(data.getMessage().toString(language, data.getParams()));
        }
        return builder.toString();
    }

    private class BabelMessageData {

        private BabelMessageType message;
        private Object[] params;

        BabelMessageData(BabelMessageType message, Object... params) {
            this.message = message;
            this.params = params;
        }

        public BabelMessageType getMessage() {
            return message;
        }

        public Object[] getParams() {
            return params;
        }
    }

}
