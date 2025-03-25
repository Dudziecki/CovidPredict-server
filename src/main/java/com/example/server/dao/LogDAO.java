package com.example.server.dao;

import com.example.server.model.Log;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    private final DatabaseManager dbManager;

    public LogDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void addLog(int userId, String action) throws SQLException {
        String sql = "INSERT INTO logs (user_id, action) VALUES (?, ?)";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setInt(1, userId);
        stmt.setString(2, action);
        stmt.executeUpdate();
    }

    public List<Log> getAllLogs() throws SQLException {
        List<Log> logs = new ArrayList<>();
        String sql = "SELECT * FROM logs";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            logs.add(new Log(rs.getInt("log_id"), rs.getInt("user_id"), rs.getString("action"), rs.getTimestamp("timestamp")));
        }
        return logs;
    }
}