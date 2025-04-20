package com.example.server.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class LogDAO {
    private final DatabaseManager dbManager;

    public LogDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void logAction(int userId, String action, String details) throws SQLException {
        String sql = "INSERT INTO logs (user_id, action, timestamp) VALUES (?, ?, ?)"; // Убрали "details"
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis())); // Сместили индексы
            stmt.executeUpdate();
        }
    }
}