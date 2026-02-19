package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import com.messagerie.protocol.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private ChatClient client;

    @FXML
    public void handleLogin() {
        String server = serverField.getText().isBlank() ? "localhost" : serverField.getText().trim();
        String portText = portField.getText().isBlank() ? "12345" : portField.getText().trim();
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
            client = new ChatClient();
            client.setMessageHandler(this::handleServerResponse);
            client.setOnDisconnect(() -> Platform.runLater(() -> showError("Connexion au serveur perdue.")));
            client.connect(server, port);
            client.login(username, password);
        } catch (IOException e) {
            showError("Impossible de se connecter au serveur: " + e.getMessage());
        }
    }

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

    @FXML
    public void goToRegister() {
        if (client != null) client.disconnect();
        MainApp.getInstance().showRegister();
    }
}
