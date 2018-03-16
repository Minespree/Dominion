package net.minespree.dominion.player;

import net.md_5.bungee.api.ChatColor;

/**
 * @since 10/9/16
 */
public enum Rank {
    MEMBER("", ChatColor.GRAY),
    IRON("Iron", ChatColor.WHITE),
    GOLD("Gold", ChatColor.GOLD),
    DIAMOND("Diamond", ChatColor.AQUA),
    VIP("VIP", ChatColor.GOLD),
    YOUTUBE("YouTube", ChatColor.RED),
    HELPER("Helper", ChatColor.LIGHT_PURPLE),
    MODERATOR("Mod", ChatColor.RED),
    ADMIN("Admin", ChatColor.DARK_RED),
    MAC_IS_A_RETARD("Admin", ChatColor.LIGHT_PURPLE);

    private String tag;
    private ChatColor color;

    Rank(String tag, ChatColor color) {
        this.tag = tag;
        this.color = color;
    }

    public String getTag() {
        return tag;
    }

    public String getColoredTag() {
        return getColor().toString() + ChatColor.BOLD + getTag() + getColor() + " ";
    }

    public ChatColor getColor() {
        return color;
    }

    public boolean hasTag() {
        return this != MEMBER;
    }

    public boolean has(Rank rank) {
        return compareTo(rank) >= 0;
    }
}
