module com.messagerie {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive jakarta.persistence;
    requires org.hibernate.orm.core;
    requires java.sql;
    requires jbcrypt;
    requires java.naming;

    opens com.messagerie.model to org.hibernate.orm.core, javafx.base;
    opens com.messagerie.ui to javafx.fxml;

    exports com.messagerie.model;
    exports com.messagerie.dao;
    exports com.messagerie.server;
    exports com.messagerie.client;
    exports com.messagerie.protocol;
    exports com.messagerie.ui;
}
