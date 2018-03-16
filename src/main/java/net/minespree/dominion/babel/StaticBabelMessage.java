package net.minespree.dominion.babel;

public class StaticBabelMessage extends BabelStringMessageType {

    private String message;

    StaticBabelMessage(String message) {
        this.message = message;
    }

    public String toString(SupportedLanguage language, Object... params) {
        return message;
    }

}
