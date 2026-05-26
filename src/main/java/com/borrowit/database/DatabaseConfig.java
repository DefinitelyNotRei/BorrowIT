package com.borrowit.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class DatabaseConfig {
    private static final String CONFIG_FILE = "/borrowit-db.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                PROPERTIES.load(input);
            }
        } catch (IOException exception) {
            throw new ExceptionInInitializerError("Unable to read database configuration: " + exception.getMessage());
        }
    }

    private DatabaseConfig() {
    }

    public static String getUrl() {
        return getProperty("db.url", "jdbc:mysql://localhost:3306/borrowit?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    }

    public static String getUsername() {
        return getProperty("db.username", "root");
    }

    public static String getPassword() {
        return getProperty("db.password", "");
    }

    private static String getProperty(String key, String defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
