package com.messagerie.protocol;

import java.util.Base64;

/**
 * Protocole texte sur TCP : une commande par ligne, champs séparés par |.
 * Le contenu utilisateur peut être encodé en Base64 pour éviter que le caractère | casse le parsing.
 * Format général : COMMAND|champ1|champ2|...|contenu (utiliser parseCommand(raw, maxParts) si le dernier champ contient des |).
 */
public class Protocol {

    // Séparateur entre les champs d'une commande (| en regex s'écrit \|)
    public static final String SEPARATOR = "|";
    public static final String SEPARATOR_REGEX = "\\|";
    // Séparateurs utilisés dans l'historique des messages (pour éviter les conflits avec le contenu)
    public static final String HISTORY_SEP = ";;";
    public static final String HISTORY_FIELD_SEP = "::";

    // ——— Commandes envoyées du CLIENT vers le SERVEUR ———
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String LOGOUT = "LOGOUT";
    public static final String SEND_MSG = "MSG";
    public static final String GET_USERS = "USERLIST";
    public static final String GET_HISTORY = "HISTORY";
    public static final String TYPING_START = "TYPING_START";
    public static final String TYPING_STOP = "TYPING_STOP";
    public static final String ADD_REACTION = "ADD_REACTION";
    public static final String MSG_READ = "MSG_READ";

    // ——— Réponses envoyées du SERVEUR vers le CLIENT ———
    public static final String LOGIN_OK = "LOGIN_OK";
    public static final String LOGIN_FAIL = "LOGIN_FAIL";
    public static final String REGISTER_OK = "REGISTER_OK";
    public static final String REGISTER_FAIL = "REGISTER_FAIL";
    public static final String MSG_OK = "MSG_OK";
    public static final String MSG_FAIL = "MSG_FAIL";
    /** Format : INCOMING_MSG|expéditeur|date|id|contenu (contenu en dernier pour pouvoir contenir des |) */
    public static final String INCOMING_MSG = "INCOMING_MSG";
    public static final String USER_LIST = "USER_LIST";
    /** Format : HISTORY_DATA|base64(payload), chaque message = expéditeur::base64(contenu)::date::id::statut */
    public static final String HISTORY_DATA = "HISTORY_DATA";
    public static final String USER_STATUS_CHANGE = "USER_STATUS_CHANGE";
    public static final String ERROR = "ERROR";
    public static final String ALREADY_CONNECTED = "ALREADY_CONNECTED";
    /** Format : TYPING_INDICATOR|username|action (START/STOP) */
    public static final String TYPING_INDICATOR = "TYPING_INDICATOR";
    /** Format : REACTION_ADDED|messageId|username|emoji */
    public static final String REACTION_ADDED = "REACTION_ADDED";
    /** Format : MSG_STATUS_UPDATE|messageId|statut (RECU/LU) */
    public static final String MSG_STATUS_UPDATE = "MSG_STATUS_UPDATE";

    /** Construit une ligne de commande en joignant les parties avec le séparateur | */
    public static String buildCommand(String... parts) {
        return String.join(SEPARATOR, parts);
    }

    /** Découpe une ligne sur tous les |. À éviter si le dernier champ peut contenir des |. */
    public static String[] parseCommand(String raw) {
        return raw.split(SEPARATOR_REGEX, -1);
    }

    /** Découpe avec au plus maxParts segments ; le dernier peut contenir des |. Ex: "MSG|a|b|c" avec maxParts=3 -> ["MSG","a","b|c"] */
    public static String[] parseCommand(String raw, int maxParts) {
        if (maxParts <= 0) return parseCommand(raw);
        String[] parts = raw.split(SEPARATOR_REGEX, maxParts);
        return parts.length >= maxParts ? parts : raw.split(SEPARATOR_REGEX, -1);
    }

    /** Encode une chaîne en Base64 (UTF-8) pour l'envoyer dans le protocole sans casser le parsing. */
    public static String encodePayload(String payload) {
        return Base64.getEncoder().encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Décode une chaîne Base64 en texte UTF-8. */
    public static String decodePayload(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
    }
}
