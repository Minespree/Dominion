package net.minespree.dominion.welcome;

import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.minespree.dominion.babel.Babel;
import net.minespree.dominion.chat.Chat;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

@UtilityClass
public class WelcomeMessage {
    private static final List<Language> languages = ImmutableList.<Language>builder()
            .add(new Language("English", "&eHello, &6&l%player%&r&e!"))
            .add(new Language("Spanish", "&e¡Hola &6&l%player%&r&e!"))
            .add(new Language("French", "&eBonjour, &6&l%player%&r&e!"))
            .add(new Language("Pirate", "&eAhoy matey, &6&l%player%&r&e!"))
            .add(new Language("German", "&eHallo, &6&l%player%&r&e!"))
            .add(new Language("Chinese", "&e你好, &6&l%player%&r&e!"))
            .add(new Language("Japanese", "&e今日は, &6&l%player%&r&e!"))
            .add(new Language("Russian", "&eЗдравствуйте, &6&l%player%&r&e!"))
            .add(new Language("Esperanto", "&eSaluton &6&l%player%&r&e!"))
            .build();

    private static final Random RANDOM = ThreadLocalRandom.current();

    public static void sendWelcome(ProxiedPlayer player) {
        Language inLanguage = languages.get(RANDOM.nextInt(languages.size()));

        Stream.of(
            "",
            Chat.center(inLanguage.get(player)),
            Chat.center(Babel.translate("welcome_to_minespree").toString(player)),
            Chat.center(Babel.translate("now_you_know_hello_in").toString(player, inLanguage)),
            ""
        ).map(TextComponent::fromLegacyText)
         .forEachOrdered(player::sendMessage);

        Babel.translateMulti("alpha_message").toString(player).forEach(message -> {
            player.sendMessage(TextComponent.fromLegacyText(Chat.center(message)));
        });
    }

    private static class Language {

        private final String name;
        private final String format;

        private Language(String name, String format) {
            this.name = name;
            this.format = format;
        }

        public String get(ProxiedPlayer player) {
            return ChatColor.translateAlternateColorCodes('&', format.replace("%player%", player.getName()));
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
