package gecko10000.TicketBot.utils;

import discord4j.common.util.Snowflake;
import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.utils.SupplierIO;

import java.io.*;
import java.util.List;

public class Config {

    private static final String CONFIG_NAME = "config.yml";

    private static YamlFile config;

    public static YamlFile getConfig() {
        return config;
    }

    public static <T> T get(String key) {
        return (T) config.get(key);
    }

    public static Snowflake getSF(String key) {
        return Snowflake.of(config.getString(key));
    }

    public static String getAndFormat(String key, Object... args) {
        return String.format(config.getString(key), args);
    }

    public static int incrementTicketCount() {
        int prev = config.getInt("ticketCount");
        config.set("ticketCount", prev + 1);
        saveConfig();
        return prev;
    }

    public static List<String> ticketTypes() {
        return config.getConfigurationSection("ticketTypes").getKeys(false).stream().toList();
    }

    public static void loadConfig() {
        config = new YamlFile(CONFIG_NAME);
        try {
            if (!config.exists()) {
                SupplierIO.InputStream resource = () -> Config.class.getResourceAsStream("/" + CONFIG_NAME);
                config.load(resource);
                saveConfig();
            }
            config.load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveConfig() {
        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
