# Application de Messagerie Instantanée

Application de messagerie instantanée **client-serveur** en Java (type WhatsApp / messagerie interne). Plusieurs utilisateurs peuvent s’inscrire, se connecter et échanger des messages en temps réel. Les messages envoyés à un utilisateur hors ligne sont stockés et livrés à sa prochaine connexion.

**Interface** : JavaFX avec thème sombre « Charcoal & Ivory », FXML + CSS.

---

## Sommaire

- [Technologies et dépendances](#-technologies-et-dépendances)
- [Structure du projet](#-structure-du-projet)
- [Architecture globale](#-architecture-globale)
- [Fonctionnalités et règles de gestion](#-fonctionnalités-et-règles-de-gestion)
- [Configuration](#-configuration)
- [Base de données](#-base-de-données)
- [Installation et lancement](#-installation-et-lancement)
- [Protocole réseau (développeurs)](#-protocole-réseau-développeurs)
- [Interface utilisateur (design)](#-interface-utilisateur-design)
- [Dépannage](#-dépannage)

---

## Technologies et dépendances

| Technologie        | Version / détail |
|--------------------|-------------------|
| **Langage**        | Java 17           |
| **Interface**      | JavaFX 21 (controls, FXML) |
| **Réseau**         | Sockets Java (`ServerSocket`, `Socket`), TCP |
| **Persistance**    | Hibernate 6.4, JPA 3.1 (Jakarta) |
| **Base de données**| MySQL 8+ (driver `mysql-connector-j` 8.3) |
| **Sécurité**      | jBCrypt (hachage des mots de passe) |
| **Build**          | Maven (compiler 17, `javafx-maven-plugin` pour l’exécution) |

Toutes les dépendances sont déclarées dans `pom.xml`.

---

## Structure du projet

```
App-Mesagerie-Java/
├── pom.xml
├── README.md
├── src/main/java/
│   ├── module-info.java
│   └── com/messagerie/
│       ├── config/          # Configuration centralisée
│       │   └── AppConfig.java
│       ├── model/           # Entités JPA et enums
│       │   ├── User.java
│       │   ├── Message.java
│       │   ├── UserStatus.java
│       │   └── MessageStatus.java
│       ├── dao/             # Accès données + Hibernate
│       │   ├── HibernateUtil.java
│       │   ├── UserDAO.java
│       │   └── MessageDAO.java
│       ├── protocol/        # Protocole texte client/serveur
│       │   └── Protocol.java
│       ├── server/          # Serveur TCP
│       │   ├── ChatServer.java
│       │   ├── ClientHandler.java
│       │   └── ServerLogger.java
│       ├── client/          # Client réseau
│       │   └── ChatClient.java
│       └── ui/              # Application JavaFX
│           ├── MainApp.java
│           ├── AuthHelper.java
│           ├── LoginController.java
│           ├── RegisterController.java
│           └── ChatController.java
└── src/main/resources/
    ├── config.properties       # Config (DB, serveur)
    ├── css/
    │   └── style.css           # Thème global
    ├── fxml/
    │   ├── login.fxml
    │   ├── register.fxml
    │   └── chat.fxml
    └── META-INF/
        └── persistence.xml    # Unité JPA (sans identifiants DB)
```

### Rôle des packages

| Package              | Rôle |
|----------------------|------|
| `com.messagerie.config` | Lecture de `config.properties` et variables d’environnement (JDBC, host/port serveur). |
| `com.messagerie.model`  | Entités JPA `User`, `Message` ; enums `UserStatus`, `MessageStatus`. |
| `com.messagerie.dao`    | `UserDAO` (auth, inscription, statut), `MessageDAO` (conversations, messages en attente), `HibernateUtil` (EMF avec config chargée depuis `AppConfig`). |
| `com.messagerie.protocol` | Constantes des commandes, `buildCommand` / `parseCommand`, encodage Base64 pour l’historique. |
| `com.messagerie.server` | `ChatServer` (écoute TCP, un thread par client), `ClientHandler` (traitement des commandes, DAO, broadcast statuts), `ServerLogger`. |
| `com.messagerie.client` | `ChatClient` : connexion socket, thread d’écoute, envoi des commandes (login, register, envoi message, liste utilisateurs, historique, logout). |
| `com.messagerie.ui`     | `MainApp` (point d’entrée, navigation entre écrans), `AuthHelper` (connexion + handlers pour login/register), contrôleurs FXML (Login, Register, Chat). |

---

## Architecture globale

```
┌─────────────────┐         TCP (ligne de texte)         ┌─────────────────┐
│  Client JavaFX │ ◄──────────────────────────────────► │  ChatServer      │
│  MainApp       │   LOGIN | REGISTER | MSG | USERLIST   │  ClientHandler   │
│  LoginController│         HISTORY | LOGOUT             │  (1 thread/client)│
│  ChatController │                                      └────────┬────────┘
│  ChatClient    │                                                 │
└────────────────┘                                                 │ JPA
                                                                   ▼
                                                        ┌─────────────────┐
                                                        │  Hibernate      │
                                                        │  UserDAO        │
                                                        │  MessageDAO     │
                                                        └────────┬────────┘
                                                                 │ JDBC
                                                                 ▼
                                                        ┌─────────────────┐
                                                        │  MySQL          │
                                                        │  (users, msgs)  │
                                                        └─────────────────┘
```

- **Client** : une fenêtre JavaFX par instance ; connexion au serveur (host/port dans la config ou saisis à l’écran login/register).
- **Serveur** : un `ClientHandler` par client connecté ; commandes traitées dans le thread du handler ; accès BDD via les DAO.
- **Config** : `config.properties` + variables d’environnement pour JDBC et host/port (voir [Configuration](#-configuration)).

---

## Fonctionnalités et règles de gestion

### Gestion des comptes

| RG  | Description |
|-----|-------------|
| **RG1** | Inscription avec nom d’utilisateur **unique**. |
| **RG9** | Mots de passe stockés de façon sécurisée (hachage **BCrypt**). |
| **RG2** | **Authentification** obligatoire pour accéder à la messagerie. |
| **RG3** | Un utilisateur ne peut être connecté qu’**une seule fois** en même temps. |
| **RG4** | Statut **ONLINE** / **OFFLINE** géré à la connexion et à la déconnexion (et au démarrage du serveur : tous OFFLINE). |

### Messagerie

| RG  | Description |
|-----|-------------|
| **RG5** | Envoi uniquement si l’expéditeur est connecté et le destinataire **existe**. |
| **RG7** | Contenu non vide, **max 1000 caractères**. |
| **RG6** | Messages envoyés à un utilisateur **hors ligne** : stockés en BDD et **livrés à la prochaine connexion**. |
| **RG8** | **Historique** des conversations affiché par ordre **chronologique**. |

### Client et serveur

| RG   | Description |
|------|-------------|
| **RG10** | Le client gère la **perte de connexion** et affiche une erreur (bannière + possibilité de fermer). |
| **RG11** | Le serveur gère chaque client dans un **thread dédié** (communications non bloquantes). |
| **RG12** | Le serveur **journalise** les événements (connexions, déconnexions, messages). |

---

## Configuration

La configuration est centralisée pour la **base de données** et le **serveur** (host/port).

### Fichier `config.properties`

Emplacement : `src/main/resources/config.properties`.

| Clé            | Description                    | Valeur par défaut (exemple) |
|----------------|--------------------------------|------------------------------|
| `jdbc.url`     | URL JDBC MySQL                 | `jdbc:mysql://localhost:3306/messagerie?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `jdbc.user`    | Utilisateur MySQL              | `root` |
| `jdbc.password`| Mot de passe MySQL             | (vide) |
| `server.host`  | Adresse du serveur (côté client) | `localhost` |
| `server.port`  | Port d’écoute du serveur        | `12345` |

Les valeurs lues ici servent aussi de **placeholders** dans les champs « Serveur » et « Port » des écrans de connexion et d’inscription.

### Variables d’environnement (surcharge)

Pour surcharger sans modifier le fichier (déploiement, CI, etc.) :

| Variable       | Surcharge de   |
|----------------|----------------|
| `JDBC_URL`     | `jdbc.url`     |
| `JDBC_USER`    | `jdbc.user`    |
| `JDBC_PASSWORD`| `jdbc.password`|
| `SERVER_HOST`  | `server.host`  |
| `SERVER_PORT`  | `server.port`  |

La classe `AppConfig` charge d’abord `config.properties`, puis applique ces variables d’environnement si elles sont définies.

---

## Base de données

1. **Créer la base MySQL** (une fois) :

   ```sql
   CREATE DATABASE messagerie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

2. **Identifiants** : définir `jdbc.user` et `jdbc.password` dans `config.properties` (ou via `JDBC_USER` / `JDBC_PASSWORD`). L’URL dans `jdbc.url` doit pointer vers la base `messagerie` (ou celle que vous utilisez).

3. **Tables** : Hibernate crée ou met à jour les tables au démarrage du **serveur** (`hbm2ddl.auto=update` dans `persistence.xml`). Les entités sont `User` et `Message`.

---

## Installation et lancement

### Prérequis

- **JDK 17** (ou plus)
- **Maven 3.6+**
- **MySQL 8+** (serveur MySQL démarré, base `messagerie` créée)
- **Deux terminaux** (CMD ou PowerShell) : un pour le serveur, un pour le client (voir ci‑dessous)
- IDE optionnel (IntelliJ IDEA, Eclipse, etc.) avec support Maven et JavaFX

### Étape 0 : Configuration MySQL (obligatoire avant de lancer le serveur)

#### A. Créer la base de données « messagerie »

L’application se connecte à une base nommée **`messagerie`**. Si elle n’existe pas, vous aurez l’erreur **« Unknown database 'messagerie' »**. Il faut créer cette base **une seule fois**.

**Avec MySQL Workbench :**

1. Ouvrez MySQL Workbench et connectez-vous à votre serveur MySQL (avec le même utilisateur/mot de passe que dans `config.properties`).
2. Ouvrez un nouvel onglet de requête SQL (ou utilisez la zone de requête).
3. Exécutez exactement cette commande :
   ```sql
   CREATE DATABASE messagerie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
4. Vérifiez dans le panneau de gauche (Navigator) que la base **`messagerie`** apparaît bien. Son nom doit être exactement **messagerie** (en minuscules).

**En ligne de commande MySQL :** vous pouvez aussi exécuter la même commande `CREATE DATABASE ...` après vous être connecté avec `mysql -u root -p`.

#### B. Configurer les identifiants dans le projet

Ouvrez le fichier **`src/main/resources/config.properties`** et adaptez les identifiants MySQL :
   - **`jdbc.user`** : utilisateur MySQL (par défaut `root`)
   - **`jdbc.password`** : **mot de passe de cet utilisateur**. Si votre MySQL a un mot de passe pour `root`, vous **devez** le renseigner ici. Par exemple :
     ```properties
     jdbc.password=MonMotDePasseMySQL
     ```
   - Si le mot de passe est vide sous MySQL, laissez `jdbc.password=` (vide).

Sans la base `messagerie`, le serveur affiche **« Unknown database 'messagerie' »**. Sans le bon mot de passe, il affiche **« Access denied for user 'root'@'localhost' (using password: NO) »**. Voir [Dépannage](#-dépannage) en cas d’erreur.

### Compilation

Dans un terminal, à la **racine du projet** (dossier où se trouve `pom.xml`) :

```bash
mvn compile
```

### Démarrage : deux terminaux nécessaires

L’application est **client–serveur**. Il faut donc **deux processus** :

- **Terminal 1** : le **serveur** (écoute les connexions et gère la base de données).
- **Terminal 2** : le **client** (interface graphique pour se connecter et envoyer des messages).

L’ordre est important : **démarrer d’abord le serveur**, puis le client.

---

#### Terminal 1 — Démarrer le serveur

1. Ouvrez un **premier** CMD ou PowerShell.
2. Allez dans le dossier du projet :
   ```bash
   cd "C:\Users\...\App-Mesagerie-Java"
   ```
   (remplacez par le chemin réel de votre projet.)

3. Lancez le serveur avec Maven :
   ```bash
   mvn exec:java@server
   ```

4. Vous devez voir dans la console quelque chose comme :
   - `Base de données initialisée.`
   - `Serveur démarré sur le port 12345`
   - `En attente de connexions...`

5. **Laissez ce terminal ouvert.** Tant que le serveur tourne, il reste actif dans ce terminal. Pour arrêter le serveur : `Ctrl+C` dans ce terminal.

**Alternative** : depuis IntelliJ, vous pouvez lancer la classe **`com.messagerie.server.ChatServer`** (bouton Run) au lieu d’utiliser ce terminal ; dans ce cas, le « Terminal 1 » est la fenêtre d’exécution IntelliJ du serveur.

---

#### Terminal 2 — Démarrer le client (interface)

1. Ouvrez un **second** CMD ou PowerShell (sans fermer le premier).

2. Allez dans le **même** dossier du projet :
   ```bash
   cd "C:\Users\...\App-Mesagerie-Java"
   ```

3. Lancez l’interface client avec Maven :
   ```bash
   mvn javafx:run
   ```

4. Une fenêtre « Messagerie — Connexion » s’ouvre.

5. Sur l’écran de connexion :
   - **Serveur** : laissez vide pour `localhost`, ou saisissez l’adresse du serveur (ex. IP d’une autre machine).
   - **Port** : laissez vide pour `12345`, ou saisissez le port configuré dans `config.properties`.
   - Créez un compte (lien « S’inscrire ») puis connectez-vous, ou connectez‑vous si vous avez déjà un compte.

**Alternative** : depuis IntelliJ, lancez la classe **`com.messagerie.ui.MainApp`** (bouton Run). Vous pouvez lancer plusieurs fois `MainApp` (plusieurs fenêtres) pour simuler plusieurs utilisateurs.

---

### Récapitulatif

| Où              | Commande / action |
|-----------------|-------------------|
| **Terminal 1**  | `mvn exec:java@server` (ou Run `ChatServer` dans l’IDE) — à lancer **en premier**, à garder ouvert. |
| **Terminal 2**  | `mvn javafx:run` (ou Run `MainApp` dans l’IDE) — à lancer **après** que le serveur affiche « En attente de connexions... ». |

### Scénarios de test

- **Une seule machine** : serveur dans le terminal 1, un ou plusieurs clients dans le terminal 2 (ou plusieurs runs de `MainApp` dans l’IDE). Tous avec `localhost` et port `12345`. Créez plusieurs comptes et échangez des messages entre les fenêtres.
- **Deux machines** : serveur sur la machine 1 (terminal 1) ; sur la machine 2, client avec l’IP de la machine 1 et le port (ex. 12345). Vérifier que le pare-feu de la machine 1 autorise les connexions entrantes sur ce port.

---

## Protocole réseau (développeurs)

- **Transport** : une **ligne de texte** par commande ou réponse, encodage **UTF-8**.
- **Champs** : séparés par `|`. Pour les commandes dont le **contenu utilisateur** peut contenir `|`, le parsing utilise une limite de champs (dernier champ = reste de la ligne) ou un payload en **Base64**.

### Commandes client → serveur

| Commande   | Format (résumé)        | Exemple |
|------------|------------------------|---------|
| LOGIN      | `LOGIN\|username\|password` | |
| REGISTER   | `REGISTER\|username\|password` | |
| LOGOUT     | `LOGOUT`               | |
| MSG        | `MSG\|receiver\|content` (content peut contenir `\|`) | |
| USERLIST   | `USERLIST`             | |
| HISTORY    | `HISTORY\|otherUsername` | |

### Réponses serveur → client

| Réponse           | Format (résumé) |
|-------------------|------------------|
| LOGIN_OK          | `LOGIN_OK\|userId\|username` |
| LOGIN_FAIL        | `LOGIN_FAIL\|message` |
| ALREADY_CONNECTED | `ALREADY_CONNECTED\|message` |
| REGISTER_OK / REGISTER_FAIL | idem |
| MSG_OK / MSG_FAIL | avec message éventuel |
| INCOMING_MSG      | `INCOMING_MSG\|sender\|date\|id\|content` (content en dernier pour autoriser `\|`) |
| USER_LIST         | `USER_LIST\|user1:ONLINE,user2:OFFLINE,...` |
| HISTORY_DATA      | `HISTORY_DATA\|base64(payload)` ; dans le payload : messages séparés par `;;`, champs par `::` (sender::content::date::id) |
| USER_STATUS_CHANGE| `USER_STATUS_CHANGE\|username\|ONLINE\|OFFLINE` |
| ERROR             | `ERROR\|message` |

Détails et constantes : `com.messagerie.protocol.Protocol`.

---

## Interface utilisateur (design)

- **Thème** : sombre « Charcoal & Ivory » (fond sombre, accent or/ivoire, liens en bleu clair).
- **Fichiers** :
  - **FXML** : `src/main/resources/fxml/login.fxml`, `register.fxml`, `chat.fxml`.
  - **CSS** : `src/main/resources/css/style.css` (variables, auth, sidebar, bulles, bannière d’erreur, etc.).
- **Écrans** :
  - **Login** : Serveur, Port, Nom d’utilisateur, Mot de passe, lien « S’inscrire ».
  - **Register** : mêmes champs + confirmation mot de passe, lien « Se connecter ».
  - **Chat** : sidebar (liste des utilisateurs avec statut en ligne/hors ligne, déconnexion), zone de conversation (en-tête contact, bulles envoyées/reçues, champ d’envoi), bannière d’erreur avec bouton « Fermer ».
- **Navigation** : `MainApp` charge les scènes (login, register, chat) et applique `style.css` à toutes.

---

## Dépannage

| Problème | Solution |
|----------|----------|
| **« Unknown database 'messagerie' »** au démarrage du serveur | La base de données **`messagerie`** n’existe pas encore sur votre MySQL. Créez-la **une fois** : dans MySQL Workbench, connectez-vous puis exécutez `CREATE DATABASE messagerie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;` (voir [Étape 0 – A](#a-créer-la-base-de-données--messagerie-) ci-dessus). Vérifiez que le nom de la base est bien **messagerie** (minuscules). |
| **« Access denied for user 'root'@'localhost' (using password: NO) »** au démarrage du serveur | Votre MySQL exige un mot de passe pour l’utilisateur `root`, mais l’application n’en envoie pas. Ouvrez **`src/main/resources/config.properties`** et renseignez le mot de passe : `jdbc.password=VotreMotDePasseMySQL`. Puis recompilez si besoin (`mvn compile`) et relancez le serveur. Si vous préférez ne pas mettre le mot de passe dans le fichier, vous pouvez définir la variable d’environnement **`JDBC_PASSWORD`** avec la valeur du mot de passe. |
| « Impossible de se connecter au serveur » (côté client) | Vérifier que le **serveur** est bien lancé dans l’autre terminal (message « En attente de connexions... »). Vérifier que Serveur et Port dans l’écran de connexion correspondent (ex. `localhost` et `12345`). Vérifier que le pare-feu ne bloque pas le port. |
| Erreur Hibernate / « Could not obtain connection » | Vérifier que le **serveur MySQL** est démarré, que la base **`messagerie`** existe (sinon la créer comme ci-dessus), et que **`config.properties`** (ou les variables `JDBC_URL`, `JDBC_USER`, `JDBC_PASSWORD`) contiennent les bons identifiants. |
| « Cet utilisateur est déjà connecté » | Un autre client est déjà connecté avec ce compte. Déconnectez l’autre fenêtre ou utilisez un autre compte. |
| Messages qui ne s’affichent pas | Vérifier que le destinataire est bien sélectionné dans la liste et que la connexion est active (pas de bannière « Connexion perdue »). |

**Remarque** : Les avertissements en console du type `sun.misc.Unsafe`, « 6 problems were encountered while building the effective model for javafx-controls », ou « Required filename-based automodules detected » viennent de Maven ou des dépendances, pas de votre code. Vous pouvez les ignorer tant que le build se termine par **BUILD SUCCESS** et que le serveur ou le client démarre correctement.

Pour les logs côté serveur : voir la sortie console du terminal où tourne `mvn exec:java@server` (ou de l’exécution de `ChatServer` dans l’IDE).

---

*Projet de messagerie instantanée — Java 17, JavaFX, Maven, Hibernate, MySQL.*
