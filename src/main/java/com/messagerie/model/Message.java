package com.messagerie.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un message échangé entre deux utilisateurs.
 * Correspond à la table "messages" en base de données.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Clé primaire auto-générée par la BDD
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)   // Plusieurs messages peuvent avoir le même expéditeur
    @JoinColumn(name = "sender_id", nullable = false)    // Colonne de jointure en base
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)   // Plusieurs messages peuvent avoir le même destinataire
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 1000)  // Contenu du message, max 1000 caractères
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();  // Date/heure d'envoi

    @Enumerated(EnumType.STRING)   // Le statut est stocké comme chaîne (ENVOYE, RECU, LU)
    @Column(nullable = false)
    private MessageStatus statut = MessageStatus.ENVOYE;

    /** Constructeur sans argument requis par JPA/Hibernate */
    public Message() {}

    /** Constructeur pratique pour créer un nouveau message */
    public Message(User sender, User receiver, String contenu) {
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.dateEnvoi = LocalDateTime.now();
        this.statut = MessageStatus.ENVOYE;
    }

    // ——— Getters et setters (requis par JPA et utilisés par l'application) ———
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public MessageStatus getStatut() { return statut; }
    public void setStatut(MessageStatus statut) { this.statut = statut; }
}
