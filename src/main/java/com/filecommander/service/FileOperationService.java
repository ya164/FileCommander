package com.filecommander.service;

import com.filecommander.command.CreateFolderCommand;
import com.filecommander.command.FileCommand;
import com.filecommander.factory.OperationFactory;
import com.filecommander.model.FileItem;
import com.filecommander.model.OperationResult;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FileOperationService {
    private static FileOperationService instance;
    private Stack<FileCommand> commandHistory = new Stack<>();
    private OperationFactory operationFactory = new OperationFactory();
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private ProgressCallback progressCallback;
    private volatile boolean cancelled = false;

    public interface ProgressCallback {
        void onProgress(int current, int total, String currentItem);
        void onStatusChange(String status);
    }

    private FileOperationService() {}

    public static synchronized FileOperationService getInstance() {
        if (instance == null) {
            instance = new FileOperationService();
        }
        return instance;
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    public void cancelCurrentOperation() {
        this.cancelled = true;
    }

    public void executeCopyOperation(List<Path> sources, Path destination,
                                     boolean addCopySuffix,
                                     Consumer<OperationResult> callback) {
        cancelled = false;
        FileCommand command = operationFactory.createCopyCommand(sources, destination, addCopySuffix);
        executeCommandAsync(command, callback);
    }

    public void executeMoveOperation(List<Path> sources, Path destination,
                                     Consumer<OperationResult> callback) {
        cancelled = false;
        FileCommand command = operationFactory.createMoveCommand(sources, destination);
        executeCommandAsync(command, callback);
    }

    public void executeDeleteOperation(List<Path> sources,
                                       Consumer<OperationResult> callback) {
        cancelled = false;
        FileCommand command = operationFactory.createDeleteCommand(sources);
        executeCommandAsync(command, callback);
    }

    public void executeCreateFolderOperation(Path folderPath,
                                             Consumer<OperationResult> callback) {
        FileCommand command = operationFactory.createFolderCommand(folderPath);
        executeCommandAsync(command, callback);
    }

    public void executeRenameOperation(Path oldPath, Path newPath,
                                       Consumer<OperationResult> callback) {
        FileCommand command = operationFactory.createRenameCommand(oldPath, newPath);
        executeCommandAsync(command, callback);
    }

    private void executeCommandAsync(FileCommand command,
                                     Consumer<OperationResult> callback) {
        executorService.submit(() -> {
            try {
                OperationResult result = command.execute();

                if (result.isSuccess()) {
                    commandHistory.push(command);
                }

                Platform.runLater(() -> callback.accept(result));

            } catch (Exception e) {
                e.printStackTrace();
                OperationResult errorResult = OperationResult.error(e);
                Platform.runLater(() -> callback.accept(errorResult));
            }
        });
    }

    public void undoLastOperation(Consumer<OperationResult> callback) {
        if (commandHistory.isEmpty()) {
            Platform.runLater(() ->
                    callback.accept(OperationResult.error("Немає операцій для скасування")));
            return;
        }

        FileCommand lastCommand = commandHistory.pop();

        executorService.submit(() -> {
            try {
                lastCommand.undo();
                OperationResult result = OperationResult.success(lastCommand.getAffectedPaths());
                Platform.runLater(() -> callback.accept(result));
            } catch (IOException e) {
                e.printStackTrace();
                OperationResult result = OperationResult.error(e);
                Platform.runLater(() -> callback.accept(result));
            }
        });
    }

    public void updateLastOperationPath(Path oldPath, Path newPath) {
        if (!commandHistory.isEmpty()) {
            FileCommand lastCommand = commandHistory.peek();
            if (lastCommand instanceof CreateFolderCommand) {
                ((CreateFolderCommand) lastCommand).updatePath(newPath);
                System.out.println("Updated CreateFolderCommand path: " + oldPath.getFileName() + " -> " + newPath.getFileName());
            }
        }
    }

    public List<FileItem> listFiles(Path directory, boolean showHidden) {
        try {
            return Files.list(directory)
                    .filter(path -> showHidden || !isHidden(path))
                    .map(FileItem::from)
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private boolean isHidden(Path path) {
        try {
            return Files.isHidden(path);
        } catch (IOException e) {
            return false;
        }
    }

    public void notifyProgress(int current, int total, String currentItem) {
        if (progressCallback != null && !cancelled) {
            Platform.runLater(() -> progressCallback.onProgress(current, total, currentItem));
        }
    }

    public void notifyStatus(String status) {
        if (progressCallback != null) {
            Platform.runLater(() -> progressCallback.onStatusChange(status));
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}