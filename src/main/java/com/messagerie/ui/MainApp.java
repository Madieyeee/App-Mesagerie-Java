package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static MainApp instance;
    private Stage primaryStage;

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 550);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Messagerie — Connexion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 500, 550);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Messagerie — Inscription");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showChat(ChatClient client, Long userId, String username) {
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
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Cleanup on app close
    }

    public static void main(String[] args) {
        launch(args);
    }
}
