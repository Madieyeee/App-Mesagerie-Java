package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApp extends Application {

    private static final Logger LOG = Logger.getLogger(MainApp.class.getName());

    private static MainApp instance;
    private Stage primaryStage;
    private ChatClient currentChatClient;

    public static MainApp getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;
        primaryStage.setTitle("Messagerie");
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(550);
        showLogin();
        primaryStage.show();
    }

    public void showLogin() {
        currentChatClient = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 550);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Messagerie — Connexion");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Erreur chargement écran connexion", e);
        }
    }

    public void showRegister() {
        currentChatClient = null;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 550);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Messagerie — Inscription");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Erreur chargement écran inscription", e);
        }
    }

    public void showChat(ChatClient client, Long userId, String username) {
        currentChatClient = client;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
            Parent root = loader.load();
            ChatController controller = loader.getController();
            controller.init(client, userId, username);

            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Messagerie — " + username);
            primaryStage.setMinWidth(700);
            primaryStage.setMinHeight(500);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Erreur chargement écran chat", e);
        }
    }

    @Override
    public void stop() {
        if (currentChatClient != null) {
            currentChatClient.disconnect();
            currentChatClient = null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
