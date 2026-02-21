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

public class RegisterController {

    private static final Logger LOG = Logger.getLogger(RegisterController.class.getName());

    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private ChatClient client;

    @FXML
    public void initialize() {
        serverField.setPromptText(AppConfig.getServerHost());
        portField.setPromptText(String.valueOf(AppConfig.getServerPort()));
    }

    @FXML
    public void handleRegister() {
        String server = serverField.getText().isBlank() ? AppConfig.getServerHost() : serverField.getText().trim();
        String portText = portField.getText().isBlank() ? String.valueOf(AppConfig.getServerPort()) : portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isBlank() || password.isBlank() || confirm.isBlank()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (password.length() < 4) {
            showError("Le mot de passe doit contenir au moins 4 caractères.");
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
            client.register(username, password);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Connexion serveur échouée", e);
            showError("Impossible de se connecter au serveur: " + e.getMessage());
        }
    }

    private void handleServerResponse(String raw) {
        String[] parts = Protocol.parseCommand(raw);
        if (parts.length == 0) return;

        Platform.runLater(() -> {
            switch (parts[0]) {
                case Protocol.REGISTER_OK -> {
                    if (client != null) client.disconnect();
                    MainApp.getInstance().showLogin();
                }
                case Protocol.REGISTER_FAIL -> {
                    String msg = parts.length > 1 ? parts[1] : "Échec de l'inscription.";
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

    @FXML
    public void goToLogin() {
        if (client != null) client.disconnect();
        MainApp.getInstance().showLogin();
    }
}
