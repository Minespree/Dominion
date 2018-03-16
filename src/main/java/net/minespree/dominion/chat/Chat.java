package net.minespree.dominion.chat;

import com.google.common.collect.Lists;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @since 15/10/2017
 */
public class Chat {
    public static final String AQUA = ChatColor.AQUA.toString();
    public static final String CYAN = ChatColor.DARK_AQUA.toString();
    public static final String BLACK = ChatColor.BLACK.toString();
    public static final String DARK_BLUE = ChatColor.DARK_BLUE.toString();
    public static final String DARK_GREEN = ChatColor.DARK_GREEN.toString();
    public static final String DARK_RED = ChatColor.DARK_RED.toString();
    public static final String PURPLE = ChatColor.DARK_PURPLE.toString();
    public static final String GOLD = ChatColor.GOLD.toString();
    public static final String GRAY = ChatColor.GRAY.toString();
    public static final String DARK_GRAY = ChatColor.DARK_GRAY.toString();
    public static final String BLUE = ChatColor.BLUE.toString();
    public static final String GREEN = ChatColor.GREEN.toString();
    public static final String RED = ChatColor.RED.toString();
    public static final String PINK = ChatColor.LIGHT_PURPLE.toString();
    public static final String YELLOW = ChatColor.YELLOW.toString();
    public static final String WHITE = ChatColor.WHITE.toString();
    public static final String MAGIC = ChatColor.MAGIC.toString();
    public static final String BOLD = ChatColor.BOLD.toString();
    public static final String DATA = CYAN + BOLD;
    public static final String STRIKE = ChatColor.STRIKETHROUGH.toString();
    public static final String UNDERLINE = ChatColor.UNDERLINE.toString();
    public static final String ITALIC = ChatColor.ITALIC.toString();
    public static final String RESET = ChatColor.RESET.toString();

    public static final List<String> lights = Lists.newArrayList(GOLD, GREEN, AQUA, BLUE, RED, PINK, YELLOW);
    public static final List<String> darks = Lists.newArrayList(DARK_BLUE, DARK_GREEN, CYAN, DARK_RED, PURPLE);
    public static final List<String> shades = Lists.newArrayList(BLACK, GRAY, DARK_GRAY, WHITE);

    public static final String DARK_STAR = "★";
    public static final String WHITE_STAR = "☆";
    public static final String CIRCLE_BLANK_STAR = "✪";
    public static final String BIG_BLOCK = "█";
    public static final String SMALL_BLOCK = "▌";
    public static final String SMALL_DOT = "•";
    public static final String LARGE_DOT = "●";
    public static final String HEART = "♥";
    public static final String SMALL_ARROWS_RIGHT = "»";
    public static final String SMALL_ARROWS_LEFT = "«";
    public static final String ALERT = "⚠";
    public static final String RADIOACTIVE = "☢";
    public static final String BIOHAZARD = "☣";
    public static final String PLUS = "✚";
    public static final String BIG_HORIZONTAL_LINE = "▍";
    public static final String SMALL_HORIZONTAL_LINE = "▏";
    public static final String PLAY = "▶";
    public static final String GOLD_ICON = "❂";
    public static final String CROSS = "✖";
    public static final String SLIM_CROSS = "✘";
    public static final String BOXED_CROSS = "☒";
    public static final String CHECKMARK = "✔";
    public static final String BOXED_CHECKMARK = "☑";
    public static final String LETTER = "✉";
    public static final String BLACK_CHESS_KING = "♚";
    public static final String BLACK_CHESS_QUEEN = "♛";
    public static final String SKULL_AND_CROSSBONES = "☠";
    public static final String WHITE_FROWNING_FACE = "☹";
    public static final String WHITE_SMILING_FACE = "☺";
    public static final String BLACK_SMILING_FACE = "☻";
    public static final String PICK = "⛏";
    public static final int CHAT_WIDTH = 320; // px
    public static final String SEPARATOR = STRIKE + repeat("-", CHAT_WIDTH);

    public static String format(String prefix, String message) {
        return DARK_GRAY + SMALL_HORIZONTAL_LINE + ' ' + GOLD + prefix + ' ' + DARK_GRAY + SMALL_HORIZONTAL_LINE + ' ' + GRAY + message;
    }

    public static String repeat(char c, int length) {
        int count = length / DefaultFontInfo.getDefaultFontInfo(c).getLength();
        return StringUtils.repeat(String.valueOf(c), count);
    }

    public static String repeat(String text, int length) {
        int width = getStringWidth(text);
        return StringUtils.repeat(text, length / width);
    }

    public static String center(String text) {
        return center(text, ' ');
    }

    public static String center(String text, char pad) {
        int length = getStringWidth(text);
        int padding = ((CHAT_WIDTH - length) / (DefaultFontInfo.getDefaultFontInfo(pad).getLength() + 1)) / 2;

        if (padding <= 0) {
            return text;
        } else {
            text += RESET; // Make sure to reset bold after the String
            return StringUtils.leftPad(text, text.length() + padding, ' ');
        }
    }

    private static int getStringWidth(String text) {
        int length = 0;
        boolean nextIsColour = false;
        boolean bold = false;
        int chars = 0;
        for (char c : text.toCharArray()) {
            if (c == ChatColor.COLOR_CHAR) {
                nextIsColour = true;
            } else if (nextIsColour) {
                ChatColor cc = ChatColor.getByChar(c);
                if (cc == ChatColor.BOLD) {
                    bold = true;
                } else if (cc == ChatColor.RESET) {
                    bold = false;
                }
                nextIsColour = false;
            } else {
                DefaultFontInfo dfi = DefaultFontInfo.getDefaultFontInfo(c);
                length += bold ? dfi.getBoldLength() : dfi.getLength();
                chars++;
            }
        }
        return length + chars;
    }

    public static String percentageBar(String colorA, String colorB, String symbol, int barAmount, double percentage) {
        StringBuilder bar = new StringBuilder(colorA);
        boolean colorChanged = false;
        for (int i = 0; i < barAmount; i++) {
            if (!colorChanged && ((double) i / (double) barAmount) * 100 >= percentage) {
                bar.append(colorB);
                colorChanged = true;
            }

            bar.append(symbol);
        }
        return bar.toString().trim();
    }

}
