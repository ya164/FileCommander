package com.filecommander.repository;

import java.sql.*;

public class SettingsRepository {
    private static SettingsRepository instance;
    private Connection connection;

    private SettingsRepository() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public static synchronized SettingsRepository getInstance() {
        if (instance == null) {
            instance = new SettingsRepository();
        }
        return instance;
    }

    public String getTheme() {
        String sql = "SELECT value FROM settings WHERE key = 'theme'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String theme = rs.getString("value");
                System.out.println("Loaded theme from DB: " + theme);
                return theme;
            }
        } catch (SQLException e) {
            System.err.println("Error loading theme: " + e.getMessage());
        }

        return "light";
    }

    public void saveTheme(String theme) {
        String sql = "INSERT OR REPLACE INTO settings (key, value, updated_at) VALUES ('theme', ?, CURRENT_TIMESTAMP)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, theme);
            stmt.executeUpdate();
            System.out.println("Theme saved to DB: " + theme);
        } catch (SQLException e) {
            System.err.println("Error saving theme: " + e.getMessage());
        }
    }

    public boolean isDarkTheme() {
        return "dark".equals(getTheme());
    }
}