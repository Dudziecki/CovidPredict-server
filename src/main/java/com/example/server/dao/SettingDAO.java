package com.example.server.dao;

import com.example.server.model.Setting;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SettingDAO {
    private final DatabaseManager dbManager;

    public SettingDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void updateSetting(String name, String value) throws SQLException {
        String sql = "UPDATE settings SET setting_value = ? WHERE setting_name = ?";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        stmt.setString(1, value);
        stmt.setString(2, name);
        int updated = stmt.executeUpdate();
        if (updated == 0) {
            sql = "INSERT INTO settings (setting_name, setting_value) VALUES (?, ?)";
            stmt = dbManager.prepareStatement(sql);
            stmt.setString(1, name);
            stmt.setString(2, value);
            stmt.executeUpdate();
        }
    }

    public List<Setting> getAllSettings() throws SQLException {
        List<Setting> settings = new ArrayList<>();
        String sql = "SELECT * FROM settings";
        PreparedStatement stmt = dbManager.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            settings.add(new Setting(rs.getInt("setting_id"), rs.getString("setting_name"), rs.getString("setting_value")));
        }
        return settings;
    }
}