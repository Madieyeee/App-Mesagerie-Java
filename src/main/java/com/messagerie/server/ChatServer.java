package com.messagerie.server;

import com.messagerie.config.AppConfig;
import com.messagerie.dao.HibernateUtil;
import com.messagerie.dao.UserDAO;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {

    private final int port = AppConfig.getServerPort();
    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;

    public void start() {
        // Initialiser Hibernate
        HibernateUtil.getEntityManagerFactory();
        ServerLogger.logInfo("Base de données initialisée.");

        // RG4: remettre tous les utilisateurs hors ligne au démarrage
        UserDAO userDAO = new UserDAO();
        userDAO.setAllOffline();

        try {
            serverSocket = new ServerSocket(port);
            ServerLogger.logInfo("Serveur démarré sur le port " + port);
            ServerLogger.logInfo("En attente de connexions...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerLogger.logInfo("Nouvelle connexion depuis " + clientSocket.getInetAddress().getHostAddress());

                // RG11: chaque client dans un thread séparé
                ClientHandler handler = new ClientHandler(clientSocket, connectedClients);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            ServerLogger.logError("Erreur serveur: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

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

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
