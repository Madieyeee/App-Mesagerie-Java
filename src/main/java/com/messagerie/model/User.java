package com.messagerie.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un utilisateur de l'application.
 * Correspond à la table "users" en base de données.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ID auto-généré
    private Long id;

    @Column(nullable = false, unique = true)  // Identifiant de connexion, unique en base
    private String username;

    @Column(nullable = false)  // Mot de passe (stocké hashé avec BCrypt en pratique)
    private String password;

    @Enumerated(EnumType.STRING)  // ONLINE ou OFFLINE, stocké comme chaîne
    @Column(nullable = false)
    private UserStatus status = UserStatus.OFFLINE;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();  // Date d'inscription

    /** Constructeur sans argument requis par JPA */
    public User() {}

    /** Constructeur pour créer un nouvel utilisateur (inscription) */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.status = UserStatus.OFFLINE;
        this.dateCreation = LocalDateTime.now();
    }

    // ——— Getters et setters ———
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
