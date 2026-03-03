package com.messagerie.server;

import com.messagerie.config.AppConfig;
import com.messagerie.dao.HibernateUtil;
import com.messagerie.dao.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serveur TCP de l'application de messagerie.
 * Écoute sur un port, accepte les connexions clients et délègue chaque client à un ClientHandler
 * dans un thread dédié. Utilise la base de données pour utilisateurs et messages.
 */
public class ChatServer {

    // Port lu depuis la configuration (fichier ou variable d'environnement)
    private final int port = AppConfig.getServerPort();
    // Map thread-safe : username -> handler du client connecté (ConcurrentHashMap pour accès multi-threads)
    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;

    /** Démarre le serveur : initialise la BDD, remet les utilisateurs hors ligne, puis boucle d'acceptation. */
    public void start() {
        HibernateUtil.getEntityManagerFactory();
        ServerLogger.logInfo("Base de données initialisée.");

        // RG4: au démarrage du serveur, tous les utilisateurs sont considérés déconnectés
        UserDAO userDAO = new UserDAO();
        userDAO.setAllOffline();

        try {
            serverSocket = new ServerSocket(port);  // Crée la socket serveur sur le port
            ServerLogger.logInfo("Serveur démarré sur le port " + port);
            ServerLogger.logInfo("En attente de connexions...");

            while (true) {
                // Bloque jusqu'à ce qu'un client se connecte
                Socket clientSocket = serverSocket.accept();
                ServerLogger.logInfo("Nouvelle connexion depuis " + clientSocket.getInetAddress().getHostAddress());

                // RG11: chaque client est géré dans son propre thread (plusieurs clients en parallèle)
                ClientHandler handler = new ClientHandler(clientSocket, connectedClients);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);  // Le thread ne bloque pas l'arrêt de la JVM
                thread.start();
            }
        } catch (IOException e) {
            ServerLogger.logError("Erreur serveur: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /** Ferme la socket serveur et libère Hibernate. */
    private void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        HibernateUtil.shutdown();
        ServerLogger.logInfo("Serveur arrêté.");
    }

    /** Point d'entrée pour lancer le serveur (exécuter cette classe). */
    public static void main(String[] args) {
        new ChatServer().start();
    }
}
