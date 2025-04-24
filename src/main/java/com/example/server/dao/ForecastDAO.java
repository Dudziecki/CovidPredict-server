package com.example.server.dao;



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

    public void saveForecast(String region, String forecastData) throws SQLException {
        String sql = "INSERT INTO forecasts (region, forecast_data) VALUES (?, ?::jsonb)";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setString(1, region);
            stmt.setString(2, forecastData);
            stmt.executeUpdate();
        }
    }


    public List<Forecast> getAllForecasts() throws SQLException {
        List<Forecast> forecasts = new ArrayList<>();
        String sql = "SELECT * FROM forecasts ORDER BY created_at DESC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Forecast forecast = new Forecast();
                forecast.setId(rs.getInt("id"));
                forecast.setRegion(rs.getString("region"));
                forecast.setForecastData(rs.getString("forecast_data"));
                forecast.setCreatedAt(rs.getTimestamp("created_at").toString());
                forecasts.add(forecast);
            }
        }
        return forecasts;
    }

    public static class Forecast {
        private int id;
        private String region;
        private String forecastData;
        private String createdAt;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getForecastData() { return forecastData; }
        public void setForecastData(String forecastData) { this.forecastData = forecastData; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
}