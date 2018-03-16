package net.minespree.dominion.lobby;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.minespree.dominion.DominionPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class HubCommand extends Command {
    private static final BaseComponent[] NOT_A_PLAYER = new ComponentBuilder("You're not a player.").color(ChatColor.RED).create();
    private static final BaseComponent[] PLEASE_CONFIRM = new ComponentBuilder("Please confirm you'd like to return to the hub by repeating this command.")
            .color(ChatColor.GREEN).create();
    private static final BaseComponent[] CONFIRMATION_EXPIRED = new ComponentBuilder("Cancelling attempt to leave for the hub.")
            .color(ChatColor.RED).create();
    private static final BaseComponent[] NOT_ON_A_HUB = new ComponentBuilder("You are already on the hub!")
            .color(ChatColor.RED).create();

    private final DominionPlugin plugin;
    private final Map<UUID, Long> confirmTime = new HashMap<>();

    public HubCommand(DominionPlugin plugin) {
        super("hub", null, "lobby", "leave");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(NOT_A_PLAYER);
            return;
        }

        ProxiedPlayer pp = (ProxiedPlayer) sender;
        if (plugin.getHubBalancer().isHub(pp.getServer().getInfo())) {
            pp.sendMessage(NOT_ON_A_HUB);
            return;
        }

        if (confirmTime.containsKey(pp.getUniqueId()) && confirmTime.remove(pp.getUniqueId()) > System.currentTimeMillis()) {
            // TODO: Use proper hub balancer, when we've packaged the hub in playpen
            ((ProxiedPlayer) sender).connect(plugin.getHubBalancer().pick());
        } else {
            confirmTime.put(pp.getUniqueId(), System.currentTimeMillis() + 15000);
            pp.sendMessage(PLEASE_CONFIRM);
            plugin.getProxy().getScheduler().schedule(plugin, () -> {
                if (confirmTime.remove(pp.getUniqueId()) != null && pp.isConnected()) {
                    pp.sendMessage(CONFIRMATION_EXPIRED);
                }
            }, 15, TimeUnit.SECONDS);
        }
    }
}
