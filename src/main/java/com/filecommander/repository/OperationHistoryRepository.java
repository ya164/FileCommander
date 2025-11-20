package com.filecommander.repository;

import com.filecommander.command.FileCommand;
import com.filecommander.model.OperationHistory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OperationHistoryRepository {
    private static OperationHistoryRepository instance;
    private Connection connection;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private OperationHistoryRepository() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public static synchronized OperationHistoryRepository getInstance() {
        if (instance == null) {
            instance = new OperationHistoryRepository();
        }
        return instance;
    }

    public void logOperation(FileCommand command) {
        String sql = "INSERT INTO operation_history " +
                "(operation_type, description, executed_at, status) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String operationType = formatOperationType(command.getClass().getSimpleName());
            stmt.setString(1, operationType);
            stmt.setString(2, command.getDescription());
            stmt.setString(3, LocalDateTime.now().format(DATE_FORMAT));
            stmt.setString(4, "SUCCESS");
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log operation: " + e.getMessage());
        }
    }

    private String formatOperationType(String className) {
        String name = className.replace("Command", "");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append(" ");
            }
            result.append(c);
        }

        return result.toString();
    }

    public List<OperationHistory> getRecentOperations(int limit) {
        List<OperationHistory> history = new ArrayList<>();
        String sql = "SELECT * FROM operation_history " +
                "ORDER BY id DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OperationHistory op = new OperationHistory(
                        rs.getInt("id"),
                        rs.getString("operation_type"),
                        rs.getString("description"),
                        rs.getString("executed_at"),
                        rs.getString("status")
                );
                history.add(op);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load history: " + e.getMessage());
        }

        return history;
    }

    public void clearHistory() {
        String sql = "DELETE FROM operation_history";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Failed to clear history: " + e.getMessage());
        }
    }
}