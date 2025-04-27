module com.example.server {
    requires java.sql;
    requires com.fasterxml.jackson.databind;

    exports com.example.server;
    exports com.example.server.model;
    opens com.example.server.model to com.fasterxml.jackson.databind;
    exports com.example.server.dao to com.fasterxml.jackson.databind;
    //добавил пакеты
    opens com.example.server to com.fasterxml.jackson.databind;
    opens com.example.server.service to com.fasterxml.jackson.databind;
}