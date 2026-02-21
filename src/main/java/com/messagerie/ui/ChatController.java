package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import com.messagerie.protocol.Protocol;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatController {

    @FXML private Label currentUserLabel;
    @FXML private ListView<String> userListView;
    @FXML private HBox errorBanner;
    @FXML private Label errorBannerText;
    @FXML private HBox chatHeader;
    @FXML private Label chatHeaderName;
    @FXML private Label chatHeaderStatus;
    @FXML private ScrollPane messagesScroll;
    @FXML private VBox messagesContainer;
    @FXML private StackPane noChatPlaceholder;
    @FXML private HBox inputArea;
    @FXML private TextField messageInput;

    private ChatClient client;
    private Long currentUserId;
    private String currentUsername;
    private String selectedUser;
    private final Map<String, String> userStatuses = new HashMap<>();
    private final ObservableList<String> userList = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void init(ChatClient client, Long userId, String username) {
        this.client = client;
        this.currentUserId = userId;
        this.currentUsername = username;

        currentUserLabel.setText("Connecté : " + username);

        client.setMessageHandler(this::handleServerMessage);
        client.setOnDisconnect(() -> Platform.runLater(this::handleDisconnect));

        userListView.setItems(userList);
        userListView.setCellFactory(lv -> new UserListCell());

        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectUser(newVal);
            }
        });

        client.requestUserList();
    }

    private void handleServerMessage(String raw) {
        String[] parts = Protocol.parseCommand(raw);
        if (parts.length == 0) return;

        Platform.runLater(() -> {
            switch (parts[0]) {
                case Protocol.USER_LIST -> handleUserList(parts);
                case Protocol.INCOMING_MSG -> handleIncomingMessage(raw);
                case Protocol.MSG_OK -> {}
                case Protocol.MSG_FAIL -> handleMsgFail(parts);
                case Protocol.HISTORY_DATA -> handleHistoryData(parts);
                case Protocol.USER_STATUS_CHANGE -> handleUserStatusChange(parts);
                case Protocol.ERROR -> handleError(parts);
                default -> {}
            }
        });
    }

    private void handleUserList(String[] parts) {
        if (parts.length < 2 || parts[1].isBlank()) return;

        String[] users = parts[1].split(",");
        userStatuses.clear();
        userList.clear();

        List<String> onlineUsers = new ArrayList<>();
        List<String> offlineUsers = new ArrayList<>();

        for (String u : users) {
            String[] info = u.split(":");
            if (info.length == 2) {
                String name = info[0];
                String status = info[1];
                userStatuses.put(name, status);
                if ("ONLINE".equals(status)) {
                    onlineUsers.add(name);
                } else {
                    offlineUsers.add(name);
                }
            }
        }

        Collections.sort(onlineUsers);
        Collections.sort(offlineUsers);
        userList.addAll(onlineUsers);
        userList.addAll(offlineUsers);
    }

    private void handleIncomingMessage(String raw) {
        // INCOMING_MSG|sender|date|id|content (content last, may contain |)
        String[] parts = Protocol.parseCommand(raw, 5);
        if (parts.length < 5) return;
        String senderUsername = parts[1];
        String dateStr = parts[2];
        String contenu = parts[4];

        if (senderUsername.equals(selectedUser)) {
            addMessageBubble(senderUsername, contenu, dateStr, false, "RECU");
            scrollToBottom();
        }

        if (!userList.contains(senderUsername)) {
            userStatuses.put(senderUsername, "ONLINE");
            userList.add(0, senderUsername);
        }
    }

    private void handleMsgFail(String[] parts) {
        String msg = parts.length > 1 ? parts[1] : "Erreur d'envoi.";
        showErrorBanner(msg);
    }

    private void handleHistoryData(String[] parts) {
        messagesContainer.getChildren().clear();
        if (parts.length < 2 || parts[1].isBlank()) return;

        String payload;
        try {
            payload = Protocol.decodePayload(parts[1]);
        } catch (IllegalArgumentException e) {
            return;
        }
        String[] messages = payload.split(Protocol.HISTORY_SEP);
        for (String m : messages) {
            if (m.isBlank()) continue;
            String[] fields = m.split(Protocol.HISTORY_FIELD_SEP, -1);
            // Format: sender::base64(content)::date::id::status (5 champs pour éviter que "::" dans le contenu casse le parsing)
            if (fields.length >= 5) {
                String sender = fields[0];
                String contentEncoded = fields[1];
                String dateStr = fields[2];
                String statusStr = fields[4];
                String contenu;
                try {
                    contenu = Protocol.decodePayload(contentEncoded);
                } catch (IllegalArgumentException e) {
                    contenu = contentEncoded; // fallback si pas du base64
                }
                boolean isMine = sender.equals(currentUsername);
                addMessageBubble(sender, contenu, dateStr, isMine, statusStr);
            } else if (fields.length >= 3) {
                // Ancien format sans statut / contenu non encodé (rétrocompat)
                String sender = fields[0];
                String contenu = fields[1];
                String dateStr = fields[2];
                boolean isMine = sender.equals(currentUsername);
                addMessageBubble(sender, contenu, dateStr, isMine, null);
            }
        }
        scrollToBottom();
    }

    private void handleUserStatusChange(String[] parts) {
        if (parts.length < 3) return;
        String username = parts[1];
        String status = parts[2];

        userStatuses.put(username, status);

        if (!userList.contains(username)) {
            userList.add(username);
        }

        sortUserList();

        if (username.equals(selectedUser)) {
            updateChatHeader();
        }

        userListView.refresh();
    }

    private void sortUserList() {
        List<String> online = new ArrayList<>();
        List<String> offline = new ArrayList<>();

        for (String u : userList) {
            String status = userStatuses.getOrDefault(u, "OFFLINE");
            if ("ONLINE".equals(status)) {
                online.add(u);
            } else {
                offline.add(u);
            }
        }

        Collections.sort(online);
        Collections.sort(offline);

        userList.clear();
        userList.addAll(online);
        userList.addAll(offline);
    }

    private void handleError(String[] parts) {
        String msg = parts.length > 1 ? parts[1] : "Erreur inconnue.";
        showErrorBanner(msg);
    }

    private void handleDisconnect() {
        showErrorBanner("Connexion au serveur perdue.");
    }

    private void selectUser(String username) {
        this.selectedUser = username;

        noChatPlaceholder.setVisible(false);
        noChatPlaceholder.setManaged(false);
        chatHeader.setVisible(true);
        chatHeader.setManaged(true);
        messagesScroll.setVisible(true);
        messagesScroll.setManaged(true);
        inputArea.setVisible(true);
        inputArea.setManaged(true);

        updateChatHeader();
        messagesContainer.getChildren().clear();

        client.requestHistory(username);
        messageInput.requestFocus();
    }

    private void updateChatHeader() {
        chatHeaderName.setText(selectedUser);
        String status = userStatuses.getOrDefault(selectedUser, "OFFLINE");
        chatHeaderStatus.setText("ONLINE".equals(status) ? "En ligne" : "Hors ligne");
        chatHeaderStatus.getStyleClass().removeAll("chat-header-status-online", "chat-header-status-offline");
        chatHeaderStatus.getStyleClass().add("ONLINE".equals(status) ? "chat-header-status-online" : "chat-header-status-offline");
    }

    @FXML
    public void handleSendMessage() {
        if (selectedUser == null) return;

        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;

        if (content.length() > 1000) {
            showErrorBanner("Le message ne doit pas dépasser 1000 caractères.");
            return;
        }

        client.sendMessage(selectedUser, content);
        addMessageBubble(currentUsername, content, LocalDateTime.now().toString(), true, "ENVOYE");
        scrollToBottom();
        messageInput.clear();
        messageInput.requestFocus();

        hideErrorBanner();
    }

    @FXML
    public void handleLogout() {
        if (client != null) {
            client.logout();
        }
        MainApp.getInstance().showLogin();
    }

    private void addMessageBubble(String sender, String content, String dateStr, boolean isMine) {
        addMessageBubble(sender, content, dateStr, isMine, null);
    }

    private void addMessageBubble(String sender, String content, String dateStr, boolean isMine, String statusStr) {
        VBox bubble = new VBox(4);
        bubble.setPadding(new Insets(0));
        bubble.setMaxWidth(400);

        Label textLabel = new Label(content);
        textLabel.setWrapText(true);
        textLabel.getStyleClass().add("message-text");

        String dateTimeStr;
        try {
            LocalDateTime dt = LocalDateTime.parse(dateStr);
            dateTimeStr = dt.format(DATE_TIME_FORMAT);
        } catch (Exception e) {
            dateTimeStr = dateStr;
        }

        String footerText = dateTimeStr;
        if (statusStr != null && !statusStr.isBlank()) {
            String statusLabel = switch (statusStr.toUpperCase()) {
                case "RECU" -> "Reçu";
                case "LU" -> "Lu";
                default -> "Envoyé";
            };
            footerText = dateTimeStr + " · " + statusLabel;
        }

        Label footerLabel = new Label(footerText);
        footerLabel.getStyleClass().add("message-time");

        bubble.getChildren().addAll(textLabel, footerLabel);

        if (isMine) {
            bubble.getStyleClass().add("message-bubble-sent");
            bubble.setAlignment(Pos.CENTER_RIGHT);
            footerLabel.setAlignment(Pos.CENTER_RIGHT);
        } else {
            bubble.getStyleClass().add("message-bubble-received");
            bubble.setAlignment(Pos.CENTER_LEFT);
        }

        HBox row = new HBox();
        row.setPadding(new Insets(2, 8, 2, 8));
        if (isMine) {
            row.setAlignment(Pos.CENTER_RIGHT);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
        }
        row.getChildren().add(bubble);

        messagesContainer.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private void showErrorBanner(String message) {
        errorBannerText.setText(message);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    @FXML
    public void hideErrorBanner() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
    }

    /** Custom cell for user list with status indicator; styles from style.css */
    private class UserListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox hbox = new HBox(10);
                hbox.setAlignment(Pos.CENTER_LEFT);

                Circle statusDot = new Circle(5);
                String status = userStatuses.getOrDefault(item, "OFFLINE");
                statusDot.getStyleClass().add("ONLINE".equals(status) ? "status-dot-online" : "status-dot-offline");

                Label nameLabel = new Label(item);
                nameLabel.getStyleClass().add("user-cell-name");

                Label statusLabel = new Label("ONLINE".equals(status) ? "En ligne" : "Hors ligne");
                statusLabel.getStyleClass().add("ONLINE".equals(status) ? "user-cell-status-online" : "user-cell-status-offline");

                VBox textBox = new VBox(2);
                textBox.getChildren().addAll(nameLabel, statusLabel);

                hbox.getChildren().addAll(statusDot, textBox);
                setGraphic(hbox);
                setText(null);
            }
        }
    }
}
