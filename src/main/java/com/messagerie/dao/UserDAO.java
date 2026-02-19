package com.messagerie.dao;

import com.messagerie.model.User;
import com.messagerie.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.NoResultException;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class UserDAO {

    public User register(String username, String password) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // RG1: username unique
            Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .getSingleResult();
            if (count > 0) {
                tx.rollback();
                return null;
            }
            // RG9: mot de passe haché
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            User user = new User(username, hashed);
            em.persist(user);
            tx.commit();
            return user;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    public User authenticate(String username, String password) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
            // RG9: vérification du mot de passe haché
            if (BCrypt.checkpw(password, user.getPassword())) {
                return user;
            }
            return null;
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public void updateStatus(Long userId, UserStatus status) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User user = em.find(User.class, userId);
            if (user != null) {
                user.setStatus(status);
                em.merge(user);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

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

    public List<User> findAll() {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

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
