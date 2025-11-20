package com.filecommander.ui;

import com.filecommander.controller.FileController;
import com.filecommander.model.FileItem;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class FXFilePanel extends BorderPane {
    private Path currentPath;
    private boolean isActive = false;
    private final FileController controller = FileController.getInstance();
    private MainWindow mainWindow;
    private int selectionAnchor = -1;
    private boolean isRenamingNewFolder = false;
    private javafx.scene.Node dragGhost;

    private TableView<FileItem> tableView;
    private TextField pathField;
    private Label statusLabel;
    private Label diskSpaceLabel;
    private HBox navBar;
    private HBox statusBar;
    private boolean allowEdit = false;

    private Button btnBack;
    private Button btnForward;
    private Button btnUp;
    private Button btnGo;

    private final Stack<Path> historyBack = new Stack<>();
    private final Stack<Path> historyForward = new Stack<>();

    private enum PanelMode {
        NORMAL,
        SEARCH_RESULTS
    }

    private PanelMode panelMode = PanelMode.NORMAL;
    private List<FileItem> searchResults = new ArrayList<>();
    private String searchQuery = "";

    private boolean isDarkTheme = false;

    private ContextMenu currentContextMenu;
    private Popup copyDestinationPopup;

    public FXFilePanel(String initialPath) {
        this.currentPath = Paths.get(initialPath);
        initializeUI();
        refreshFileList();
    }

    public void setMainWindow(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
    }

    private void initializeUI() {
        getStyleClass().add("file-panel");

        setFocusTraversable(true);

        navBar = createNavigationBar();
        setTop(navBar);

        tableView = createTableView();
        setCenter(tableView);

        statusBar = createStatusBar();
        setBottom(statusBar);

        loadStyles();
        setupContextMenu();
        setupDragAndDrop();
        setupKeyboardNavigation();

        addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            javafx.scene.Node target = (javafx.scene.Node) e.getTarget();
            while (target != null) {
                if (target == pathField) {
                    if (mainWindow != null) {
                        mainWindow.activatePanel(this);
                    }

                    Platform.runLater(() -> {
                        pathField.requestFocus();
                        pathField.positionCaret(pathField.getText().length());
                    });

                    return;
                }
                target = target.getParent();
            }

            if (!pathField.isFocused() && tableView.getEditingCell() == null) {
                requestFocus();
                requestActivation();

                if (!tableView.getItems().isEmpty() && isActive) {
                    if (tableView.getFocusModel().getFocusedIndex() < 0) {
                        tableView.getFocusModel().focus(0);
                        if (tableView.getSelectionModel().isEmpty()) {
                            tableView.getSelectionModel().select(0);
                        }
                    }
                }
            }
        });

        setOnMouseClicked(e -> {
            requestActivation();
            closeContextMenu();
        });
    }

    private void loadStyles() {
        try {
            String css = getClass().getResource("/styles/file-panel.css").toExternalForm();
            getStylesheets().clear();
            getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Failed to load CSS: " + e.getMessage());
        }
    }

    private HBox createNavigationBar() {
        HBox nav = new HBox(8);
        nav.getStyleClass().add("nav-bar");
        nav.setPadding(new Insets(12, 16, 12, 16));
        nav.setAlignment(Pos.CENTER_LEFT);

        String backPath = "M 10 0 L 0 5 L 10 10 Z";
        String forwardPath = "M 0 0 L 10 5 L 0 10 Z";
        String upPath = "M 5 0 L 10 10 L 0 10 Z";
        String goPath = "M 0 0 L 8 5 L 0 10 Z";
        String themePath = "M15.5 2.1c-6.1 0.2-10.7 5.3-10.4 11.4c0.2 4.8 3.9 8.8 8.6 9.4c-1.3-0.7-2.4-1.8-3.3-3.1c-2.3-3.4-1.5-8.1 1.9-10.4c1.9-1.3 4.3-1.8 6.2-1.4c-0.9-3.2-3.8-5.6-7-5.9h-4z";

        btnBack = createNavButton(backPath, "Back");
        btnBack.setOnAction(e -> navigateBack());

        btnForward = createNavButton(forwardPath, "Forward");
        btnForward.setOnAction(e -> navigateForward());

        btnUp = createNavButton(upPath, "Up");
        btnUp.setOnAction(e -> navigateUp());

        pathField = new TextField();
        pathField.getStyleClass().add("path-field");
        pathField.setPromptText("Enter path...");
        pathField.setOnAction(e -> navigateToInputPath());

        pathField.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.DOWN) {
                e.consume();
                tableView.requestFocus();
                if (!tableView.getItems().isEmpty()) {
                    tableView.getSelectionModel().select(0);
                    tableView.getFocusModel().focus(0);
                    updateStatus();
                }
            }
        });

        HBox.setHgrow(pathField, Priority.ALWAYS);

        btnGo = createNavButton(goPath, "Go");
        btnGo.setOnAction(e -> navigateToInputPath());

        Button btnTheme = createNavButton(themePath, "Toggle theme");
        btnTheme.setOnAction(e -> toggleTheme());

        nav.getChildren().addAll(btnBack, btnForward, btnUp, pathField, btnGo, btnTheme);
        return nav;
    }

    private Button createNavButton(String svgPath, String tooltip) {
        Button btn = new Button();
        btn.getStyleClass().add("nav-button");

        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("svg-icon");

        StackPane iconPane = new StackPane(icon);
        iconPane.setPrefSize(16, 16);

        btn.setGraphic(iconPane);
        btn.setText(null);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setMinSize(36, 36);
        btn.setMaxSize(36, 36);
        return btn;
    }

    private TableView<FileItem> createTableView() {
        TableView<FileItem> table = new TableView<>();
        table.getStyleClass().add("file-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        TableColumn<FileItem, String> nameCol = new TableColumn<>("Name");
        nameCol.getStyleClass().add("name-column");
        nameCol.setPrefWidth(400);

        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        nameCol.setCellFactory(col -> new SmartEditingCell());

        nameCol.setOnEditCommit(event -> {
            FileItem item = event.getRowValue();
            if (item == null) return;

            String newName = event.getNewValue();

            if (newName != null && !newName.trim().isEmpty() && !newName.equals(item.getName())) {
                Path oldPath = item.getPath();
                Path newPath = oldPath.getParent().resolve(newName.trim());
                final String finalNewName = newName.trim();

                if (isRenamingNewFolder) {
                    controller.executeDirectRename(oldPath, newPath);
                    isRenamingNewFolder = false;
                } else {
                    controller.executeRenameOperation(oldPath, newPath);
                }

                refreshFileList();

                new Thread(() -> {
                    try {
                        Thread.sleep(150);
                        Platform.runLater(() -> {
                            selectAndScrollToFile(finalNewName);
                        });
                    } catch (InterruptedException ignored) {
                    }
                }).start();
            } else {
                isRenamingNewFolder = false;

                Platform.runLater(() -> {
                    tableView.requestFocus();
                    int index = tableView.getItems().indexOf(item);
                    if (index >= 0 && index < tableView.getItems().size()) {
                        tableView.getFocusModel().focus(index);
                        tableView.getSelectionModel().clearAndSelect(index);
                    }
                });
            }
        });

        TableColumn<FileItem, String> sizeCol = new TableColumn<>("Size");
        sizeCol.getStyleClass().add("size-column");
        sizeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedSize()));
        sizeCol.setPrefWidth(100);
        sizeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<FileItem, String> modifiedCol = new TableColumn<>("Modified");
        modifiedCol.getStyleClass().add("modified-column");
        modifiedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedDate()));
        modifiedCol.setPrefWidth(130);
        modifiedCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        table.getColumns().addAll(nameCol, sizeCol, modifiedCol);

        Comparator<FileItem> defaultComparator = (o1, o2) -> {
            if (o1.isDirectory() && !o2.isDirectory()) return -1;
            if (!o1.isDirectory() && o2.isDirectory()) return 1;
            return o1.getName().compareToIgnoreCase(o2.getName());
        };

        table.setSortPolicy(param -> {
            FXCollections.sort(table.getItems(), table.getComparator());
            if (table.getSortOrder().isEmpty()) {
                FXCollections.sort(table.getItems(), defaultComparator);
                return true;
            }
            return true;
        });

        table.setRowFactory(tv -> {
            TableRow<FileItem> row = new TableRow<>() {
                @Override
                protected void updateItem(FileItem item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("file-row", "hidden-file-row", "folder-row", "focused-row");

                    if (item != null) {
                        if (item.isDirectory()) {
                            getStyleClass().add("folder-row");
                        }

                        if (item.isHidden()) {
                            getStyleClass().add("hidden-file-row");
                        } else {
                            getStyleClass().add("file-row");
                        }

                        if (getIndex() == tableView.getFocusModel().getFocusedIndex()) {
                            getStyleClass().add("focused-row");
                        }
                    }
                }
            };

            row.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    requestActivation();
                    closeContextMenu();

                    if (e.getClickCount() == 2 && !row.isEmpty()) {
                        openFile(row.getItem());
                        e.consume();
                    }
                }
            });
            return row;
        });

        table.getFocusModel().focusedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != null && oldVal.intValue() >= 0) {
                table.refresh();
            }
            if (newVal != null && newVal.intValue() >= 0) {
                table.refresh();
            }
        });

        table.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                requestActivation();
                closeContextMenu();
            }
        });

        return table;
    }

    private class SmartEditingCell extends TableCell<FileItem, String> {
        private TextField textField;
        private final HBox contentBox = new HBox(6);
        private final Label iconLabel = new Label();
        private boolean isCommitting = false;

        public SmartEditingCell() {
            contentBox.setAlignment(Pos.CENTER_LEFT);
            contentBox.setMaxWidth(Double.MAX_VALUE);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            iconLabel.getStyleClass().add("file-icon-label");
        }

        @Override
        public void startEdit() {
            if (!allowEdit) {
                return;
            }

            if (!isEmpty()) {
                super.startEdit();
                createTextField();

                FileItem item = getItemObject();
                if (item != null) {
                    iconLabel.setText(item.getIcon());
                }

                setText(null);
                contentBox.getChildren().clear();
                contentBox.getChildren().addAll(iconLabel, textField);
                HBox.setHgrow(textField, Priority.ALWAYS);
                setGraphic(contentBox);

                Platform.runLater(() -> {
                    if (textField != null) {
                        textField.requestFocus();
                        textField.selectAll();
                    }
                });
            }
        }

        @Override
        public void cancelEdit() {
            if (isCommitting) return;

            if (isEmpty() || getTableRow() == null || getTableRow().getItem() == null) {
                super.cancelEdit();
                setText(null);
                setGraphic(null);
                return;
            }

            if (textField != null && isEditing()) {
                String newText = textField.getText();
                if (newText != null && !newText.trim().isEmpty() && !newText.equals(getItem())) {
                    isCommitting = true;
                    try {
                        commitEdit(newText);
                    } finally {
                        isCommitting = false;
                    }
                } else {
                    performCancel();
                }
            } else {
                super.cancelEdit();
            }

            setText((String) getItem());
            setGraphic(null);

            if (getTableRow() != null && getTableRow().getItem() != null) {
                updateItem(getTableRow().getItem().getName(), false);
            }
        }

        private void performCancel() {
            isRenamingNewFolder = false;
            super.cancelEdit();

            Platform.runLater(() -> {
                if (tableView != null) {
                    tableView.requestFocus();
                    int index = getIndex();
                    if (index >= 0 && index < tableView.getItems().size()) {
                        tableView.getFocusModel().focus(index);
                    }
                }
            });
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(contentBox);
                } else {
                    FileItem fileItem = getItemObject();

                    if (fileItem != null) {
                        iconLabel.setText(fileItem.getIcon());
                        Label textPart = new Label(getString());
                        if (fileItem.isHidden()) textPart.setText(textPart.getText() + " Ⓗ");
                        textPart.setStyle("-fx-text-fill: inherit; -fx-font-weight: 700;");

                        contentBox.getChildren().clear();
                        contentBox.getChildren().addAll(iconLabel, textPart);
                        setGraphic(contentBox);
                    } else {
                        setText(getString());
                        setGraphic(null);
                    }
                }
            }
        }

        private FileItem getItemObject() {
            if (getTableRow() != null) {
                return getTableRow().getItem();
            } else if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                return getTableView().getItems().get(getIndex());
            }
            return null;
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.getStyleClass().add("file-edit-field");
            textField.setMinWidth(50);

            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    t.consume();

                    String newText = textField.getText();

                    if (newText != null && !newText.trim().isEmpty()) {
                        isCommitting = true;
                        try {
                            commitEdit(newText);
                        } finally {
                            isCommitting = false;
                        }
                    } else {
                        performCancel();
                    }
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    t.consume();
                    performCancel();
                }
            });

            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    if (!isCommitting) {
                        cancelEdit();
                    }
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }

    public void setTheme(boolean isDark) {
        this.isDarkTheme = isDark;

        getStyleClass().remove("dark-theme");
        if (isDarkTheme) {
            getStyleClass().add("dark-theme");
        }

        refreshFileList();
    }

    private HBox createStatusBar() {
        HBox status = new HBox();
        status.getStyleClass().add("status-bar");
        status.setPadding(new Insets(8, 16, 8, 16));

        statusLabel = new Label("Selected: 0 | 0 folders, 0 files");
        statusLabel.getStyleClass().add("status-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        diskSpaceLabel = new Label("0.0 GB free");
        diskSpaceLabel.getStyleClass().add("disk-space-label");

        status.getChildren().addAll(statusLabel, spacer, diskSpaceLabel);
        return status;
    }

    private void setupKeyboardNavigation() {
        addEventFilter(KeyEvent.KEY_PRESSED, this::handleExternalKeyEvent);
        tableView.setOnMousePressed(e -> selectionAnchor = -1);
    }

    public void handleExternalKeyEvent(KeyEvent e) {
        if (pathField.isFocused()) return;

        if (tableView.getEditingCell() != null) {
            javafx.scene.Node focusOwner = getScene().getFocusOwner();
            if (focusOwner instanceof TextField) {
                return;
            }
        }

        if (e.getCode() == KeyCode.LEFT && !e.isControlDown()) {
            e.consume();
            if (mainWindow != null) mainWindow.activatePanel(mainWindow.getLeftPanel());
            return;
        } else if (e.getCode() == KeyCode.RIGHT && !e.isControlDown()) {
            e.consume();
            if (mainWindow != null) mainWindow.activatePanel(mainWindow.getRightPanel());
            return;
        }

        if (e.isControlDown() && e.getCode() == KeyCode.A) {
            e.consume();
            tableView.getSelectionModel().selectAll();
            updateStatus();
            return;
        }

        if (tableView.getItems().isEmpty()) {
            if (e.getCode() == KeyCode.TAB && !e.isControlDown()) {
                e.consume();
                if (mainWindow != null) {
                    if (this == mainWindow.getLeftPanel()) mainWindow.activatePanel(mainWindow.getRightPanel());
                    else mainWindow.activatePanel(mainWindow.getLeftPanel());
                }
            }
            else if (e.getCode() == KeyCode.UP && !isInSearchMode()) {
                e.consume();
                pathField.requestFocus();
                pathField.positionCaret(pathField.getText().length());
            }
            return;
        }

        TableView.TableViewSelectionModel<FileItem> selectionModel = tableView.getSelectionModel();
        FocusModel<FileItem> focusModel = tableView.getFocusModel();
        int itemsCount = tableView.getItems().size();
        int focusedIndex = focusModel.getFocusedIndex();

        if (focusedIndex < 0 && !selectionModel.isEmpty()) {
            focusedIndex = selectionModel.getSelectedIndex();
            focusModel.focus(focusedIndex);
        }
        if (focusedIndex < 0 && itemsCount > 0) {
            focusedIndex = 0;
            focusModel.focus(0);
            selectionModel.select(0);
        }

        if (e.getCode() == KeyCode.UP || e.getCode() == KeyCode.DOWN) {
            e.consume();

            if (!tableView.isFocused()) tableView.requestFocus();

            if (e.getCode() == KeyCode.UP && focusedIndex == 0 && !e.isShiftDown() && !e.isControlDown()) {
                if (!isInSearchMode()) {
                    pathField.requestFocus();
                    pathField.positionCaret(pathField.getText().length());
                    updateStatus();
                }
                return;
            }

            int direction = e.getCode() == KeyCode.UP ? -1 : 1;
            int candidateIndex = focusedIndex + direction;

            if (e.isShiftDown()) {
                if (selectionAnchor == -1) selectionAnchor = focusedIndex;
                if (candidateIndex < 0) candidateIndex = 0;
                if (candidateIndex >= itemsCount) candidateIndex = itemsCount - 1;

                selectionModel.clearSelection();
                int from = Math.min(selectionAnchor, candidateIndex);
                int to = Math.max(selectionAnchor, candidateIndex) + 1;
                selectionModel.selectRange(from, to);
                focusModel.focus(candidateIndex);
            } else if (e.isControlDown()) {
                if (candidateIndex >= 0 && candidateIndex < itemsCount) focusModel.focus(candidateIndex);
                selectionAnchor = -1;
            } else {
                if (candidateIndex >= 0 && candidateIndex < itemsCount) {
                    selectionModel.clearAndSelect(candidateIndex);
                    focusModel.focus(candidateIndex);
                }
                selectionAnchor = -1;
            }
            scrollToIfNotVisible(focusModel.getFocusedIndex());
            updateStatus();
        } else if (e.getCode() == KeyCode.TAB && !e.isControlDown()) {
            e.consume();
            if (mainWindow != null) {
                FXFilePanel other = mainWindow.getOtherPanel(this);
                mainWindow.activatePanel(other);
            }
        } else if (e.isControlDown() && e.getCode() == KeyCode.TAB) {
            e.consume();
            if (focusedIndex >= 0 && focusedIndex < itemsCount) {
                if (selectionModel.isSelected(focusedIndex)) {
                    selectionModel.clearSelection(focusedIndex);
                } else {
                    selectionModel.select(focusedIndex);
                }
                updateStatus();
            }
        } else if (e.isControlDown() && e.getCode() == KeyCode.SPACE) {
            e.consume();
            if (selectionModel.isSelected(focusedIndex)) selectionModel.clearSelection(focusedIndex);
            else selectionModel.select(focusedIndex);
            updateStatus();
        } else if (e.getCode() == KeyCode.ENTER) {
            e.consume();
            FileItem item = focusModel.getFocusedItem();
            if (item == null) item = selectionModel.getSelectedItem();
            if (item != null) openFile(item);
        }
    }

    private void scrollToIfNotVisible(int index) {
        if (index < 0 || index >= tableView.getItems().size()) {
            return;
        }

        try {
            Object virtualFlow = tableView.lookup(".virtual-flow");
            if (virtualFlow == null) {
                tableView.scrollTo(Math.max(0, index - 1));
                return;
            }

            java.lang.reflect.Method getFirstVisibleCellMethod = virtualFlow.getClass().getMethod("getFirstVisibleCell");
            java.lang.reflect.Method getLastVisibleCellMethod = virtualFlow.getClass().getMethod("getLastVisibleCell");
            Object firstCell = getFirstVisibleCellMethod.invoke(virtualFlow);
            Object lastCell = getLastVisibleCellMethod.invoke(virtualFlow);

            if (firstCell != null && lastCell != null) {
                java.lang.reflect.Method getIndexMethod = firstCell.getClass().getMethod("getIndex");
                int firstVisible = (int) getIndexMethod.invoke(firstCell);
                int lastVisible = (int) getIndexMethod.invoke(lastCell);

                if (index <= firstVisible) {
                    tableView.scrollTo(Math.max(0, index - 1));
                }
                else if (index >= lastVisible - 1) {
                    int visibleRows = lastVisible - firstVisible;
                    tableView.scrollTo(Math.max(0, index - visibleRows + 2));
                }

            } else {
                tableView.scrollTo(Math.max(0, index - 1));
            }
        } catch (Exception e) {
            tableView.scrollTo(Math.max(0, index - 1));
        }
    }

    private void setupContextMenu() {
        tableView.setOnContextMenuRequested(e -> {
            e.consume();

            if (tableView.getEditingCell() != null) {
                tableView.edit(-1, null);
            }

            closeContextMenu();
            requestActivation();

            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getStyleClass().add("modern-context-menu");
            currentContextMenu = contextMenu;

            contextMenu.setOnHidden(event -> {
                currentContextMenu = null;

                if (mainWindow != null) {
                    mainWindow.activatePanel(this);
                }

                Platform.runLater(() -> {
                    tableView.requestFocus();

                    if (!tableView.getItems().isEmpty()) {
                        int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
                        if (selectedIndex >= 0) {
                            tableView.getFocusModel().focus(selectedIndex);
                            tableView.scrollTo(selectedIndex);
                        } else {
                            tableView.getSelectionModel().select(0);
                            tableView.getFocusModel().focus(0);
                        }
                    }
                });
            });

            boolean hasSelection = !tableView.getSelectionModel().getSelectedItems().isEmpty();
            boolean singleSelection = tableView.getSelectionModel().getSelectedItems().size() == 1;

            if (isInSearchMode()) {
                if (singleSelection) {
                    MenuItem gotoItem = new MenuItem("\uD83D\uDCCD Go to Location");
                    gotoItem.setOnAction(ev -> showGoToLocationChoice(tableView.getSelectionModel().getSelectedItem().getPath()));
                    contextMenu.getItems().add(gotoItem);

                    if (tableView.getSelectionModel().getSelectedItem().isDirectory()) {
                        MenuItem openFolderItem = new MenuItem("\uD83D\uDCC1 Open Folder");
                        openFolderItem.setOnAction(ev -> showSearchModeActionDialog(tableView.getSelectionModel().getSelectedItem()));
                        contextMenu.getItems().add(openFolderItem);
                    } else {
                        MenuItem openFileItem = new MenuItem("\uD83D\uDCC1 Open File");
                        openFileItem.setOnAction(ev -> {
                            try {
                                java.awt.Desktop.getDesktop().open(tableView.getSelectionModel().getSelectedItem().getPath().toFile());
                            } catch (Exception ex) {
                                System.err.println("Cannot open file: " + ex.getMessage());
                            }
                        });
                        contextMenu.getItems().add(openFileItem);
                    }
                }

                if (!contextMenu.getItems().isEmpty()) {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }

                MenuItem copyItem = new MenuItem("\uD83D\uDCCB Copy to Other Panel");
                copyItem.setDisable(!hasSelection);
                copyItem.setOnAction(ev -> {
                    if (mainWindow != null) {
                        FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
                        Path destination = otherPanel.getCurrentPath();
                        List<Path> sources = tableView.getSelectionModel().getSelectedItems().stream()
                                .map(FileItem::getPath)
                                .collect(Collectors.toList());
                        controller.executeCopyOperationDirect(sources, destination);
                    }
                });
                contextMenu.getItems().add(copyItem);

                MenuItem copyToCache = new MenuItem("\uD83D\uDCCB Copy to Cache (Ctrl+C)");
                copyToCache.setDisable(!hasSelection);
                copyToCache.setOnAction(ev -> copyToClipboard());
                contextMenu.getItems().add(copyToCache);

            } else {
                if (singleSelection && !tableView.getSelectionModel().getSelectedItem().isDirectory()) {
                    MenuItem openItem = new MenuItem("\uD83D\uDCC1 Open");
                    openItem.setOnAction(ev -> openFile(tableView.getSelectionModel().getSelectedItem()));
                    contextMenu.getItems().add(openItem);
                }

                if (!contextMenu.getItems().isEmpty()) {
                    contextMenu.getItems().add(new SeparatorMenuItem());
                }

                MenuItem copyItem = new MenuItem("\uD83D\uDCCB Copy (F3)");
                copyItem.setDisable(!hasSelection);
                copyItem.setOnAction(ev -> executeCommand("copy"));
                contextMenu.getItems().add(copyItem);

                MenuItem moveItem = new MenuItem("\uD83D\uDD00 Move (F6)");
                moveItem.setDisable(!hasSelection || isOtherPanelInSearchMode());
                moveItem.setOnAction(ev -> executeCommand("move"));
                contextMenu.getItems().add(moveItem);

                MenuItem deleteItem = new MenuItem("\uD83D\uDDD1 Delete (F8)");
                deleteItem.setDisable(!hasSelection);
                deleteItem.setOnAction(ev -> executeCommand("delete"));
                contextMenu.getItems().add(deleteItem);

                contextMenu.getItems().add(new SeparatorMenuItem());

                MenuItem newFolderItem = new MenuItem("\u271A New Folder (F7)");
                newFolderItem.setOnAction(ev -> executeCommand("newFolder"));
                contextMenu.getItems().add(newFolderItem);

                MenuItem renameItem = new MenuItem("\u270F Rename (F2)");
                renameItem.setDisable(!singleSelection);
                renameItem.setOnAction(ev -> executeCommand("rename"));
                contextMenu.getItems().add(renameItem);

                contextMenu.getItems().add(new SeparatorMenuItem());

                MenuItem copyToCache = new MenuItem("\uD83D\uDCCB Copy to Cache (Ctrl+C)");
                copyToCache.setDisable(!hasSelection);
                copyToCache.setOnAction(ev -> copyToClipboard());
                contextMenu.getItems().add(copyToCache);

                MenuItem pasteItem = new MenuItem("\uD83D\uDCCB Paste from Cache (Ctrl+V)");
                pasteItem.setOnAction(ev -> pasteFromClipboard());
                contextMenu.getItems().add(pasteItem);
            }

            contextMenu.show(tableView, e.getScreenX(), e.getScreenY());
        });
    }

    private void closeContextMenu() {
        if (currentContextMenu != null && currentContextMenu.isShowing()) {
            currentContextMenu.hide();
            currentContextMenu = null;
        }
        if (copyDestinationPopup != null && copyDestinationPopup.isShowing()) {
            copyDestinationPopup.hide();
            copyDestinationPopup = null;
        }
    }

    private void setupDragAndDrop() {
        tableView.setOnDragDetected(e -> {
            ObservableList<FileItem> selectedItems = tableView.getSelectionModel().getSelectedItems();
            if (selectedItems == null || selectedItems.isEmpty()) {
                return;
            }

            Dragboard db = tableView.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            ClipboardContent content = new ClipboardContent();

            List<Path> paths = selectedItems.stream()
                    .map(FileItem::getPath)
                    .collect(Collectors.toList());

            content.putString(
                    paths.stream()
                            .map(Path::toString)
                            .collect(Collectors.joining("\n"))
            );

            db.setContent(content);
            e.consume();
        });

        tableView.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();

            if (db.hasString()) {
                if (e.getGestureSource() == tableView) {
                    TableRow<FileItem> row = findRowAt(e.getY());
                    if (row != null && row.getItem() != null && row.getItem().isDirectory()) {
                        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                } else {
                    e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
            }
            e.consume();
        });

        tableView.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                String[] pathStrings = db.getString().split("\n");
                List<Path> sources = Arrays.stream(pathStrings)
                        .map(Paths::get)
                        .collect(Collectors.toList());

                if (e.getGestureSource() == tableView) {
                    TableRow<FileItem> row = findRowAt(e.getY());
                    if (row != null && row.getItem() != null && row.getItem().isDirectory()) {
                        Path target = row.getItem().getPath();

                        if (e.getTransferMode() == TransferMode.COPY) {
                            controller.executeCopyDirect(sources, target, this);
                        } else {
                            controller.executeMoveDirect(sources, target, this);
                        }
                        success = true;
                    }
                } else {
                    if (e.getTransferMode() == TransferMode.COPY) {
                        controller.executeCopyDirect(sources, currentPath, this);
                    } else {
                        controller.executeMoveDirect(sources, currentPath, this);
                    }
                    success = true;
                }
            }

            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void selectAndScrollToFile(String fileName) {
        tableView.requestFocus();

        int index = -1;
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (tableView.getItems().get(i).getName().equals(fileName)) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            tableView.getSelectionModel().clearAndSelect(index);
            tableView.getFocusModel().focus(index);
            tableView.scrollTo(index);
            scrollToIfNotVisible(index);
        }
    }

    private TableRow<FileItem> findRowAt(double y) {
        for (int i = 0; i < tableView.getItems().size(); i++) {
            TableRow<FileItem> row = (TableRow<FileItem>) tableView.lookup(".table-row-cell:nth-child(" + (i + 1) + ")");
            if (row != null && row.getBoundsInParent().contains(0, y)) {
                return row;
            }
        }
        return null;
    }

    public void requestActivation() {
        if (mainWindow != null) {
            mainWindow.activatePanel(this);
        }

        Platform.runLater(() -> {
            requestFocus();
            if (!tableView.getItems().isEmpty()) {
                if (tableView.getFocusModel().getFocusedIndex() < 0) {
                    tableView.getFocusModel().focus(0);
                    if (tableView.getSelectionModel().isEmpty()) {
                        tableView.getSelectionModel().select(0);
                    }
                }
            }
        });
    }

    public void setActive(boolean active) {
        this.isActive = active;

        if (active) {
            getStyleClass().removeAll("file-panel-inactive");
            getStyleClass().add("file-panel-active");

            Platform.runLater(() -> {
                if (pathField.isFocused()) {
                    return;
                }

                if (!tableView.getItems().isEmpty()) {
                    if (tableView.getFocusModel().getFocusedIndex() < 0) {
                        tableView.getFocusModel().focus(0);
                        tableView.getSelectionModel().clearAndSelect(0);
                        tableView.scrollTo(0);
                    }

                    tableView.requestFocus();
                    updateStatus();
                } else {
                    this.requestFocus();
                }
            });
        } else {
            getStyleClass().removeAll("file-panel-active");
            getStyleClass().add("file-panel-inactive");
            tableView.getSelectionModel().clearSelection();
            updateStatus();
            closeContextMenu();
        }
    }

    public void ensureFirstItemSelected() {
        if (!tableView.getItems().isEmpty() && tableView.getSelectionModel().isEmpty()) {
            tableView.getSelectionModel().clearAndSelect(0);
            tableView.getFocusModel().focus(0);
            tableView.scrollTo(0);
            updateStatus();
        }
        if (isActive) {
            tableView.requestFocus();
        }
    }

    public void toggleTheme() {
        isDarkTheme = !isDarkTheme;

        if (isDarkTheme) {
            getStyleClass().add("dark-theme");
        } else {
            getStyleClass().remove("dark-theme");
        }
    }

    public void refreshFileList() {
        refreshFileListData();
    }

    private void refreshFileListData() {
        Platform.runLater(() -> {
            List<FileItem> files;

            if (panelMode == PanelMode.SEARCH_RESULTS) {
                files = searchResults;
                pathField.setText("Search results: \"" + searchQuery + "\" (" + searchResults.size() + " items)");
                diskSpaceLabel.setText("0.0 GB free");

                pathField.setDisable(true);
                btnBack.setDisable(true);
                btnForward.setDisable(true);
                btnUp.setDisable(true);
                btnGo.setDisable(true);
            } else {
                if (!Files.exists(currentPath)) {
                    Path parent = currentPath.getParent();
                    while (parent != null && !Files.exists(parent)) {
                        parent = parent.getParent();
                    }
                    if (parent != null) {
                        currentPath = parent;
                    } else {
                        currentPath = Paths.get(System.getProperty("user.home"));
                    }
                }

                files = controller.getFilesInDirectory(currentPath);
                pathField.setText(currentPath.toString());
                updateDiskSpace();

                pathField.setDisable(false);
                btnBack.setDisable(false);
                btnForward.setDisable(false);
                btnUp.setDisable(false);
                btnGo.setDisable(false);
            }

            FileItem selectedItem = tableView.getSelectionModel().getSelectedItem();
            int selectedIndex = tableView.getSelectionModel().getSelectedIndex();

            ObservableList<FileItem> items = FXCollections.observableArrayList(files);
            tableView.setItems(items);

            if (selectedItem != null && selectedIndex >= 0 && selectedIndex < items.size()) {
                tableView.getSelectionModel().select(selectedIndex);
                tableView.getFocusModel().focus(selectedIndex);
            } else if (!items.isEmpty() && isActive) {
                tableView.getSelectionModel().clearAndSelect(0);
                tableView.getFocusModel().focus(0);
            }

            updateStatus();

            if (isActive) {
                Platform.runLater(() -> {
                    if (!items.isEmpty()) {
                        tableView.requestFocus();
                        if (panelMode == PanelMode.SEARCH_RESULTS) {
                            if (tableView.getFocusModel().getFocusedIndex() < 0) {
                                tableView.getFocusModel().focus(0);
                                tableView.getSelectionModel().clearAndSelect(0);
                            }
                        }
                    } else {
                        this.requestFocus();
                    }
                });
            }
        });
    }

    private void updateDiskSpace() {
        try {
            long availableBytes = Files.getFileStore(currentPath).getUsableSpace();
            double availableGB = availableBytes / (1024.0 * 1024.0 * 1024.0);
            diskSpaceLabel.setText(String.format("%.1f GB free", availableGB));
        } catch (Exception e) {
            diskSpaceLabel.setText("0.0 GB free");
        }
    }

    private void updateStatus() {
        int selected = tableView.getSelectionModel().getSelectedItems().size();
        int total = tableView.getItems().size();
        int folders = (int) tableView.getItems().stream().filter(FileItem::isDirectory).count();
        int files = total - folders;
        statusLabel.setText(String.format("Selected: %d | %d folders, %d files", selected, folders, files));
    }

    public void handleKeyPress(KeyCode keyCode) {
        switch (keyCode) {
            case F2:
                executeCommand("rename");
                break;
            case F3:
                executeCommand("copy");
                break;
            case F5:
                refreshFileList();
                break;
            case F6:
            case F4:
                executeCommand("move");
                break;
            case F7:
                executeCommand("newFolder");
                break;
            case F8:
            case DELETE:
                executeCommand("delete");
                break;
            case ESCAPE:
                if (isInSearchMode()) {
                    exitSearchMode();
                } else {
                    navigateUp();
                }
                break;
        }
    }

    private void executeCommand(String command) {
        if (isInSearchMode() && (command.equals("move") || command.equals("newFolder") || command.equals("rename") || command.equals("delete"))) {
            showSearchModeError();
            return;
        }

        switch (command) {
            case "copy":
                if (isInSearchMode()) {
                    if (mainWindow != null && !tableView.getSelectionModel().getSelectedItems().isEmpty()) {
                        FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
                        Path destination = otherPanel.getCurrentPath();
                        List<Path> sources = tableView.getSelectionModel().getSelectedItems().stream()
                                .map(FileItem::getPath)
                                .collect(Collectors.toList());
                        controller.executeCopyOperationDirect(sources, destination);
                    }
                } else {
                    controller.showCopyDestinationMenu(this);
                }
                break;
            case "move":
                if (!tableView.getSelectionModel().getSelectedItems().isEmpty()) {
                    controller.initiateMoveOperation(this);
                }
                break;
            case "delete":
                if (!tableView.getSelectionModel().getSelectedItems().isEmpty()) {
                    controller.initiateDeleteOperation(this);
                }
                break;
            case "newFolder":
                createNewFolder();
                break;
            case "rename":
                if (tableView.getSelectionModel().getSelectedItems().size() == 1) {
                    renameFile(tableView.getSelectionModel().getSelectedItem());
                }
                break;
        }
    }

    public void showCopyDestinationPopup() {
        List<FileItem> selectedFiles = tableView.getSelectionModel().getSelectedItems();
        if (selectedFiles.isEmpty()) {
            return;
        }
        closeContextMenu();

        Path currentFolderPath = getCurrentPath();
        Path otherPanelPath = getDestinationPath(this);

        if (mainWindow != null) {
            FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
            if (otherPanel != null && otherPanel.isInSearchMode()) {
                controller.executeCopyOperation(this, currentFolderPath);
                return;
            }
        }

        String currentName = currentFolderPath.getFileName() != null ?
                currentFolderPath.getFileName().toString() : currentFolderPath.toString();
        String otherName = otherPanelPath.getFileName() != null ?
                otherPanelPath.getFileName().toString() : otherPanelPath.toString();

        copyDestinationPopup = new Popup();
        VBox popupContent = new VBox(8);
        popupContent.getStyleClass().add("copy-destination-popup");

        String bgColor = isDarkTheme ? "linear-gradient(to bottom, rgba(35,35,35,0.98), rgba(30,30,30,0.98))" : "linear-gradient(to bottom, rgba(255,255,255,0.98), rgba(252,252,253,0.98))";
        String borderColor = isDarkTheme ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";
        popupContent.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.2, 0, 8);" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;"
        );

        Label titleLabel = new Label("Copy " + selectedFiles.size() + " item(s) to:");
        String textColor = isDarkTheme ? "#f1f5f9" : "#1e293b";
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + textColor + "; -fx-padding: 0 0 8 0;");

        String currentButtonText = String.format("Current Panel: '%s' (C)", currentName);
        String otherButtonText = String.format("Other Panel: '%s' (O)", otherName);

        Button currentButton = new Button("📂 " + currentButtonText);

        currentButton.getStyleClass().add("popup-button");
        String currentBtnBg = isDarkTheme ? "linear-gradient(to bottom, rgba(50,50,50,0.98), rgba(42,42,42,0.95))" : "linear-gradient(to bottom, rgba(248,250,252,0.98), rgba(241,245,249,0.98))";
        String currentBtnBorder = isDarkTheme ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)";
        currentButton.setStyle(
                "-fx-background-color: " + currentBtnBg + ";" +
                        "-fx-border-color: " + currentBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + textColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left;"
        );

        Button otherButton = new Button("📂 " + otherButtonText);
        otherButton.getStyleClass().add("popup-button");
        String otherBtnBg = isDarkTheme ? "linear-gradient(from 0% 0% to 100% 100%, #00d4e6 0%, #00e8ff 100%)" : "linear-gradient(from 0% 0% to 100% 100%, #0099a8 0%, #00b8cc 100%)";
        String otherBtnEffect = isDarkTheme ? "dropshadow(gaussian, rgba(0,212,230,0.5), 12, 0, 0, 3)" : "dropshadow(gaussian, rgba(0,153,168,0.4), 12, 0, 0, 3)";
        otherButton.setStyle(
                "-fx-background-color: " + otherBtnBg + ";" +
                        "-fx-border-color: transparent; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left; -fx-effect: " + otherBtnEffect + ";"
        );

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("popup-button");
        String cancelBtnBg = isDarkTheme ? "rgba(50,50,50,0.5)" : "transparent";
        String cancelBtnBorder = isDarkTheme ? "rgba(255,255,255,0.12)" : "rgba(0,0,0,0.12)";
        String cancelTextColor = isDarkTheme ? "#94a3b8" : "#64748b";
        cancelButton.setStyle(
                "-fx-background-color: " + cancelBtnBg + "; -fx-border-color: " + cancelBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + cancelTextColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px;"
        );

        currentButton.setOnAction(e -> {
            copyDestinationPopup.hide();
            controller.executeCopyOperation(this, currentFolderPath);
        });
        otherButton.setOnAction(e -> {
            copyDestinationPopup.hide();
            controller.executeCopyOperation(this, otherPanelPath);
        });
        cancelButton.setOnAction(e -> copyDestinationPopup.hide());

        popupContent.getChildren().addAll(titleLabel, currentButton, otherButton, cancelButton);
        copyDestinationPopup.getContent().add(popupContent);

        popupContent.setOnKeyPressed(e -> {
            String text = e.getText().toLowerCase();
            if (e.getCode() == KeyCode.C || text.equals("с")) {
                e.consume();
                copyDestinationPopup.hide();
                controller.executeCopyOperation(this, currentFolderPath);
            } else if (e.getCode() == KeyCode.O || text.equals("о")) {
                e.consume();
                copyDestinationPopup.hide();
                controller.executeCopyOperation(this, otherPanelPath);
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                copyDestinationPopup.hide();
            }
        });

        double centerX = this.getScene().getWindow().getX() + this.getScene().getWindow().getWidth() / 2 - 150;
        double centerY = this.getScene().getWindow().getY() + this.getScene().getWindow().getHeight() / 2 - 100;
        copyDestinationPopup.show(this.getScene().getWindow(), centerX, centerY);
        Platform.runLater(popupContent::requestFocus);
    }

    private void createNewFolder() {
        controller.createDefaultFolder(this);
    }

    public void selectAndEditFile(String fileName) {
        selectAndEditFileWithRetry(fileName, 0);
    }

    private void selectAndEditFileWithRetry(String fileName, int attempt) {
        int index = -1;
        for (int i = 0; i < tableView.getItems().size(); i++) {
            if (tableView.getItems().get(i).getName().equals(fileName)) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            final int finalIndex = index;

            String name = tableView.getItems().get(index).getName();
            if (name.startsWith("New folder")) {
                isRenamingNewFolder = true;
            }

            tableView.getSelectionModel().clearAndSelect(finalIndex);
            tableView.scrollTo(finalIndex);
            tableView.getFocusModel().focus(finalIndex);

            Platform.runLater(() -> {
                tableView.layout();

                allowEdit = true;
                tableView.edit(finalIndex, tableView.getColumns().get(0));
                allowEdit = false;

                boolean success = tableView.getEditingCell() != null
                        && tableView.getEditingCell().getRow() == finalIndex
                        && tableView.getEditingCell().getTableColumn() == tableView.getColumns().get(0);

                if (!success && attempt < 10) {
                    new Thread(() -> {
                        try {
                            Thread.sleep(100);
                            Platform.runLater(() -> selectAndEditFileWithRetry(fileName, attempt + 1));
                        } catch (InterruptedException ignored) {
                        }
                    }).start();
                }
            });
        } else {
            if (attempt < 20) {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        Platform.runLater(() -> selectAndEditFileWithRetry(fileName, attempt + 1));
                    } catch (InterruptedException ignored) {
                    }
                }).start();
            }
        }
    }


    private void renameFile(FileItem item) {
        if (item != null) {
            int index = tableView.getItems().indexOf(item);
            if (index >= 0) {
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }

                    Platform.runLater(() -> {
                        tableView.getSelectionModel().clearAndSelect(index);
                        tableView.getFocusModel().focus(index);
                        tableView.scrollTo(index);

                        allowEdit = true;
                        tableView.edit(index, tableView.getColumns().get(0));
                        allowEdit = false;
                    });
                }).start();
            }
        }
    }

    void copyToClipboard() {
        List<Path> paths = tableView.getSelectionModel().getSelectedItems().stream()
                .map(FileItem::getPath)
                .collect(Collectors.toList());
        if (!paths.isEmpty()) {
            controller.copyToClipboard(paths);
        }
    }

    void pasteFromClipboard() {
        if (isInSearchMode()) {
            return;
        }
        controller.pasteFromClipboard(this);
    }

    private void openFile(FileItem file) {
        if (isInSearchMode()) {
            showSearchModeActionDialog(file);
        } else {
            if (file.isDirectory()) {
                navigateToPath(file.getPath());
            } else {
                try {
                    java.awt.Desktop.getDesktop().open(file.getPath().toFile());
                } catch (Exception e) {
                    System.err.println("Cannot open file: " + e.getMessage());
                }
            }
        }
    }

    private void showSearchModeActionDialog(FileItem file) {
        Popup actionPopup = new Popup();
        VBox popupContent = new VBox(8);

        String bgColor = isDarkTheme ? "linear-gradient(to bottom, rgba(35,35,35,0.98), rgba(30,30,30,0.98))" : "linear-gradient(to bottom, rgba(255,255,255,0.98), rgba(252,252,253,0.98))";
        String borderColor = isDarkTheme ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";
        popupContent.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.2, 0, 8);" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;"
        );

        String itemName = file.getName().length() > 30 ? file.getName().substring(0, 30) + "..." : file.getName();
        Label titleLabel = new Label("What would you like to do with '" + itemName + "'?");
        String textColor = isDarkTheme ? "#f1f5f9" : "#1e293b";
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + textColor + "; -fx-padding: 0 0 8 0;");

        String gotoButtonText = file.isDirectory() ? "📂 Go to Location (G)" : "📍 Go to Location (G)";
        String openButtonText = file.isDirectory() ? "📂 Open Folder (O)" : "📄 Open File (O)";

        Button gotoButton = new Button(gotoButtonText);
        gotoButton.getStyleClass().add("popup-button");
        String gotoBtnBg = isDarkTheme ? "linear-gradient(to bottom, rgba(50,50,50,0.98), rgba(42,42,42,0.95))" : "linear-gradient(to bottom, rgba(248,250,252,0.98), rgba(241,245,249,0.98))";
        String gotoBtnBorder = isDarkTheme ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)";
        gotoButton.setStyle(
                "-fx-background-color: " + gotoBtnBg + ";" +
                        "-fx-border-color: " + gotoBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + textColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left;"
        );

        Button openButton = new Button(openButtonText);
        openButton.getStyleClass().add("popup-button");
        String openBtnBg = isDarkTheme ? "linear-gradient(from 0% 0% to 100% 100%, #00d4e6 0%, #00e8ff 100%)" : "linear-gradient(from 0% 0% to 100% 100%, #0099a8 0%, #00b8cc 100%)";
        String openBtnEffect = isDarkTheme ? "dropshadow(gaussian, rgba(0,212,230,0.5), 12, 0, 0, 3)" : "dropshadow(gaussian, rgba(0,153,168,0.4), 12, 0, 0, 3)";
        openButton.setStyle(
                "-fx-background-color: " + openBtnBg + ";" +
                        "-fx-border-color: transparent; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left; -fx-effect: " + openBtnEffect + ";"
        );

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("popup-button");
        String cancelBtnBg = isDarkTheme ? "rgba(50,50,50,0.5)" : "transparent";
        String cancelBtnBorder = isDarkTheme ? "rgba(255,255,255,0.12)" : "rgba(0,0,0,0.12)";
        String cancelTextColor = isDarkTheme ? "#94a3b8" : "#64748b";
        cancelButton.setStyle(
                "-fx-background-color: " + cancelBtnBg + "; -fx-border-color: " + cancelBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + cancelTextColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px;"
        );

        gotoButton.setOnAction(e -> {
            actionPopup.hide();
            showGoToLocationChoice(file.getPath());
        });

        openButton.setOnAction(e -> {
            actionPopup.hide();
            if (file.isDirectory()) {
                showOpenFolderChoice(file.getPath());
            } else {
                try {
                    java.awt.Desktop.getDesktop().open(file.getPath().toFile());
                } catch (Exception ex) {
                    System.err.println("Cannot open file: " + ex.getMessage());
                }
            }
        });

        cancelButton.setOnAction(e -> actionPopup.hide());

        popupContent.getChildren().addAll(titleLabel, gotoButton, openButton, cancelButton);
        actionPopup.getContent().add(popupContent);

        popupContent.setOnKeyPressed(e -> {
            String text = e.getText().toLowerCase();
            if (e.getCode() == KeyCode.G || text.equals("п") || text.equals("g")) {
                e.consume();
                actionPopup.hide();
                showGoToLocationChoice(file.getPath());
            } else if (e.getCode() == KeyCode.O || text.equals("щ") || text.equals("o")) {
                e.consume();
                actionPopup.hide();
                if (file.isDirectory()) {
                    showOpenFolderChoice(file.getPath());
                } else {
                    try {
                        java.awt.Desktop.getDesktop().open(file.getPath().toFile());
                    } catch (Exception ex) {
                        System.err.println("Cannot open file: " + ex.getMessage());
                    }
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                actionPopup.hide();
            }
        });

        double centerX = this.getScene().getWindow().getX() + this.getScene().getWindow().getWidth() / 2 - 150;
        double centerY = this.getScene().getWindow().getY() + this.getScene().getWindow().getHeight() / 2 - 100;
        actionPopup.show(this.getScene().getWindow(), centerX, centerY);
        Platform.runLater(popupContent::requestFocus);
    }

    private void showOpenFolderChoice(Path path) {
        Popup choicePopup = new Popup();
        VBox popupContent = new VBox(8);

        String bgColor = isDarkTheme ? "linear-gradient(to bottom, rgba(35,35,35,0.98), rgba(30,30,30,0.98))" : "linear-gradient(to bottom, rgba(255,255,255,0.98), rgba(252,252,253,0.98))";
        String borderColor = isDarkTheme ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";
        popupContent.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.2, 0, 8);" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;"
        );

        Label titleLabel = new Label("Open folder in which panel?");
        String textColor = isDarkTheme ? "#f1f5f9" : "#1e293b";
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + textColor + "; -fx-padding: 0 0 8 0;");

        Button currentButton = new Button("📂 Current Panel (C)");
        currentButton.getStyleClass().add("popup-button");
        String currentBtnBg = isDarkTheme ? "linear-gradient(to bottom, rgba(50,50,50,0.98), rgba(42,42,42,0.95))" : "linear-gradient(to bottom, rgba(248,250,252,0.98), rgba(241,245,249,0.98))";
        String currentBtnBorder = isDarkTheme ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)";
        currentButton.setStyle(
                "-fx-background-color: " + currentBtnBg + ";" +
                        "-fx-border-color: " + currentBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + textColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left;"
        );

        Button otherButton = new Button("📂 Other Panel (O)");
        otherButton.getStyleClass().add("popup-button");
        String otherBtnBg = isDarkTheme ? "linear-gradient(from 0% 0% to 100% 100%, #00d4e6 0%, #00e8ff 100%)" : "linear-gradient(from 0% 0% to 100% 100%, #0099a8 0%, #00b8cc 100%)";
        String otherBtnEffect = isDarkTheme ? "dropshadow(gaussian, rgba(0,212,230,0.5), 12, 0, 0, 3)" : "dropshadow(gaussian, rgba(0,153,168,0.4), 12, 0, 0, 3)";
        otherButton.setStyle(
                "-fx-background-color: " + otherBtnBg + ";" +
                        "-fx-border-color: transparent; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left; -fx-effect: " + otherBtnEffect + ";"
        );

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("popup-button");
        String cancelBtnBg = isDarkTheme ? "rgba(50,50,50,0.5)" : "transparent";
        String cancelBtnBorder = isDarkTheme ? "rgba(255,255,255,0.12)" : "rgba(0,0,0,0.12)";
        String cancelTextColor = isDarkTheme ? "#94a3b8" : "#64748b";
        cancelButton.setStyle(
                "-fx-background-color: " + cancelBtnBg + "; -fx-border-color: " + cancelBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + cancelTextColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px;"
        );

        currentButton.setOnAction(e -> {
            choicePopup.hide();
            navigateToPath(path);
        });

        otherButton.setOnAction(e -> {
            choicePopup.hide();
            if (mainWindow != null) {
                FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
                if (otherPanel != null) {
                    otherPanel.navigateToPath(path);
                    mainWindow.activatePanel(otherPanel);
                }
            }
        });

        cancelButton.setOnAction(e -> choicePopup.hide());

        popupContent.getChildren().addAll(titleLabel, currentButton, otherButton, cancelButton);
        choicePopup.getContent().add(popupContent);

        popupContent.setOnKeyPressed(e -> {
            String text = e.getText().toLowerCase();
            if (e.getCode() == KeyCode.C || text.equals("с") || text.equals("c")) {
                e.consume();
                choicePopup.hide();
                navigateToPath(path);
            } else if (e.getCode() == KeyCode.O || text.equals("щ") || text.equals("o")) {
                e.consume();
                choicePopup.hide();
                if (mainWindow != null) {
                    FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
                    if (otherPanel != null) {
                        otherPanel.navigateToPath(path);
                        mainWindow.activatePanel(otherPanel);
                    }
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                choicePopup.hide();
            }
        });

        double centerX = this.getScene().getWindow().getX() + this.getScene().getWindow().getWidth() / 2 - 150;
        double centerY = this.getScene().getWindow().getY() + this.getScene().getWindow().getHeight() / 2 - 100;
        choicePopup.show(this.getScene().getWindow(), centerX, centerY);
        Platform.runLater(popupContent::requestFocus);
    }

    private void showGoToLocationChoice(Path path) {
        Popup choicePopup = new Popup();
        VBox popupContent = new VBox(8);

        String bgColor = isDarkTheme ? "linear-gradient(to bottom, rgba(35,35,35,0.98), rgba(30,30,30,0.98))" : "linear-gradient(to bottom, rgba(255,255,255,0.98), rgba(252,252,253,0.98))";
        String borderColor = isDarkTheme ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)";
        popupContent.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-padding: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 30, 0.2, 0, 8);" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;"
        );

        Label titleLabel = new Label("Go to location in which panel?");
        String textColor = isDarkTheme ? "#f1f5f9" : "#1e293b";
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + textColor + "; -fx-padding: 0 0 8 0;");

        Button currentButton = new Button("📍 Current Panel (C)");
        currentButton.getStyleClass().add("popup-button");
        String currentBtnBg = isDarkTheme ? "linear-gradient(to bottom, rgba(50,50,50,0.98), rgba(42,42,42,0.95))" : "linear-gradient(to bottom, rgba(248,250,252,0.98), rgba(241,245,249,0.98))";
        String currentBtnBorder = isDarkTheme ? "rgba(255,255,255,0.08)" : "rgba(0,0,0,0.08)";
        currentButton.setStyle(
                "-fx-background-color: " + currentBtnBg + ";" +
                        "-fx-border-color: " + currentBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + textColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left;"
        );

        Button otherButton = new Button("📍 Other Panel (O)");
        otherButton.getStyleClass().add("popup-button");
        String otherBtnBg = isDarkTheme ? "linear-gradient(from 0% 0% to 100% 100%, #00d4e6 0%, #00e8ff 100%)" : "linear-gradient(from 0% 0% to 100% 100%, #0099a8 0%, #00b8cc 100%)";
        String otherBtnEffect = isDarkTheme ? "dropshadow(gaussian, rgba(0,212,230,0.5), 12, 0, 0, 3)" : "dropshadow(gaussian, rgba(0,153,168,0.4), 12, 0, 0, 3)";
        otherButton.setStyle(
                "-fx-background-color: " + otherBtnBg + ";" +
                        "-fx-border-color: transparent; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 12 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: white; -fx-cursor: hand;" +
                        "-fx-min-width: 280px; -fx-alignment: center-left; -fx-effect: " + otherBtnEffect + ";"
        );

        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("popup-button");
        String cancelBtnBg = isDarkTheme ? "rgba(50,50,50,0.5)" : "transparent";
        String cancelBtnBorder = isDarkTheme ? "rgba(255,255,255,0.12)" : "rgba(0,0,0,0.12)";
        String cancelTextColor = isDarkTheme ? "#94a3b8" : "#64748b";
        cancelButton.setStyle(
                "-fx-background-color: " + cancelBtnBg + "; -fx-border-color: " + cancelBtnBorder + ";" +
                        "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10 20;" +
                        "-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + cancelTextColor + "; -fx-cursor: hand;" +
                        "-fx-min-width: 280px;"
        );

        currentButton.setOnAction(e -> {
            choicePopup.hide();
            controller.navigateToFileLocationInPanel(path, true);
            if (mainWindow != null) {
                mainWindow.activatePanel(this);
            }
        });

        otherButton.setOnAction(e -> {
            choicePopup.hide();
            controller.navigateToFileLocationInPanel(path, false);
            if (mainWindow != null) {
                FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
                if (otherPanel != null) {
                    mainWindow.activatePanel(otherPanel);
                }
            }
        });

        cancelButton.setOnAction(e -> choicePopup.hide());

        popupContent.getChildren().addAll(titleLabel, currentButton, otherButton, cancelButton);
        choicePopup.getContent().add(popupContent);

        popupContent.setOnKeyPressed(e -> {
            String text = e.getText().toLowerCase();
            if (e.getCode() == KeyCode.C || text.equals("с") || text.equals("c")) {
                e.consume();
                choicePopup.hide();
                controller.navigateToFileLocationInPanel(path, true);
                if (mainWindow != null) {
                    mainWindow.activatePanel(this);
                }
            } else if (e.getCode() == KeyCode.O || text.equals("щ") || text.equals("o")) {
                e.consume();
                choicePopup.hide();
                controller.navigateToFileLocationInPanel(path, false);
                if (mainWindow != null) {
                    FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
                    if (otherPanel != null) {
                        mainWindow.activatePanel(otherPanel);
                    }
                }
            } else if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
                choicePopup.hide();
            }
        });

        double centerX = this.getScene().getWindow().getX() + this.getScene().getWindow().getWidth() / 2 - 150;
        double centerY = this.getScene().getWindow().getY() + this.getScene().getWindow().getHeight() / 2 - 100;
        choicePopup.show(this.getScene().getWindow(), centerX, centerY);
        Platform.runLater(popupContent::requestFocus);
    }

    private void showSearchModeError() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Operation Not Available");
        alert.setHeaderText(null);
        alert.setContentText("This operation is not available in search results mode.\nOnly copying to the other panel and copying to cache are available.");
        alert.showAndWait();
    }

    public void navigateToPath(Path path) {
        exitSearchMode();

        if (currentPath != null && !currentPath.equals(path)) {
            historyBack.push(currentPath);
            historyForward.clear();
        }
        this.currentPath = path;
        refreshFileList();

        Platform.runLater(() -> {
            if (isActive && !tableView.getItems().isEmpty()) {
                tableView.requestFocus();

                if (tableView.getFocusModel().getFocusedIndex() < 0) {
                    tableView.getFocusModel().focus(0);
                    tableView.getSelectionModel().clearAndSelect(0);
                }
            }
        });
    }

    private void navigateBack() {
        if (!historyBack.isEmpty()) {
            Path previousPath = historyBack.pop();
            historyForward.push(currentPath);
            currentPath = previousPath;
            refreshFileList();
        }
    }

    private void navigateForward() {
        if (!historyForward.isEmpty()) {
            Path nextPath = historyForward.pop();
            historyBack.push(currentPath);
            currentPath = nextPath;
            refreshFileList();
        }
    }

    private void navigateUp() {
        Path parent = currentPath.getParent();
        if (parent != null) {
            navigateToPath(parent);
        }
    }

    private void navigateToInputPath() {
        try {
            Path newPath = Paths.get(pathField.getText());
            if (Files.exists(newPath) && Files.isDirectory(newPath)) {
                navigateToPath(newPath);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Path Error");
                alert.setHeaderText(null);
                alert.setContentText("Path does not exist or is not a directory");
                alert.showAndWait();
                pathField.setText(currentPath.toString());
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Path Error");
            alert.setHeaderText(null);
            alert.setContentText("Invalid path format");
            alert.showAndWait();
            pathField.setText(currentPath.toString());
        }
    }

    public void showSearchResults(List<FileItem> results, String query) {
        this.panelMode = PanelMode.SEARCH_RESULTS;
        this.searchResults = new ArrayList<>(results);
        this.searchQuery = query;
        refreshFileList();

        Platform.runLater(() -> {
            requestActivation();
            if (!tableView.getItems().isEmpty()) {
                tableView.requestFocus();
                tableView.getFocusModel().focus(0);
                tableView.getSelectionModel().clearAndSelect(0);
            } else {
                this.requestFocus();
            }
        });
    }

    public void exitSearchMode() {
        if (panelMode == PanelMode.SEARCH_RESULTS) {
            panelMode = PanelMode.NORMAL;
            searchResults.clear();
            searchQuery = "";
            refreshFileList();
        }
    }

    public boolean isInSearchMode() {
        return panelMode == PanelMode.SEARCH_RESULTS;
    }

    private boolean isOtherPanelInSearchMode() {
        if (mainWindow != null) {
            FXFilePanel otherPanel = mainWindow.getOtherPanel(this);
            return otherPanel != null && otherPanel.isInSearchMode();
        }
        return false;
    }

    public void selectFileByName(String fileName) {
        Platform.runLater(() -> {
            for (int i = 0; i < tableView.getItems().size(); i++) {
                if (tableView.getItems().get(i).getName().equals(fileName)) {
                    tableView.getSelectionModel().clearSelection();
                    tableView.getSelectionModel().select(i);
                    tableView.getFocusModel().focus(i);
                    tableView.scrollTo(i);
                    scrollToIfNotVisible(i);
                    break;
                }
            }
        });
    }

    public void updateClipboardStatus(boolean isEmpty) {
    }

    private Path getDestinationPath(FXFilePanel sourcePanel) {
        if (mainWindow != null) {
            FXFilePanel otherPanel = mainWindow.getOtherPanel(sourcePanel);
            return otherPanel.getCurrentPath();
        }
        return sourcePanel.getCurrentPath();
    }

    public Path getCurrentPath() {
        return currentPath;
    }

    public List<FileItem> getSelectedFiles() {
        return new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
    }

    public void showCopyDestinationMenu(Path otherPanelPath) {
        showCopyDestinationPopup();
    }

    public boolean isDarkTheme() {
        return isDarkTheme;
    }
}