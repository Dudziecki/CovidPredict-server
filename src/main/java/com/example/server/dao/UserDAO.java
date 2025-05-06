package com.example.server.dao;

import com.example.server.model.User;
import com.example.server.util.PasswordUtil; // Импортируем PasswordUtil

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        System.out.println("Executing query: " + sql + " with username: " + username);
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("User found: username=" + rs.getString("username") + ", role=" + rs.getString("role"));
                return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            } else {
                System.out.println("No user found with username: " + username);
            }
            return null;
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                ));
            }
            return users;
        }
    }

    public boolean saveUser(String username, String password, String role) throws SQLException {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Новый метод для хеширования существующих паролей
    public void updatePasswordsWithHash() throws SQLException {
        String sqlSelect = "SELECT user_id, password FROM users";
        String sqlUpdate = "UPDATE users SET password = ? WHERE user_id = ?";
        try (PreparedStatement selectStmt = dbManager.prepareStatement(sqlSelect);
             PreparedStatement updateStmt = dbManager.prepareStatement(sqlUpdate)) {
            ResultSet rs = selectStmt.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String plainPassword = rs.getString("password");
                // Хешируем только если пароль ещё не захеширован (BCrypt хеш начинается с $2a$)
                if (!plainPassword.startsWith("$2a$")) {
                    String hashedPassword = PasswordUtil.hashPassword(plainPassword);
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setInt(2, userId);
                    updateStmt.executeUpdate();
                    System.out.println("Updated password for user_id: " + userId);
                }
            }
            System.out.println("All passwords have been hashed.");
        }
    }
}