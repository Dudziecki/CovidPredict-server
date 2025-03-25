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
        User user = userDAO.getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            logDAO.addLog(user.getId(), "User logged in: " + username);
            return user;
        }
        return null;
    }

    public boolean register(String username, String password, String role) throws SQLException {
        User existingUser = userDAO.getUserByUsername(username);
        if (existingUser != null) {
            return false; // Пользователь уже существует
        }
        userDAO.addUser(username, password, role);
        User newUser = userDAO.getUserByUsername(username);
        logDAO.addLog(newUser.getId(), "User registered: " + username);
        return true;
    }
}