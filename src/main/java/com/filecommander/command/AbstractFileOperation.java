package com.filecommander.command;

import com.filecommander.model.OperationResult;
import com.filecommander.repository.OperationHistoryRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFileOperation implements FileCommand {
    protected List<Path> sources;
    protected Path destination;
    protected OperationResult result;
    protected String validationError;

    @Override
    public final OperationResult execute() throws IOException {
        try {
            if (!validate()) {
                String errorMsg = validationError != null ? validationError : "Validation failed";
                return OperationResult.error(errorMsg);
            }

            prepare();
            performOperation();
            finish();
            logOperation();

            result = OperationResult.success(getAffectedPaths());

        } catch (IOException e) {
            handleError(e);
            String errorMessage = translateError(e);
            result = OperationResult.error(errorMessage);
        } finally {
            cleanup();
        }

        return result;
    }

    protected String translateError(IOException e) {
        String message = e.getMessage();

        if (message != null && message.contains("A required privilege is not held by the client")) {
            Path targetPath = destination != null ? destination : (sources != null && !sources.isEmpty() ? sources.get(0) : null);
            String pathStr = targetPath != null ? targetPath.toString() : "";

            if (pathStr.startsWith("C:\\") && !pathStr.startsWith("C:\\Users\\")) {
                return "Access Error: Cannot write to system folder " + pathStr + ". " +
                        "Please select another folder or run as administrator.";
            }

            return "Access Error: Cannot write to folder " + pathStr + ". " +
                    "Please select another folder or check permissions.";
        }

        if (message != null && message.contains("Access is denied")) {
            return "Access Denied. Please check the permissions for the file or folder.";
        }

        return message != null ? message : "An unknown error occurred during the operation";
    }

    protected abstract void performOperation() throws IOException;
    protected abstract boolean validate();

    protected void prepare() {
        System.out.println("Preparing: " + getDescription());
    }

    protected void finish() {
        System.out.println("Completed: " + getDescription());
    }

    protected void logOperation() {
        OperationHistoryRepository.getInstance().logOperation(this);
    }

    protected void handleError(IOException e) {
        System.err.println("Failed: " + e.getMessage());
    }

    protected void cleanup() {
    }

    @Override
    public List<Path> getAffectedPaths() {
        List<Path> paths = new ArrayList<>();

        if (sources != null) {
            paths.addAll(sources);
        }

        if (destination != null) {
            paths.add(destination);
        }

        return paths;
    }
}
