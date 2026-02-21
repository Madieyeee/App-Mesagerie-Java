package com.messagerie.ui;

import com.messagerie.client.ChatClient;
import javafx.application.Platform;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Factorises connection and handler setup for login/register screens.
 */
public final class AuthHelper {

    private AuthHelper() {}

    /**
     * Creates a ChatClient, connects to the server, and sets the message handler and disconnect callback.
     * @param server host
     * @param port port
     * @param onMessage called on each line received (may be from any thread)
     * @param onDisconnect called when connection is lost (will be run on JavaFX thread if needed)
     * @return connected client
     * @throws IOException if connection fails
     */
    public static ChatClient connect(String server, int port,
                                    Consumer<String> onMessage,
                                    Runnable onDisconnect) throws IOException {
        ChatClient client = new ChatClient();
        client.setMessageHandler(onMessage);
        client.setOnDisconnect(() -> {
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
