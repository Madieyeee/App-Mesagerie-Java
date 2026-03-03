/**
 * Déclaration du module Java (système de modules introduit en Java 9).
 * Ce fichier décrit les dépendances et les parties du projet exposées aux autres modules.
 */
module com.messagerie {
    // Dépendances : bibliothèques requises par l'application
    requires javafx.controls;           // Composants d'interface (boutons, listes, etc.)
    requires javafx.fxml;               // Fichiers FXML pour définir les écrans
    requires transitive jakarta.persistence;  // API JPA pour la persistance (base de données)
    requires org.hibernate.orm.core;    // Hibernate : implémentation JPA (ORM)
    requires java.sql;                  // Accès SQL (utilisé indirectement par Hibernate)
    requires jbcrypt;                   // Hash des mots de passe (sécurité)
    requires java.naming;               // Pour la configuration JNDI si besoin

    // Autorise Hibernate et JavaFX à accéder par réflexion aux classes (nécessaire pour ORM et binding)
    opens com.messagerie.model to org.hibernate.orm.core, javafx.base;
    opens com.messagerie.ui to javafx.fxml;

    // Packages visibles par d'autres modules qui dépendent de com.messagerie
    exports com.messagerie.model;
    exports com.messagerie.dao;
    exports com.messagerie.server;
    exports com.messagerie.client;
    exports com.messagerie.protocol;
    exports com.messagerie.ui;
}
