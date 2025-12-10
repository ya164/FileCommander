package com.filecommander.ui.dialogs;

import com.filecommander.localization.LocalizationManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

public class WebViewProgressDialog extends Stage {
    private WebView webView;
    private WebEngine webEngine;
    private JSBridge jsBridge;
    private boolean isDarkTheme;
    private Runnable onCancel;
    private boolean isSearchMode = false;
    private LocalizationManager loc;

    public WebViewProgressDialog(String title, boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        this.loc = LocalizationManager.getInstance();
        initializeWebView(title);
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);
    }

    private void initializeWebView(String title) {
        webView = new WebView();
        webEngine = webView.getEngine();

        webEngine.setJavaScriptEnabled(true);
        webEngine.loadContent(getHTMLContent(title));

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                jsBridge = new JSBridge();
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", jsBridge);

                if (isDarkTheme) {
                    webEngine.executeScript("document.body.classList.add('dark');");
                }
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(webView);

        Scene scene = new Scene(root, 550, 340);
        setScene(scene);
    }

    public void setSearchMode(boolean isSearchMode) {
        this.isSearchMode = isSearchMode;
        Platform.runLater(() -> {
            if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("setSearchMode(" + isSearchMode + ");");
            }
        });
    }

    public void updateProgress(int current, int total, String currentFile) {
        Platform.runLater(() -> {
            if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                int percentage = total > 0 ? (int) ((current * 100.0) / total) : 0;
                webEngine.executeScript("updateProgress(" + percentage + ", " + current + ", " + total + ", '" + escapeJs(currentFile) + "');");
            }
        });
    }

    public void updateSearchProgress(int filesScanned, int filesFound, String currentPath) {
        Platform.runLater(() -> {
            if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("updateSearchProgress(" + filesScanned + ", " + filesFound + ", '" + escapeJs(currentPath) + "');");
            }
        });
    }

    public void setStatus(String status) {
        Platform.runLater(() -> {
            if (webEngine != null && webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript("setStatus('" + escapeJs(status) + "');");
            }
        });
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    public void close() {
        Platform.runLater(() -> {
            if (this.isShowing()) {
                this.hide();
            }
        });
    }

    private String escapeJs(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String getHTMLContent(String title) {
        String preparingText = loc.getString("progress.preparing");
        String cancelText = loc.getString("progress.cancel");
        String foundText = loc.getString("progress.found");
        String scannedText = loc.getString("progress.scanned");

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<style>\n" +
                "* { margin:0; padding:0; box-sizing:border-box; user-select:none; }\n" +
                ":root {\n" +
                "  --bg-primary:#ffffff;\n" +
                "  --bg-secondary:#f3f3f3;\n" +
                "  --text-primary:#1c1c1c;\n" +
                "  --text-secondary:#707070;\n" +
                "  --accent:#0099a8;\n" +
                "  --accent-bg:rgba(0,153,168,0.1);\n" +
                "  --border:rgba(0,0,0,0.08);\n" +
                "  --shadow:0 4px 20px rgba(0,0,0,0.15);\n" +
                "}\n" +
                "body {\n" +
                "  font-family:'Segoe UI Variable','Segoe UI',system-ui,sans-serif;\n" +
                "  background:var(--bg-primary);\n" +
                "  height:100vh;\n" +
                "  display:flex;\n" +
                "  flex-direction:column;\n" +
                "  overflow:hidden;\n" +
                "}\n" +
                "body.dark {\n" +
                "  --bg-primary:#2b2b2b;\n" +
                "  --bg-secondary:#1a1a1a;\n" +
                "  --text-primary:#e8e8e8;\n" +
                "  --text-secondary:#a0a0a0;\n" +
                "  --accent:#00d4e6;\n" +
                "  --accent-bg:rgba(0,212,230,0.15);\n" +
                "  --border:rgba(255,255,255,0.08);\n" +
                "  --shadow:0 4px 20px rgba(0,0,0,0.5);\n" +
                "}\n" +
                ".header {\n" +
                "  background:linear-gradient(135deg,var(--accent) 0%,#00b8cc 100%);\n" +
                "  padding:20px 24px;\n" +
                "  color:white;\n" +
                "}\n" +
                ".title {\n" +
                "  font-size:18px;\n" +
                "  font-weight:600;\n" +
                "}\n" +
                ".content {\n" +
                "  flex:1;\n" +
                "  padding:26px 24px;\n" +
                "  display:flex;\n" +
                "  flex-direction:column;\n" +
                "  gap:18px;\n" +
                "}\n" +
                ".status {\n" +
                "  font-size:13px;\n" +
                "  color:var(--text-secondary);\n" +
                "  font-weight:500;\n" +
                "  min-height:20px;\n" +
                "}\n" +
                ".progress-container {\n" +
                "  width:100%;\n" +
                "  height:8px;\n" +
                "  background:var(--bg-secondary);\n" +
                "  border-radius:4px;\n" +
                "  overflow:hidden;\n" +
                "  position:relative;\n" +
                "}\n" +
                ".progress-bar {\n" +
                "  height:100%;\n" +
                "  background:linear-gradient(90deg,var(--accent) 0%,#00b8cc 100%);\n" +
                "  border-radius:4px;\n" +
                "  transition:width 0.3s ease;\n" +
                "  position:relative;\n" +
                "  overflow:hidden;\n" +
                "}\n" +
                ".progress-bar.indeterminate {\n" +
                "  width:100% !important;\n" +
                "  background:linear-gradient(90deg,var(--accent) 0%,#00b8cc 50%,var(--accent) 100%);\n" +
                "  background-size:200% 100%;\n" +
                "  animation:indeterminate 1.5s linear infinite;\n" +
                "}\n" +
                "@keyframes indeterminate {\n" +
                "  0% { background-position:200% 0; }\n" +
                "  100% { background-position:-200% 0; }\n" +
                "}\n" +
                ".progress-bar::after {\n" +
                "  content:'';\n" +
                "  position:absolute;\n" +
                "  top:0;\n" +
                "  left:0;\n" +
                "  bottom:0;\n" +
                "  right:0;\n" +
                "  background:linear-gradient(90deg,transparent,rgba(255,255,255,0.3),transparent);\n" +
                "  animation:shimmer 1.5s infinite;\n" +
                "}\n" +
                "@keyframes shimmer {\n" +
                "  0% { transform:translateX(-100%); }\n" +
                "  100% { transform:translateX(100%); }\n" +
                "}\n" +
                ".stats {\n" +
                "  display:flex;\n" +
                "  justify-content:space-between;\n" +
                "  font-size:12px;\n" +
                "  color:var(--text-secondary);\n" +
                "  min-height:18px;\n" +
                "}\n" +
                ".current-file {\n" +
                "  font-size:12px;\n" +
                "  color:var(--text-primary);\n" +
                "  white-space:nowrap;\n" +
                "  overflow:hidden;\n" +
                "  text-overflow:ellipsis;\n" +
                "  padding:12px 14px;\n" +
                "  background:var(--bg-secondary);\n" +
                "  border-radius:6px;\n" +
                "  border:1px solid var(--border);\n" +
                "  min-height:44px;\n" +
                "  display:flex;\n" +
                "  align-items:center;\n" +
                "}\n" +
                ".footer {\n" +
                "  padding:20px 24px;\n" +
                "  border-top:1px solid var(--border);\n" +
                "  display:flex;\n" +
                "  justify-content:flex-end;\n" +
                "}\n" +
                ".btn-cancel {\n" +
                "  padding:12px 32px;\n" +
                "  background:var(--bg-secondary);\n" +
                "  border:1px solid var(--border);\n" +
                "  border-radius:8px;\n" +
                "  color:var(--text-primary);\n" +
                "  font-size:13px;\n" +
                "  font-weight:600;\n" +
                "  cursor:pointer;\n" +
                "  transition:all 0.2s;\n" +
                "}\n" +
                ".btn-cancel:hover {\n" +
                "  background:rgba(239,68,68,0.1);\n" +
                "  color:#ef4444;\n" +
                "  border-color:#ef4444;\n" +
                "  transform:translateY(-1px);\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"header\">\n" +
                "  <div class=\"title\">" + escapeJs(title) + "</div>\n" +
                "</div>\n" +
                "<div class=\"content\">\n" +
                "  <div class=\"status\" id=\"status\">" + escapeJs(preparingText) + "</div>\n" +
                "  <div class=\"progress-container\">\n" +
                "    <div class=\"progress-bar\" id=\"progressBar\" style=\"width:0%\"></div>\n" +
                "  </div>\n" +
                "  <div class=\"stats\">\n" +
                "    <span id=\"percentage\">0%</span>\n" +
                "    <span id=\"fileCount\">0 / 0</span>\n" +
                "  </div>\n" +
                "  <div class=\"current-file\" id=\"currentFile\">...</div>\n" +
                "</div>\n" +
                "<div class=\"footer\">\n" +
                "  <button class=\"btn-cancel\" onclick=\"cancel()\">" + escapeJs(cancelText) + "</button>\n" +
                "</div>\n" +
                "<script>\n" +
                "let isSearchMode = false;\n" +
                "const foundLabel = '" + escapeJs(foundText) + "';\n" +
                "const scannedLabel = '" + escapeJs(scannedText) + "';\n" +
                "function setSearchMode(mode) {\n" +
                "  isSearchMode = mode;\n" +
                "  if (mode) {\n" +
                "    document.getElementById('progressBar').classList.add('indeterminate');\n" +
                "  }\n" +
                "}\n" +
                "function updateProgress(percentage, current, total, file) {\n" +
                "  document.getElementById('progressBar').style.width = percentage + '%';\n" +
                "  document.getElementById('percentage').textContent = percentage + '%';\n" +
                "  document.getElementById('fileCount').textContent = current + ' / ' + total;\n" +
                "  document.getElementById('currentFile').textContent = file;\n" +
                "}\n" +
                "function updateSearchProgress(scanned, found, path) {\n" +
                "  document.getElementById('percentage').textContent = foundLabel + ': ' + found;\n" +
                "  document.getElementById('fileCount').textContent = scannedLabel + ': ' + scanned;\n" +
                "  document.getElementById('currentFile').textContent = path;\n" +
                "}\n" +
                "function setStatus(status) {\n" +
                "  document.getElementById('status').textContent = status;\n" +
                "}\n" +
                "function cancel() {\n" +
                "  javaBridge.cancel();\n" +
                "}\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    public class JSBridge {
        public void cancel() {
            Platform.runLater(() -> {
                if (onCancel != null) {
                    onCancel.run();
                }
                close();
            });
        }
    }
}