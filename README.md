# Application de Messagerie Instantanée (Type WhatsApp)

 <!-- Placeholder: Remplacez par une URL de screenshot -->

Projet d'application de messagerie instantanée client-serveur développée en Java dans le cadre d'un examen. L'application permet à plusieurs utilisateurs de s'inscrire, de se connecter et d'échanger des messages en temps réel.

Le design de l'interface est un thème sombre moderne et épuré, "Charcoal & Ivory", sans gradients.

---

## ➤ Technologies & Dépendances

- **Langage** : Java 17
- **Interface Graphique** : JavaFX 21
- **Réseau** : Sockets Java (`ServerSocket`, `Socket`)
- **Persistance** : Hibernate 6 / JPA 3.1
- **Base de données** : MySQL 8+
- **Gestion de projet** : Maven
- **Hachage de mot de passe** : jBCrypt

---

## ➤ Fonctionnalités

L'application implémente toutes les règles de gestion (RG) requises par le sujet :

- **Gestion des comptes**
  - ✅ **RG1** : Inscription avec un nom d'utilisateur unique.
  - ✅ **RG9** : Les mots de passe sont stockés de manière sécurisée (hachage avec BCrypt).
  - ✅ **RG2** : Authentification requise pour accéder à la messagerie.
  - ✅ **RG3** : Un utilisateur ne peut être connecté qu'une seule fois simultanément.
  - ✅ **RG4** : Le statut (ONLINE / OFFLINE) est géré automatiquement à la connexion/déconnexion.

- **Messagerie**
  - ✅ **RG5** : Envoi de messages uniquement si l'expéditeur est connecté et le destinataire existe.
  - ✅ **RG7** : Le contenu des messages ne peut être vide et est limité à 1000 caractères.
  - ✅ **RG6** : Les messages envoyés à un utilisateur hors ligne sont stockés et livrés à sa prochaine connexion.
  - ✅ Réception des messages en temps réel.
  - ✅ **RG8** : L'historique des conversations est affiché par ordre chronologique.

- **Client & Serveur**
  - ✅ **RG11** : Le serveur gère chaque client dans un thread dédié pour des communications non bloquantes.
  - ✅ **RG12** : Le serveur journalise les événements importants (connexions, déconnexions, messages).
  - ✅ **RG10** : Le client gère la perte de connexion au serveur et affiche une erreur.

---

## ➤ Architecture

Le projet est structuré en plusieurs packages pour une séparation claire des responsabilités :

- `com.messagerie.model` : Entités JPA (`User`, `Message`) et enums.
- `com.messagerie.dao` : Data Access Objects pour les opérations CRUD sur la base de données (`UserDAO`, `MessageDAO`) et configuration Hibernate (`HibernateUtil`).
- `com.messagerie.protocol` : Constantes définissant le protocole de communication texte simple entre le client et le serveur.
- `com.messagerie.server` : Logique du serveur (`ChatServer`, `ClientHandler`).
- `com.messagerie.client` : Logique réseau du client (`ChatClient`).
- `com.messagerie.ui` : Contrôleurs JavaFX (`LoginController`, `ChatController`, etc.) et point d'entrée de l'application (`MainApp`).
- `resources/` : Fichiers FXML pour les vues, la feuille de style CSS et le fichier de configuration `persistence.xml`.

---

## ➤ Prérequis

1.  **JDK 17** ou supérieur.
2.  **Maven 3.6** ou supérieur.
3.  **MySQL Server 8.0** ou supérieur.
4.  Un **IDE Java** comme IntelliJ IDEA ou Eclipse (avec support Maven).

---

## ➤ Guide d'installation et de lancement

### 1. Configuration de la base de données

- Lancez votre serveur MySQL.
- Créez une nouvelle base de données pour le projet.

  ```sql
  CREATE DATABASE messagerie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

- Ouvrez le fichier `src/main/resources/META-INF/persistence.xml`.
- Modifiez les propriétés suivantes avec vos identifiants MySQL si nécessaire :

  ```xml
  <property name="jakarta.persistence.jdbc.user" value="root"/>
  <property name="jakarta.persistence.jdbc.password" value=""/>
  ```

  Hibernate créera automatiquement les tables `users` et `messages` au premier lancement du serveur.

### 2. Lancement de l'application

Il y a deux façons de lancer l'application :

#### Scénario 1 : Tester en local (une seule machine)

C'est le mode le plus simple pour tester l'application.

1.  **Lancez le serveur** : Dans votre IDE, exécutez la méthode `main()` de `src/main/java/com/messagerie/server/ChatServer.java`. La console devrait afficher que le serveur a démarré sur le port **12345**.
2.  **Lancez le premier client** : Exécutez la méthode `main()` de `src/main/java/com/messagerie/ui/MainApp.java`. Sur l'écran de connexion, laissez l'adresse du serveur sur `localhost` (valeur par défaut). Créez un compte et connectez-vous.
3.  **Lancez le deuxième client** : Exécutez à nouveau la méthode `main()` de `MainApp.java` pour ouvrir une deuxième fenêtre. Utilisez un autre compte pour vous connecter, toujours avec `localhost` comme serveur.

Vous pouvez maintenant discuter entre les deux fenêtres client.

#### Scénario 2 : Utiliser deux machines (client/serveur)

Ce mode simule une utilisation réelle sur un réseau local.

1.  **Machine 1 (Serveur)** : Lancez le `ChatServer.main()` comme décrit ci-dessus.
2.  **Machine 1 (Serveur)** : Trouvez votre adresse IP locale (tapez `ipconfig` dans le `cmd` sur Windows).
3.  **Machine 1 (Client 1)** : Lancez `MainApp.main()`, laissez l'IP du serveur sur `localhost` et connectez-vous.
4.  **Machine 2 (Client 2)** : Lancez `MainApp.main()`, entrez l'adresse IP de la Machine 1 dans le champ "Serveur" et connectez-vous.

> **Note** : Assurez-vous que le pare-feu de la Machine 1 autorise les connexions entrantes sur le port **12345**.
