package net.coalcube.bansystem.spigot;

import net.coalcube.bansystem.core.BanSystem;
import net.coalcube.bansystem.core.command.*;
import net.coalcube.bansystem.core.util.*;
import net.coalcube.bansystem.spigot.listener.AsyncPlayerChatListener;
import net.coalcube.bansystem.spigot.listener.PlayerCommandPreprocessListener;
import net.coalcube.bansystem.spigot.listener.PlayerConnectionListener;
import net.coalcube.bansystem.spigot.util.SpigotConfig;
import net.coalcube.bansystem.spigot.util.SpigotUser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BanSystemSpigot extends JavaPlugin implements BanSystem {

    private static Plugin instance;
    private static BanManager banmanager;

    public static MySQL mysql;
    private ServerSocket serversocket;
    private TimeFormatUtil timeFormatUtil;
    private static Config config;
    private static Config messages;
    private static Config blacklist;
    private static Config bans;
    private static Config banhistories;
    private static Config unbans;
    private static String Banscreen;
    private static List<String> blockedCommands;
    private static List<String> ads;
    private static List<String> blockedWords;
    private static Map<InetAddress, UUID> bannedIPs;
    private static File fileDatabaseFolder;
    private static String hostname, database, user, pw;
    private static int port;
    private static CommandSender console;
    public static String PREFIX = "§8§l┃ §cBanSystem §8» §7";

    @Override
    public void onEnable() {
        super.onEnable();

        BanSystem.setInstance(this);

        instance = this;
        PluginManager pluginmanager = Bukkit.getPluginManager();
        console = Bukkit.getConsoleSender();
        UpdateChecker updatechecker = new UpdateChecker(65863);
        timeFormatUtil = new TimeFormatUtil();

        console.sendMessage("§c  ____                    ____                  _                      ");
        console.sendMessage("§c | __ )    __ _   _ __   / ___|   _   _   ___  | |_    ___   _ __ ___  ");
        console.sendMessage("§c |  _ \\   / _` | | '_ \\  \\___ \\  | | | | / __| | __|  / _ \\ | '_ ` _ \\ ");
        console.sendMessage("§c | |_) | | (_| | | | | |  ___) | | |_| | \\__ \\ | |_  |  __/ | | | | | |");
        console.sendMessage("§c |____/   \\__,_| |_| |_| |____/   \\__, | |___/  \\__|  \\___| |_| |_| |_|");
        console.sendMessage("§c                                  |___/                           §7v2.0");

        createConfig();
        loadConfig();

        // Set mysql instance
        if (config.getBoolean("mysql.enable")) {
            mysql = new MySQL(hostname, port, database, user, pw);
            banmanager = new BanManagerMySQL(config, messages, mysql);
            try {
                mysql.connect();
                console.sendMessage(PREFIX + "§7Datenbankverbindung §2erfolgreich §7hergestellt.");
            } catch (SQLException e) {
                console.sendMessage(PREFIX + "§7Datenbankverbindung konnte §4nicht §7hergestellt werden.");
                console.sendMessage(PREFIX + "§cBitte überprüfe die eingetragenen MySQL daten in der Config.yml.");
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.createTables(config);
                    console.sendMessage(PREFIX + "§7Die MySQL Tabellen wurden §2erstellt§7.");
                }
            } catch (SQLException e) {
                console.sendMessage(PREFIX + "§7Die MySQL Tabellen §ckonnten nicht §7erstellt werden.");
                e.printStackTrace();
            }
            try {
                if(mysql.isConnected()) {
                    mysql.syncIDs(config);
                    console.sendMessage(PREFIX + "§7Die Ban IDs wurden §2synchronisiert§7.");
                }

            } catch (SQLException e) {
                console.sendMessage(PREFIX + "§7Die IDs konnten nicht mit MySQL synchronisiert werden.");
                e.printStackTrace();
            }

            try {
                ResultSet resultSet = mysql.getResult("SELECT * FROM `bans`");

                while (resultSet.next()) {
                    bannedIPs.put(InetAddress.getByName(resultSet.getString("ip")),
                            UUID.fromString(resultSet.getString("player")));
                }
                console.sendMessage(PREFIX + "§7Die Gebannten Spieler wurden initialisiert§7.");
            } catch (SQLException | UnknownHostException e) {
                console.sendMessage(PREFIX + "§7Die Gebannten Spieler konnten nicht initialisiert werden.");
                e.printStackTrace();
            }

        } else {
            fileDatabaseFolder = new File(this.getDataFolder().getPath() + "/database");
            createFileDatabase();
            banmanager = new BanManagerFile(bans, banhistories, unbans, fileDatabaseFolder);
            mysql = null;
        }

        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this, () -> UUIDFetcher.clearCache(), 72000, 72000);

        if (config.getString("VPN.serverIP").equals("00.00.00.00") && config.getBoolean("VPN.autoban.enable"))
            console.sendMessage(
                    PREFIX + "§cBitte trage die IP des Servers in der config.yml ein.");


        console.sendMessage(PREFIX + "§7Das BanSystem wurde gestartet.");

        UpdateManager updatemanager = new UpdateManager(mysql);

        try {
            if (updatechecker.checkForUpdates()) {
                console.sendMessage(PREFIX + "§cEin neues Update ist verfügbar.");
                console.sendMessage(PREFIX + "§7Lade es dir unter " +
                        "§ehttps://www.spigotmc.org/resources/bansystem-mit-ids.65863/ §7runter um aktuell zu bleiben.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        new Thread(() -> {
//            try {
//                serversocket = new ServerSocket(6000);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();




        init(pluginmanager);

    }

    @Override
    public void onDisable() {
        super.onDisable();

        try {
            if (config.getBoolean("mysql.enable")) {
                if (mysql.isConnected())
                    mysql.disconnect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PluginManager pm = Bukkit.getPluginManager();


        AsyncPlayerChatEvent.getHandlerList().unregister(instance);
        PlayerCommandPreprocessEvent.getHandlerList().unregister(instance);
        PlayerQuitEvent.getHandlerList().unregister(instance);
        PlayerJoinEvent.getHandlerList().unregister(instance);
        PlayerPreLoginEvent.getHandlerList().unregister(instance);

        console.sendMessage(BanSystemSpigot.PREFIX + "§7Das BanSystem wurde gestoppt.");

    }


    // create Config files
    private void createConfig() {
        try {
            File configfile = new File(this.getDataFolder(), "config.yml");
            if(!this.getDataFolder().exists()) {
                this.getDataFolder().mkdir();
            }
            if(!configfile.exists()) {
                configfile.createNewFile();

                config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));

                ConfigurationUtil.initConfig(config);

                config.save(configfile);
            }
            File messagesfile = new File(this.getDataFolder(), "messages.yml");
            if(!messagesfile.exists()) {
                messagesfile.createNewFile();

                messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));

                ConfigurationUtil.initMessages(messages);

                messages.save(messagesfile);

            }
            messages = new SpigotConfig(YamlConfiguration.loadConfiguration(messagesfile));
            config = new SpigotConfig(YamlConfiguration.loadConfiguration(configfile));
        } catch (IOException e) {
            System.err.println("[Bansystem] Dateien konnten nicht erstellt werden.");
        }
    }

    private void createFileDatabase() {
        try {
            if (!fileDatabaseFolder.exists()) {
                fileDatabaseFolder.mkdir();
            }
            File bansfile = new File(fileDatabaseFolder.getPath(), "bans.yml");
            if (!bansfile.exists()) {
                bansfile.createNewFile();
                bans = new SpigotConfig(YamlConfiguration.loadConfiguration(bansfile));
            }
            File banhistoriesfile = new File(fileDatabaseFolder.getPath(), "banhistories.yml");
            if (!banhistoriesfile.exists()) {
                banhistoriesfile.createNewFile();
                banhistories = new SpigotConfig(YamlConfiguration.loadConfiguration(banhistoriesfile));
            }
            File unbansfile = new File(fileDatabaseFolder.getPath(), "unbans.yml");
            if (!unbansfile.exists()) {
                unbansfile.createNewFile();
                unbans = new SpigotConfig(YamlConfiguration.loadConfiguration(unbansfile));
            }

            bans = new SpigotConfig(YamlConfiguration.loadConfiguration(bansfile));
            banhistories = new SpigotConfig(YamlConfiguration.loadConfiguration(banhistoriesfile));
            unbans = new SpigotConfig(YamlConfiguration.loadConfiguration(unbansfile));
        } catch (IOException e) {
            console.sendMessage(PREFIX + "Die Filedatenbank konnten nicht erstellt werden.");
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {
        try {
            PREFIX = messages.getString("prefix").replaceAll("&", "§");

            Banscreen = "";
            for (String screen : messages.getStringList("Ban.Network.Screen")) {
                if (Banscreen == null) {
                    Banscreen = screen.replaceAll("%P%", PREFIX) + "\n";
                } else
                    Banscreen = Banscreen + screen.replaceAll("%P%", PREFIX) + "\n";
            }
            user = config.getString("mysql.user");
            hostname = config.getString("mysql.host");
            port = config.getInt("mysql.port");
            pw = config.getString("mysql.password");
            database = config.getString("mysql.database");

            ads = new ArrayList<>();
            blockedCommands = new ArrayList<>();
            blockedWords = new ArrayList<>();

            for(String ad : blacklist.getSection("Ads").getKeys()) {
                ads.add(ad);
            }

            for(String cmd : config.getSection("mute.blockedCommands").getKeys()) {
                blockedCommands.add(cmd);
            }

            for(String word : blacklist.getSection("Words").getKeys()) {
                blockedWords.add(word);
            }

        } catch (NullPointerException e) {
            System.err.println("[Bansystem] Es ist ein Fehler beim laden der Config/messages Datei aufgetreten. "
                    + e.getMessage());
        }
    }

    @Override
    public User getUser(String name) {
        SpigotUser su = new SpigotUser(Bukkit.getPlayer(name));
        return su;
    }

    @Override
    public void disconnect(User u, String msg) {
        if (u.getRawUser() instanceof Player) {
            ((Player) u.getRawUser()).kickPlayer(msg);
        }
    }

    @Override
    public Config getMessages() {
        return messages;
    }

    @Override
    public Config getConfiguration() {
        return config;
    }

    private void init(PluginManager pluginManager) {
        getCommand("ban").setExecutor(new CommandWrapper(new CMDban(banmanager, config, messages, mysql),true));
        getCommand("check").setExecutor(new CommandWrapper(new CMDcheck(banmanager, mysql, messages), true));
        getCommand("deletehistory").setExecutor(new CommandWrapper(new CMDdeletehistory(banmanager, messages, mysql), true));
        getCommand("history").setExecutor(new CommandWrapper(new CMDhistory(banmanager, messages, mysql), true));
        getCommand("kick").setExecutor(new CommandWrapper(new CMDkick(messages, mysql), true));
        getCommand("unban").setExecutor(new CommandWrapper(new CMDunban(banmanager, mysql, messages, config), true));
        getCommand("unmute").setExecutor(new CommandWrapper(new CMDunmute(banmanager, messages, config, mysql), true));
        getCommand("bansystem").setExecutor(new CommandWrapper(new CMDbansystem(messages, config, mysql), false));
        getCommand("bansys").setExecutor(new CommandWrapper(new CMDbansystem(messages, config, mysql), false));

        pluginManager.registerEvents(new AsyncPlayerChatListener(config, messages, banmanager), this);
        pluginManager.registerEvents(new PlayerCommandPreprocessListener(banmanager, config, messages, blockedCommands), this);
        pluginManager.registerEvents(new PlayerConnectionListener(banmanager, config, messages, Banscreen, instance), this);
    }

    public MySQL getMySQL() {
        return mysql;
    }

    @Override
    public TimeFormatUtil getTimeFormatUtil() {
        return timeFormatUtil;
    }

    @Override
    public List<User> getAllPlayers() {
        List<User> users = new ArrayList<>();
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            users.add(new SpigotUser(p));
        }
        return users;
    }

    @Override
    public User getConsole() {
        return new SpigotUser(Bukkit.getConsoleSender());
    }

    @Override
    public String getVersion() {
        return this.getDescription().getVersion();
    }

    public static BanManager getBanmanager() {
        return banmanager;
    }

    public static Plugin getPlugin() {
        return instance;
    }

    public static Map<InetAddress, UUID> getBannedIPs() {
        return bannedIPs;
    }
}