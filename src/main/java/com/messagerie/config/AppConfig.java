package com.messagerie.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised configuration from config.properties with optional env var overrides.
 * Env: JDBC_URL, JDBC_USER, JDBC_PASSWORD, MESSAGERIE_HOST, MESSAGERIE_PORT.
 */
public final class AppConfig {

    private static final Properties props = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // use defaults from code
        }
    }

    private static String get(String key, String defaultValue) {
        String envKey = key.replace(".", "_").toUpperCase();
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env.trim();
        String value = props.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }

    public static String getJdbcUrl() {
        return get("jdbc.url", "jdbc:mysql://localhost:3306/messagerie?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    }

    public static String getJdbcUser() {
        return get("jdbc.user", "root");
    }

    public static String getJdbcPassword() {
        return get("jdbc.password", "");
    }

    public static String getServerHost() {
        return get("server.host", "localhost");
    }

    public static int getServerPort() {
        String p = get("server.port", "12345");
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return 12345;
        }
    }

    private AppConfig() {}
}
