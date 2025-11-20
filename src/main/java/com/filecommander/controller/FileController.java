package com.filecommander.controller;

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
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();

        if (selectedFiles.isEmpty()) {
            showWarningDialog("No files selected");
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
        progressDialog = new WebViewProgressDialog("Copying Files", mainWindow.isDarkTheme());
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
        FXFilePanel originalActivePanel = mainWindow != null ? mainWindow.getActivePanel() : null;

        progressDialog = new WebViewProgressDialog("Moving Files", mainWindow.isDarkTheme());
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
        FXFilePanel originalActivePanel = mainWindow != null ? mainWindow.getActivePanel() : null;

        progressDialog = new WebViewProgressDialog("Moving Files", mainWindow.isDarkTheme());
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
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            showWarningDialog("No files selected");
            return;
        }

        Path currentFolderPath = sourcePanel.getCurrentPath();
        Path otherPanelPath = getDestinationPath(sourcePanel);

        if (currentFolderPath.equals(otherPanelPath)) {
            showWarningDialog("Cannot move files to the same folder");
            return;
        }

        List<Path> sourcePaths = selectedFiles.stream()
                .map(FileItem::getPath)
                .collect(Collectors.toList());

        executeMoveOperationDirect(sourcePaths, otherPanelPath);
    }

    public void initiateDeleteOperation(FXFilePanel sourcePanel) {
        List<FileItem> selectedFiles = sourcePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            showWarningDialog("No files selected");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete " + selectedFiles.size() + " item(s)?");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        List<Path> pathsToDelete = selectedFiles.stream()
                .map(FileItem::getPath)
                .collect(Collectors.toList());

        FXFilePanel originalActivePanel = mainWindow.getActivePanel();

        progressDialog = new WebViewProgressDialog("Deleting Files", mainWindow.isDarkTheme());
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
            if (!result.getMessage().contains("cancelled")) {
                showErrorDialog("Operation Failed", result.getMessage());
            }
        }
    }

    public void createFolderWithName(FXFilePanel panel, String folderName) {
        Path currentPath = panel.getCurrentPath();
        Path newFolderPath = currentPath.resolve(folderName);

        if (Files.exists(newFolderPath)) {
            showWarningDialog("A folder with this name already exists");
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
                                    Platform.runLater(() -> {
                                        panel.selectFileByName(folderName);
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        } else {
                            showErrorDialog("Create Folder Failed", result.getMessage());
                            refreshAllPanels();
                        }
                    });
                }
        );
    }

    public void executeRenameOperation(Path oldPath, Path newPath) {
        if (Files.exists(newPath)) {
            showWarningDialog("A file or folder with this name already exists");
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
                            showErrorDialog("Rename Failed", result.getMessage());
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
        if (clipboard.isEmpty()) {
            showWarningDialog("Clipboard is empty");
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
        boolean isDarkTheme = mainWindow != null && mainWindow.isDarkTheme();
        WebViewSearchDialog dialog = new WebViewSearchDialog((searchParams, panelChoice) -> {
            String criteria = searchParams.getCriteria();
            if (criteria == null || criteria.trim().isEmpty()) {
                showWarningDialog("Please enter search criteria");
                return;
            }

            FXFilePanel targetPanel = getTargetPanel(panelChoice);
            if (targetPanel == null) {
                showWarningDialog("Cannot determine target panel");
                return;
            }

            WebViewProgressDialog searchProgress = new WebViewProgressDialog("Searching Files", isDarkTheme);
            searchProgress.setSearchMode(true);
            searchProgress.setStatus("Searching for: " + criteria);
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
        dialog.show();
    }



    public void executeDirectRename(Path oldPath, Path newPath) {
        if (Files.exists(newPath)) {
            showWarningDialog("A file or folder with this name already exists");
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
            showErrorDialog("Rename Failed", e.getMessage());
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
        System.out.println("=== UNDO REQUESTED ===");

        progressDialog = new WebViewProgressDialog("Undoing Operation", mainWindow.isDarkTheme());
        progressDialog.setStatus("Preparing undo...");
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
                        // Обновляем панели
                        checkAndFixPanelPaths(mainWindow.getLeftPanel());
                        checkAndFixPanelPaths(mainWindow.getRightPanel());
                    }

                    refreshAllPanels();
                    showInfoDialog("Undo", "Operation undone successfully");
                } else {
                    System.err.println("=== UNDO FAILED: " + result.getMessage() + " ===");
                    showErrorDialog("Undo Failed", result.getMessage());
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
        Path currentPath = panel.getCurrentPath();
        String baseName = "New folder";
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
                            showErrorDialog("Create Folder Failed", result.getMessage());
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarningDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}