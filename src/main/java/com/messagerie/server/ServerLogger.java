package com.messagerie.server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServerLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(FORMATTER) + "] " + message);
    }

    public static void logConnection(String username) {
        log("CONNEXION: " + username + " s'est connecté.");
    }

    public static void logDisconnection(String username) {
        log("DECONNEXION: " + username + " s'est déconnecté.");
    }

    public static void logMessage(String sender, String receiver) {
        log("MESSAGE: " + sender + " -> " + receiver);
    }

    public static void logError(String error) {
        log("ERREUR: " + error);
    }

    public static void logInfo(String info) {
        log("INFO: " + info);
    }
}
