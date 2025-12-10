package com.filecommander.ui;

import com.filecommander.controller.FileController;
import com.filecommander.localization.LocalizationManager;
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

                Platform.runLater(() -> {
                    if (mainWindow != null) {
                        setTheme(mainWindow.isDarkTheme());
                    }
                });
            }
        });

        setCenter(webView);
        setPrefHeight(85);
        setMinHeight(85);
        setMaxHeight(85);
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

    public void updateLanguage() {
        if (webEngine != null) {
            webEngine.loadContent(getHTMLContent());
            Platform.runLater(() -> setTheme(mainWindow.isDarkTheme()));
        }
    }

    public void updateHiddenFilesButton() {
        boolean showing = controller.isShowHiddenFiles();
        if (webEngine != null) {
            webEngine.executeScript("updateHiddenFilesButton(" + showing + ");");
        }
    }

    private String getHTMLContent() {
        LocalizationManager loc = LocalizationManager.getInstance();

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
                "  min-height:80px;" +
                "  background:linear-gradient(180deg,var(--bg-toolbar) 0%,rgba(245,247,250,0.5) 100%);" +
                "  backdrop-filter:blur(20px) saturate(180%);" +
                "  -webkit-backdrop-filter:blur(20px) saturate(180%);" +
                "  display:flex;" +
                "  align-items:center;" +
                "  padding:0 16px;" +
                "  gap:8px;" +
                "  border-bottom:1px solid var(--border);" +
                "  box-shadow:var(--shadow-md);" +
                "  position:relative;" +
                "  overflow-x:auto;" +
                "  overflow-y:hidden;" +
                "}" +
                ".toolbar::-webkit-scrollbar { height:5px; }" +
                ".toolbar::-webkit-scrollbar-track { background:transparent; }" +
                ".toolbar::-webkit-scrollbar-thumb { background:var(--border); border-radius:3px; }" +
                ".toolbar::-webkit-scrollbar-thumb:hover { background:var(--accent); }" +
                "body.dark .toolbar {" +
                "  background:linear-gradient(180deg,var(--bg-toolbar) 0%,rgba(20,20,20,0.5) 100%);" +
                "}" +
                ".button-group {" +
                "  display:flex;" +
                "  gap:4px;" +
                "  padding:6px;" +
                "  background:var(--bg-group);" +
                "  backdrop-filter:blur(10px);" +
                "  -webkit-backdrop-filter:blur(10px);" +
                "  border-radius:11px;" +
                "  border:1px solid var(--border);" +
                "  box-shadow:var(--shadow-sm);" +
                "  opacity:0;" +
                "  animation:slideDown 0.5s cubic-bezier(0.34,1.56,0.64,1) forwards;" +
                "  position:relative;" +
                "  flex-shrink:0;" +
                "}" +
                ".button-group::after {" +
                "  content:'';" +
                "  position:absolute;" +
                "  right:-5px;" +
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
                "  gap:6px;" +
                "  padding:8px 11px;" +
                "  background:transparent;" +
                "  border:none;" +
                "  border-radius:9px;" +
                "  cursor:pointer;" +
                "  transition:all 0.3s cubic-bezier(0.4,0,0.2,1);" +
                "  position:relative;" +
                "  overflow:hidden;" +
                "  white-space:nowrap;" +
                "}" +
                ".tool-button::before {" +
                "  content:'';" +
                "  position:absolute;" +
                "  inset:0;" +
                "  background:linear-gradient(135deg,var(--accent) 0%,var(--accent-hover) 100%);" +
                "  opacity:0;" +
                "  transition:opacity 0.3s;" +
                "  border-radius:9px;" +
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
                "  font-size:12.5px;" +
                "  font-weight:600;" +
                "  color:var(--text-primary);" +
                "  position:relative;" +
                "  z-index:1;" +
                "  letter-spacing:0.2px;" +
                "}" +
                ".button-hotkey {" +
                "  font-size:9.5px;" +
                "  color:var(--text-secondary);" +
                "  font-weight:700;" +
                "  padding:2px 6px;" +
                "  background:linear-gradient(135deg,rgba(0,0,0,0.05) 0%,rgba(0,0,0,0.02) 100%);" +
                "  border-radius:5px;" +
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
                ".flag-icon {" +
                "  width:22px;" +
                "  height:16px;" +
                "  display:inline-block;" +
                "  border-radius:2px;" +
                "  box-shadow:0 1px 2px rgba(0,0,0,0.2);" +
                "  vertical-align:middle;" +
                "  position:relative;" +
                "  z-index:1;" +
                "}" +
                ".flag-separator {" +
                "  color:var(--text-secondary);" +
                "  font-size:11px;" +
                "  margin:0 2px;" +
                "  position:relative;" +
                "  z-index:1;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"toolbar\">" +
                "  <div class=\"button-group\">" +
                "    <button class=\"tool-button\" onclick=\"executeAction('copy')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.copy") + "</span>" +
                "      <span class=\"button-hotkey\">F3</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('move')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.move") + "</span>" +
                "      <span class=\"button-hotkey\">F6</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('delete')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.delete") + "</span>" +
                "      <span class=\"button-hotkey\">F8</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('newFolder')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.newFolder") + "</span>" +
                "      <span class=\"button-hotkey\">F7</span>" +
                "    </button>" +
                "  </div>" +
                "  <div class=\"button-group\">" +
                "    <button class=\"tool-button\" onclick=\"executeAction('rename')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.rename") + "</span>" +
                "      <span class=\"button-hotkey\">F2</span>" +
                "    </button>" +
                "    <button class=\"tool-button accent\" onclick=\"executeAction('search')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.search") + "</span>" +
                "      <span class=\"button-hotkey\">Ctrl+F</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('undo')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.undo") + "</span>" +
                "      <span class=\"button-hotkey\">Ctrl+Z</span>" +
                "    </button>" +
                "  </div>" +
                "  <div class=\"button-group\">" +
                "    <button class=\"tool-button\" onclick=\"executeAction('history')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.history") + "</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" id=\"hiddenBtn\" onclick=\"executeAction('toggleHidden')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.showHidden") + "</span>" +
                "      <span class=\"button-hotkey\">Ctrl+H</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('toggleTheme')\">" +
                "      <span class=\"button-text\">" + loc.getString("toolbar.theme") + "</span>" +
                "      <span class=\"button-hotkey\">Ctrl+T</span>" +
                "    </button>" +
                "    <button class=\"tool-button\" onclick=\"executeAction('changeLanguage')\">" +
                "      <svg class=\"flag-icon\" viewBox=\"0 0 3 2\">" +
                "        <rect width=\"3\" height=\"1\" fill=\"#0057B7\"/>" +
                "        <rect width=\"3\" height=\"1\" y=\"1\" fill=\"#FFD700\"/>" +
                "      </svg>" +
                "      <span class=\"flag-separator\">|</span>" +
                "      <svg class=\"flag-icon\" viewBox=\"0 0 60 30\">" +
                "        <clipPath id=\"t\"><path d=\"M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z\"/></clipPath>" +
                "        <path d=\"M0,0 v30 h60 v-30 z\" fill=\"#012169\"/>" +
                "        <path d=\"M0,0 L60,30 M60,0 L0,30\" stroke=\"#fff\" stroke-width=\"6\"/>" +
                "        <path d=\"M0,0 L60,30 M60,0 L0,30\" clip-path=\"url(#t)\" stroke=\"#C8102E\" stroke-width=\"4\"/>" +
                "        <path d=\"M30,0 v30 M0,15 h60\" stroke=\"#fff\" stroke-width=\"10\"/>" +
                "        <path d=\"M30,0 v30 M0,15 h60\" stroke=\"#C8102E\" stroke-width=\"6\"/>" +
                "      </svg>" +
                "      <span class=\"button-text\">Мова/Language</span>" +
                "      <span class=\"button-hotkey\">Ctrl+L</span>" +
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
                "    textSpan.textContent='" + loc.getString("toolbar.hideHidden") + "';" +
                "  }else{" +
                "    textSpan.textContent='" + loc.getString("toolbar.showHidden") + "';" +
                "  }" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    private void showSearchModeError() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        mainWindow.setIconForDialog(alert);
        alert.setTitle(LocalizationManager.getInstance().getString("error.operationUnavailable"));
        alert.setHeaderText(null);
        alert.setContentText(LocalizationManager.getInstance().getString("error.searchModeRestriction"));
        alert.showAndWait();
    }

    public class JSBridge {
        public void executeAction(String action) {
            javafx.application.Platform.runLater(() -> {
                FXFilePanel activePanel = mainWindow.getActivePanel();

                boolean isSafeAction = action.equals("copy") ||
                        action.equals("history") ||
                        action.equals("toggleTheme") ||
                        action.equals("changeLanguage") ||
                        action.equals("toggleHidden");

                if (activePanel != null && activePanel.isInSearchMode()) {
                    if (!isSafeAction) {
                        showSearchModeError();
                        return;
                    }
                }

                switch (action) {
                    case "copy":
                        if (activePanel != null) {
                            if (activePanel.isInSearchMode()) {
                                controller.showCopyDestinationMenu(activePanel);
                            } else {
                                controller.showCopyDestinationMenu(activePanel);
                            }
                        }
                        break;

                    case "move":
                        if (activePanel != null) controller.initiateMoveOperation(activePanel);
                        break;

                    case "delete":
                        if (activePanel != null) controller.initiateDeleteOperation(activePanel);
                        break;

                    case "newFolder":
                        if (activePanel != null) activePanel.handleKeyPress(javafx.scene.input.KeyCode.F7);
                        break;

                    case "rename":
                        if (activePanel != null) activePanel.handleKeyPress(javafx.scene.input.KeyCode.F2);
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

                    case "changeLanguage":
                        mainWindow.changeLanguage();
                        break;
                }
            });
        }
    }
}