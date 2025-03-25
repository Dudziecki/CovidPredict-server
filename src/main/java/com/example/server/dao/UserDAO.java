package com.example.server.dao;

import com.example.server.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setString(1, username);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password"), rs.getString("role"));
        }
        return null;
    }

    public void addUser(String username, String password, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.setString(3, role);
        stmt.executeUpdate();
    }
}