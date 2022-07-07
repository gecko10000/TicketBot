package gecko10000.TicketBot;

import java.io.*;
import java.util.Properties;

public class Config {

    private static final String CONFIG_NAME = "config.txt";

    private static Properties prop;

    public static String getProperty(String key) {
        return prop.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return prop.getProperty(key, defaultValue);
    }

    public static int incrementTicketCount() {
        String prev = prop.getProperty("ticketCount");
        int parsed = Integer.parseInt(prev);
        prop.setProperty("ticketCount", "" + (parsed + 1));
        saveConfig();
        return parsed;
    }

    public Config() {
        prop = new Properties();
        InputStream file = openInput();
        if (file != null) {
            try {
                prop.load(file);
                file.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        setDefaults();
    }

    private static InputStream openInput() {
        try {
            return new FileInputStream(CONFIG_NAME);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private static OutputStream openOutput() {
        try {
            return new FileOutputStream(CONFIG_NAME);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private void setDefaults() {
        prop.putIfAbsent("botToken", "123");
        prop.putIfAbsent("ticketCategory", "879559524850237481");
        prop.putIfAbsent("supportChannel", "994395993506320495");
        prop.putIfAbsent("ticketCount", "1");
        saveConfig();
    }

    public static void saveConfig() {
        OutputStream file = openOutput();
        try {
            prop.store(file, null);
            file.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
