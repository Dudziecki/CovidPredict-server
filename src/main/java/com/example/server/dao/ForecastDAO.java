package com.example.server.dao;



import com.example.server.service.ForecastService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ForecastDAO {
    private final DatabaseManager dbManager;

    public ForecastDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void saveForecast(int userId, String region, List<ForecastService.ForecastResult> forecastResults) throws SQLException {
        int regionId = getRegionIdByName(region);
        String sql = "INSERT INTO forecasts (region_id, forecast_date, predicted_cases, created_by, created_at) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            for (ForecastService.ForecastResult result : forecastResults) {
                stmt.setInt(1, regionId);
                // Преобразуем строку даты в java.sql.Date
                java.sql.Date forecastDate = java.sql.Date.valueOf(result.getDate());
                stmt.setDate(2, forecastDate);
                stmt.setInt(3, (int) result.getPredictedInfected());
                stmt.setInt(4, userId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }


    public List<Forecast> getAllForecasts() throws SQLException {
        List<Forecast> forecasts = new ArrayList<>();
        String sql = "SELECT f.*, r.name AS region_name " +
                "FROM forecasts f " +
                "JOIN regions r ON f.region_id = r.id " +
                "ORDER BY f.created_at DESC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Forecast forecast = new Forecast();
                forecast.setId(rs.getInt("forecast_id"));
                forecast.setRegionId(rs.getInt("region_id"));
                forecast.setForecastDate(rs.getString("forecast_date"));
                forecast.setPredictedCases(rs.getInt("predicted_cases"));
                forecast.setCreatedBy(rs.getInt("created_by"));
                forecast.setCreatedAt(rs.getTimestamp("created_at").toString());
                forecasts.add(forecast);
            }
        }
        return forecasts;
    }
    public int getRegionIdByName(String regionName) throws SQLException {
        String sql = "SELECT region_id FROM regions WHERE region_name = ?"; // Изменили id на region_id
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setString(1, regionName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("region_id"); // Изменили id на region_id
            } else {
                throw new SQLException("Region not found: " + regionName);
            }
        }
    }

    public static class Forecast {
        private int id;
        private int regionId; // Изменили на regionId
        private String forecastDate; // Дата прогноза
        private int predictedCases; // Прогнозируемое число заражённых
        private int createdBy; // ID пользователя, создавшего прогноз
        private String createdAt;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getRegionId() { return regionId; }
        public void setRegionId(int regionId) { this.regionId = regionId; }
        public String getForecastDate() { return forecastDate; }
        public void setForecastDate(String forecastDate) { this.forecastDate = forecastDate; }
        public int getPredictedCases() { return predictedCases; }
        public void setPredictedCases(int predictedCases) { this.predictedCases = predictedCases; }
        public int getCreatedBy() { return createdBy; }
        public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}