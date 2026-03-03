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

/**
 * Gère la communication avec un seul client connecté au serveur.
 * S'exécute dans un thread dédié : lit les commandes (ligne par ligne), les traite et envoie les réponses.
 * Implémente Runnable pour être lancé dans un Thread par ChatServer.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;                           // Socket TCP vers le client
    private final Map<String, ClientHandler> connectedClients;  // Référence partagée avec le serveur
    private BufferedReader in;                             // Lecture des lignes envoyées par le client
    private PrintWriter out;                                // Envoi des réponses au client
    private User currentUser;                               // Utilisateur connecté (null tant que non logué)
    private final UserDAO userDAO = new UserDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private volatile boolean running = true;                // volatile pour visibilité entre threads

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
            // Boucle principale : lire une ligne, traiter la commande, répéter
            while (running && (line = in.readLine()) != null) {
                handleCommand(line);
            }
        } catch (IOException e) {
            ServerLogger.logError("Connexion perdue: " + (currentUser != null ? currentUser.getUsername() : "inconnu"));
        } finally {
            disconnect();
        }
    }

    /** Découpe la ligne reçue et appelle le bon gestionnaire selon la commande (premier champ). */
    private void handleCommand(String raw) {
        String[] parts = Protocol.parseCommand(raw);
        if (parts.length == 0) return;

        String command = parts[0];

        switch (command) {
            case Protocol.LOGIN -> handleLogin(parts);
            case Protocol.REGISTER -> handleRegister(parts);
            case Protocol.SEND_MSG -> handleSendMessage(parseSendMessage(raw));
            case Protocol.GET_USERS -> handleGetUsers();
            case Protocol.GET_HISTORY -> handleGetHistory(parts);
            case Protocol.LOGOUT -> disconnect();
            default -> sendMessage(Protocol.buildCommand(Protocol.ERROR, "Commande inconnue"));
        }
    }

    /** MSG|destinataire|contenu — le contenu peut contenir des |, donc on découpe avec max 3 parties. */
    private String[] parseSendMessage(String raw) {
        return Protocol.parseCommand(raw, 3);
    }

    /** Traite LOGIN|username|password : authentification et enregistrement dans connectedClients. */
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

        // RG3: un seul client connecté par compte (éviter deux sessions avec le même user)
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

        // RG6: à la connexion, envoyer les messages reçus pendant l'absence
        deliverPendingMessages();
    }

    /** Traite REGISTER|username|password : création du compte en base (username unique). */
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

    /** Traite MSG|destinataire|contenu : enregistre en BDD et envoie au destinataire s'il est connecté. */
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

        // Recharger l'expéditeur depuis la BDD pour que Hibernate gère correctement les entités (éviter "detached")
        User sender = userDAO.findByUsername(currentUser.getUsername());

        Message message = new Message(sender, receiver, contenu);
        message = messageDAO.save(message);

        if (message == null) {
            sendMessage(Protocol.buildCommand(Protocol.MSG_FAIL, "Erreur lors de l'envoi"));
            return;
        }

        ServerLogger.logMessage(currentUser.getUsername(), receiverUsername);
        sendMessage(Protocol.buildCommand(Protocol.MSG_OK, String.valueOf(message.getId())));

        // Si le destinataire est connecté, lui envoyer le message tout de suite et passer le statut à RECU
        ClientHandler receiverHandler;
        synchronized (connectedClients) {
            receiverHandler = connectedClients.get(receiverUsername);
        }

        if (receiverHandler != null) {
            receiverHandler.sendMessage(Protocol.buildCommand(
                    Protocol.INCOMING_MSG,
                    currentUser.getUsername(),
                    message.getDateEnvoi().toString(),
                    String.valueOf(message.getId()),
                    contenu
            ));
            messageDAO.updateStatus(message.getId(), MessageStatus.RECU);
        }
        // RG6: Si le destinataire est hors ligne, le message reste en BDD avec statut ENVOYE et sera livré à sa connexion
    }

    /** Envoie la liste des utilisateurs (username:statut) au client, sauf lui-même. */
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

    /** Envoie l'historique de la conversation avec l'utilisateur donné (HISTORY|otherUsername). Données encodées en Base64. */
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
            if (sb.length() > 0) sb.append(Protocol.HISTORY_SEP);
            // Contenu en Base64 pour éviter que "::" ou ";;" dans le texte casse le parsing
            String contentEncoded = Protocol.encodePayload(m.getContenu());
            sb.append(m.getSender().getUsername())
              .append(Protocol.HISTORY_FIELD_SEP).append(contentEncoded)
              .append(Protocol.HISTORY_FIELD_SEP).append(m.getDateEnvoi().toString())
              .append(Protocol.HISTORY_FIELD_SEP).append(m.getId())
              .append(Protocol.HISTORY_FIELD_SEP).append(m.getStatut().name());
        }

        sendMessage(Protocol.buildCommand(Protocol.HISTORY_DATA, Protocol.encodePayload(sb.toString())));
    }

    /** Livre les messages en attente (statut ENVOYE) au client qui vient de se connecter. */
    private void deliverPendingMessages() {
        if (currentUser == null) return;
        List<Message> pending = messageDAO.getPendingMessages(currentUser.getId());
        for (Message m : pending) {
            sendMessage(Protocol.buildCommand(
                    Protocol.INCOMING_MSG,
                    m.getSender().getUsername(),
                    m.getDateEnvoi().toString(),
                    String.valueOf(m.getId()),
                    m.getContenu()
            ));
            messageDAO.updateStatus(m.getId(), MessageStatus.RECU);
        }
    }

    /** Notifie tous les autres clients connectés du changement de statut d'un utilisateur. */
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

    /** Envoie une ligne de texte au client (thread-safe : appelé depuis ce thread ou depuis un autre handler). */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    /** Déconnecte le client : statut OFFLINE, retrait de la map, fermeture de la socket. */
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
