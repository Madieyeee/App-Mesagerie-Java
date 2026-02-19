package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import com.messagerie.protocol.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField serverField;
    @FXML private TextField portField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private ChatClient client;

    @FXML
    public void handleRegister() {
        String server = serverField.getText().isBlank() ? "localhost" : serverField.getText().trim();
        String portText = portField.getText().isBlank() ? "12345" : portField.getText().trim();
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
            client = new ChatClient();
            client.setMessageHandler(this::handleServerResponse);
            client.setOnDisconnect(() -> Platform.runLater(() -> showError("Connexion au serveur perdue.")));
            client.connect(server, port);
            client.register(username, password);
        } catch (IOException e) {
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
