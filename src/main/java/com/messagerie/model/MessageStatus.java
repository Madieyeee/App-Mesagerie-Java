package com.messagerie.model;

/**
 * Statut d'un message dans la conversation.
 * - ENVOYE : envoyé par l'expéditeur, pas encore reçu par le destinataire
 * - RECU : reçu par l'application du destinataire
 * - LU : lu par le destinataire (accusé de lecture)
 */
public enum MessageStatus {
    ENVOYE,
    RECU,
    LU
}
