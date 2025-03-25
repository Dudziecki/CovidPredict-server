package com.example.server.dao;

import com.example.server.model.Case;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CaseDAO {
    private final DatabaseManager dbManager;

    public CaseDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void addCase(Case newCase) throws SQLException {
        String sql = "INSERT INTO cases (region_id, date, confirmed, deaths, recovered) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setInt(1, newCase.getRegionId());
        stmt.setDate(2, newCase.getDate());
        stmt.setInt(3, newCase.getConfirmed());
        stmt.setInt(4, newCase.getDeaths());
        stmt.setInt(5, newCase.getRecovered());
        stmt.executeUpdate();
    }

    public List<Case> getCasesByRegion(int regionId) throws SQLException {
        List<Case> cases = new ArrayList<>();
        String sql = "SELECT * FROM cases WHERE region_id = ?";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setInt(1, regionId);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            cases.add(new Case(rs.getInt("case_id"), rs.getInt("region_id"), rs.getDate("date"),
                    rs.getInt("confirmed"), rs.getInt("deaths"), rs.getInt("recovered")));
        }
        return cases;
    }

    public List<Case> getCasesByDateRange(Date startDate, Date endDate) throws SQLException {
        List<Case> cases = new ArrayList<>();
        String sql = "SELECT * FROM cases WHERE date BETWEEN ? AND ?";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setDate(1, startDate);
        stmt.setDate(2, endDate);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            cases.add(new Case(rs.getInt("case_id"), rs.getInt("region_id"), rs.getDate("date"),
                    rs.getInt("confirmed"), rs.getInt("deaths"), rs.getInt("recovered")));
        }
        return cases;
    }
}