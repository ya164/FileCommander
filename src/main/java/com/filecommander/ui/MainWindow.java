package com.filecommander.ui;

import com.filecommander.controller.FileController;
import com.filecommander.repository.SettingsRepository;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.stage.Stage;

public class MainWindow {
    private FXFilePanel leftPanel;
    private FXFilePanel rightPanel;
    private FXFilePanel activePanel;
    private WebViewToolbar toolbar;
    private WebViewSidebar sidebar;
    private boolean isDarkTheme = false;
    private SettingsRepository settingsRepository;

    public void start(Stage primaryStage) {
        settingsRepository = SettingsRepository.getInstance();

        isDarkTheme = settingsRepository.isDarkTheme();
        System.out.println("Starting application with theme: " + (isDarkTheme ? "dark" : "light"));

        BorderPane root = new BorderPane();

        applyBackgroundTheme(root);

        String initialPath = "C:\\";
        leftPanel = new FXFilePanel(initialPath);
        rightPanel = new FXFilePanel(initialPath);

        leftPanel.setMainWindow(this);
        rightPanel.setMainWindow(this);

        toolbar = new WebViewToolbar(this);
        root.setTop(toolbar);

        sidebar = new WebViewSidebar(this);

        SplitPane splitPane = new SplitPane(leftPanel, rightPanel);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.5);
        splitPane.setStyle("-fx-background-color: transparent;");

        HBox centerContent = new HBox(0);
        centerContent.getChildren().addAll(sidebar, splitPane);
        HBox.setHgrow(splitPane, Priority.ALWAYS);

        root.setCenter(centerContent);

        Scene scene = new Scene(root, 1800, 1000);
        setupGlobalHotkeys(scene);

        primaryStage.setScene(scene);
        primaryStage.setTitle("File Commander");
        primaryStage.setMaximized(true);
        primaryStage.show();

        Platform.runLater(() -> {
            applyThemeToAllComponents();

            activatePanel(leftPanel);
            System.out.println("All panels loaded successfully");
            System.out.println("Initial path: C:\\");
        });

        FileController.getInstance().setMainWindow(this);
    }

    private void setupGlobalHotkeys(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl) {
                if (event.getCode().isFunctionKey() || event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                } else {
                    return;
                }
            }

            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case Z:
                        FileController.getInstance().undoLastOperation();
                        event.consume();
                        break;
                    case H:
                        FileController.getInstance().toggleHiddenFiles();
                        toolbar.updateHiddenFilesButton();
                        event.consume();
                        break;
                    case T:
                        toggleTheme();
                        event.consume();
                        break;
                    case F:
                        FileController.getInstance().openSearchDialog();
                        event.consume();
                        break;
                    case C:
                        if (activePanel != null) {
                            activePanel.copyToClipboard();
                            event.consume();
                        }
                        break;
                    case V:
                        if (activePanel != null) {
                            activePanel.pasteFromClipboard();
                            event.consume();
                        }
                        break;
                    case A:
                        if (activePanel != null) {
                            activePanel.handleExternalKeyEvent(event);
                        }
                        break;
                }
            } else {
                switch (event.getCode()) {
                    case F2:
                    case F3:
                    case F4:
                    case F5:
                    case F6:
                    case F7:
                    case F8:
                    case DELETE:
                    case ESCAPE:
                        if (activePanel != null) {
                            activePanel.handleKeyPress(event.getCode());
                            event.consume();
                        }
                        break;
                    case TAB:
                        if (activePanel == leftPanel) {
                            activatePanel(rightPanel);
                        } else {
                            activatePanel(leftPanel);
                        }
                        event.consume();
                        break;
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                    case ENTER:
                    case SPACE:
                        if (activePanel != null) {
                            activePanel.handleExternalKeyEvent(event);
                        }
                        break;
                }
            }
        });
    }

    public void toggleTheme() {
        isDarkTheme = !isDarkTheme;

        String themeValue = isDarkTheme ? "dark" : "light";
        settingsRepository.saveTheme(themeValue);
        System.out.println("Theme toggled to: " + themeValue);

        BorderPane root = (BorderPane) leftPanel.getScene().getRoot();
        applyBackgroundTheme(root);
        applyThemeToAllComponents();
    }

    private void applyBackgroundTheme(BorderPane root) {
        if (isDarkTheme) {
            root.setBackground(new Background(new BackgroundFill(
                    Color.web("#1a1a1a"), null, null
            )));
        } else {
            Stop[] stops = new Stop[] {
                    new Stop(0, Color.web("#e0f7fa")),
                    new Stop(0.25, Color.web("#b2ebf2")),
                    new Stop(0.5, Color.web("#80deea")),
                    new Stop(0.75, Color.web("#4dd0e1")),
                    new Stop(1, Color.web("#26c6da"))
            };
            LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops);
            root.setBackground(new Background(new BackgroundFill(gradient, null, null)));
        }
    }

    private void applyThemeToAllComponents() {
        if (leftPanel != null) {
            leftPanel.setTheme(isDarkTheme);
        }
        if (rightPanel != null) {
            rightPanel.setTheme(isDarkTheme);
        }

        Platform.runLater(() -> {
            if (toolbar != null) {
                toolbar.setTheme(isDarkTheme);
            }
            if (sidebar != null) {
                sidebar.setTheme(isDarkTheme);
            }
        });
    }

    public boolean isDarkTheme() {
        return isDarkTheme;
    }

    public void activatePanel(FXFilePanel panel) {
        if (activePanel != null && activePanel != panel) {
            activePanel.setActive(false);
        }
        if (panel != null) {
            panel.setActive(true);
            activePanel = panel;

            Platform.runLater(() -> {
                panel.requestFocus();
                panel.ensureFirstItemSelected();
            });
        }
    }

    public FXFilePanel getActivePanel() {
        return activePanel;
    }

    public FXFilePanel getOtherPanel(FXFilePanel panel) {
        return panel == leftPanel ? rightPanel : leftPanel;
    }

    public FXFilePanel getLeftPanel() {
        return leftPanel;
    }

    public FXFilePanel getRightPanel() {
        return rightPanel;
    }
}