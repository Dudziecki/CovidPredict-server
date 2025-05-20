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
        if (regionId == -1) {
            throw new SQLException("Region not found: " + region);
        }
        String sql = "INSERT INTO forecasts (region_id, forecast_date, predicted_cases, created_by, created_at) VALUES (?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            for (ForecastService.ForecastResult result : forecastResults) {
                stmt.setInt(1, regionId);
                String forecastDateStr = result.getForecastDate(); // Теперь строка в формате yyyy-MM-dd
                stmt.setString(2, forecastDateStr);
                stmt.setInt(3, (int) result.getPredictedCases()); // Приводим double к int
                stmt.setInt(4, userId);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<Forecast> getAllForecasts() throws SQLException {
        List<Forecast> forecasts = new ArrayList<>();
        String sql = "SELECT f.*, r.region_name AS region_name " +
                "FROM forecasts f " +
                "JOIN regions r ON f.region_id = r.region_id " +
                "ORDER BY f.created_at DESC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Forecast forecast = new Forecast();
                forecast.setId(rs.getInt("forecast_id"));
                forecast.setRegionId(rs.getInt("region_id"));
                forecast.setRegionName(rs.getString("region_name"));
                forecast.setForecastDate(rs.getString("forecast_date"));
                forecast.setPredictedCases(rs.getInt("predicted_cases"));
                forecast.setCreatedBy(rs.getInt("created_by"));
                forecast.setCreatedAt(rs.getTimestamp("created_at").toString());
                forecasts.add(forecast);
            }
        }
        return forecasts;
    }

    public List<Forecast> getForecastsByRegionAndDateRange(String region, String startDate, String endDate) throws SQLException {
        List<Forecast> forecasts = new ArrayList<>();
        int regionId = getRegionIdByName(region);
        String sql = "SELECT f.*, r.region_name AS region_name " +
                "FROM forecasts f " +
                "JOIN regions r ON f.region_id = r.region_id " +
                "WHERE f.region_id = ? AND f.forecast_date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD')";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, regionId);
            stmt.setString(2, startDate);
            stmt.setString(3, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Forecast forecast = new Forecast();
                forecast.setId(rs.getInt("forecast_id"));
                forecast.setRegionId(rs.getInt("region_id"));
                forecast.setRegionName(rs.getString("region_name"));
                forecast.setForecastDate(rs.getString("forecast_date"));
                forecast.setPredictedCases(rs.getInt("predicted_cases"));
                forecast.setCreatedBy(rs.getInt("created_by"));
                forecast.setCreatedAt(rs.getTimestamp("created_at").toString());
                forecasts.add(forecast);
            }
        }
        return forecasts;
    }

    // В ForecastDAO.java
    public int getRegionIdByName(String regionName) throws SQLException {
        String sql = "SELECT region_id FROM regions WHERE region_name = ?";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setString(1, regionName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("region_id");
            }
            return -1; // Возвращаем -1, если регион не найден
        }
    }

    public static class Forecast {
        private int id;
        private int regionId;
        private String regionName; // Добавляем для передачи имени региона
        private String forecastDate;
        private int predictedCases;
        private int createdBy;
        private String createdAt;
        private String region;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getRegionId() { return regionId; }
        public void setRegionId(int regionId) { this.regionId = regionId; }
        public String getRegionName() { return regionName; }
        public void setRegionName(String regionName) { this.regionName = regionName; }
        public String getForecastDate() { return forecastDate; }
        public void setForecastDate(String forecastDate) { this.forecastDate = forecastDate; }
        public int getPredictedCases() { return predictedCases; }
        public void setPredictedCases(int predictedCases) { this.predictedCases = predictedCases; }
        public int getCreatedBy() { return createdBy; }
        public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getRegion() { return region; } // Добавляем геттер
        public void setRegion(String region) { this.region = region; }
    }
}