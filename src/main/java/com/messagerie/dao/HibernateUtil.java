package com.messagerie.dao;

import com.messagerie.config.AppConfig;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HibernateUtil {

    private static final Logger LOG = Logger.getLogger(HibernateUtil.class.getName());
    private static final EntityManagerFactory emf;

    static {
        try {
            Map<String, Object> overrides = new HashMap<>();
            overrides.put("jakarta.persistence.jdbc.url", AppConfig.getJdbcUrl());
            overrides.put("jakarta.persistence.jdbc.user", AppConfig.getJdbcUser());
            overrides.put("jakarta.persistence.jdbc.password", AppConfig.getJdbcPassword());
            emf = Persistence.createEntityManagerFactory("messageriePU", overrides);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur initialisation Hibernate: " + e.getMessage(), e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
