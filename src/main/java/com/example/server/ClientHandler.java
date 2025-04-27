package com.example.server;

import com.example.server.model.*;
import com.example.server.service.AuthService;
import com.example.server.dao.*;
import com.example.server.service.ForecastService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final ObjectMapper objectMapper;
    private User currentUser;

    public ClientHandler(Socket socket, String dbUrl, String dbUser, String dbPassword) {
        this.socket = socket;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.objectMapper = new ObjectMapper();
        this.currentUser = null;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            String jsonRequest;
            while ((jsonRequest = reader.readLine()) != null) {
                System.out.println("Received request: " + jsonRequest);
                Request request = objectMapper.readValue(jsonRequest, Request.class);

                Response response = processRequest(request, conn, writer);
                String jsonResponse = objectMapper.writeValueAsString(response);
                System.out.println("Sending response: " + jsonResponse);
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

    private Response processRequest(Request request, Connection conn, BufferedWriter writer) throws SQLException, IOException {
        DatabaseManager dbManager = new DatabaseManager(conn);
        UserDAO userDAO = new UserDAO(dbManager);
        LogDAO logDAO = new LogDAO(dbManager);
        EpidemicDataDAO epidemicDataDAO = new EpidemicDataDAO(dbManager);
        ForecastDAO forecastDAO = new ForecastDAO(dbManager);
        AuthService authService = new AuthService(userDAO, logDAO);

        if (request.getCommand() == null) {
            return new Response("FAIL: Invalid request command");
        }

        try {
            switch (request.getCommand()) {
                case "LOGIN":
                    if (request.getData() == null) {
                        System.out.println("LOGIN failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    LoginData loginData = objectMapper.readValue(request.getData(), LoginData.class);
                    String username = loginData.getUsername();
                    String password = loginData.getPassword();
                    if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                        System.out.println("LOGIN failed: Invalid login data");
                        return new Response("FAIL: Invalid login data");
                    }
                    User user = authService.login(username, password);
                    if (user != null) {
                        this.currentUser = user;
                        System.out.println("LOGIN success for user: " + username);
                        return new Response("SUCCESS:" + user.getRole());
                    }
                    System.out.println("LOGIN failed: Invalid credentials for user: " + username);
                    return new Response("FAIL");

                case "REGISTER":
                    if (request.getData() == null) {
                        System.out.println("REGISTER failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    RegisterData regData = objectMapper.readValue(request.getData(), RegisterData.class);
                    String regUsername = regData.getUsername();
                    String regPassword = regData.getPassword();
                    String role = regData.getRole();
                    if (regUsername == null || regUsername.trim().isEmpty() || regPassword == null || regPassword.trim().isEmpty()) {
                        System.out.println("REGISTER failed: Invalid registration data");
                        return new Response("FAIL: Invalid registration data");
                    }
                    boolean registered = authService.register(regUsername, regPassword, role != null ? role : "guest");
                    if (registered) {
                        System.out.println("REGISTER success for user: " + regUsername);
                        return new Response("SUCCESS");
                    }
                    System.out.println("REGISTER failed: User already exists: " + regUsername);
                    return new Response("FAIL");

                case "SUBMIT_DATA":
                    if (currentUser == null) {
                        System.out.println("SUBMIT_DATA failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    if (request.getData() == null) {
                        System.out.println("SUBMIT_DATA failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    EpidemicData epidemicData = objectMapper.readValue(request.getData(), EpidemicData.class);
                    String submitRegion = epidemicData.getRegion();
                    String date = epidemicData.getDate();
                    int infected = epidemicData.getInfected();
                    if (submitRegion == null || submitRegion.trim().isEmpty() || date == null || date.trim().isEmpty() || infected < 0) {
                        System.out.println("SUBMIT_DATA failed: Invalid epidemic data");
                        return new Response("FAIL: Invalid epidemic data");
                    }
                    boolean saved = epidemicDataDAO.saveEpidemicData(currentUser.getUserId(), submitRegion, date, infected);
                    if (saved) {
                        System.out.println("SUBMIT_DATA success for user " + currentUser.getUsername());
                        return new Response("SUCCESS");
                    }
                    System.out.println("SUBMIT_DATA failed: Could not save to database");
                    return new Response("FAIL");

                case "GET_ALL_DATA":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("GET_ALL_DATA failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    List<EpidemicDataDAO.EpidemicDataWithUsername> allData = epidemicDataDAO.getAllData();
                    String allDataJson = objectMapper.writeValueAsString(allData);
                    System.out.println("GET_ALL_DATA success for user " + currentUser.getUsername());
                    return new Response("SUCCESS:" + allDataJson);

                case "DELETE_DATA":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("DELETE_DATA failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    if (request.getData() == null) {
                        System.out.println("DELETE_DATA failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    DeleteData deleteData = objectMapper.readValue(request.getData(), DeleteData.class);
                    int dataId = deleteData.getId();
                    boolean deleted = epidemicDataDAO.deleteData(dataId);
                    if (deleted) {
                        System.out.println("DELETE_DATA success for data ID " + dataId);
                        return new Response("SUCCESS");
                    }
                    System.out.println("DELETE_DATA failed for data ID " + dataId);
                    return new Response("FAIL");

                case "GET_ALL_USERS":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("GET_ALL_USERS failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    List<User> allUsers = userDAO.getAllUsers();
                    String usersJson = objectMapper.writeValueAsString(allUsers);
                    System.out.println("GET_ALL_USERS success for user " + currentUser.getUsername());
                    return new Response("SUCCESS:" + usersJson);

                case "FIND_USER":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("FIND_USER failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    if (request.getData() == null) {
                        System.out.println("FIND_USER failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    String searchUsername = objectMapper.readValue(request.getData(), String.class);
                    User foundUser = userDAO.findByUsername(searchUsername);
                    if (foundUser != null) {
                        String userJson = objectMapper.writeValueAsString(foundUser);
                        System.out.println("FIND_USER success for username " + searchUsername);
                        return new Response("SUCCESS:" + userJson);
                    }
                    System.out.println("FIND_USER failed: User not found - " + searchUsername);
                    return new Response("FAIL: User not found");

                case "DELETE_USER":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("DELETE_USER failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    if (request.getData() == null) {
                        System.out.println("DELETE_USER failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    int userId = objectMapper.readValue(request.getData(), Integer.class);
                    if (userId == currentUser.getUserId()) {
                        System.out.println("DELETE_USER failed: Cannot delete admin");
                        return new Response("FAIL: Cannot delete admin");
                    }
                    boolean userDeleted = userDAO.deleteUser(userId);
                    if (userDeleted) {
                        System.out.println("DELETE_USER success for user ID " + userId);
                        return new Response("SUCCESS");
                    }
                    System.out.println("DELETE_USER failed for user ID " + userId);
                    return new Response("FAIL");

                case "ADD_USER":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("ADD_USER failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    if (request.getData() == null) {
                        System.out.println("ADD_USER failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    RegisterData addUserData = objectMapper.readValue(request.getData(), RegisterData.class);
                    String newUsername = addUserData.getUsername();
                    String newPassword = addUserData.getPassword();
                    String newRole = addUserData.getRole();
                    if (newUsername == null || newUsername.trim().isEmpty() || newPassword == null || newPassword.trim().isEmpty()) {
                        System.out.println("ADD_USER failed: Invalid user data");
                        return new Response("FAIL: Invalid user data");
                    }
                    boolean added = authService.register(newUsername, newPassword, newRole != null ? newRole : "guest");
                    if (added) {
                        System.out.println("ADD_USER success for user " + newUsername);
                        return new Response("SUCCESS");
                    }
                    System.out.println("ADD_USER failed: User already exists - " + newUsername);
                    return new Response("FAIL: User already exists");

                case "ADMIN_SUBMIT_DATA":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("ADMIN_SUBMIT_DATA failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    if (request.getData() == null) {
                        System.out.println("ADMIN_SUBMIT_DATA failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    EpidemicData adminEpidemicData = objectMapper.readValue(request.getData(), EpidemicData.class);
                    String adminRegion = adminEpidemicData.getRegion();
                    String adminDate = adminEpidemicData.getDate();
                    int adminInfected = adminEpidemicData.getInfected();
                    if (adminRegion == null || adminRegion.trim().isEmpty() || adminDate == null || adminDate.trim().isEmpty() || adminInfected < 0) {
                        System.out.println("ADMIN_SUBMIT_DATA failed: Invalid epidemic data");
                        return new Response("FAIL: Invalid epidemic data");
                    }
                    boolean adminSaved = epidemicDataDAO.saveEpidemicData(currentUser.getUserId(), adminRegion, adminDate, adminInfected);
                    if (adminSaved) {
                        System.out.println("ADMIN_SUBMIT_DATA success for user " + currentUser.getUsername());
                        return new Response("SUCCESS");
                    }
                    System.out.println("ADMIN_SUBMIT_DATA failed: Could not save to database");
                    return new Response("FAIL");

                case "GET_LOGS":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("GET_LOGS failed: Access denied (currentUser=" + (currentUser != null ? currentUser.getUsername() + ", role=" + currentUser.getRole() : "null") + ")");
                        return new Response("FAIL: Access denied");
                    }
                    List<Log> logs = logDAO.getAllLogs();
                    String logsJson = objectMapper.writeValueAsString(logs);
                    System.out.println("GET_LOGS success for user " + currentUser.getUsername());
                    return new Response("SUCCESS:" + logsJson);

                case "GET_STATISTICS":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("GET_STATISTICS failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    List<EpidemicDataDAO.Statistics> statistics = epidemicDataDAO.getStatistics();
                    String statsJson = objectMapper.writeValueAsString(statistics);
                    System.out.println("GET_STATISTICS success for user " + currentUser.getUsername());
                    return new Response("SUCCESS:" + statsJson);

                case "GET_EPIDEMIC_DATA":
                    if (currentUser == null) {
                        System.out.println("GET_EPIDEMIC_DATA failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    if (request.getData() == null) {
                        System.out.println("GET_EPIDEMIC_DATA failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    String region = objectMapper.readValue(request.getData(), String.class);
                    List<EpidemicData> data = epidemicDataDAO.getDataByRegion(region);
                    String dataJson = objectMapper.writeValueAsString(data);
                    System.out.println("GET_EPIDEMIC_DATA success for region: " + region);
                    return new Response("SUCCESS:" + dataJson);

                case "FORECAST":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("FORECAST failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    if (request.getData() == null) {
                        System.out.println("FORECAST failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    try {
                        ForecastRequest forecastRequest = objectMapper.readValue(request.getData(), ForecastRequest.class);
                        String forecastRegion = forecastRequest.getRegion();
                        int forecastPeriod = forecastRequest.getPeriod();
                        List<EpidemicData> historicalData = epidemicDataDAO.getDataByRegion(forecastRegion);
                        if (historicalData.isEmpty()) {
                            System.out.println("FORECAST failed: No data for region: " + forecastRegion);
                            return new Response("FAIL: No data for region: " + forecastRegion);
                        }
                        ForecastService forecastService = new ForecastService();
                        List<ForecastService.ForecastResult> forecastResults = forecastService.forecastInfections(historicalData, forecastPeriod);
                        forecastDAO.saveForecast(currentUser.getUserId(), forecastRegion, forecastResults); // Передаём userId
                        System.out.println("FORECAST success for region: " + forecastRegion);
                        String forecastJson = objectMapper.writeValueAsString(forecastResults);
                        return new Response("SUCCESS:" + forecastJson);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error processing request FORECAST: " + e.getMessage());
                        return new Response("FAIL: Error processing request - " + e.getMessage());
                    }

                case "GET_FORECASTS":
                    List<ForecastDAO.Forecast> forecasts = forecastDAO.getAllForecasts();
                    String forecastsJson = objectMapper.writeValueAsString(forecasts);
                    System.out.println("GET_FORECASTS success");
                    return new Response("SUCCESS:" + forecastsJson);

                default:
                    System.out.println("Unknown command: " + request.getCommand());
                    return new Response("FAIL: Unknown command");
            }
        } catch (Exception e) {
            System.out.println("Error processing request " + request.getCommand() + ": " + e.getMessage());
            return new Response("FAIL: Error processing request - " + e.getMessage());
        }
    }

    public static class ForecastRequest {
        private String region;
        private int period;

        @JsonCreator
        public ForecastRequest(
                @JsonProperty("region") String region,
                @JsonProperty("period") int period) {
            this.region = region;
            this.period = period;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) { // Добавляем сеттер
            this.region = region;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) { // Добавляем сеттер
            this.period = period;
        }
    }
}