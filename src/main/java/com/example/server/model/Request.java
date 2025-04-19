package com.example.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Request {
    @JsonProperty("command")
    private String command;

    @JsonProperty("data")
    private String data;

    public Request() {} // Пустой конструктор для Jackson

    public Request(String command, String data) {
        this.command = command;
        this.data = data;
    }

    public String getCommand() {
        return command;
    }

    public String getData() {
        return data;
    }
}