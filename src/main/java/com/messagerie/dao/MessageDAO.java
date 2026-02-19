package com.messagerie.dao;

import com.messagerie.model.Message;
import com.messagerie.model.MessageStatus;
import com.messagerie.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

public class MessageDAO {

    public Message save(Message message) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(message);
            tx.commit();
            return message;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
            return null;
        } finally {
            em.close();
        }
    }

    // RG8: historique par ordre chronologique
    public List<Message> getConversation(Long userId1, Long userId2) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                    "SELECT m FROM Message m WHERE " +
                    "(m.sender.id = :u1 AND m.receiver.id = :u2) OR " +
                    "(m.sender.id = :u2 AND m.receiver.id = :u1) " +
                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("u1", userId1)
                    .setParameter("u2", userId2)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // RG6: messages en attente pour un utilisateur hors ligne
    public List<Message> getPendingMessages(Long receiverId) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        try {
            return em.createQuery(
                    "SELECT m FROM Message m WHERE m.receiver.id = :receiverId AND m.statut = :statut ORDER BY m.dateEnvoi ASC",
                    Message.class)
                    .setParameter("receiverId", receiverId)
                    .setParameter("statut", MessageStatus.ENVOYE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public void updateStatus(Long messageId, MessageStatus status) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Message msg = em.find(Message.class, messageId);
            if (msg != null) {
                msg.setStatut(status);
                em.merge(msg);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    public void markAsReceived(Long receiverId) {
        EntityManager em = HibernateUtil.getEntityManagerFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("UPDATE Message m SET m.statut = :newStatus WHERE m.receiver.id = :receiverId AND m.statut = :oldStatus")
                    .setParameter("newStatus", MessageStatus.RECU)
                    .setParameter("receiverId", receiverId)
                    .setParameter("oldStatus", MessageStatus.ENVOYE)
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
