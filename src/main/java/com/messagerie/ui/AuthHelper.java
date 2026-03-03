package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import javafx.application.Platform;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Utilitaire pour la connexion au serveur depuis les écrans de connexion et d'inscription.
 * Crée un ChatClient, configure le handler des messages et le callback de déconnexion.
 * Important : le callback onDisconnect est exécuté sur le thread JavaFX (Platform.runLater)
 * car les réponses du serveur arrivent dans un autre thread.
 */
public final class AuthHelper {

    private AuthHelper() {}  // Classe utilitaire : pas d'instanciation

    /**
     * Crée un client, se connecte au serveur et enregistre les callbacks.
     * @param server adresse du serveur
     * @param port port du serveur
     * @param onMessage appelé à chaque ligne reçue (peut être appelé depuis le thread réseau)
     * @param onDisconnect appelé quand la connexion est perdue (sera exécuté sur le thread JavaFX si besoin)
     * @return le client connecté
     */
    public static ChatClient connect(String server, int port,
                                    Consumer<String> onMessage,
                                    Runnable onDisconnect) throws IOException {
        ChatClient client = new ChatClient();
        client.setMessageHandler(onMessage);
        client.setOnDisconnect(() -> {
            // Les mises à jour de l'interface JavaFX doivent être faites sur le thread JavaFX
            if (Platform.isFxApplicationThread()) {
                onDisconnect.run();
            } else {
                Platform.runLater(onDisconnect);
            }
        });
        client.connect(server, port);
        return client;
    }
}
