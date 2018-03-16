package net.minespree.dominion.punishments;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.event.LoginEvent;
import net.minespree.dominion.DominionPlugin;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * @since 12/09/2017
 */
public class PunishmentListener {

    private static final BaseComponent[] ERROR = new ComponentBuilder("Unable to verify your information. Try again later.")
            .color(ChatColor.RED)
            .create();

    private DominionPlugin plugin;

    public PunishmentListener(DominionPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> on(LoginEvent event) {
        UUID uuid = event.getConnection().getUniqueId();
        return plugin.getPunishmentManager().fetchPunishments(uuid).whenCompleteAsync((punishments, throwable) -> {
            if (throwable != null) {
                event.setCancelled(true);
                event.setCancelReason(ERROR);
            } else {
                if (!punishments.isEmpty()) {
                    punishments.stream().filter(punishment -> punishment.getType() == PunishmentType.BAN
                            || punishment.getType() == PunishmentType.TEMP_BAN
                            || punishment.getType() == PunishmentType.H_BAN
                            || punishment.getType() == PunishmentType.E_TEMP_BAN
                            || punishment.getType() == PunishmentType.H_TEMP_BAN).findFirst().ifPresent(punishment -> {
                        ComponentBuilder builder = new ComponentBuilder("You are " + ((punishment.getType() == PunishmentType.H_BAN || punishment.getType() == PunishmentType.BAN) ? "permanently" : "temporarily") + " banned from the server")
                                .color(ChatColor.RED)
                                .append("\n")
                                .append("For: ")
                                .color(ChatColor.GRAY)
                                .append(punishment.getReason())
                                .color(ChatColor.WHITE)
                                .append("\n");
                        if (punishment.getType() == PunishmentType.TEMP_BAN || punishment.getType() == PunishmentType.H_TEMP_BAN || punishment.getType() == PunishmentType.E_TEMP_BAN) {
                            builder.append("Until: ").color(ChatColor.GRAY).append(new Date(punishment.getUntil()).toGMTString()).color(ChatColor.WHITE).append("\n");
                        }
                        builder.append("Appeal At: ")
                                .color(ChatColor.GRAY)
                                .append("https://minespree.net/appeal/")
                                .color(ChatColor.YELLOW)
                                .append("\n")
                                .append("Appeal Code: ")
                                .color(ChatColor.GRAY)
                                .append(punishment.getAppealCode())
                                .color(ChatColor.WHITE);

                        event.setCancelled(true);
                        event.setCancelReason(builder.create());
                    });
                }
            }
        }).thenApply(s -> null);
    }

}
