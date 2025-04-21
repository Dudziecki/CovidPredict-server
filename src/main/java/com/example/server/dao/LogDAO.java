package com.example.server.dao;

import com.example.server.model.Log;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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


    public List<Log> getAllLogs() throws SQLException {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT l.log_id, l.user_id, l.action, l.timestamp, u.username " +
                "FROM logs l " +
                "LEFT JOIN users u ON l.user_id = u.user_id " +
                "ORDER BY l.timestamp DESC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Log log = new Log();
                log.setLogId(rs.getInt("log_id"));
                log.setUserId(rs.getInt("user_id"));
                log.setUsername(rs.getString("username")); // Получаем username из JOIN
                log.setAction(rs.getString("action"));
                log.setTimestamp(rs.getTimestamp("timestamp"));
                logs.add(log);
            }
        }
        return logs;
    }
}