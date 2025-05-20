package com.example.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final ExecutorService threadPool;

    public Server(String configFile) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
        }
        this.port = Integer.parseInt(props.getProperty("port"));
        this.dbUrl = props.getProperty("db.url");
        this.dbUser = props.getProperty("db.user");
        this.dbPassword = props.getProperty("db.password");
        int poolSize = Integer.parseInt(props.getProperty("thread.pool.size"));
        this.threadPool = Executors.newFixedThreadPool(poolSize);
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                threadPool.submit(new ClientHandler(clientSocket, dbUrl, dbUser, dbPassword));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            Server server = new Server("src/main/resources/com/example/server/server.properties");
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}