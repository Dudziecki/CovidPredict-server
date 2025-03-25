package com.example.server;

import com.example.server.dao.*;
import com.example.server.model.*;
import com.example.server.service.AuthService;
import com.example.server.service.PredictionService;

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

    public ClientHandler(Socket socket, String dbUrl, String dbUser, String dbPassword) {
        this.socket = socket;
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {

            Request request = (Request) in.readObject();
            Response response = processRequest(request, conn);
            out.writeObject(response);

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
        RegionDAO regionDAO = new RegionDAO(dbManager);
        CaseDAO caseDAO = new CaseDAO(dbManager);
        ForecastDAO forecastDAO = new ForecastDAO(dbManager);
        LogDAO logDAO = new LogDAO(dbManager);
        SettingDAO settingDAO = new SettingDAO(dbManager);
        AuthService authService = new AuthService(userDAO, logDAO);
        PredictionService predictionService = new PredictionService(caseDAO, forecastDAO);

        switch (request.getCommand()) {
            // 1. Авторизация
            case "LOGIN":
                String[] creds = request.getData().split(":");
                String username = creds[0];
                String password = creds[1];
                User user = authService.login(username, password);
                if (user != null) {
                    return new Response("SUCCESS:" + user.getRole());
                }
                return new Response("FAIL");

            // 2. Регистрация
            case "REGISTER":
                String[] regData = request.getData().split(":");
                String regUsername = regData[0];
                String regPassword = regData[1];
                boolean registered = authService.register(regUsername, regPassword, "guest");
                if (registered) {
                    return new Response("SUCCESS");
                }
                return new Response("FAIL");

            // 3. Получение списка регионов
            case "GET_REGIONS":
                List<Region> regions = regionDAO.getAllRegions();
                return new Response(regions.toString());

            // 4. Получение статистики по региону
            case "GET_CASES_BY_REGION":
                int regionId = Integer.parseInt(request.getData());
                List<Case> cases = caseDAO.getCasesByRegion(regionId);
                return new Response(cases.toString());

            // 5. Прогнозирование заболеваемости
            case "PREDICT":
                String[] predictData = request.getData().split(":");
                int predictRegionId = Integer.parseInt(predictData[0]);
                int userId = Integer.parseInt(predictData[1]);
                double predictedCases = predictionService.predictCases(predictRegionId);
                Forecast forecast = new Forecast(0, predictRegionId, new java.sql.Date(System.currentTimeMillis()), (int) predictedCases, userId);
                forecastDAO.addForecast(forecast);
                return new Response("Predicted cases: " + predictedCases);

            // 6. Получение списка прогнозов
            case "GET_FORECASTS":
                List<Forecast> forecasts = forecastDAO.getAllForecasts();
                return new Response(forecasts.toString());

            // 7. Добавление нового региона (для администратора)
            case "ADD_REGION":
                String[] regionData = request.getData().split(":");
                String regionName = regionData[0];
                String role = regionData[1];
                if (!role.equals("admin")) {
                    return new Response("FAIL: Permission denied");
                }
                Region region = new Region(0, regionName);
                regionDAO.addRegion(region);
                return new Response("SUCCESS");

            // 8. Удаление региона (для администратора)
            case "DELETE_REGION":
                String[] deleteRegionData = request.getData().split(":");
                int regionIdToDelete = Integer.parseInt(deleteRegionData[0]);
                String deleteRole = deleteRegionData[1];
                if (!deleteRole.equals("admin")) {
                    return new Response("FAIL: Permission denied");
                }
                regionDAO.deleteRegion(regionIdToDelete);
                return new Response("SUCCESS");

            // 9. Получение логов (для администратора)
            case "GET_LOGS":
                String logRole = request.getData();
                if (!logRole.equals("admin")) {
                    return new Response("FAIL: Permission denied");
                }
                List<Log> logs = logDAO.getAllLogs();
                return new Response(logs.toString());

            // 10. Получение настроек системы (для администратора)
            case "GET_SETTINGS":
                String settingRole = request.getData();
                if (!settingRole.equals("admin")) {
                    return new Response("FAIL: Permission denied");
                }
                List<Setting> settings = settingDAO.getAllSettings();
                return new Response(settings.toString());

            // 11. Обновление настроек системы (для администратора)
            case "UPDATE_SETTING":
                String[] settingData = request.getData().split(":");
                String settingName = settingData[0];
                String settingValue = settingData[1];
                String updateRole = settingData[2];
                if (!updateRole.equals("admin")) {
                    return new Response("FAIL: Permission denied");
                }
                settingDAO.updateSetting(settingName, settingValue);
                return new Response("SUCCESS");

            // 12. Добавление статистики по региону (для сотрудника)
            case "ADD_CASE":
                String[] caseData = request.getData().split(":");
                int caseRegionId = Integer.parseInt(caseData[0]);
                java.sql.Date caseDate = java.sql.Date.valueOf(caseData[1]);
                int confirmed = Integer.parseInt(caseData[2]);
                int deaths = Integer.parseInt(caseData[3]);
                int recovered = Integer.parseInt(caseData[4]);
                String caseRole = caseData[5];
                if (!caseRole.equals("employee")) {
                    return new Response("FAIL: Permission denied");
                }
                Case newCase = new Case(0, caseRegionId, caseDate, confirmed, deaths, recovered);
                caseDAO.addCase(newCase);
                return new Response("SUCCESS");

            // 13. Получение статистики за период
            case "GET_CASES_BY_DATE_RANGE":
                String[] dateRangeData = request.getData().split(":");
                java.sql.Date startDate = java.sql.Date.valueOf(dateRangeData[0]);
                java.sql.Date endDate = java.sql.Date.valueOf(dateRangeData[1]);
                List<Case> casesByDate = caseDAO.getCasesByDateRange(startDate, endDate);
                return new Response(casesByDate.toString());

            default:
                return new Response("Unknown command");
        }
    }
}