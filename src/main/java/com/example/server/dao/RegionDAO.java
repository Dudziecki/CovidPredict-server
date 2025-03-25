package com.example.server.dao;

import com.example.server.model.Region;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegionDAO {
    private final DatabaseManager dbManager;

    public RegionDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void addRegion(Region region) throws SQLException {
        String sql = "INSERT INTO regions (region_name) VALUES (?)";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setString(1, region.getName());
        stmt.executeUpdate();
    }

    public void deleteRegion(int regionId) throws SQLException {
        String sql = "DELETE FROM regions WHERE region_id = ?";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setInt(1, regionId);
        stmt.executeUpdate();
    }

    public List<Region> getAllRegions() throws SQLException {
        List<Region> regions = new ArrayList<>();
        String sql = "SELECT * FROM regions";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            regions.add(new Region(rs.getInt("region_id"), rs.getString("region_name")));
        }
        return regions;
    }
}