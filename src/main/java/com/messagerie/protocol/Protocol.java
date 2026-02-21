package com.messagerie.protocol;

import java.util.Base64;

/**
 * Text protocol over TCP: one command per line, fields separated by |.
 * Commands that carry user content use limited split or Base64 to avoid | in content breaking parsing.
 * Format: COMMAND|field1|field2|...|payload (payload may contain | when using parseCommand(raw, maxParts)).
 */
public class Protocol {

    public static final String SEPARATOR = "|";
    public static final String SEPARATOR_REGEX = "\\|";
    public static final String HISTORY_SEP = ";;";
    public static final String HISTORY_FIELD_SEP = "::";

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
    /** Format: INCOMING_MSG|sender|date|id|content (content last so it may contain |) */
    public static final String INCOMING_MSG = "INCOMING_MSG";
    public static final String USER_LIST = "USER_LIST";
    /** Format: HISTORY_DATA|base64(payload), payload = msg1;;msg2, each msg = sender::base64(content)::date::id::status (content encodé pour éviter que "::" casse le parsing) */
    public static final String HISTORY_DATA = "HISTORY_DATA";
    public static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String ERROR = "ERROR";
    public static final String ALREADY_CONNECTED = "ALREADY_CONNECTED";

    public static String buildCommand(String... parts) {
        return String.join(SEPARATOR, parts);
    }

    /** Full split on |; use parseCommand(raw, maxParts) when the last field may contain |. */
    public static String[] parseCommand(String raw) {
        return raw.split(SEPARATOR_REGEX, -1);
    }

    /** Split with at most maxParts segments; the last segment may contain |. E.g. parseCommand("MSG|a|b|c", 3) -> ["MSG","a","b|c"]. */
    public static String[] parseCommand(String raw, int maxParts) {
        if (maxParts <= 0) return parseCommand(raw);
        String[] parts = raw.split(SEPARATOR_REGEX, maxParts);
        return parts.length >= maxParts ? parts : raw.split(SEPARATOR_REGEX, -1);
    }

    public static String encodePayload(String payload) {
        return Base64.getEncoder().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String decodePayload(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
    }
}
