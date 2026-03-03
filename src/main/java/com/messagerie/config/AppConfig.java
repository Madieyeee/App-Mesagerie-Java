package com.messagerie.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration centralisée de l'application.
 * Lit le fichier config.properties et permet de surcharger par variables d'environnement.
 * Variables d'environnement utiles : JDBC_URL, JDBC_USER, JDBC_PASSWORD, MESSAGERIE_HOST, MESSAGERIE_PORT.
 */
public final class AppConfig {

    // Objet Properties : stocke des paires clé=valeur (comme dans un fichier .properties)
    private static final Properties props = new Properties();
    // Nom du fichier de configuration dans les ressources du projet
    private static final String CONFIG_FILE = "config.properties";

    // Bloc static : exécuté une seule fois au chargement de la classe
    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);  // Charge les lignes key=value dans props
            }
        } catch (IOException e) {
            // En cas d'erreur, on garde les valeurs par défaut définies dans le code
        }
    }

    /**
     * Récupère une valeur : d'abord variable d'environnement (ex: jdbc.url -> JDBC_URL),
     * sinon la propriété du fichier, sinon la valeur par défaut.
     */
    private static String get(String key, String defaultValue) {
        String envKey = key.replace(".", "_").toUpperCase();  // jdbc.url -> JDBC_URL
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) return env.trim();
        String value = props.getProperty(key);
        return value != null ? value.trim() : defaultValue;
    }

    /** URL de connexion à la base MySQL (par défaut : localhost, base "messagerie"). */
    public static String getJdbcUrl() {
        return get("jdbc.url", "jdbc:mysql://localhost:3306/messagerie?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
    }

    /** Utilisateur MySQL. */
    public static String getJdbcUser() {
        return get("jdbc.user", "root");
    }

    /** Mot de passe MySQL. */
    public static String getJdbcPassword() {
        return get("jdbc.password", "");
    }

    /** Adresse du serveur de messagerie (où le client se connecte). */
    public static String getServerHost() {
        return get("server.host", "localhost");
    }

    /** Port du serveur de messagerie (ex: 12345). */
    public static int getServerPort() {
        String p = get("server.port", "12345");
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException e) {
            return 12345;  // Valeur de secours si le port configuré est invalide
        }
    }

    // Constructeur privé : empêche d'instancier cette classe (que des méthodes static)
    private AppConfig() {}
}
