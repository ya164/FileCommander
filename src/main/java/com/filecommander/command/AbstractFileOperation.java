package com.filecommander.command;

import com.filecommander.localization.LocalizationManager;
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
                LocalizationManager loc = LocalizationManager.getInstance();
                String errorMsg = validationError != null ? validationError : loc.getString("error.validation");
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
        LocalizationManager loc = LocalizationManager.getInstance();
        String message = e.getMessage();

        if (message != null && message.contains("A required privilege is not held by the client")) {
            Path targetPath = destination != null ? destination : (sources != null && !sources.isEmpty() ? sources.get(0) : null);
            String pathStr = targetPath != null ? targetPath.toString() : "";

            if (pathStr.startsWith("C:\\") && !pathStr.startsWith("C:\\Users\\")) {
                return loc.getString("error.accessDeniedSystem", pathStr);
            }

            return loc.getString("error.accessDeniedFolder", pathStr);
        }

        if (message != null && message.contains("Access is denied")) {
            return loc.getString("error.accessDeniedGeneral");
        }

        return message != null ? message : loc.getString("error.unknownOperation");
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