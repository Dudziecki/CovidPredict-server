package com.example.server;

import com.example.server.dto.CompareDataRequest;
import com.example.server.dto.CompareDataResponse;
import com.example.server.dto.ForecastDTO;
import com.example.server.model.*;
import com.example.server.service.AuthService;
import com.example.server.dao.*;
import com.example.server.service.ForecastService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.server.model.SupportMessage;

import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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

                case "GET_ALL_DATA": {
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("GET_ALL_DATA failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    List<EpidemicDataDAO.EpidemicDataWithUsername> allData = epidemicDataDAO.getAllData();
                    String allDataJson = objectMapper.writeValueAsString(allData);
                    System.out.println("GET_ALL_DATA success for user " + currentUser.getUsername());
                    return new Response("SUCCESS:" + allDataJson);
                }

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

                case "GET_EPIDEMIC_DATA": {
                    if (currentUser == null) {
                        System.out.println("GET_EPIDEMIC_DATA failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    if (request.getData() == null) {
                        System.out.println("GET_EPIDEMIC_DATA failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    String region = objectMapper.readValue(request.getData(), String.class);
                    RegionDAO regionDAO = new RegionDAO(dbManager);
                    int regionId = regionDAO.getRegionIdByName(region);
                    if (regionId == -1) {
                        System.out.println("GET_EPIDEMIC_DATA failed: Region not found - " + region);
                        return new Response("FAIL: Region not found");
                    }
                    List<EpidemicData> data = epidemicDataDAO.getDataByRegion(regionId);
                    String dataJson = objectMapper.writeValueAsString(data);
                    System.out.println("GET_EPIDEMIC_DATA success for region: " + region);
                    return new Response("SUCCESS:" + dataJson);
                }

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
                        int regionId = forecastRequest.getRegionId();
                        int forecastPeriod = forecastRequest.getPeriod();

                        List<EpidemicData> historicalData = epidemicDataDAO.getDataByRegion(regionId);
                        if (historicalData.isEmpty()) {
                            System.out.println("FORECAST failed: No data for region_id: " + regionId);
                            return new Response("FAIL: No data for region_id: " + regionId);
                        }

                        ForecastService forecastService = new ForecastService();
                        List<ForecastService.ForecastResult> forecastResults = forecastService.forecastInfections(historicalData, forecastPeriod);

                        RegionDAO regionDAO = new RegionDAO(dbManager);
                        String regionName = regionDAO.getRegionNameById(regionId);
                        if (regionName == null) {
                            return new Response("FAIL: Region not found for region_id: " + regionId);
                        }

                        forecastDAO.saveForecast(currentUser.getUserId(), regionName, forecastResults);

                        List<ForecastDTO> forecastDTOs = forecastResults.stream()
                                .map(fr -> new ForecastDTO(fr.getForecastDate(), (int) fr.getPredictedCases(), fr.getCreatedAt(), fr.getRegionName()))
                                .collect(Collectors.toList());

                        System.out.println("FORECAST success for region: " + regionName);
                        String forecastJson = objectMapper.writeValueAsString(forecastDTOs);
                        return new Response("SUCCESS:" + forecastJson);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error processing request FORECAST: " + e.getMessage());
                        return new Response("FAIL: Error processing request - " + e.getMessage());
                    }
                case "GET_FORECASTS": {
                    List<ForecastDAO.Forecast> forecasts = forecastDAO.getAllForecasts();
                    List<ForecastDTO> forecastDTOs = forecasts.stream()
                            .map(f -> new ForecastDTO(f.getForecastDate(), f.getPredictedCases(), f.getCreatedAt(), f.getRegionName()))
                            .collect(Collectors.toList());
                    String forecastsJson = objectMapper.writeValueAsString(forecastDTOs);
                    System.out.println("GET_FORECASTS success");
                    return new Response("SUCCESS:" + forecastsJson);
                }
                case "COMPARE_DATA": {
                    CompareDataRequest compareDataRequest = objectMapper.readValue(request.getData(), CompareDataRequest.class);
                    RegionDAO regionDAO = new RegionDAO(dbManager);
                    int regionId = regionDAO.getRegionIdByName(compareDataRequest.getRegion());
                    if (regionId == -1) {
                        return new Response("FAIL: Region not found");
                    }
                    List<EpidemicData> historicalData = epidemicDataDAO.getDataByRegionAndDateRange(
                            compareDataRequest.getRegion(),
                            compareDataRequest.getStartDate(),
                            compareDataRequest.getEndDate()
                    );
                    List<ForecastDAO.Forecast> forecastData = forecastDAO.getForecastsByRegionAndDateRange(
                            compareDataRequest.getRegion(),
                            compareDataRequest.getStartDate(),
                            compareDataRequest.getEndDate()
                    );
                    List<ForecastDTO> forecastDTO = forecastData.stream()
                            .map(f -> new ForecastDTO(f.getForecastDate(), f.getPredictedCases(), f.getCreatedAt(), f.getRegionName()))
                            .collect(Collectors.toList());
                    CompareDataResponse compareDataResponse = new CompareDataResponse();
                    compareDataResponse.setHistoricalData(historicalData);
                    compareDataResponse.setForecasts(forecastDTO);
                    String compareDataJson = objectMapper.writeValueAsString(compareDataResponse);
                    return new Response("SUCCESS:" + compareDataJson);
                }

                case "GET_USER_MESSAGES":
                    if (currentUser == null) {
                        System.out.println("GET_USER_MESSAGES failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    List<SupportMessage> messages = dbManager.getUserMessages(currentUser.getUsername());
                    dbManager.markMessagesAsRead(currentUser.getUsername()); // Помечаем как прочитанные
                    String messagesJson = objectMapper.writeValueAsString(messages);
                    return new Response("SUCCESS:" + messagesJson);

                case "GET_UNREAD_MESSAGES_COUNT":
                    if (currentUser == null) {
                        System.out.println("GET_UNREAD_MESSAGES_COUNT failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    int unreadCount = dbManager.getUnreadMessagesCount(currentUser.getUsername());
                    System.out.println("GET_UNREAD_MESSAGES_COUNT success for user " + currentUser.getUsername() + ": " + unreadCount);
                    return new Response("SUCCESS:" + unreadCount);

                case "SEND_MESSAGE":
                    if (currentUser == null) {
                        System.out.println("SEND_MESSAGE failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    String messageText = objectMapper.readValue(request.getData(), String.class);
                    com.example.server.model.SupportMessage message = new SupportMessage();
                    message.setMessage(messageText);
                    message.setStatus("PENDING");
                    message.setCreatedAt(LocalDate.now().toString());
                    dbManager.saveSupportMessage(currentUser.getUsername(), message);
                    return new Response("SUCCESS");

                case "GET_ALL_MESSAGES":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("GET_ALL_MESSAGES failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    List<SupportMessage> allMessages = dbManager.getAllMessages();
                    String allMessagesJson = objectMapper.writeValueAsString(allMessages);
                    return new Response("SUCCESS:" + allMessagesJson);

                case "RESPOND_MESSAGE":
                    if (currentUser == null || !"admin".equals(currentUser.getRole())) {
                        System.out.println("RESPOND_MESSAGE failed: Access denied");
                        return new Response("FAIL: Access denied");
                    }
                    SupportMessage updatedMessage = objectMapper.readValue(request.getData(), SupportMessage.class);
                    dbManager.updateSupportMessage(updatedMessage);
                    return new Response("SUCCESS");
                case "GET_REGION_RANKING":
                    if (currentUser == null) {
                        System.out.println("GET_REGION_RANKING failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    List<RegionRanking> rankings = epidemicDataDAO.getRegionRanking(5); // Топ-5 регионов
                    String rankingsJson = objectMapper.writeValueAsString(rankings);
                    System.out.println("GET_REGION_RANKING success for user " + currentUser.getUsername());
                    return new Response("SUCCESS:" + rankingsJson);
                case "EXPORT_DATA":
                    if (currentUser == null) {
                        System.out.println("EXPORT_DATA failed: User not authenticated");
                        return new Response("FAIL: User not authenticated");
                    }
                    if (request.getData() == null) {
                        System.out.println("EXPORT_DATA failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    try {
                        ExportRequest exportRequest = objectMapper.readValue(request.getData(), ExportRequest.class);
                        String dataType = exportRequest.getDataType();
                        String region = exportRequest.getRegion();

                        RegionDAO regionDAO = new RegionDAO(dbManager);
                        StringBuilder csv = new StringBuilder();
                        if ("historical".equals(dataType)) {
                            if ("all".equals(region)) {
                                List<EpidemicDataDAO.EpidemicDataWithUsername> allData = epidemicDataDAO.getAllData();
                                csv.append("Date,Region,Infected,SubmittedBy\n");
                                for (EpidemicDataDAO.EpidemicDataWithUsername d : allData) {
                                    csv.append(String.format("%s,%s,%d,%s\n", d.getDate(), d.getRegion(), d.getInfected(), d.getUsername()));
                                }
                            } else {
                                int regionId = regionDAO.getRegionIdByName(region);
                                if (regionId == -1) {
                                    return new Response("FAIL: Region not found");
                                }
                                List<EpidemicData> historicalData = epidemicDataDAO.getDataByRegion(regionId);
                                csv.append("Date,Region,Infected\n");
                                for (EpidemicData d : historicalData) {
                                    csv.append(String.format("%s,%s,%d\n", d.getDate(), d.getRegion(), d.getInfected()));
                                }
                            }
                        } else if ("forecast".equals(dataType)) {
                            List<ForecastDAO.Forecast> forecasts;
                            if ("all".equals(region)) {
                                forecasts = forecastDAO.getAllForecasts();
                            } else {
                                int regionId = regionDAO.getRegionIdByName(region);
                                if (regionId == -1) {
                                    return new Response("FAIL: Region not found");
                                }
                                forecasts = forecastDAO.getAllForecasts().stream()
                                        .filter(f -> f.getRegionId() == regionId)
                                        .collect(Collectors.toList());
                            }
                            csv.append("Forecast Date,Region,Predicted Cases,Created At\n");
                            for (ForecastDAO.Forecast f : forecasts) {
                                csv.append(String.format("%s,%s,%d,%s\n", f.getForecastDate(), f.getRegion(), f.getPredictedCases(), f.getCreatedAt()));
                            }
                        } else {
                            return new Response("FAIL: Invalid data type");
                        }

                        System.out.println("EXPORT_DATA success for user " + currentUser.getUsername());
                        return new Response("SUCCESS:" + csv.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return new Response("FAIL: Error processing request - " + e.getMessage());
                    }

                case "GET_REGION_ID":
                    if (request.getData() == null) {
                        System.out.println("GET_REGION_ID failed: No data provided");
                        return new Response("FAIL: No data provided");
                    }
                    try {
                        String region = objectMapper.readValue(request.getData(), String.class);
                        if (region == null || region.trim().isEmpty()) {
                            System.out.println("GET_REGION_ID failed: Invalid region name");
                            return new Response("FAIL: Invalid region name");
                        }
                        RegionDAO regionDAO = new RegionDAO(dbManager);
                        int regionId = regionDAO.getRegionIdByName(region);
                        if (regionId == -1) {
                            System.out.println("GET_REGION_ID failed: Region not found - " + region);
                            return new Response("FAIL: Region not found");
                        }
                        System.out.println("GET_REGION_ID success for region: " + region + ", region_id: " + regionId);
                        return new Response("SUCCESS:" + regionId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Error processing GET_REGION_ID: " + e.getMessage());
                        return new Response("FAIL: Error processing request - " + e.getMessage());
                    }
                default:
                    System.out.println("Unknown command: " + request.getCommand());
                    return new Response("FAIL: Unknown command");
            }
        } catch (Exception e) {
            System.out.println("Error processing request " + request.getCommand() + ": " + e.getMessage());
            return new Response("FAIL: Error processing request - " + e.getMessage());
        }
    }

    public static class RegionRanking {
        private String region;
        private int totalInfected;

        public RegionRanking(String region, int totalInfected) {
            this.region = region;
            this.totalInfected = totalInfected;
        }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public int getTotalInfected() { return totalInfected; }
        public void setTotalInfected(int totalInfected) { this.totalInfected = totalInfected; }
    }

    public static class ExportRequest {
        private String dataType; // "historical" или "forecast"
        private String region;

        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    public static class ForecastRequest {
        private int regionId;
        private int period;

        @JsonCreator
        public ForecastRequest(
                @JsonProperty("regionId") int regionId, // Изменяем на regionId
                @JsonProperty("period") int period) {
            this.regionId = regionId;
            this.period = period;
        }

        public int getRegionId() {
            return regionId;
        }

        public void setRegionId(int regionId) { // Переименовываем сеттер
            this.regionId = regionId;
        }

        public int getPeriod() {
            return period;
        }

        public void setPeriod(int period) {
            this.period = period;
        }
    }
}