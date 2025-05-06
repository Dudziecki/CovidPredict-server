package com.example.server.service;

import com.example.server.dao.LogDAO;
import com.example.server.dao.UserDAO;
import com.example.server.model.User;
import com.example.server.util.PasswordUtil; // Импортируем PasswordUtil

import java.sql.SQLException;

public class AuthService {
    private final UserDAO userDAO;
    private final LogDAO logDAO;

    public AuthService(UserDAO userDAO, LogDAO logDAO) {
        this.userDAO = userDAO;
        this.logDAO = logDAO;
    }

    public User login(String username, String password) throws SQLException {
        System.out.println("Attempting login for username: " + username);
        User user = userDAO.findByUsername(username);
        if (user == null) {
            System.out.println("User not found: " + username);
            return null;
        }
        System.out.println("User found: " + username + ", stored password (hashed): " + user.getPassword() + ", provided password: " + password);
        if (PasswordUtil.checkPassword(password, user.getPassword())) { // Проверяем пароль
            System.out.println("Password match for user: " + username);
            logDAO.logAction(user.getUserId(), "LOGIN", "User logged in");
            return user;
        }
        System.out.println("Password mismatch for user: " + username);
        return null;
    }

    public boolean register(String username, String password, String role) throws SQLException {
        System.out.println("Attempting registration for username: " + username);
        User existingUser = userDAO.findByUsername(username);
        if (existingUser != null) {
            System.out.println("User already exists: " + username);
            return false;
        }
        // Хешируем пароль перед сохранением
        String hashedPassword = PasswordUtil.hashPassword(password);
        boolean saved = userDAO.saveUser(username, hashedPassword, role);
        if (saved) {
            User newUser = userDAO.findByUsername(username);
            logDAO.logAction(newUser.getUserId(), "REGISTER", "User registered with role " + role);
            System.out.println("Registration successful for user: " + username);
        } else {
            System.out.println("Registration failed for user: " + username);
        }
        return saved;
    }
}