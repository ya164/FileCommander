package com.filecommander.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class OperationResult {
    private boolean success;
    private String message;
    private List<Path> affectedPaths;
    private Exception error;

    public static OperationResult success(List<Path> affectedPaths) {
        return new OperationResult(true, "Operation completed successfully", affectedPaths, null);
    }

    public static OperationResult error(String message) {
        return new OperationResult(false, message, Collections.emptyList(), null);
    }

    public static OperationResult error(Exception error) {
        return new OperationResult(false, error.getMessage(), Collections.emptyList(), error);
    }

    private OperationResult(boolean success, String message,
                            List<Path> affectedPaths, Exception error) {
        this.success = success;
        this.message = message;
        this.affectedPaths = affectedPaths;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Path> getAffectedPaths() { return affectedPaths; }
    public Exception getError() { return error; }
}