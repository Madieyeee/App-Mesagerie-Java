package com.messagerie.dao;

import com.messagerie.model.User;
import com.messagerie.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

/**
 * Couche d'accès aux données pour les utilisateurs (table "users").
 * Gère l'inscription, l'authentification, le statut et les requêtes de lecture.
 */
public class UserDAO {

    /**
     * Inscrit un nouvel utilisateur. Le mot de passe est hashé avec BCrypt (sécurité).
     * RG1 : le nom d'utilisateur doit être unique.
     * @return l'utilisateur créé, ou null si le username est déjà pris ou en cas d'erreur
     */
    public User register(String username, String password) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // RG1: vérifier que le username n'existe pas déjà
            Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult();
            if (count > 0) {
                tx.rollback();
                return null;
            }
            // RG9: ne jamais stocker le mot de passe en clair, toujours le hacher (BCrypt)
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            User user = new User(username, hashed);
            em.persist(user);  // Insère l'entité en base
            tx.commit();
            return user;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
            return null;
        } finally {
            em.close();  // Toujours fermer l'EntityManager pour libérer les ressources
        }
    }

    /**
     * Vérifie identifiant et mot de passe. Compare le mot de passe saisi au hash stocké (BCrypt).
     * @return l'utilisateur si authentification OK, null sinon
     */
    public User authenticate(String username, String password) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            // Requête JPQL : langage orienté objet (User) au lieu de SQL brut
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            // RG9: vérification du mot de passe haché
                    if (BCrypt.checkpw(password, user.getPassword())) {
                return user;
            }
            return null;
        } catch (NoResultException e) {
            return null;  // Aucun utilisateur avec ce username
        } finally {
            em.close();
        }
    }

    /** Met à jour le statut (ONLINE/OFFLINE) d'un utilisateur en base. */
    public void updateStatus(Long userId, UserStatus status) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User user = em.find(User.class, userId);  // Charger l'entité par ID
            if (user != null) {
                user.setStatus(status);
                em.merge(user);  // Synchronise les modifications avec la BDD
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    /** Recherche un utilisateur par son nom d'utilisateur. Retourne null si non trouvé. */
    public User findByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    /** Retourne la liste de tous les utilisateurs, triés par username. */
    public List<User> findAll() {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Remet le statut de tous les utilisateurs à OFFLINE (ex: au redémarrage du serveur). */
    public void setAllOffline() {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE User u SET u.status = :status")
                    .setParameter("status", UserStatus.OFFLINE)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }
}
