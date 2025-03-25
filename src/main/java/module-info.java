module com.example.server {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    exports com.example.server;
    exports com.example.server.model;

    opens com.example.server to javafx.fxml;

}