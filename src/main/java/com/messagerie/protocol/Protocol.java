package com.messagerie.protocol;

public class Protocol {

    public static final String SEPARATOR = "|";
    public static final String SEPARATOR_REGEX = "\\|";

    // Client -> Server commands
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGOUT = "LOGOUT";
    public static final String SEND_MSG = "MSG";
    public static final String GET_USERS = "USERLIST";
    public static final String GET_HISTORY = "HISTORY";

    // Server -> Client responses
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String REGISTER_OK = "REGISTER_OK";
    public static final String REGISTER_FAIL = "REGISTER_FAIL";
    public static final String MSG_OK = "MSG_OK";
    public static final String MSG_FAIL = "MSG_FAIL";
    public static final String INCOMING_MSG = "INCOMING_MSG";
    public static final String USER_LIST = "USER_LIST";
    public static final String HISTORY_DATA = "HISTORY_DATA";
    public static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String ERROR = "ERROR";
    public static final String ALREADY_CONNECTED = "ALREADY_CONNECTED";

    public static String buildCommand(String... parts) {
        return String.join(SEPARATOR, parts);
    }

    public static String[] parseCommand(String raw) {
        return raw.split(SEPARATOR_REGEX, -1);
    }
}
