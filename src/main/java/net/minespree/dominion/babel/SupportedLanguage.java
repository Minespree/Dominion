package net.minespree.dominion.babel;

/**
 * @since 01/09/2017
 */
public enum SupportedLanguage {
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr");

    private String localeName;

    SupportedLanguage(String localeName) {
        this.localeName = localeName;
    }

    public String getLocaleName() {
        return localeName;
    }

    public static SupportedLanguage from(String locale) {
        for (SupportedLanguage language : values()) {
            if (language.getLocaleName().equalsIgnoreCase(locale)) {
                return language;
            }
        }

        return ENGLISH;
    }
}
