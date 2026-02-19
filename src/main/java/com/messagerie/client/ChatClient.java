package com.messagerie.client;

import com.messagerie.protocol.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class ChatClient {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private Consumer<String> messageHandler;
    private Runnable onDisconnect;
    private volatile boolean connected = false;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        connected = true;

        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

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

    public void send(String message) {
        if (out != null && connected) {
            out.println(message);
        }
    }

    public void login(String username, String password) {
        send(Protocol.buildCommand(Protocol.LOGIN, username, password));
    }

    public void register(String username, String password) {
        send(Protocol.buildCommand(Protocol.REGISTER, username, password));
    }

    public void sendMessage(String receiverUsername, String content) {
        send(Protocol.buildCommand(Protocol.SEND_MSG, receiverUsername, content));
    }

    public void requestUserList() {
        send(Protocol.buildCommand(Protocol.GET_USERS));
    }

    public void requestHistory(String otherUsername) {
        send(Protocol.buildCommand(Protocol.GET_HISTORY, otherUsername));
    }

    public void logout() {
        send(Protocol.buildCommand(Protocol.LOGOUT));
        disconnect();
    }

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

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    public boolean isConnected() {
        return connected;
    }
}
