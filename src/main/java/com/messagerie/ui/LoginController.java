package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import com.messagerie.config.AppConfig;
import com.messagerie.protocol.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur de l'écran de connexion (login.fxml).
 * Récupère serveur/port/utilisateur/mot de passe, se connecte via AuthHelper, envoie LOGIN
 * et réagit aux réponses LOGIN_OK / LOGIN_FAIL / ALREADY_CONNECTED.
 */
public class LoginController {

    private static final Logger LOG = Logger.getLogger(LoginController.class.getName());

    // Champs liés au FXML par @FXML (même nom que fx:id dans le fichier .fxml)
    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private ChatClient client;

    /** Appelé par JavaFX après le chargement du FXML. Affiche les valeurs par défaut (serveur, port). */
    @FXML
    public void initialize() {
        serverField.setPromptText(AppConfig.getServerHost());
        portField.setPromptText(String.valueOf(AppConfig.getServerPort()));
    }

    /** Clic sur "Connexion" : validation des champs, connexion au serveur, envoi de LOGIN. */
    @FXML
    public void handleLogin() {
        String server = serverField.getText().isBlank() ? AppConfig.getServerHost() : serverField.getText().trim();
        String portText = portField.getText().isBlank() ? String.valueOf(AppConfig.getServerPort()) : portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showError("Le port doit être un nombre valide.");
            return;
        }

        try {
            client = AuthHelper.connect(server, port, this::handleServerResponse,
                    () -> showError("Connexion au serveur perdue."));
            client.login(username, password);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Connexion serveur échouée", e);
            showError("Impossible de se connecter au serveur: " + e.getMessage());
        }
    }

    /** Callback appelé à chaque ligne reçue du serveur. On découpe la commande et on réagit (sur le thread JavaFX). */
    private void handleServerResponse(String raw) {
        String[] parts = Protocol.parseCommand(raw);
        if (parts.length == 0) return;

        Platform.runLater(() -> {
            switch (parts[0]) {
                case Protocol.LOGIN_OK -> {
                    if (parts.length >= 3) {
                        Long userId = Long.parseLong(parts[1]);
                        String username = parts[2];
                        MainApp.getInstance().showChat(client, userId, username);
                    }
                }
                case Protocol.LOGIN_FAIL -> {
                    String msg = parts.length > 1 ? parts[1] : "Échec de la connexion.";
                    showError(msg);
                    if (client != null) client.disconnect();
                }
                case Protocol.ALREADY_CONNECTED -> {
                    String msg = parts.length > 1 ? parts[1] : "Cet utilisateur est déjà connecté.";
                    showError(msg);
                    if (client != null) client.disconnect();
                }
                default -> {}
            }
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
    }

    /** Clic sur "S'inscrire" : aller à l'écran d'inscription. */
    @FXML
    public void goToRegister() {
        if (client != null) client.disconnect();
        MainApp.getInstance().showRegister();
    }
}
