package com.example.server.dao;

import com.example.server.model.SupportMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(Connection connection) throws SQLException {
        this.connection = connection;
        initializeTables(); // Убедимся, что метод вызывается
    }

    private void initializeTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Таблица users
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL UNIQUE, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL)");

            // Таблица logs
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "action VARCHAR(255) NOT NULL, " +
                    "timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            // Таблица epidemic_data
            stmt.execute("CREATE TABLE IF NOT EXISTS epidemic_data (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_id INTEGER NOT NULL, " +
                    "region VARCHAR(255) NOT NULL, " +
                    "date DATE NOT NULL, " +
                    "infected INTEGER NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            // Таблица forecasts
            stmt.execute("CREATE TABLE IF NOT EXISTS forecasts (" +
                    "id SERIAL PRIMARY KEY, " +
                    "region_id INTEGER NOT NULL, " +
                    "forecast_date DATE NOT NULL, " +
                    "predicted_cases INTEGER NOT NULL, " +
                    "created_by INTEGER NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL)");

            // Таблица regions
            stmt.execute("CREATE TABLE IF NOT EXISTS regions (" +
                    "region_id SERIAL PRIMARY KEY, " +
                    "region_name VARCHAR(255) NOT NULL UNIQUE)");

            // Таблица support_messages
            stmt.execute("CREATE TABLE IF NOT EXISTS support_messages (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "response TEXT, " +
                    "created_at VARCHAR(50) NOT NULL)");
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return connection.prepareStatement(sql);
    }

    public List<SupportMessage> getUserMessages(String username) throws SQLException {
        List<SupportMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM support_messages WHERE username = ?";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SupportMessage message = new SupportMessage();
                message.setId(rs.getInt("id"));
                message.setUsername(rs.getString("username"));
                message.setMessage(rs.getString("message"));
                message.setStatus(rs.getString("status"));
                message.setResponse(rs.getString("response"));
                message.setCreatedAt(rs.getString("created_at"));
                messages.add(message);
            }
        }
        return messages;
    }

    public void saveSupportMessage(String username, SupportMessage message) throws SQLException {
        String sql = "INSERT INTO support_messages (username, message, status, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, message.getMessage());
            stmt.setString(3, message.getStatus());
            stmt.setString(4, message.getCreatedAt());
            stmt.executeUpdate();
        }
    }

    public List<SupportMessage> getAllMessages() throws SQLException {
        List<SupportMessage> messages = new ArrayList<>();
        String sql = "SELECT * FROM support_messages ORDER BY created_at DESC";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                SupportMessage message = new SupportMessage();
                message.setId(rs.getInt("id"));
                message.setUsername(rs.getString("username"));
                message.setMessage(rs.getString("message"));
                message.setStatus(rs.getString("status"));
                message.setResponse(rs.getString("response"));
                message.setCreatedAt(rs.getString("created_at"));
                messages.add(message);
            }
        }
        return messages;
    }

    public void updateSupportMessage(SupportMessage message) throws SQLException {
        String sql = "UPDATE support_messages SET status = ?, response = ? WHERE id = ?";
        try (PreparedStatement stmt = prepareStatement(sql)) {
            stmt.setString(1, message.getStatus());
            stmt.setString(2, message.getResponse());
            stmt.setInt(3, message.getId());
            stmt.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}