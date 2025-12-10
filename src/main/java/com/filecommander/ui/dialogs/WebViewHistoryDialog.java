package com.filecommander.ui.dialogs;

import com.filecommander.model.OperationHistory;
import com.filecommander.repository.OperationHistoryRepository;
import com.filecommander.localization.LocalizationManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.util.List;

public class WebViewHistoryDialog extends Stage {
    private WebView webView;
    private WebEngine webEngine;
    private OperationHistoryRepository repository;
    private JSBridge jsBridge;
    private boolean isDarkTheme;
    private LocalizationManager loc;

    public WebViewHistoryDialog(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        this.loc = LocalizationManager.getInstance();
        repository = OperationHistoryRepository.getInstance();
        initializeWebView();
        setTitle(loc.getString("history.title"));
    }

    private void initializeWebView() {
        webView = new WebView();
        webEngine = webView.getEngine();

        if (isDarkTheme) {
            webView.setPageFill(javafx.scene.paint.Color.rgb(43, 43, 43));
        } else {
            webView.setPageFill(javafx.scene.paint.Color.rgb(255, 255, 255));
        }

        webEngine.setJavaScriptEnabled(true);
        webEngine.loadContent(getHTMLContent());

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                jsBridge = new JSBridge();
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", jsBridge);

                if (isDarkTheme) {
                    webEngine.executeScript("document.body.classList.add('dark');");
                }

                loadHistory();
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(webView);
        root.setStyle(isDarkTheme ? "-fx-background-color: #2b2b2b;" : "-fx-background-color: #ffffff;");

        Scene scene = new Scene(root, 900, 600);
        scene.setFill(isDarkTheme ? javafx.scene.paint.Color.rgb(43, 43, 43) : javafx.scene.paint.Color.WHITE);

        setScene(scene);
        initStyle(javafx.stage.StageStyle.UNDECORATED);
    }

    private String getLocalizedStatus(String rawStatus) {
        if (rawStatus == null) return "";
        String key = "history.status." + rawStatus;
        String localized = loc.getString(key);
        return localized.equals(key) ? rawStatus : localized;
    }

    private String getLocalizedOperationType(String rawType) {
        if (rawType == null) return "";
        String keySuffix = rawType.replace(" ", "");
        String key = "operation.type." + keySuffix;
        String localized = loc.getString(key);
        return localized.equals(key) ? rawType : localized;
    }

    private String localizeDescription(String desc) {
        if (desc == null || desc.isEmpty()) return "";

        String currentLang = loc.getCurrentLanguage();

        String[] ukKeys = {
                "Створено папку", "Створити папку",
                "Видалити", "Видалено",
                "Скопіювати", "Скопійовано",
                "Перемістити", "Переміщено",
                "Перейменувати", "Перейменовано",
                "елементів", "елементи", "елемент",
                "до", "на"
        };

        String[] enKeys = {
                "Created folder", "Create folder",
                "Delete", "Deleted",
                "Copy", "Copied",
                "Move", "Moved",
                "Rename", "Renamed",
                "items", "items", "item",
                "to", "to"
        };

        if ("en".equals(currentLang)) {
            for (int i = 0; i < ukKeys.length; i++) {
                desc = desc.replace(ukKeys[i], enKeys[i]);
            }
        } else if ("uk".equals(currentLang)) {
            for (int i = 0; i < enKeys.length; i++) {
                desc = desc.replace(enKeys[i], ukKeys[i]);
            }
        }

        return desc;
    }

    private void loadHistory() {
        Platform.runLater(() -> {
            try {
                List<OperationHistory> history = repository.getRecentOperations(50);
                StringBuilder jsonBuilder = new StringBuilder("[");

                for (int i = 0; i < history.size(); i++) {
                    OperationHistory op = history.get(i);
                    if (i > 0) jsonBuilder.append(",");

                    String localizedType = getLocalizedOperationType(op.getOperationType());
                    String localizedStatus = getLocalizedStatus(op.getStatus());
                    String localizedDesc = localizeDescription(op.getDescription());

                    jsonBuilder.append("{")
                            .append("\"id\":").append(op.getId()).append(",")
                            .append("\"type\":\"").append(escapeJson(localizedType)).append("\",")
                            .append("\"description\":\"").append(escapeJson(localizedDesc)).append("\",")
                            .append("\"time\":\"").append(escapeJson(op.getExecutedAt())).append("\",")
                            .append("\"status\":\"").append(escapeJson(localizedStatus)).append("\"")
                            .append("}");
                }

                jsonBuilder.append("]");
                String script = "if(typeof updateHistory === 'function') { updateHistory(" + jsonBuilder.toString() + "); }";
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("Error loading history: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String getHTMLContent() {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<style>" +
                "* { margin: 0; padding: 0; box-sizing: border-box; user-select: none; }" +
                ":root {" +
                "  --bg-secondary: rgba(255,255,255,0.95);" +
                "  --bg-card: rgba(255,255,255,0.7);" +
                "  --text-primary: #1c1c1c;" +
                "  --text-secondary: #707070;" +
                "  --accent: #0099a8;" +
                "  --border: rgba(0,0,0,0.08);" +
                "  --shadow: 0 4px 20px rgba(0,0,0,0.15);" +
                "}" +
                "body.dark {" +
                "  --bg-secondary: rgba(43,43,43,0.95);" +
                "  --bg-card: rgba(35,35,35,0.7);" +
                "  --text-primary: #e8e8e8;" +
                "  --text-secondary: #a0a0a0;" +
                "  --accent: #00d4e6;" +
                "  --border: rgba(255,255,255,0.08);" +
                "  --shadow: 0 4px 20px rgba(0,0,0,0.5);" +
                "}" +
                "body {" +
                "  font-family: 'Segoe UI Variable', 'Segoe UI', system-ui, sans-serif;" +
                "  background: transparent;" +
                "  height: 100vh;" +
                "  overflow: hidden;" +
                "  display: flex;" +
                "  flex-direction: column;" +
                "  padding: 20px;" +
                "}" +
                ".window-container {" +
                "  background: var(--bg-card);" +
                "  border-radius: 16px;" +
                "  border: 1px solid var(--border);" +
                "  box-shadow: var(--shadow);" +
                "  height: 100%;" +
                "  display: flex;" +
                "  flex-direction: column;" +
                "  overflow: hidden;" +
                "}" +
                ".header {" +
                "  background: var(--bg-secondary);" +
                "  padding: 20px 30px;" +
                "  border-bottom: 1px solid var(--border);" +
                "  position: relative;" +
                "  z-index: 10;" +
                "}" +
                ".header::after {" +
                "  content: ''; position: absolute; bottom: 0; left: 30px; right: 30px; height: 2px; background: var(--accent); border-radius: 2px;" +
                "}" +
                ".title { font-size: 24px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }" +
                ".subtitle { font-size: 13px; color: var(--text-secondary); font-weight: 500; }" +
                ".content {" +
                "  flex: 1;" +
                "  overflow-y: auto;" +
                "  padding: 20px 30px;" +
                "}" +
                ".history-grid { display: grid; gap: 10px; }" +
                ".history-card {" +
                "  background: rgba(255,255,255,0.5);" +
                "  border-radius: 10px;" +
                "  padding: 15px;" +
                "  border: 1px solid var(--border);" +
                "  transition: all 0.2s ease;" +
                "  position: relative;" +
                "}" +
                "body.dark .history-card { background: rgba(45,45,45,0.5); }" +
                ".history-card:hover { transform: translateX(4px); border-color: var(--accent); }" +
                ".card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }" +
                ".card-id {" +
                "  font-size: 10px; font-weight: 700; color: var(--text-secondary);" +
                "  padding: 2px 8px; background: rgba(0,153,168,0.1); border-radius: 12px; border: 1px solid var(--border);" +
                "}" +
                ".card-status {" +
                "  font-size: 11px; font-weight: 700; color: #10b981;" +
                "  padding: 2px 10px; background: rgba(16,185,129,0.1); border-radius: 12px;" +
                "}" +
                ".card-type {" +
                "  font-size: 15px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px;" +
                "}" +
                ".card-description {" +
                "  font-size: 13px; color: var(--text-secondary); line-height: 1.5; margin-bottom: 10px;" +
                "}" +
                ".card-footer {" +
                "  display: flex; justify-content: flex-end; font-size: 11px; color: var(--text-secondary);" +
                "}" +
                ".footer {" +
                "  background: var(--bg-secondary);" +
                "  padding: 15px 30px;" +
                "  border-top: 1px solid var(--border);" +
                "  display: flex; gap: 10px; justify-content: flex-end;" +
                "}" +
                ".btn {" +
                "  padding: 8px 24px; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: 0.2s;" +
                "}" +
                ".btn-refresh { background: var(--accent); color: white; }" +
                ".btn-close { background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border); }" +
                ".btn:hover { opacity: 0.9; transform: translateY(-1px); }" +
                "::-webkit-scrollbar { width: 8px; }" +
                "::-webkit-scrollbar-track { background: transparent; }" +
                "::-webkit-scrollbar-thumb { background: rgba(0,153,168,0.3); border-radius: 4px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"window-container\">" +
                "<div class=\"header\">" +
                "  <div class=\"title\">" + escapeJs(loc.getString("history.title")) + "</div>" +
                "  <div class=\"subtitle\" id=\"subtitle\">" + escapeJs(loc.getString("history.loading")) + "</div>" +
                "</div>" +
                "<div class=\"content\">" +
                "  <div class=\"history-grid\" id=\"historyGrid\"></div>" +
                "</div>" +
                "<div class=\"footer\">" +
                "  <button class=\"btn btn-refresh\" onclick=\"refresh()\">" + escapeJs(loc.getString("history.refresh")) + "</button>" +
                "  <button class=\"btn btn-close\" onclick=\"closeDialog()\">" + escapeJs(loc.getString("history.close")) + "</button>" +
                "</div>" +
                "</div>" +
                "<script>" +
                "const emptyText = '" + escapeJs(loc.getString("history.empty")) + "';" +
                "const emptySubtext = '" + escapeJs(loc.getString("history.emptySubtext")) + "';" +
                "const noOperations = '" + escapeJs(loc.getString("history.noOperations")) + "';" +
                "const recentOperations = '" + escapeJs(loc.getString("history.recentOperations")) + "';" +
                "const totalLabel = '" + escapeJs(loc.getString("history.total")) + "';" +
                "function updateHistory(data) {" +
                "  const grid = document.getElementById('historyGrid');" +
                "  const fragment = document.createDocumentFragment();" +
                "  grid.innerHTML = '';" +
                "  if (data.length === 0) {" +
                "    grid.innerHTML = `<div style='text-align:center;padding:40px;color:#888'>${emptyText}<br><small>${emptySubtext}</small></div>`;" +
                "  } else {" +
                "    data.forEach(op => {" +
                "      const card = document.createElement('div');" +
                "      card.className = 'history-card';" +
                "      card.innerHTML = `" +
                "        <div class=\\\"card-header\\\">" +
                "          <div class=\\\"card-id\\\">#${op.id}</div>" +
                "          <div class=\\\"card-status\\\">${escapeHtml(op.status)}</div>" +
                "        </div>" +
                "        <div class=\\\"card-type\\\">" +
                "          <span>${escapeHtml(op.type)}</span>" +
                "        </div>" +
                "        <div class=\\\"card-description\\\">${escapeHtml(op.description)}</div>" +
                "        <div class=\\\"card-footer\\\">" +
                "          <div class=\\\"card-time\\\">" +
                "            <span>&#9201; ${escapeHtml(op.time)}</span>" +
                "          </div>" +
                "        </div>" +
                "      `;" +
                "      fragment.appendChild(card);" +
                "    });" +
                "    grid.appendChild(fragment);" +
                "  }" +
                "  document.getElementById('subtitle').textContent = data.length === 0 ? noOperations : `${recentOperations}: ${data.length} ${totalLabel}`;" +
                "}" +
                "function escapeHtml(text) { const div = document.createElement('div'); div.textContent = text; return div.innerHTML; }" +
                "function refresh() { javaBridge.refresh(); }" +
                "function closeDialog() { javaBridge.close(); }" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    public class JSBridge {
        public void refresh() {
            Platform.runLater(() -> loadHistory());
        }

        public void close() {
            Platform.runLater(() -> WebViewHistoryDialog.this.close());
        }
    }
}