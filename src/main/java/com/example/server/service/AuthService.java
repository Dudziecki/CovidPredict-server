package com.example.server.service;

import com.example.server.dao.LogDAO;
import com.example.server.dao.UserDAO;
import com.example.server.model.User;

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
        System.out.println("User found: " + username + ", stored password: " + user.getPassword() + ", provided password: " + password);
        if (user.getPassword().equals(password)) {
            System.out.println("Password match for user: " + username);
            logDAO.logAction(user.getUserId(), "LOGIN", "User logged in"); // "details" не используется в базе
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
        boolean saved = userDAO.saveUser(username, password, role);
        if (saved) {
            User newUser = userDAO.findByUsername(username);
            logDAO.logAction(newUser.getUserId(), "REGISTER", "User registered with role " + role); // "details" не используется в базе
            System.out.println("Registration successful for user: " + username);
        } else {
            System.out.println("Registration failed for user: " + username);
        }
        return saved;
    }
}