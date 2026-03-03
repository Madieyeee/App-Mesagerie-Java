package com.messagerie.client;

import com.messagerie.protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Client TCP de l'application de messagerie.
 * Se connecte au serveur, envoie des commandes (lignes de texte) et reçoit les réponses.
 * Les messages reçus sont transmis à un "handler" (callback) ; un thread en arrière-plan lit les lignes.
 */
public class ChatClient {

    private Socket socket;
    private BufferedReader in;   // Lecture des réponses du serveur
    private PrintWriter out;     // Envoi des commandes au serveur
    private Thread listenerThread;  // Thread qui lit les réponses en continu
    private Consumer<String> messageHandler;  // Callback appelé à chaque ligne reçue
    private Runnable onDisconnect;  // Callback appelé quand la connexion est perdue
    private volatile boolean connected = false;  // volatile pour visibilité entre threads

    /** Établit la connexion TCP et démarre le thread qui écoute les réponses du serveur. */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        connected = true;

        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setDaemon(true);  // Ne bloque pas l'arrêt de l'application
        listenerThread.start();
    }

    /** Boucle de lecture : chaque ligne reçue est passée au messageHandler (peut être appelé depuis un autre thread). */
    private void listenForMessages() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                if (messageHandler != null) {
                    messageHandler.accept(line);
                }
            }
        } catch (IOException e) {
            if (connected) {
                connected = false;
                if (onDisconnect != null) {
                    onDisconnect.run();
                }
            }
        }
    }

    /** Envoie une ligne au serveur (une commande au format du protocole). */
    public void send(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }

    /** Envoie la commande de connexion : LOGIN|username|password */
    public void login(String username, String password) {
        send(Protocol.buildCommand(Protocol.LOGIN, username, password));
    }

    /** Envoie la commande d'inscription : REGISTER|username|password */
    public void register(String username, String password) {
        send(Protocol.buildCommand(Protocol.REGISTER, username, password));
    }

    /** Envoie un message à un utilisateur : MSG|destinataire|contenu */
    public void sendMessage(String receiverUsername, String content) {
        send(Protocol.buildCommand(Protocol.SEND_MSG, receiverUsername, content));
    }

    /** Demande la liste des utilisateurs (réponse : USER_LIST|...) */
    public void requestUserList() {
        send(Protocol.buildCommand(Protocol.GET_USERS));
    }

    /** Demande l'historique avec un utilisateur : HISTORY|username */
    public void requestHistory(String otherUsername) {
        send(Protocol.buildCommand(Protocol.GET_HISTORY, otherUsername));
    }

    /** Déconnecte proprement : envoie LOGOUT puis ferme la socket. */
    public void logout() {
        send(Protocol.buildCommand(Protocol.LOGOUT));
        disconnect();
    }

    /** Ferme la socket sans envoyer LOGOUT (connexion perdue ou changement d'écran). */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    /** Définit le callback appelé à chaque ligne reçue du serveur (peut être appelé depuis le thread du réseau). */
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    /** Définit le callback appelé quand la connexion est perdue. */
    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    public boolean isConnected() {
        return connected;
    }
}
