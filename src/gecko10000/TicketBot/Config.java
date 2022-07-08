package gecko10000.TicketBot;

import org.simpleyaml.configuration.file.YamlFile;
import org.simpleyaml.utils.SupplierIO;

import java.io.*;

public class Config {

    private static final String CONFIG_NAME = "config.txt";

    private static YamlFile config;

    public static YamlFile getConfig() {
        return config;
    }

    public static <T> T get(String key) {
        return (T) config.get(key);
    }

    public Config() {
        config = new YamlFile("config.yml");
        try {
            if (!config.exists()) {
                    SupplierIO.InputStream resource = () -> this.getClass().getResourceAsStream("/config.yml");
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
