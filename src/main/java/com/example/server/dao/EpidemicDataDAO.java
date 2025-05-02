package com.example.server.dao;

import com.example.server.ClientHandler;
import com.example.server.model.EpidemicData;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EpidemicDataDAO {
    private final DatabaseManager dbManager;

    public EpidemicDataDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean saveEpidemicData(int userId, String region, String date, int infected) throws SQLException {
        String sql = "INSERT INTO epidemic_data (user_id, region, date, infected) VALUES (?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?)";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, region);
            stmt.setString(3, date); // Передаём строку, которая будет преобразована в DATE
            stmt.setInt(4, infected);
            return stmt.executeUpdate() > 0;
        }
    }
    public List<EpidemicData> getDataByRegion(int regionId) throws SQLException {
        List<EpidemicData> dataList = new ArrayList<>();
        String sql = "SELECT ed.* FROM epidemic_data ed " +
                "JOIN regions r ON ed.region = r.region_name " +
                "WHERE r.region_id = ? ORDER BY ed.date ASC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, regionId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                EpidemicData data = new EpidemicData();
                data.setId(rs.getInt("id"));
                data.setRegion(rs.getString("region"));
                data.setDate(rs.getString("date"));
                data.setInfected(rs.getInt("infected"));
                dataList.add(data);
            }
        }
        return dataList;
    }
    public List<Statistics> getStatistics() throws SQLException {
        List<Statistics> statistics = new ArrayList<>();
        String sql = "SELECT region, AVG(infected) as avg_infected " +
                "FROM epidemic_data " +
                "GROUP BY region " +
                "ORDER BY avg_infected DESC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Statistics stat = new Statistics(rs.getString("region"), rs.getDouble("avg_infected"));
                stat.setRegion(rs.getString("region"));
                stat.setAvgInfected(rs.getDouble("avg_infected"));
                statistics.add(stat);
            }
        }
        return statistics;
    }

    public static class Statistics {
        private String region;
        private double avgInfected;

        @JsonCreator
        public Statistics(
                @JsonProperty("region") String region,
                @JsonProperty("avgInfected") double avgInfected) {
            this.region = region;
            this.avgInfected = avgInfected;
        }

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public double getAvgInfected() { return avgInfected; }
        public void setAvgInfected(double avgInfected) { this.avgInfected = avgInfected; }
    }

    public List<EpidemicDataWithUsername> getAllData() throws SQLException {
        String sql = "SELECT ed.id, u.username, ed.region, ed.date, ed.infected " +
                "FROM epidemic_data ed " +
                "JOIN users u ON ed.user_id = u.id";
        List<EpidemicDataWithUsername> dataList = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                dataList.add(new EpidemicDataWithUsername(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("region"),
                        rs.getString("date"),
                        rs.getInt("infected")
                ));
            }
            return dataList;
        }
    }
    public List<EpidemicData> getDataByRegionAndDateRange(String region, String startDate, String endDate) throws SQLException {
        List<EpidemicData> data = new ArrayList<>();
        RegionDAO regionDAO = new RegionDAO(dbManager);
        int regionId = regionDAO.getRegionIdByName(region);
        if (regionId == -1) {
            throw new SQLException("Region not found: " + region);
        }
        String sql = "SELECT * FROM epidemic_data WHERE region = ? AND date BETWEEN TO_DATE(?, 'YYYY-MM-DD') AND TO_DATE(?, 'YYYY-MM-DD') ORDER BY date ASC";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setString(1, region); // Используем имя региона напрямую, если в таблице epidemic_data есть поле region
            stmt.setString(2, startDate);
            stmt.setString(3, endDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                EpidemicData epidemicData = new EpidemicData();
                epidemicData.setId(rs.getInt("id"));
                epidemicData.setRegion(rs.getString("region"));
                epidemicData.setDate(rs.getString("date"));
                epidemicData.setInfected(rs.getInt("infected"));
                data.add(epidemicData);
            }
        }
        return data;
    }
    public List<ClientHandler.RegionRanking> getRegionRanking(int limit) throws SQLException {
        List<ClientHandler.RegionRanking> rankings = new ArrayList<>();
        String sql = "SELECT region, SUM(infected) as total_infected " +
                "FROM epidemic_data " +
                "GROUP BY region " +
                "ORDER BY total_infected DESC " +
                "LIMIT ?";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String region = rs.getString("region");
                int totalInfected = rs.getInt("total_infected");
                rankings.add(new ClientHandler.RegionRanking(region, totalInfected));
            }
        }
        return rankings;
    }


    public boolean deleteData(int dataId) throws SQLException {
        String sql = "DELETE FROM epidemic_data WHERE id = ?";
        try (PreparedStatement stmt = dbManager.prepareStatement(sql)) {
            stmt.setInt(1, dataId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Внутренний класс для представления данных с именем пользователя
    public static class EpidemicDataWithUsername {
        private final int id;
        private final String username;
        private final String region;
        private final String date;
        private final int infected;

        public EpidemicDataWithUsername(int id, String username, String region, String date, int infected) {
            this.id = id;
            this.username = username;
            this.region = region;
            this.date = date;
            this.infected = infected;
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getRegion() {
            return region;
        }

        public String getDate() {
            return date;
        }

        public int getInfected() {
            return infected;
        }
    }
}