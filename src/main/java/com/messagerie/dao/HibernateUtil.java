package com.messagerie.dao;

import com.messagerie.config.AppConfig;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitaire pour obtenir la fabrique EntityManager (Hibernate/JPA).
 * La configuration JDBC (URL, user, password) est lue depuis AppConfig et injectée
 * au lieu des valeurs du persistence.xml, ce qui permet de configurer la BDD sans recompiler.
 */
public class HibernateUtil {

    private static final Logger LOG = Logger.getLogger(HibernateUtil.class.getName());
    // Fabrique d'EntityManager : une seule instance pour toute l'application (singleton)
    private static final EntityManagerFactory emf;

    static {
        try {
            // Surcharge des paramètres de persistence avec ceux de AppConfig (fichier ou variables d'env)
            Map<String, Object> overrides = new HashMap<>();
            overrides.put("jakarta.persistence.jdbc.url", AppConfig.getJdbcUrl());
            overrides.put("jakarta.persistence.jdbc.user", AppConfig.getJdbcUser());
            overrides.put("jakarta.persistence.jdbc.password", AppConfig.getJdbcPassword());
            // "messageriePU" = nom de l'unité de persistance dans persistence.xml
            emf = Persistence.createEntityManagerFactory("messageriePU", overrides);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur initialisation Hibernate: " + e.getMessage(), e);
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Retourne la fabrique pour créer des EntityManager (une par opération/transaction en général). */
    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    /** Ferme la fabrique et libère les ressources (à appeler à l'arrêt du serveur). */
    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
