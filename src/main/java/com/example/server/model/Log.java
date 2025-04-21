package com.example.server.model;

import java.sql.Timestamp;

public class Log {
    private int id;
    private int userId;
    private String username; // Добавляем поле username
    private String action;
    private Timestamp timestamp;

    public Log() {
    }

    public Log(int id, int userId, String username, String action, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setLogId(int id) {
        this.id = id;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Log{id=" + id + ", userId=" + userId + ", username='" + username + "', action='" + action + "', timestamp=" + timestamp + "}";
    }
}