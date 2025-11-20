package com.filecommander.ui.dialogs;

import com.filecommander.model.OperationHistory;
import com.filecommander.repository.OperationHistoryRepository;
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

    public WebViewHistoryDialog(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        repository = OperationHistoryRepository.getInstance();
        initializeWebView();
        setTitle("Operation History");
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

        Scene scene = new Scene(root, 1200, 700);

        scene.setFill(isDarkTheme ? javafx.scene.paint.Color.rgb(43, 43, 43) : javafx.scene.paint.Color.WHITE);

        setScene(scene);

        initStyle(javafx.stage.StageStyle.UNDECORATED);

        setMaximized(true);
    }

    private void loadHistory() {
        Platform.runLater(() -> {
            try {
                List<OperationHistory> history = repository.getRecentOperations(50);
                StringBuilder jsonBuilder = new StringBuilder("[");

                for (int i = 0; i < history.size(); i++) {
                    OperationHistory op = history.get(i);
                    if (i > 0) jsonBuilder.append(",");

                    jsonBuilder.append("{")
                            .append("\"id\":").append(op.getId()).append(",")
                            .append("\"type\":\"").append(escapeJson(op.getOperationType())).append("\",")
                            .append("\"description\":\"").append(escapeJson(op.getDescription())).append("\",")
                            .append("\"time\":\"").append(escapeJson(op.getExecutedAt())).append("\",")
                            .append("\"status\":\"").append(escapeJson(op.getStatus())).append("\"")
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
        return str.replace("\\", "\\\\")
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
                "  --bg-primary: linear-gradient(135deg, #e0f7fa 0%, #b2ebf2 25%, #80deea 50%, #4dd0e1 75%, #26c6da 100%);" +
                "  --bg-secondary: rgba(255,255,255,0.95);" +
                "  --bg-card: rgba(255,255,255,0.7);" +
                "  --bg-hover: rgba(0,153,168,0.08);" +
                "  --text-primary: #1c1c1c;" +
                "  --text-secondary: #707070;" +
                "  --accent: #0099a8;" +
                "  --accent-gradient: linear-gradient(135deg, #0099a8 0%, #00b8cc 100%);" +
                "  --border: rgba(0,0,0,0.08);" +
                "  --shadow: 0 4px 20px rgba(0,0,0,0.15);" +
                "  --shadow-card: 0 2px 12px rgba(0,0,0,0.08);" +
                "  --glow: 0 0 20px rgba(0,153,168,0.3);" +
                "}" +
                "body.dark {" +
                "  --bg-secondary: rgba(43,43,43,0.95);" +
                "  --bg-card: rgba(35,35,35,0.7);" +
                "  --bg-hover: rgba(0,153,168,0.12);" +
                "  --text-primary: #e8e8e8;" +
                "  --text-secondary: #a0a0a0;" +
                "  --accent: #00d4e6;" +
                "  --accent-gradient: linear-gradient(135deg, #00d4e6 0%, #00e8ff 100%);" +
                "  --border: rgba(255,255,255,0.08);" +
                "  --shadow: 0 4px 20px rgba(0,0,0,0.5);" +
                "  --shadow-card: 0 2px 12px rgba(0,0,0,0.3);" +
                "  --glow: 0 0 20px rgba(0,212,230,0.4);" +
                "}" +
                "body {" +
                "  font-family: 'Segoe UI Variable', 'Segoe UI', 'Segoe UI Emoji', system-ui, sans-serif;" +
                "  background: transparent;" +
                "  height: 100vh;" +
                "  overflow: hidden;" +
                "  display: flex;" +
                "  flex-direction: column;" +
                "  position: relative;" +
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
                "body.dark .window-container {" +
                "  box-shadow: var(--shadow);" +
                "}" +
                ".header {" +
                "  background: var(--bg-secondary);" +
                "  padding: 28px 40px;" +
                "  border-bottom: 1px solid var(--border);" +
                "  position: relative;" +
                "  z-index: 10;" +
                "  flex-shrink: 0;" +
                "}" +
                ".header::after {" +
                "  content: '';" +
                "  position: absolute;" +
                "  bottom: 0;" +
                "  left: 40px;" +
                "  right: 40px;" +
                "  height: 2px;" +
                "  background: var(--accent);" +
                "  border-radius: 2px;" +
                "}" +
                ".title {" +
                "  font-size: 28px;" +
                "  font-weight: 700;" +
                "  color: var(--text-primary);" +
                "  margin-bottom: 6px;" +
                "  letter-spacing: -0.5px;" +
                "}" +
                ".subtitle { font-size: 14px; color: var(--text-secondary); font-weight: 500; }" +
                ".content {" +
                "  flex: 1;" +
                "  overflow-y: auto;" +
                "  padding: 24px 40px;" +
                "  position: relative;" +
                "  z-index: 1;" +
                "}" +
                ".history-grid {" +
                "  display: grid;" +
                "  gap: 12px;" +
                "}" +
                ".history-card {" +
                "  background: rgba(255,255,255,0.5);" +
                "  border-radius: 12px;" +
                "  padding: 18px 22px;" +
                "  border: 1px solid var(--border);" +
                "  transition: all 0.2s ease;" +
                "  position: relative;" +
                "}" +
                "body.dark .history-card {" +
                "  background: rgba(45,45,45,0.5);" +
                "}" +
                ".history-card::before {" +
                "  content: '';" +
                "  position: absolute;" +
                "  top: 0;" +
                "  left: 0;" +
                "  width: 3px;" +
                "  height: 100%;" +
                "  background: var(--accent);" +
                "  opacity: 0;" +
                "  transition: opacity 0.2s;" +
                "}" +
                ".history-card:hover {" +
                "  transform: translateX(6px);" +
                "  border-color: var(--accent);" +
                "}" +
                ".history-card:hover::before { opacity: 1; }" +
                ".card-header {" +
                "  display: flex;" +
                "  justify-content: space-between;" +
                "  align-items: center;" +
                "  margin-bottom: 16px;" +
                "}" +
                ".card-id {" +
                "  font-size: 11px;" +
                "  font-weight: 700;" +
                "  color: var(--text-secondary);" +
                "  text-transform: uppercase;" +
                "  letter-spacing: 1px;" +
                "  padding: 4px 12px;" +
                "  background: rgba(0,153,168,0.1);" +
                "  border-radius: 20px;" +
                "  border: 1px solid var(--border);" +
                "}" +
                ".card-status {" +
                "  font-size: 12px;" +
                "  font-weight: 700;" +
                "  color: #10b981;" +
                "  padding: 6px 16px;" +
                "  background: linear-gradient(135deg, rgba(16,185,129,0.15) 0%, rgba(16,185,129,0.08) 100%);" +
                "  border-radius: 20px;" +
                "  border: 1px solid rgba(16,185,129,0.3);" +
                "  text-transform: uppercase;" +
                "  letter-spacing: 0.5px;" +
                "}" +
                ".card-type {" +
                "  font-size: 16px;" +
                "  font-weight: 700;" +
                "  color: var(--text-primary);" +
                "  margin-bottom: 12px;" +
                "  display: flex;" +
                "  align-items: center;" +
                "  gap: 12px;" +
                "}" +
                ".card-type-icon {" +
                "  width: 34px;" +
                "  height: 34px;" +
                "  background: var(--accent);" +
                "  border-radius: 8px;" +
                "  display: flex;" +
                "  align-items: center;" +
                "  justify-content: center;" +
                "  font-size: 18px;" +
                "}" +
                ".card-description {" +
                "  font-size: 14px;" +
                "  color: var(--text-secondary);" +
                "  line-height: 1.6;" +
                "  margin-bottom: 16px;" +
                "  padding-left: 48px;" +
                "}" +
                ".card-footer {" +
                "  display: flex;" +
                "  align-items: center;" +
                "  gap: 8px;" +
                "  font-size: 12px;" +
                "  color: var(--text-secondary);" +
                "  padding-left: 48px;" +
                "}" +
                ".card-time {" +
                "  display: flex;" +
                "  align-items: center;" +
                "  gap: 6px;" +
                "  padding: 4px 12px;" +
                "  background: var(--bg-hover);" +
                "  border-radius: 8px;" +
                "  font-weight: 500;" +
                "}" +
                ".footer {" +
                "  background: var(--bg-secondary);" +
                "  padding: 20px 40px;" +
                "  border-top: 1px solid var(--border);" +
                "  display: flex;" +
                "  gap: 12px;" +
                "  justify-content: flex-end;" +
                "  position: relative;" +
                "  z-index: 10;" +
                "  flex-shrink: 0;" +
                "}" +
                ".btn {" +
                "  padding: 11px 28px;" +
                "  border: none;" +
                "  border-radius: 10px;" +
                "  font-size: 14px;" +
                "  font-weight: 600;" +
                "  cursor: pointer;" +
                "  transition: all 0.2s ease;" +
                "  position: relative;" +
                "  letter-spacing: 0.3px;" +
                "}" +
                ".btn:hover { transform: translateY(-2px); }" +
                ".btn:active { transform: translateY(0); }" +
                ".btn-refresh {" +
                "  background: var(--accent);" +
                "  color: white;" +
                "}" +
                ".btn-close {" +
                "  background: var(--bg-card);" +
                "  color: var(--text-primary);" +
                "  border: 1px solid var(--border);" +
                "}" +
                ".empty-state {" +
                "  text-align: center;" +
                "  padding: 80px 40px;" +
                "  color: var(--text-secondary);" +
                "}" +
                ".empty-icon {" +
                "  font-size: 72px;" +
                "  margin-bottom: 24px;" +
                "  opacity: 0.5;" +
                "}" +
                ".empty-text {" +
                "  font-size: 18px;" +
                "  font-weight: 600;" +
                "  margin-bottom: 8px;" +
                "}" +
                ".empty-subtext {" +
                "  font-size: 14px;" +
                "  opacity: 0.7;" +
                "}" +
                "::-webkit-scrollbar { width: 12px; }" +
                "::-webkit-scrollbar-track {" +
                "  background: transparent;" +
                "  margin: 8px 0;" +
                "}" +
                "::-webkit-scrollbar-thumb {" +
                "  background: linear-gradient(135deg, rgba(0,153,168,0.3) 0%, rgba(0,184,204,0.3) 100%);" +
                "  border-radius: 6px;" +
                "  border: 2px solid transparent;" +
                "  background-clip: padding-box;" +
                "}" +
                "::-webkit-scrollbar-thumb:hover {" +
                "  background: linear-gradient(135deg, rgba(0,153,168,0.5) 0%, rgba(0,184,204,0.5) 100%);" +
                "  background-clip: padding-box;" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"window-container\">" +
                "<div class=\"header\">" +
                "  <div class=\"title\">Operation History</div>" +
                "  <div class=\"subtitle\" id=\"subtitle\">Loading...</div>" +
                "</div>" +
                "<div class=\"content\">" +
                "  <div class=\"history-grid\" id=\"historyGrid\"></div>" +
                "</div>" +
                "<div class=\"footer\">" +
                "  <button class=\"btn btn-refresh\" onclick=\"refresh()\">Refresh</button>" +
                "  <button class=\"btn btn-close\" onclick=\"closeDialog()\">Close</button>" +
                "</div>" +
                "</div>" +
                "<script>" +
                "function getOperationIcon(type) {" +
                "  if (type.includes('Copy')) return 'C';" +
                "  if (type.includes('Move')) return 'M';" +
                "  if (type.includes('Delete')) return 'D';" +
                "  if (type.includes('Folder')) return 'F';" +
                "  if (type.includes('Rename')) return 'R';" +
                "  return 'O';" +
                "}" +
                "function updateHistory(data) {" +
                "  const grid = document.getElementById('historyGrid');" +
                "  const fragment = document.createDocumentFragment();" +
                "  grid.innerHTML = '';" +
                "  if (data.length === 0) {" +
                "    grid.innerHTML = `" +
                "      <div class=\\\"empty-state\\\">" +
                "        <div class=\\\"empty-icon\\\">&#128202;</div>" +
                "        <div class=\\\"empty-text\\\">No operations yet</div>" +
                "        <div class=\\\"empty-subtext\\\">Your operation history will appear here</div>" +
                "      </div>" +
                "    `;" +
                "  } else {" +
                "    data.forEach(op => {" +
                "      const card = document.createElement('div');" +
                "      card.className = 'history-card';" +
                "      const icon = getOperationIcon(op.type);" +
                "      card.innerHTML = `" +
                "        <div class=\\\"card-header\\\">" +
                "          <div class=\\\"card-id\\\">#${op.id}</div>" +
                "          <div class=\\\"card-status\\\">${escapeHtml(op.status)}</div>" +
                "        </div>" +
                "        <div class=\\\"card-type\\\">" +
                "          <div class=\\\"card-type-icon\\\">${icon}</div>" +
                "          <span>${escapeHtml(op.type)}</span>" +
                "        </div>" +
                "        <div class=\\\"card-description\\\">${escapeHtml(op.description)}</div>" +
                "        <div class=\\\"card-footer\\\">" +
                "          <div class=\\\"card-time\\\">" +
                "            <span>⏱</span>" +
                "            <span>${escapeHtml(op.time)}</span>" +
                "          </div>" +
                "        </div>" +
                "      `;" +
                "      fragment.appendChild(card);" +
                "    });" +
                "    grid.appendChild(fragment);" +
                "  }" +
                "  document.getElementById('subtitle').textContent = " +
                "    data.length === 0 ? 'No operations recorded' : " +
                "    `Last 50 Operations: ${data.length} total`;" +
                "}" +
                "function escapeHtml(text) {" +
                "  const div = document.createElement('div');" +
                "  div.textContent = text;" +
                "  return div.innerHTML;" +
                "}" +
                "function refresh() {" +
                "  if (typeof javaBridge !== 'undefined' && javaBridge.refresh) {" +
                "    javaBridge.refresh();" +
                "  }" +
                "}" +
                "function closeDialog() {" +
                "  if (typeof javaBridge !== 'undefined' && javaBridge.close) {" +
                "    javaBridge.close();" +
                "  }" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    public class JSBridge {
        public void refresh() {
            Platform.runLater(() -> {
                System.out.println("Refresh button clicked");
                loadHistory();
            });
        }

        public void close() {
            Platform.runLater(() -> {
                System.out.println("Close button clicked");
                WebViewHistoryDialog.this.close();
            });
        }
    }
}