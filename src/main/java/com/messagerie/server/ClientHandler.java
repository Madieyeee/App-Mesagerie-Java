package com.messagerie.server;

import com.messagerie.dao.MessageDAO;
import com.messagerie.dao.UserDAO;
import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.User;
import com.messagerie.model.UserStatus;
import com.messagerie.protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Map<String, ClientHandler> connectedClients;
    private BufferedReader in;
    private PrintWriter out;
    private User currentUser;
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private volatile boolean running = true;

    public ClientHandler(Socket socket, Map<String, ClientHandler> connectedClients) {
        this.socket = socket;
        this.connectedClients = connectedClients;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while (running && (line = in.readLine()) != null) {
                handleCommand(line);
            }
        } catch (IOException e) {
            ServerLogger.logError("Connexion perdue: " + (currentUser != null ? currentUser.getUsername() : "inconnu"));
        } finally {
            disconnect();
        }
    }

    private void handleCommand(String raw) {
        String[] parts = Protocol.parseCommand(raw);
        if (parts.length == 0) return;

        String command = parts[0];

        switch (command) {
            case Protocol.LOGIN -> handleLogin(parts);
            case Protocol.REGISTER -> handleRegister(parts);
            case Protocol.SEND_MSG -> handleSendMessage(parts);
            case Protocol.GET_USERS -> handleGetUsers();
            case Protocol.GET_HISTORY -> handleGetHistory(parts);
            case Protocol.LOGOUT -> disconnect();
            default -> sendMessage(Protocol.buildCommand(Protocol.ERROR, "Commande inconnue"));
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendMessage(Protocol.buildCommand(Protocol.LOGIN_FAIL, "Paramètres manquants"));
            return;
        }
        String username = parts[1];
        String password = parts[2];

        User user = userDAO.authenticate(username, password);
        if (user == null) {
            sendMessage(Protocol.buildCommand(Protocol.LOGIN_FAIL, "Identifiants incorrects"));
            return;
        }

        // RG3: connexion unique
        synchronized (connectedClients) {
            if (connectedClients.containsKey(username)) {
                sendMessage(Protocol.buildCommand(Protocol.ALREADY_CONNECTED, "Cet utilisateur est déjà connecté"));
                return;
            }
            connectedClients.put(username, this);
        }

        this.currentUser = user;
        // RG4: statut ONLINE
        userDAO.updateStatus(user.getId(), UserStatus.ONLINE);
        ServerLogger.logConnection(username);

        sendMessage(Protocol.buildCommand(Protocol.LOGIN_OK, String.valueOf(user.getId()), username));

        // Notifier les autres utilisateurs du changement de statut
        broadcastStatusChange(username, "ONLINE");

        // RG6: livrer les messages en attente
        deliverPendingMessages();
    }

    private void handleRegister(String[] parts) {
        if (parts.length < 3) {
            sendMessage(Protocol.buildCommand(Protocol.REGISTER_FAIL, "Paramètres manquants"));
            return;
        }
        String username = parts[1];
        String password = parts[2];

        if (username.isBlank() || password.isBlank()) {
            sendMessage(Protocol.buildCommand(Protocol.REGISTER_FAIL, "Le nom d'utilisateur et le mot de passe ne peuvent pas être vides"));
            return;
        }

        // RG1: username unique (vérifié dans le DAO)
        User user = userDAO.register(username, password);
        if (user == null) {
            sendMessage(Protocol.buildCommand(Protocol.REGISTER_FAIL, "Ce nom d'utilisateur est déjà pris"));
            return;
        }

        ServerLogger.logInfo("Nouvel utilisateur inscrit: " + username);
        sendMessage(Protocol.buildCommand(Protocol.REGISTER_OK, "Inscription réussie"));
    }

    private void handleSendMessage(String[] parts) {
        // RG2: doit être authentifié
        if (currentUser == null) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Vous devez être connecté"));
            return;
        }

        if (parts.length < 3) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Paramètres manquants"));
            return;
        }

        String receiverUsername = parts[1];
        String contenu = parts[2];

        // RG7: contenu non vide et max 1000 caractères
        if (contenu.isBlank()) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Le message ne peut pas être vide"));
            return;
        }
        if (contenu.length() > 1000) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Le message ne doit pas dépasser 1000 caractères"));
            return;
        }

        // RG5: le destinataire doit exister
        User receiver = userDAO.findByUsername(receiverUsername);
        if (receiver == null) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Destinataire introuvable"));
            return;
        }

        // Recharger le sender depuis la BDD pour éviter les entités détachées
        User sender = userDAO.findByUsername(currentUser.getUsername());

        Message message = new Message(sender, receiver, contenu);
        message = messageDAO.save(message);

        if (message == null) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Erreur lors de l'envoi"));
            return;
        }

        ServerLogger.logMessage(currentUser.getUsername(), receiverUsername);
        sendMessage(Protocol.buildCommand(Protocol.MSG_OK, String.valueOf(message.getId())));

        // Envoyer au destinataire s'il est connecté
        ClientHandler receiverHandler;
        synchronized (connectedClients) {
            receiverHandler = connectedClients.get(receiverUsername);
        }

        if (receiverHandler != null) {
            receiverHandler.sendMessage(Protocol.buildCommand(
                    Protocol.INCOMING_MSG,
                    currentUser.getUsername(),
                    contenu,
                    message.getDateEnvoi().toString(),
                    String.valueOf(message.getId())
            ));
            messageDAO.updateStatus(message.getId(), MessageStatus.RECU);
        }
        // RG6: si hors ligne, le message reste en BDD avec statut ENVOYE
    }

    private void handleGetUsers() {
        if (currentUser == null) {
            sendMessage(Protocol.buildCommand(Protocol.ERROR, "Non authentifié"));
            return;
        }

        List<User> users = userDAO.findAll();
        String userListStr = users.stream()
                .filter(u -> !u.getUsername().equals(currentUser.getUsername()))
                .map(u -> u.getUsername() + ":" + u.getStatus().name())
                .collect(Collectors.joining(","));

        sendMessage(Protocol.buildCommand(Protocol.USER_LIST, userListStr));
    }

    private void handleGetHistory(String[] parts) {
        if (currentUser == null) {
            sendMessage(Protocol.buildCommand(Protocol.ERROR, "Non authentifié"));
            return;
        }
        if (parts.length < 2) {
            sendMessage(Protocol.buildCommand(Protocol.ERROR, "Paramètres manquants"));
            return;
        }

        String otherUsername = parts[1];
        User otherUser = userDAO.findByUsername(otherUsername);
        if (otherUser == null) {
            sendMessage(Protocol.buildCommand(Protocol.ERROR, "Utilisateur introuvable"));
            return;
        }

        List<Message> messages = messageDAO.getConversation(currentUser.getId(), otherUser.getId());
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            if (sb.length() > 0) sb.append(";;");
            sb.append(m.getSender().getUsername())
              .append("::").append(m.getContenu())
              .append("::").append(m.getDateEnvoi().toString())
              .append("::").append(m.getId());
        }

        sendMessage(Protocol.buildCommand(Protocol.HISTORY_DATA, sb.toString()));
    }

    private void deliverPendingMessages() {
        if (currentUser == null) return;
        List<Message> pending = messageDAO.getPendingMessages(currentUser.getId());
        for (Message m : pending) {
            sendMessage(Protocol.buildCommand(
                    Protocol.INCOMING_MSG,
                    m.getSender().getUsername(),
                    m.getContenu(),
                    m.getDateEnvoi().toString(),
                    String.valueOf(m.getId())
            ));
            messageDAO.updateStatus(m.getId(), MessageStatus.RECU);
        }
    }

    private void broadcastStatusChange(String username, String status) {
        synchronized (connectedClients) {
            for (Map.Entry<String, ClientHandler> entry : connectedClients.entrySet()) {
                if (!entry.getKey().equals(username)) {
                    entry.getValue().sendMessage(
                            Protocol.buildCommand(Protocol.USER_STATUS_CHANGE, username, status)
                    );
                }
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void disconnect() {
        running = false;
        if (currentUser != null) {
            String username = currentUser.getUsername();
            // RG4: statut OFFLINE
            userDAO.updateStatus(currentUser.getId(), UserStatus.OFFLINE);
            synchronized (connectedClients) {
                connectedClients.remove(username);
            }
            broadcastStatusChange(username, "OFFLINE");
            ServerLogger.logDisconnection(username);
            currentUser = null;
        }
        try {
            socket.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
