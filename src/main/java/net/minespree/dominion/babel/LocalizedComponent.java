package net.minespree.dominion.babel;

import java.util.Locale;

public interface LocalizedComponent {

    default String localize(Locale locale) {
        return "";
    }

    default String localize(SupportedLanguage locale) {
        return "";
    }
}
