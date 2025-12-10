package com.filecommander.controller;

import com.filecommander.localization.LocalizationManager;
import com.filecommander.model.FileItem;
import com.filecommander.model.OperationResult;
import com.filecommander.observer.FileSystemEvent;
import com.filecommander.observer.FileSystemObserver;
import com.filecommander.service.FileOperationService;
import com.filecommander.service.SearchService;
import com.filecommander.ui.FXFilePanel;
import com.filecommander.ui.MainWindow;
import com.filecommander.ui.dialogs.*;
import javafx.application.Platform;
import javafx.scene.control.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class FileController {
    private static FileController instance;
    private List<FileSystemObserver> observers = new ArrayList<>();
    private FileOperationService operationService;
    private SearchService searchService;
    private boolean showHiddenFiles = false;
    private MainWindow mainWindow;
    private List<Path> clipboard = new ArrayList<>();
    private WebViewProgressDialog progressDialog;

    private FileController() {
        this.operationService = FileOperationService.getInstance();
        this.searchService = SearchService.getInstance();
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public boolean isAnyPanelInSearchMode() {
        if (mainWindow != null) {
            return mainWindow.getLeftPanel().isInSearchMode() || mainWindow.getRightPanel().isInSearchMode();
        }
        return false;
    }

    public static synchronized FileController getInstance() {
        if (instance == null) {
            instance = new FileController();
        }
        return instance;
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    public void addObserver(FileSystemObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(FileSystemObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(FileSystemEvent event) {
        for (FileSystemObserver observer : observers) {
            observer.onFileSystemChanged(event);
        }
    }

    public FileOperationService getOperationService() {
        return operationService;
    }

    public void refreshAllPanels() {
        notifyObservers(new FileSystemEvent(
                Collections.emptyList(),
                FileSystemEvent.Type.MODIFIED
        ));

        if (mainWindow != null) {
            FXFilePanel activePanel = mainWindow.getActivePanel();

            checkAndFixPanelPaths(mainWindow.getLeftPanel());
            checkAndFixPanelPaths(mainWindow.getRightPanel());

            Platform.runLater(() -> {
                if (mainWindow.getLeftPanel() != null) {
                    mainWindow.getLeftPanel().refreshFileList();
                }
                if (mainWindow.getRightPanel() != null) {
                    mainWindow.getRightPanel().refreshFileList();
                }

                if (activePanel != null) {
                    mainWindow.activatePanel(activePanel);
                }
            });
        }
    }

    public void toggleHiddenFiles() {
        showHiddenFiles = !showHiddenFiles;
        notifyObservers(new FileSystemEvent(
                Collections.emptyList(),
                FileSystemEvent.Type.SETTINGS_CHANGED
        ));
        refreshAllPanels();
    }

    public boolean isShowHiddenFiles() {
        return showHiddenFiles;
    }

    public void showCopyDestinationMenu(FXFilePanel sourcePanel) {
        LocalizationManager loc = LocalizationManager.getInstance();
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();

        if (selectedFiles.isEmpty()) {
            showWarningDialog(loc.getString("warning.noSelection"));
            return;
        }

        Path currentFolderPath = sourcePanel.getCurrentPath().toAbsolutePath().normalize();
        Path otherPanelPath = getDestinationPath(sourcePanel).toAbsolutePath().normalize();

        boolean sameFolders = currentFolderPath.equals(otherPanelPath);

        if (sameFolders) {
            executeCopyWithProgress(
                    selectedFiles.stream().map(FileItem::getPath).collect(Collectors.toList()),
                    currentFolderPath,
                    true
            );
        } else {
            sourcePanel.showCopyDestinationMenu(otherPanelPath);
        }
    }

    public void executeCopyOperation(FXFilePanel sourcePanel, Path destination) {
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            return;
        }

        Path currentFolderPath = sourcePanel.getCurrentPath();
        boolean addCopySuffix = destination.equals(currentFolderPath);

        executeCopyWithProgress(
                selectedFiles.stream().map(FileItem::getPath).collect(Collectors.toList()),
                destination,
                addCopySuffix
        );
    }

    public void executeCopyWithDestination(FXFilePanel sourcePanel, boolean toOtherPanel) {
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            return;
        }

        Path currentFolderPath = sourcePanel.getCurrentPath();
        Path otherPanelPath = getDestinationPath(sourcePanel);

        Path destination = toOtherPanel ? otherPanelPath : currentFolderPath;
        boolean addCopySuffix = destination.equals(currentFolderPath);

        executeCopyWithProgress(
                selectedFiles.stream().map(FileItem::getPath).collect(Collectors.toList()),
                destination,
                addCopySuffix
        );
    }

    private void executeCopyWithProgress(List<Path> sources, Path destination, boolean addCopySuffix) {
        LocalizationManager loc = LocalizationManager.getInstance();

        progressDialog = new WebViewProgressDialog(loc.getString("operation.copying"), mainWindow.isDarkTheme());
        progressDialog.setOnCancel(() -> operationService.cancelCurrentOperation());

        operationService.setProgressCallback(new FileOperationService.ProgressCallback() {
            @Override
            public void onProgress(int current, int total, String currentItem) {
                progressDialog.updateProgress(current, total, currentItem);
            }

            @Override
            public void onStatusChange(String status) {
                progressDialog.setStatus(status);
            }
        });

        progressDialog.show();

        operationService.executeCopyOperation(
                sources,
                destination,
                addCopySuffix,
                result -> {
                    Platform.runLater(() -> {
                        progressDialog.close();
                        operationService.setProgressCallback(null);
                        handleOperationComplete(result);
                        refreshAllPanels();
                    });
                }
        );
    }

    public void executeCopyOperationDirect(List<Path> sources, Path destination) {
        executeCopyWithProgress(sources, destination, false);
    }

    public void executeMoveOperationDirect(List<Path> sources, Path destination) {
        LocalizationManager loc = LocalizationManager.getInstance();
        FXFilePanel originalActivePanel = mainWindow != null ? mainWindow.getActivePanel() : null;

        progressDialog = new WebViewProgressDialog(loc.getString("operation.moving"), mainWindow.isDarkTheme());
        progressDialog.setOnCancel(() -> operationService.cancelCurrentOperation());

        operationService.setProgressCallback(new FileOperationService.ProgressCallback() {
            @Override
            public void onProgress(int current, int total, String currentItem) {
                progressDialog.updateProgress(current, total, currentItem);
            }

            @Override
            public void onStatusChange(String status) {
                progressDialog.setStatus(status);
            }
        });

        progressDialog.show();

        operationService.executeMoveOperation(
                sources,
                destination,
                result -> {
                    Platform.runLater(() -> {
                        progressDialog.close();
                        operationService.setProgressCallback(null);
                        handleOperationComplete(result);
                        checkAndFixPanelPaths(mainWindow.getLeftPanel());
                        checkAndFixPanelPaths(mainWindow.getRightPanel());
                        refreshAllPanels();

                        if (originalActivePanel != null) {
                            mainWindow.activatePanel(originalActivePanel);
                        }
                    });
                }
        );
    }

    public void executeCopyDirect(List<Path> sources, Path destination, FXFilePanel sourcePanel) {
        executeCopyWithProgress(sources, destination, false);
    }

    public void executeMoveDirect(List<Path> sources, Path destination, FXFilePanel sourcePanel) {
        LocalizationManager loc = LocalizationManager.getInstance();
        FXFilePanel originalActivePanel = mainWindow != null ? mainWindow.getActivePanel() : null;

        progressDialog = new WebViewProgressDialog(loc.getString("operation.moving"), mainWindow.isDarkTheme());
        progressDialog.setOnCancel(() -> operationService.cancelCurrentOperation());

        operationService.setProgressCallback(new FileOperationService.ProgressCallback() {
            @Override
            public void onProgress(int current, int total, String currentItem) {
                progressDialog.updateProgress(current, total, currentItem);
            }

            @Override
            public void onStatusChange(String status) {
                progressDialog.setStatus(status);
            }
        });

        progressDialog.show();

        operationService.executeMoveOperation(
                sources,
                destination,
                result -> {
                    Platform.runLater(() -> {
                        progressDialog.close();
                        operationService.setProgressCallback(null);
                        handleOperationComplete(result);
                        checkAndFixPanelPaths(mainWindow.getLeftPanel());
                        checkAndFixPanelPaths(mainWindow.getRightPanel());
                        refreshAllPanels();

                        if (originalActivePanel != null) {
                            mainWindow.activatePanel(originalActivePanel);
                        }
                    });
                }
        );
    }

    public void initiateMoveOperation(FXFilePanel sourcePanel) {
        LocalizationManager loc = LocalizationManager.getInstance();
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();

        if (selectedFiles.isEmpty()) {
            showWarningDialog(loc.getString("warning.noSelection"));
            return;
        }

        Path currentFolderPath = sourcePanel.getCurrentPath();
        Path otherPanelPath = getDestinationPath(sourcePanel);

        if (currentFolderPath.equals(otherPanelPath)) {
            showWarningDialog(loc.getString("warning.cannotMoveSameFolder"));
            return;
        }

        List<Path> sourcePaths = selectedFiles.stream()
                .map(FileItem::getPath)
                .collect(Collectors.toList());

        executeMoveOperationDirect(sourcePaths, otherPanelPath);
    }

    public void initiateDeleteOperation(FXFilePanel sourcePanel) {
        LocalizationManager loc = LocalizationManager.getInstance();
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();

        if (selectedFiles.isEmpty()) {
            showWarningDialog(loc.getString("warning.noSelection"));
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        mainWindow.setIconForDialog(confirmAlert);
        confirmAlert.setTitle(loc.getString("dialog.confirmDelete"));
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText(loc.getString("dialog.confirmDeleteMessage", selectedFiles.size()));

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        List<Path> pathsToDelete = selectedFiles.stream()
                .map(FileItem::getPath)
                .collect(Collectors.toList());

        FXFilePanel originalActivePanel = mainWindow.getActivePanel();

        progressDialog = new WebViewProgressDialog(loc.getString("operation.deleting"), mainWindow.isDarkTheme());
        progressDialog.setOnCancel(() -> operationService.cancelCurrentOperation());

        operationService.setProgressCallback(new FileOperationService.ProgressCallback() {
            @Override
            public void onProgress(int current, int total, String currentItem) {
                progressDialog.updateProgress(current, total, currentItem);
            }

            @Override
            public void onStatusChange(String status) {
                progressDialog.setStatus(status);
            }
        });

        progressDialog.show();

        operationService.executeDeleteOperation(
                pathsToDelete,
                operationResult -> {
                    Platform.runLater(() -> {
                        progressDialog.close();
                        operationService.setProgressCallback(null);
                        handleOperationComplete(operationResult);
                        checkAndFixPanelPaths(mainWindow.getLeftPanel());
                        checkAndFixPanelPaths(mainWindow.getRightPanel());
                        refreshAllPanels();

                        if (originalActivePanel != null) {
                            mainWindow.activatePanel(originalActivePanel);
                        }
                    });
                }
        );
    }

    private void handleOperationComplete(OperationResult result) {
        if (!result.isSuccess()) {
            if (result.getMessage() != null &&
                    (result.getMessage().contains("скасовано") || result.getMessage().contains("cancelled"))) {
                return;
            }

            LocalizationManager loc = LocalizationManager.getInstance();
            String title = loc.getString("dialog.error");
            String message = result.getMessage(); // Дефолтне

            if (result.getError() != null) {
                Exception e = result.getError();

                if (e instanceof java.nio.file.AccessDeniedException) {
                    String file = ((java.nio.file.AccessDeniedException) e).getFile();

                    if (file == null || file.isEmpty()) {
                        if (result.getAffectedPaths() != null && !result.getAffectedPaths().isEmpty()) {
                            file = result.getAffectedPaths().get(0).toString();
                        } else {
                            file = "C:\\";
                        }
                    }

                    boolean isSystemFolder = file != null && (file.endsWith(":\\") || file.equals("/") || file.contains("Windows") || file.contains("Program Files"));

                    if (isSystemFolder) {
                        message = loc.getString("error.accessDeniedSystem", file);
                    } else {
                        message = loc.getString("error.accessDeniedDetails", file);
                    }
                } else if (e instanceof java.nio.file.FileAlreadyExistsException) {
                    message = loc.getString("error.fileAlreadyExists", ((java.nio.file.FileAlreadyExistsException) e).getFile());
                } else if (e instanceof java.nio.file.NoSuchFileException) {
                    message = loc.getString("error.fileNotFound", ((java.nio.file.NoSuchFileException) e).getFile());
                } else if (e instanceof java.nio.file.DirectoryNotEmptyException) {
                    message = loc.getString("error.notEmpty", ((java.nio.file.DirectoryNotEmptyException) e).getFile());
                } else if (e instanceof IOException) {
                    message = loc.getString("error.io", e.getMessage());
                }
            }

            showErrorDialog(title, message);
        }
    }

    public void createFolderWithName(FXFilePanel panel, String folderName) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Path currentPath = panel.getCurrentPath();
        Path newFolderPath = currentPath.resolve(folderName);

        if (Files.exists(newFolderPath)) {
            showWarningDialog(loc.getString("warning.fileExists"));
            refreshAllPanels();
            return;
        }

        operationService.executeCreateFolderOperation(
                newFolderPath,
                result -> {
                    Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            refreshAllPanels();
                            new Thread(() -> {
                                try {
                                    Thread.sleep(300);
                                    Platform.runLater(() -> panel.selectFileByName(folderName));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        } else {
                            handleOperationComplete(result);
                            refreshAllPanels();
                        }
                    });
                }
        );
    }

    public void executeRenameOperation(Path oldPath, Path newPath) {
        LocalizationManager loc = LocalizationManager.getInstance();
        if (Files.exists(newPath)) {
            showWarningDialog(loc.getString("warning.fileExists"));
            refreshAllPanels();
            return;
        }

        operationService.executeRenameOperation(
                oldPath,
                newPath,
                result -> {
                    Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            checkAndFixPanelPaths(mainWindow.getLeftPanel());
                            checkAndFixPanelPaths(mainWindow.getRightPanel());
                            refreshAllPanels();
                            new Thread(() -> {
                                try {
                                    Thread.sleep(300);
                                    Platform.runLater(() -> {
                                        if (mainWindow != null) {
                                            FXFilePanel activePanel = mainWindow.getActivePanel();
                                            if (activePanel != null) {
                                                activePanel.selectFileByName(newPath.getFileName().toString());
                                            }
                                        }
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        } else {
                            handleOperationComplete(result);
                            refreshAllPanels();
                        }
                    });
                }
        );
    }

    public void copyToClipboard(List<Path> paths) {
        clipboard.clear();
        clipboard.addAll(paths);

        if (mainWindow != null) {
            mainWindow.getLeftPanel().updateClipboardStatus(clipboard.isEmpty());
            mainWindow.getRightPanel().updateClipboardStatus(clipboard.isEmpty());
        }
    }

    public void pasteFromClipboard(FXFilePanel targetPanel) {
        LocalizationManager loc = LocalizationManager.getInstance();
        if (clipboard.isEmpty()) {
            showWarningDialog(loc.getString("warning.bufferEmpty"));
            return;
        }

        Path destination = targetPanel.getCurrentPath();
        executeCopyWithProgress(new ArrayList<>(clipboard), destination, false);
    }

    public List<FileItem> getFilesInDirectory(Path path) {
        try {
            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return new ArrayList<>();
            }

            List<FileItem> files = new ArrayList<>();
            Files.list(path).forEach(filePath -> {
                try {
                    FileItem item = FileItem.from(filePath);
                    if (item != null && (showHiddenFiles || !item.isHidden())) {
                        files.add(item);
                    }
                } catch (Exception e) {
                    System.err.println("Error creating FileItem: " + e.getMessage());
                }
            });

            files.sort(Comparator.comparing(FileItem::isDirectory).reversed()
                    .thenComparing(FileItem::getName, String.CASE_INSENSITIVE_ORDER));

            return files;
        } catch (Exception e) {
            System.err.println("Error listing directory: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void openSearchDialog() {
        LocalizationManager loc = LocalizationManager.getInstance();
        boolean isDarkTheme = mainWindow != null && mainWindow.isDarkTheme();

        WebViewSearchDialog dialog = new WebViewSearchDialog((searchParams, panelChoice) -> {
            String criteria = searchParams.getCriteria();
            if (criteria == null || criteria.trim().isEmpty()) {
                showWarningDialog(loc.getString("search.enterCriteria"));
                return;
            }

            FXFilePanel targetPanel = getTargetPanel(panelChoice);
            if (targetPanel == null) {
                showWarningDialog(loc.getString("search.cannotDetermine"));
                return;
            }

            WebViewProgressDialog searchProgress = new WebViewProgressDialog(loc.getString("search.title"), isDarkTheme);
            searchProgress.setSearchMode(true);
            searchProgress.setStatus(loc.getString("search.search") + ": " + criteria);
            searchProgress.setOnCancel(() -> searchService.cancelCurrentSearch());
            searchProgress.show();

            new Thread(() -> {
                List<FileItem> results = searchService.search(
                        searchParams.getRootPath(),
                        criteria,
                        (scanned, found, path) -> {
                            searchProgress.updateSearchProgress(scanned, found, path);
                        }
                );

                results = filterByItemType(results, searchParams.getItemType());

                List<FileItem> finalResults = results;
                Platform.runLater(() -> {
                    searchProgress.close();
                    targetPanel.showSearchResults(finalResults, criteria);
                });
            }).start();
        }, isDarkTheme);

        mainWindow.setIconForStage(dialog);
        dialog.show();
    }

    public void executeDirectRename(Path oldPath, Path newPath) {
        LocalizationManager loc = LocalizationManager.getInstance();
        if (Files.exists(newPath)) {
            showWarningDialog(loc.getString("warning.fileExists"));
            refreshAllPanels();
            return;
        }

        try {
            Files.move(oldPath, newPath);
            System.out.println("Direct rename (no history): " + oldPath.getFileName() + " -> " + newPath.getFileName());

            operationService.updateLastOperationPath(oldPath, newPath);

            Platform.runLater(() -> {
                checkAndFixPanelPaths(mainWindow.getLeftPanel());
                checkAndFixPanelPaths(mainWindow.getRightPanel());
                refreshAllPanels();

                new Thread(() -> {
                    try {
                        Thread.sleep(300);
                        Platform.runLater(() -> {
                            if (mainWindow != null) {
                                FXFilePanel activePanel = mainWindow.getActivePanel();
                                if (activePanel != null) {
                                    activePanel.selectFileByName(newPath.getFileName().toString());
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            });
        } catch (IOException e) {
            String message = e.getMessage();
            if (e instanceof java.nio.file.AccessDeniedException) {
                message = loc.getString("error.accessDeniedDetails", newPath.toString());
            } else if (e instanceof java.nio.file.FileAlreadyExistsException) {
                message = loc.getString("warning.fileExists");
            }

            showErrorDialog(loc.getString("operation.renameFailed"), message);
            refreshAllPanels();
        }
    }

    private FXFilePanel getTargetPanel(WebViewSearchDialog.PanelChoice panelChoice) {
        if (mainWindow == null) return null;

        switch (panelChoice) {
            case LEFT:
                return mainWindow.getLeftPanel();
            case RIGHT:
                return mainWindow.getRightPanel();
            case ACTIVE:
            default:
                return mainWindow.getActivePanel();
        }
    }

    public void navigateToFileLocation(Path filePath) {
        if (mainWindow == null) return;

        FXFilePanel activePanel = mainWindow.getActivePanel();
        if (activePanel != null) {
            if (Files.isDirectory(filePath)) {
                activePanel.navigateToPath(filePath);
            } else {
                navigateToFileLocationAndSelect(filePath);
            }
        }
    }

    public void navigateToFileLocationAndSelect(Path filePath) {
        if (mainWindow == null) return;

        Path parentPath = filePath.getParent();
        if (parentPath == null) return;

        FXFilePanel activePanel = mainWindow.getActivePanel();
        if (activePanel != null) {
            activePanel.navigateToPath(parentPath);

            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(() -> {
                        String fileName = filePath.getFileName().toString();
                        activePanel.selectFileByName(fileName);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    public void navigateToFileLocationInPanel(Path filePath, boolean inCurrentPanel) {
        if (mainWindow == null) return;

        Path parentPath = filePath.getParent();
        if (parentPath == null) return;

        FXFilePanel targetPanel;
        if (inCurrentPanel) {
            targetPanel = mainWindow.getActivePanel();
        } else {
            targetPanel = mainWindow.getOtherPanel(mainWindow.getActivePanel());
        }

        if (targetPanel != null) {
            targetPanel.navigateToPath(parentPath);

            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    Platform.runLater(() -> {
                        String fileName = filePath.getFileName().toString();
                        targetPanel.selectFileByName(fileName);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private List<FileItem> filterByItemType(List<FileItem> results, SearchParams.ItemType itemType) {
        return results.stream()
                .filter(item -> {
                    switch (itemType) {
                        case FILES:
                            return !item.isDirectory();
                        case FOLDERS:
                            return item.isDirectory();
                        case BOTH:
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    public void undoLastOperation() {
        LocalizationManager loc = LocalizationManager.getInstance();
        System.out.println("=== UNDO REQUESTED ===");

        progressDialog = new WebViewProgressDialog(loc.getString("operation.undoing"), mainWindow.isDarkTheme());
        progressDialog.setStatus(loc.getString("progress.preparing"));
        progressDialog.setOnCancel(() -> operationService.cancelCurrentOperation());

        operationService.setProgressCallback(new FileOperationService.ProgressCallback() {
            @Override
            public void onProgress(int current, int total, String currentItem) {
                progressDialog.updateProgress(current, total, currentItem);
            }

            @Override
            public void onStatusChange(String status) {
                progressDialog.setStatus(status);
            }
        });

        progressDialog.show();

        operationService.undoLastOperation((result) -> {
            Platform.runLater(() -> {
                if (progressDialog != null) {
                    progressDialog.close();
                    progressDialog = null;
                }

                operationService.setProgressCallback(null);

                if (result.isSuccess()) {
                    System.out.println("=== UNDO SUCCESSFUL ===");
                    if (mainWindow != null) {
                        checkAndFixPanelPaths(mainWindow.getLeftPanel());
                        checkAndFixPanelPaths(mainWindow.getRightPanel());
                    }
                    refreshAllPanels();
                    showInfoDialog(loc.getString("dialog.info"), loc.getString("operation.undoSuccess"));
                } else {
                    System.err.println("=== UNDO FAILED: " + result.getMessage() + " ===");
                    showErrorDialog(loc.getString("operation.undoFailed"), result.getMessage());
                }
            });
        });
    }

    private void checkAndFixPanelPaths(FXFilePanel panel) {
        if (panel == null) return;

        Path currentPath = panel.getCurrentPath();
        if (currentPath != null && !Files.exists(currentPath)) {
            System.out.println("Path no longer exists: " + currentPath + ", navigating to parent");

            Path parent = currentPath.getParent();
            while (parent != null && !Files.exists(parent)) {
                parent = parent.getParent();
            }

            if (parent != null) {
                panel.navigateToPath(parent);
            } else {
                panel.navigateToPath(Path.of(System.getProperty("user.home")));
            }
        }
    }

    private Path getDestinationPath(FXFilePanel sourcePanel) {
        if (mainWindow != null) {
            FXFilePanel otherPanel = mainWindow.getOtherPanel(sourcePanel);
            return otherPanel.getCurrentPath();
        }
        return sourcePanel.getCurrentPath();
    }

    public void createDefaultFolder(FXFilePanel panel) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Path currentPath = panel.getCurrentPath();

        String baseName = loc.getString("toolbar.newFolder");

        Path newFolderPath = currentPath.resolve(baseName);
        int counter = 2;

        while (Files.exists(newFolderPath)) {
            newFolderPath = currentPath.resolve(baseName + " (" + counter + ")");
            counter++;
        }

        final Path finalPath = newFolderPath;
        final String folderName = finalPath.getFileName().toString();

        operationService.executeCreateFolderOperation(
                finalPath,
                result -> {
                    Platform.runLater(() -> {
                        if (result.isSuccess()) {
                            refreshAllPanels();
                            panel.selectAndEditFile(folderName);
                        } else {
                            showErrorDialog(loc.getString("operation.createFolderFailed"), result.getMessage());
                        }
                    });
                }
        );
    }

    private boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showErrorDialog(String title, String message) {
        if (title == null || title.isEmpty()) {
            title = LocalizationManager.getInstance().getString("dialog.error");
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        mainWindow.setIconForDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningDialog(String message) {
        LocalizationManager loc = LocalizationManager.getInstance();
        Alert alert = new Alert(Alert.AlertType.WARNING);
        mainWindow.setIconForDialog(alert);
        alert.setTitle(loc.getString("dialog.warning"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        mainWindow.setIconForDialog(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}