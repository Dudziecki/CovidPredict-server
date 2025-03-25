package com.example.server.model;

import java.io.Serializable;

public class Request implements Serializable {
    private String command;
    private String data;

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