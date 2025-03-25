package com.example.server.model;

import java.io.Serializable;

public class Response implements Serializable {
    private String message;

    public Response(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}