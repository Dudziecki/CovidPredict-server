package com.example.server.dao;

import com.example.server.model.Forecast;

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

    public void addForecast(Forecast forecast) throws SQLException {
        String sql = "INSERT INTO forecasts (region_id, forecast_date, predicted_cases, created_by) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setInt(1, forecast.getRegionId());
        stmt.setDate(2, forecast.getForecastDate());
        stmt.setInt(3, forecast.getPredictedCases());
        stmt.setInt(4, forecast.getCreatedBy());
        stmt.executeUpdate();
    }

    public List<Forecast> getAllForecasts() throws SQLException {
        List<Forecast> forecasts = new ArrayList<>();
        String sql = "SELECT * FROM forecasts";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            Forecast forecast = new Forecast(rs.getInt("forecast_id"), rs.getInt("region_id"),
                    rs.getDate("forecast_date"), rs.getInt("predicted_cases"), rs.getInt("created_by"));
            forecast = new Forecast(rs.getInt("forecast_id"), rs.getInt("region_id"),
                    rs.getDate("forecast_date"), rs.getInt("predicted_cases"), rs.getInt("created_by"));
            forecasts.add(forecast);
        }
        return forecasts;
    }
}