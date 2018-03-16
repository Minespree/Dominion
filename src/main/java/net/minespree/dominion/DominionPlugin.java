package net.minespree.dominion;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import lombok.Getter;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.minespree.dominion.babel.Babel;
import net.minespree.dominion.lobby.HubBalancer;
import net.minespree.dominion.lobby.HubBalancerListener;
import net.minespree.dominion.lobby.HubCommand;
import net.minespree.dominion.marketing.MarketingListener;
import net.minespree.dominion.motd.MotdListener;
import net.minespree.dominion.player.FeatherPlayerProvider;
import net.minespree.dominion.player.PlayerLoadListener;
import net.minespree.dominion.player.loader.MongoFeatherPlayerLoader;
import net.minespree.dominion.player.parties.PartyCommand;
import net.minespree.dominion.player.parties.PartyListener;
import net.minespree.dominion.player.parties.PartyManager;
import net.minespree.dominion.player.repository.*;
import net.minespree.dominion.player.tracker.PlayerTrackerListener;
import net.minespree.dominion.player.tracker.PlayerTrackerStorage;
import net.minespree.dominion.player.tracker.RedisPlayerTrackerStorage;
import net.minespree.dominion.punishments.PunishmentListener;
import net.minespree.dominion.punishments.PunishmentManager;
import net.minespree.dominion.servers.ServerStatusUpdater;
import net.minespree.dominion.util.LoginEventMultiplexer;
import net.minespree.dominion.welcome.WelcomeListener;
import net.minespree.dominion.whitelist.WhitelistListener;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class DominionPlugin extends Plugin {
    @Getter
    private final HubBalancer hubBalancer = new HubBalancer();
    @Getter
    private Configuration configuration;
    @Getter
    private FeatherPlayerProvider featherPlayerProvider;
    @Getter
    private PunishmentManager punishmentManager;
    @Getter
    private PlayerTrackerStorage playerTrackerStorage;
    @Getter
    private ServerStatusUpdater serverStatusUpdater;
    @Getter
    private PartyManager partyManager;
    @Getter
    private PlayerRepository playerRepository;

    @Override
    public void onEnable() {
        try {
            configuration = loadConfig();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load configuration", e);
        }

        // Initialize Redis
        JedisPool pool = new JedisPool(configuration.getString("redis.host"), configuration.getInt("redis.port"));

        // Initialize MongoDB + Feather stuff
        MongoClient client = new MongoClient(Collections.singletonList(
                new ServerAddress(configuration.getString("feather.mongodb.host"), configuration.getInt("feather.mongodb.port"))
        ), Collections.singletonList(
                MongoCredential.createCredential(configuration.getString("feather.mongodb.user"),
                        configuration.getString("feather.mongodb.db"),
                        configuration.getString("feather.mongodb.password").toCharArray())));
        featherPlayerProvider = new FeatherPlayerProvider(new MongoFeatherPlayerLoader(this, client));
        getProxy().getPluginManager().registerListener(this, new PlayerLoadListener(this));
        punishmentManager = new PunishmentManager(this, client);

        // Initialize player repository
        playerRepository = new ChainedPlayerRepository(
                new LocalPlayerRepository(),
                new CachedPlayerRepository(new MongoPlayerRepository(client.getDatabase(configuration.getString("feather.mongodb.db"))))
        );

        // Initialize Babel
        Babel.initialize(client.getDatabase(getConfiguration().getString("feather.mongodb.db")).getCollection("babel"));

        // player tracker
        playerTrackerStorage = new RedisPlayerTrackerStorage(pool);
        getProxy().getPluginManager().registerListener(this, new PlayerTrackerListener(this));

        // LoginEvent multiplexer
        LoginEventMultiplexer lem = new LoginEventMultiplexer(this);
        lem.addListener(new PunishmentListener(this)::on);
        lem.addListener(new WhitelistListener(pool, featherPlayerProvider)::on);

        getProxy().getPluginManager().registerListener(this, new WelcomeListener());
        getProxy().getPluginManager().registerListener(this, new MarketingListener(this, client));

        getProxy().getPluginManager().registerListener(this, lem);

        // motd
        getProxy().getPluginManager().registerListener(this, new MotdListener(this, pool));

        // parties
        partyManager = new PartyManager(pool, this);
        getProxy().getPluginManager().registerListener(this, new PartyListener(this, partyManager));
        getProxy().getPluginManager().registerCommand(this, new PartyCommand(partyManager, this));

        // next game handler
        serverStatusUpdater = new ServerStatusUpdater(pool, partyManager);
        getProxy().getScheduler().schedule(this, serverStatusUpdater, 1, 5, TimeUnit.SECONDS);

        // hub balancer
        getProxy().registerChannel("Dominion");
        getProxy().getPluginManager().registerCommand(this, new HubCommand(this));
        getProxy().getPluginManager().registerListener(this, new HubBalancerListener(this));
        hubBalancer.refresh();
        getProxy().getScheduler().schedule(this, hubBalancer::refresh, 1, 5, TimeUnit.SECONDS);
    }

    private Configuration loadConfig() throws IOException {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        Path configYml = getDataFolder().toPath().resolve("config.yml");
        if (!Files.exists(configYml)) {
            try (InputStream r = getResourceAsStream("config.yml")) {
                Files.copy(r, configYml);
            }
        }

        try (Reader reader = Files.newBufferedReader(configYml)) {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(reader);
        }
    }
}
