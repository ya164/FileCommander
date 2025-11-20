package com.filecommander.repository;

import java.io.File;
import java.sql.*;

public class DatabaseManager {
    private static DatabaseManager instance;
    private Connection connection;
    private static final String DB_PATH = System.getProperty("user.home") + File.separator + "FileCommander" + File.separator + "file_commander.db";

    private DatabaseManager() {
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            File dbFile = new File(DB_PATH);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    System.out.println("Created database directory: " + parentDir.getAbsolutePath());
                }
            }

            String url = "jdbc:sqlite:" + DB_PATH;
            connection = DriverManager.getConnection(url);
            System.out.println("Database connected at: " + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createHistoryTable = """
            CREATE TABLE IF NOT EXISTS operation_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                operation_type VARCHAR(50) NOT NULL,
                description TEXT,
                executed_at VARCHAR(20) NOT NULL,
                status VARCHAR(20)
            )
            """;

        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS settings (
                key VARCHAR(100) PRIMARY KEY,
                value TEXT,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createHistoryTable);
            stmt.execute(createSettingsTable);

            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('theme', 'light')");

            System.out.println("Database tables created successfully");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Failed to close database: " + e.getMessage());
        }
    }
}