package com.filecommander.ui;

import com.filecommander.controller.FileController;
import com.filecommander.ui.dialogs.WebViewHistoryDialog;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class WebViewToolbar extends BorderPane {
    private WebView webView;
    private WebEngine webEngine;
    private FileController controller = FileController.getInstance();
    private MainWindow mainWindow;
    private JSBridge jsBridge;

    public WebViewToolbar(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        initializeWebView();
    }

    private void initializeWebView() {
        webView = new WebView();
        webEngine = webView.getEngine();

        webEngine.setJavaScriptEnabled(true);
        webEngine.loadContent(getHTMLContent());

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                jsBridge = new JSBridge();
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", jsBridge);

                // Применяем тему после загрузки
                Platform.runLater(() -> {
                    if (mainWindow != null) {
                        setTheme(mainWindow.isDarkTheme());
                    }
                });
            }
        });

        setCenter(webView);
        setPrefHeight(70);
        setMinHeight(70);
        setMaxHeight(70);
    }

    public void setTheme(boolean isDark) {
        if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
            try {
                if (isDark) {
                    webEngine.executeScript("if (document.body) { isDarkTheme = true; document.body.classList.add('dark'); }");
                } else {
                    webEngine.executeScript("if (document.body) { isDarkTheme = false; document.body.classList.remove('dark'); }");
                }
                System.out.println("Toolbar theme set to: " + (isDark ? "dark" : "light"));
            } catch (Exception e) {
                System.err.println("Error setting toolbar theme: " + e.getMessage());
            }
        }
    }

    public void updateHiddenFilesButton() {
        boolean showing = controller.isShowHiddenFiles();
        if (webEngine != null) {
            webEngine.executeScript("updateHiddenFilesButton(" + showing + ");");
        }
    }

    private String getHTMLContent() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "* { margin:0; padding:0; box-sizing:border-box; user-select:none; }" +
                ":root {" +
                "  --bg-toolbar:#ffffff;" +
                "  --bg-group:rgba(245,247,250,0.8);" +
                "  --bg-hover:rgba(0,153,168,0.1);" +
                "  --bg-active:rgba(0,153,168,0.15);" +
                "  --text-primary:#1c1c1c;" +
                "  --text-secondary:#6b7280;" +
                "  --accent:#0099a8;" +
                "  --accent-hover:#00b8cc;" +
                "  --accent-light:rgba(0,153,168,0.15);" +
                "  --border:rgba(0,0,0,0.06);" +
                "  --shadow-sm:0 1px 3px rgba(0,0,0,0.08);" +
                "  --shadow-md:0 4px 12px rgba(0,0,0,0.1);" +
                "  --shadow-lg:0 8px 24px rgba(0,0,0,0.12);" +
                "  --glow:0 0 20px rgba(0,153,168,0.4);" +
                "}" +
                "body {" +
                "  font-family:'Segoe UI Variable','Segoe UI',system-ui,sans-serif;" +
                "  background:var(--bg-toolbar);" +
                "  height:100%;" +
                "  overflow:hidden;" +
                "  color:var(--text-primary);" +
                "}" +
                "body.dark {" +
                "  --bg-toolbar:#1f1f1f;" +
                "  --bg-group:rgba(35,35,35,0.8);" +
                "  --bg-hover:rgba(0,153,168,0.15);" +
                "  --bg-active:rgba(0,153,168,0.25);" +
                "  --text-primary:#e8e8e8;" +
                "  --text-secondary:#9ca3af;" +
                "  --border:rgba(255,255,255,0.06);" +
                "  --shadow-sm:0 1px 3px rgba(0,0,0,0.3);" +
                "  --shadow-md:0 4px 12px rgba(0,0,0,0.4);" +
                "  --shadow-lg:0 8px 24px rgba(0,0,0,0.5);" +
                "  --glow:0 0 20px rgba(0,212,230,0.5);" +
                "}" +
                ".toolbar {" +
                "  height:100%;" +
                "  background:linear-gradient(180deg,var(--bg-toolbar) 0%,rgba(245,247,250,0.5) 100%);" +
                "  backdrop-filter:blur(20px) saturate(180%);" +
                "  -webkit-backdrop-filter:blur(20px) saturate(180%);" +
                "  display:flex;" +
                "  align-items:center;" +
                "  padding:0 24px;" +
                "  gap:12px;" +
                "  border-bottom:1px solid var(--border);" +
                "  box-shadow:var(--shadow-md);" +
                "  position:relative;" +
                "}" +
                "body.dark .toolbar {" +
                "  background:linear-gradient(180deg,var(--bg-toolbar) 0%,rgba(20,20,20,0.5) 100%);" +
                "}" +
                ".button-group {" +
                "  display:flex;" +
                "  gap:6px;" +
                "  padding:6px;" +
                "  background:var(--bg-group);" +
                "  backdrop-filter:blur(10px);" +
                "  -webkit-backdrop-filter:blur(10px);" +
                "  border-radius:12px;" +
                "  border:1px solid var(--border);" +
                "  box-shadow:var(--shadow-sm);" +
                "  opacity:0;" +
                "  animation:slideDown 0.5s cubic-bezier(0.34,1.56,0.64,1) forwards;" +
                "  position:relative;" +
                "}" +
                ".button-group::after {" +
                "  content:'';" +
                "  position:absolute;" +
                "  right:-7px;" +
                "  top:50%;" +
                "  transform:translateY(-50%);" +
                "  width:1px;" +
                "  height:60%;" +
                "  background:linear-gradient(180deg,transparent 0%,var(--border) 50%,transparent 100%);" +
                "}" +
                ".button-group:last-child::after { display:none; }" +
                ".button-group:nth-child(1) { animation-delay:0.1s; }" +
                ".button-group:nth-child(2) { animation-delay:0.2s; }" +
                ".button-group:nth-child(3) { animation-delay:0.3s; }" +
                ".tool-button {" +
                "  display:flex;" +
                "  align-items:center;" +
                "  gap:10px;" +
                "  padding:10px 16px;" +
                "  background:transparent;" +
                "  border:none;" +
                "  border-radius:10px;" +
                "  cursor:pointer;" +
                "  transition:all 0.3s cubic-bezier(0.4,0,0.2,1);" +
                "  position:relative;" +
                "  overflow:hidden;" +
                "}" +
                ".tool-button::before {" +
                "  content:'';" +
                "  position:absolute;" +
                "  inset:0;" +
                "  background:linear-gradient(135deg,var(--accent) 0%,var(--accent-hover) 100%);" +
                "  opacity:0;" +
                "  transition:opacity 0.3s;" +
                "  border-radius:10px;" +
                "}" +
                ".tool-button::after {" +
                "  content:'';" +
                "  position:absolute;" +
                "  top:50%;" +
                "  left:50%;" +
                "  width:0;" +
                "  height:0;" +
                "  border-radius:50%;" +
                "  background:rgba(255,255,255,0.5);" +
                "  transform:translate(-50%,-50%);" +
                "  transition:width 0.6s,height 0.6s;" +
                "}" +
                ".tool-button:active::after { width:300px; height:300px; }" +
                ".tool-button:hover {" +
                "  background:var(--bg-hover);" +
                "  transform:translateY(-2px);" +
                "  box-shadow:var(--shadow-md);" +
                "}" +
                ".tool-button:hover::before { opacity:0.08; }" +
                ".tool-button:active {" +
                "  transform:translateY(0) scale(0.97);" +
                "  box-shadow:var(--shadow-sm);" +
                "}" +
                ".button-text {" +
                "  font-size:13px;" +
                "  font-weight:600;" +
                "  color:var(--text-primary);" +
                "  position:relative;" +
                "  z-index:1;" +
                "  letter-spacing:0.3px;" +
                "}" +
                ".button-hotkey {" +
                "  font-size:10px;" +
                "  color:var(--text-secondary);" +
                "  font-weight:700;" +
                "  padding:3px 8px;" +
                "  background:linear-gradient(135deg,rgba(0,0,0,0.05) 0%,rgba(0,0,0,0.02) 100%);" +
                "  border-radius:6px;" +
                "  position:relative;" +
                "  z-index:1;" +
                "  border:1px solid var(--border);" +
                "  box-shadow:inset 0 1px 2px rgba(0,0,0,0.05);" +
                "}" +
                "body.dark .button-hotkey {" +
                "  background:linear-gradient(135deg,rgba(255,255,255,0.08) 0%,rgba(255,255,255,0.03) 100%);" +
                "  border-color:rgba(255,255,255,0.1);" +
                "}" +
                "@keyframes slideDown {" +
                "  from { opacity:0; transform:translateY(-20px) scale(0.95); }" +
                "  to { opacity:1; transform:translateY(0) scale(1); }" +
                "}" +
                ".tool-button.accent {" +
                "  background:linear-gradient(135deg,var(--accent) 0%,var(--accent-hover) 100%);" +
                "  color:white;" +
                "  box-shadow:var(--shadow-md),inset 0 1px 0 rgba(255,255,255,0.2);" +
                "}" +
                ".tool-button.accent::before {" +
                "  background:linear-gradient(135deg,rgba(255,255,255,0.2) 0%,rgba(255,255,255,0.1) 100%);" +
                "  opacity:1;" +
                "}" +
                ".tool-button.accent .button-text {" +
                "  color:white;" +
                "  text-shadow:0 1px 2px rgba(0,0,0,0.2);" +
                "}" +
                ".tool-button.accent .button-hotkey {" +
                "  background:rgba(255,255,255,0.25);" +
                "  backdrop-filter:blur(10px);" +
                "  -webkit-backdrop-filter:blur(10px);" +
                "  color:white;" +
                "  border-color:rgba(255,255,255,0.3);" +
                "  text-shadow:0 1px 2px rgba(0,0,0,0.2);" +
                "  box-shadow:inset 0 1px 2px rgba(255,255,255,0.1);" +
                "}" +
                ".tool-button.accent:hover {" +
                "  transform:translateY(-3px) scale(1.02);" +
                "  box-shadow:var(--shadow-lg),var(--glow),inset 0 1px 0 rgba(255,255,255,0.3);" +
                "}" +
                ".tool-button.accent:hover::before { opacity:1; }" +
                ".tool-button.accent:active {" +
                "  transform:translateY(-1px) scale(0.98);" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"toolbar\">" +
                "  <div class=\"button-group\">" +
                "    <button class=\"tool-button\" onclick=\"executeAction('copy')\">" +
                "      <span class=\"button-text\">Copy</span>" +
                "      <span class=\"button-hotkey\">F3</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('move')\">" +
                "      <span class=\"button-text\">Move</span>" +
                "      <span class=\"button-hotkey\">F6</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('delete')\">" +
                "      <span class=\"button-text\">Delete</span>" +
                "      <span class=\"button-hotkey\">F8</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('newFolder')\">" +
                "      <span class=\"button-text\">New Folder</span>" +
                "      <span class=\"button-hotkey\">F7</span>" +
                "    </button>" +
                "  </div>" +
                "  <div class=\"button-group\">" +
                "    <button class=\"tool-button\" onclick=\"executeAction('rename')\">" +
                "      <span class=\"button-text\">Rename</span>" +
                "      <span class=\"button-hotkey\">F2</span>" +
                "    </button>" +
                "    <button class=\"tool-button accent\" onclick=\"executeAction('search')\">" +
                "      <span class=\"button-text\">Search</span>" +
                "      <span class=\"button-hotkey\">Ctrl+F</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('undo')\">" +
                "      <span class=\"button-text\">Undo</span>" +
                "      <span class=\"button-hotkey\">Ctrl+Z</span>" +
                "    </button>" +
                "  </div>" +
                "  <div class=\"button-group\">" +
                "    <button class=\"tool-button\" onclick=\"executeAction('history')\">" +
                "      <span class=\"button-text\">History</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" id=\"hiddenBtn\" onclick=\"executeAction('toggleHidden')\">" +
                "      <span class=\"button-text\">Show Hidden</span>" +
                "      <span class=\"button-hotkey\">Ctrl+H</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('toggleTheme')\">" +
                "      <span class=\"button-text\">Theme</span>" +
                "      <span class=\"button-hotkey\">Ctrl+T</span>" +
                "    </button>" +
                "  </div>" +
                "</div>" +
                "<script>" +
                "let isDarkTheme=false;" +
                "function executeAction(action){" +
                "  console.log('Toolbar action:',action);" +
                "  javaBridge.executeAction(action);" +
                "}" +
                "function toggleTheme(){" +
                "  isDarkTheme=!isDarkTheme;" +
                "  if(isDarkTheme){" +
                "    document.body.classList.add('dark');" +
                "  }else{" +
                "    document.body.classList.remove('dark');" +
                "  }" +
                "}" +
                "function updateHiddenFilesButton(showing){" +
                "  const btn=document.getElementById('hiddenBtn');" +
                "  const textSpan=btn.querySelector('.button-text');" +
                "  if(showing){" +
                "    textSpan.textContent='Hide Hidden';" +
                "  }else{" +
                "    textSpan.textContent='Show Hidden';" +
                "  }" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    private void showSearchModeError() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Operation Not Available");
        alert.setHeaderText(null);
        alert.setContentText("This operation is not available in search results mode.\nOnly copying is available.");
        alert.showAndWait();
    }

    public class JSBridge {
        public void executeAction(String action) {
            javafx.application.Platform.runLater(() -> {
                FXFilePanel activePanel = mainWindow.getActivePanel();

                if (activePanel != null && activePanel.isInSearchMode()) {
                    if (!action.equals("copy")) {
                        showSearchModeError();
                        return;
                    }
                }

                switch (action) {
                    case "copy":
                        if (activePanel != null) {
                            controller.showCopyDestinationMenu(activePanel);
                        }
                        break;

                    case "move":
                        if (activePanel != null) {
                            controller.initiateMoveOperation(activePanel);
                        }
                        break;

                    case "delete":
                        if (activePanel != null) {
                            controller.initiateDeleteOperation(activePanel);
                        }
                        break;

                    case "newFolder":
                        if (activePanel != null) {
                            activePanel.handleKeyPress(javafx.scene.input.KeyCode.F7);
                        }
                        break;

                    case "rename":
                        if (activePanel != null) {
                            activePanel.handleKeyPress(javafx.scene.input.KeyCode.F2);
                        }
                        break;

                    case "search":
                        controller.openSearchDialog();
                        break;

                    case "undo":
                        controller.undoLastOperation();
                        break;

                    case "history":
                        boolean isDarkTheme = mainWindow.isDarkTheme();
                        WebViewHistoryDialog dialog = new WebViewHistoryDialog(isDarkTheme);
                        dialog.show();
                        break;

                    case "toggleHidden":
                        controller.toggleHiddenFiles();
                        updateHiddenFilesButton();
                        break;

                    case "toggleTheme":
                        mainWindow.toggleTheme();
                        break;
                }
            });
        }
    }
}