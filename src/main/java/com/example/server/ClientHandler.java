package com.example.server;

import com.example.server.model.Request;
import com.example.server.model.Response;
import com.example.server.model.User;
import com.example.server.service.AuthService;
import com.example.server.dao.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final ObjectMapper objectMapper;

    public ClientHandler(Socket socket, String dbUrl, String dbUser, String dbPassword) {
        this.socket = socket;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            // Цикл для обработки нескольких запросов от клиента
            String jsonRequest;
            while ((jsonRequest = reader.readLine()) != null) {
                // Десериализуем JSON в объект Request
                Request request = objectMapper.readValue(jsonRequest, Request.class);

                // Обрабатываем запрос
                Response response = processRequest(request, conn);

                // Сериализуем ответ в JSON
                String jsonResponse = objectMapper.writeValueAsString(response);
                // Отправляем JSON-строку
                writer.write(jsonResponse + "\n");
                writer.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Response processRequest(Request request, Connection conn) throws SQLException {
        DatabaseManager dbManager = new DatabaseManager(conn);
        UserDAO userDAO = new UserDAO(dbManager);
        LogDAO logDAO = new LogDAO(dbManager);
        AuthService authService = new AuthService(userDAO, logDAO);

        switch (request.getCommand()) {
            case "LOGIN":
                String[] creds = request.getData().split(":");
                String username = creds[0];
                String password = creds[1];
                User user = authService.login(username, password);
                if (user != null) {
                    return new Response("SUCCESS:" + user.getRole());
                }
                return new Response("FAIL");

            case "REGISTER":
                String[] regData = request.getData().split(":");
                String regUsername = regData[0];
                String regPassword = regData[1];
                boolean registered = authService.register(regUsername, regPassword, "guest");
                if (registered) {
                    return new Response("SUCCESS");
                }
                return new Response("FAIL");

            default:
                return new Response("FAIL: Unknown command");
        }
    }
}