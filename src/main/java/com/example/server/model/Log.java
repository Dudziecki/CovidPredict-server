package com.example.server.model;

import java.sql.Timestamp;

public class Log {
    private int id;
    private int userId;
    private String action;
    private Timestamp timestamp;

    public Log(int id, int userId, String action, Timestamp timestamp) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Log{id=" + id + ", userId=" + userId + ", action='" + action + "', timestamp=" + timestamp + "}";
    }
}