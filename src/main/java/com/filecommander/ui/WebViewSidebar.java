package com.filecommander.ui;

import com.filecommander.localization.LocalizationManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WebViewSidebar extends BorderPane {
    private WebView webView;
    private WebEngine webEngine;
    private MainWindow mainWindow;
    private JSBridge jsBridge;

    public WebViewSidebar(MainWindow mainWindow) {
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
        setPrefWidth(240);
        setMinWidth(240);
        setMaxWidth(240);
    }

    public void setTheme(boolean isDark) {
        if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
            try {
                if (isDark) {
                    webEngine.executeScript("if (document.body) { isDarkTheme = true; document.body.classList.add('dark'); }");
                } else {
                    webEngine.executeScript("if (document.body) { isDarkTheme = false; document.body.classList.remove('dark'); }");
                }
                System.out.println("Sidebar theme set to: " + (isDark ? "dark" : "light"));
            } catch (Exception e) {
                System.err.println("Error setting sidebar theme: " + e.getMessage());
            }
        }
    }

    public void updateLanguage() {
        if (webEngine != null) {
            webEngine.loadContent(getHTMLContent());
            Platform.runLater(() -> setTheme(mainWindow.isDarkTheme()));
        }
    }

    private String getDesktopPath() {
        String userHome = System.getProperty("user.home");

        String[] desktopPaths = {
                userHome + "\\OneDrive\\Робочий стол",
                userHome + "\\OneDrive\\Desktop",
                userHome + "\\Desktop",
                "C:\\Users\\Public\\Desktop"
        };

        for (String path : desktopPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return userHome + "\\Desktop";
    }

    private String getDocumentsPath() {
        String userHome = System.getProperty("user.home");

        String[] documentsPaths = {
                userHome + "\\OneDrive\\Документы",
                userHome + "\\OneDrive\\Documents",
                userHome + "\\OneDrive\\Документи",
                userHome + "\\Documents",
                userHome + "\\Документы",
                userHome + "\\Документи"
        };

        for (String path : documentsPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return userHome + "\\Documents";
    }

    private String getPicturesPath() {
        String userHome = System.getProperty("user.home");

        String[] picturesPaths = {
                userHome + "\\OneDrive\\Изображения",
                userHome + "\\OneDrive\\Pictures",
                userHome + "\\OneDrive\\Зображення",
                userHome + "\\Pictures",
                userHome + "\\Изображения",
                userHome + "\\Зображення"
        };

        for (String path : picturesPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return userHome + "\\Pictures";
    }

    private String getDownloadsPath() {
        String userHome = System.getProperty("user.home");

        String[] downloadsPaths = {
                userHome + "\\Downloads",
                userHome + "\\Загрузки",
                userHome + "\\Завантаження"
        };

        for (String path : downloadsPaths) {
            if (Files.exists(Paths.get(path))) {
                return path;
            }
        }

        return userHome + "\\Downloads";
    }

    private String getHTMLContent() {
        LocalizationManager loc = LocalizationManager.getInstance();

        String userHome = System.getProperty("user.home").replace("\\", "\\\\");
        String desktopPath = getDesktopPath().replace("\\", "\\\\");
        String documentsPath = getDocumentsPath().replace("\\", "\\\\");
        String picturesPath = getPicturesPath().replace("\\", "\\\\");
        String downloadsPath = getDownloadsPath().replace("\\", "\\\\");

        StringBuilder drivesHtml = new StringBuilder();
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++) {
            String drivePath = roots[i].getAbsolutePath().replace("\\", "\\\\");
            String driveName = roots[i].getAbsolutePath().replace("\\", "");
            String icon = driveName.startsWith("C") ? "&#x1F4BB;" : "&#x1F4BF;";

            drivesHtml.append("<div class=\"nav-item\" style=\"animation-delay: ")
                    .append(0.39 + i * 0.03)
                    .append("s;\" onclick=\"navigate('")
                    .append(drivePath)
                    .append("')\"><span class=\"nav-icon\">")
                    .append(icon)
                    .append("</span><span class=\"nav-text\">")
                    .append(loc.getString("sidebar.localDisk"))
                    .append(" (")
                    .append(driveName)
                    .append(")</span></div>\n");
        }

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<style>\n" +
                "* { margin: 0; padding: 0; box-sizing: border-box; user-select: none; }\n" +
                ":root {\n" +
                "  --bg-primary: #f3f3f3;\n" +
                "  --bg-secondary: #ffffff;\n" +
                "  --bg-hover: rgba(0, 153, 168, 0.1);\n" +
                "  --bg-active: rgba(0, 153, 168, 0.2);\n" +
                "  --text-primary: #1c1c1c;\n" +
                "  --text-secondary: #707070;\n" +
                "  --accent: #0099a8;\n" +
                "  --accent-hover: #00b8cc;\n" +
                "  --border: rgba(0, 0, 0, 0.08);\n" +
                "  --shadow: rgba(0, 0, 0, 0.05);\n" +
                "}\n" +
                "body {\n" +
                "  font-family: 'Segoe UI Variable', 'Segoe UI', 'Segoe UI Emoji', 'Noto Color Emoji', 'Apple Color Emoji', system-ui, sans-serif;\n" +
                "  background: var(--bg-primary);\n" +
                "  height: 100vh;\n" +
                "  overflow: hidden;\n" +
                "  color: var(--text-primary);\n" +
                "}\n" +
                "body.dark {\n" +
                "  --bg-primary: #202020;\n" +
                "  --bg-secondary: #2b2b2b;\n" +
                "  --bg-hover: rgba(0, 153, 168, 0.15);\n" +
                "  --bg-active: rgba(0, 153, 168, 0.25);\n" +
                "  --text-primary: #e8e8e8;\n" +
                "  --text-secondary: #a0a0a0;\n" +
                "  --border: rgba(255, 255, 255, 0.08);\n" +
                "  --shadow: rgba(0, 0, 0, 0.3);\n" +
                "}\n" +
                ".sidebar {\n" +
                "  height: 100%;\n" +
                "  background: var(--bg-secondary);\n" +
                "  display: flex;\n" +
                "  flex-direction: column;\n" +
                "  overflow-y: auto;\n" +
                "  overflow-x: hidden;\n" +
                "  border-right: 1px solid var(--border);\n" +
                "}\n" +
                ".section {\n" +
                "  margin: 20px 0 8px 16px;\n" +
                "  font-size: 11px;\n" +
                "  font-weight: 600;\n" +
                "  color: var(--text-secondary);\n" +
                "  letter-spacing: 0.5px;\n" +
                "  text-transform: uppercase;\n" +
                "  opacity: 0;\n" +
                "  animation: fadeIn 0.4s ease-out forwards;\n" +
                "}\n" +
                ".section:nth-child(1) { animation-delay: 0.1s; }\n" +
                ".section:nth-child(9) { animation-delay: 0.2s; }\n" +
                ".nav-item {\n" +
                "  display: flex;\n" +
                "  align-items: center;\n" +
                "  padding: 10px 16px;\n" +
                "  margin: 2px 8px;\n" +
                "  border-radius: 6px;\n" +
                "  cursor: pointer;\n" +
                "  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);\n" +
                "  position: relative;\n" +
                "  overflow: hidden;\n" +
                "  opacity: 0;\n" +
                "  animation: slideIn 0.4s ease-out forwards;\n" +
                "}\n" +
                ".nav-item:nth-child(2) { animation-delay: 0.15s; }\n" +
                ".nav-item:nth-child(3) { animation-delay: 0.18s; }\n" +
                ".nav-item:nth-child(4) { animation-delay: 0.21s; }\n" +
                ".nav-item:nth-child(5) { animation-delay: 0.24s; }\n" +
                ".nav-item:nth-child(6) { animation-delay: 0.27s; }\n" +
                ".nav-item:nth-child(7) { animation-delay: 0.30s; }\n" +
                ".nav-item:nth-child(8) { animation-delay: 0.33s; }\n" +
                ".nav-item::before {\n" +
                "  content: '';\n" +
                "  position: absolute;\n" +
                "  left: 0;\n" +
                "  top: 0;\n" +
                "  bottom: 0;\n" +
                "  width: 3px;\n" +
                "  background: var(--accent);\n" +
                "  transform: scaleY(0);\n" +
                "  transition: transform 0.2s ease;\n" +
                "}\n" +
                ".nav-item:hover {\n" +
                "  background: var(--bg-hover);\n" +
                "  transform: translateX(2px);\n" +
                "}\n" +
                ".nav-item:hover::before { transform: scaleY(1); }\n" +
                ".nav-item:active {\n" +
                "  background: var(--bg-active);\n" +
                "  transform: translateX(2px) scale(0.98);\n" +
                "}\n" +
                ".nav-icon {\n" +
                "  font-size: 20px;\n" +
                "  margin-right: 12px;\n" +
                "  flex-shrink: 0;\n" +
                "  filter: drop-shadow(0 1px 2px var(--shadow));\n" +
                "  transition: transform 0.2s ease;\n" +
                "}\n" +
                ".nav-item:hover .nav-icon { transform: scale(1.1); }\n" +
                ".nav-text {\n" +
                "  font-size: 14px;\n" +
                "  font-weight: 500;\n" +
                "  color: var(--text-primary);\n" +
                "  flex: 1;\n" +
                "}\n" +
                "::-webkit-scrollbar { width: 8px; }\n" +
                "::-webkit-scrollbar-track { background: transparent; }\n" +
                "::-webkit-scrollbar-thumb {\n" +
                "  background: rgba(0, 153, 168, 0.3);\n" +
                "  border-radius: 4px;\n" +
                "  transition: background 0.2s;\n" +
                "}\n" +
                "::-webkit-scrollbar-thumb:hover { background: rgba(0, 153, 168, 0.5); }\n" +
                "@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }\n" +
                "@keyframes slideIn { from { opacity: 0; transform: translateX(-20px); } to { opacity: 1; transform: translateX(0); } }\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"sidebar\">\n" +
                "<div class=\"section\">" + loc.getString("sidebar.quickAccess") + "</div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('" + desktopPath + "')\"><span class=\"nav-icon\">&#x1F5A5;</span><span class=\"nav-text\">" + loc.getString("sidebar.desktop") + "</span></div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('" + documentsPath + "')\"><span class=\"nav-icon\">&#x1F4C1;</span><span class=\"nav-text\">" + loc.getString("sidebar.documents") + "</span></div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('" + picturesPath + "')\"><span class=\"nav-icon\">&#x1F5BC;</span><span class=\"nav-text\">" + loc.getString("sidebar.pictures") + "</span></div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('" + downloadsPath + "')\"><span class=\"nav-icon\">&#x1F4E5;</span><span class=\"nav-text\">" + loc.getString("sidebar.downloads") + "</span></div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('" + userHome + "\\\\\\\\Music')\"><span class=\"nav-icon\">&#x1F3B5;</span><span class=\"nav-text\">" + loc.getString("sidebar.music") + "</span></div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('" + userHome + "\\\\\\\\Videos')\"><span class=\"nav-icon\">&#x1F3AC;</span><span class=\"nav-text\">" + loc.getString("sidebar.videos") + "</span></div>\n" +
                "<div class=\"nav-item\" onclick=\"navigate('C:\\\\\\\\\\\\\\\\Users')\"><span class=\"nav-icon\">&#x1F465;</span><span class=\"nav-text\">" + loc.getString("sidebar.users") + "</span></div>\n" +
                "<div class=\"section\">" + loc.getString("sidebar.drives") + "</div>\n" +
                drivesHtml.toString() +
                "</div>\n" +
                "<script>\n" +
                "let isDarkTheme = false;\n" +
                "function navigate(path) {\n" +
                "  console.log('Navigating to:', path);\n" +
                "  javaBridge.navigateToPath(path);\n" +
                "}\n" +
                "function toggleTheme() {\n" +
                "  isDarkTheme = !isDarkTheme;\n" +
                "  if (isDarkTheme) {\n" +
                "    document.body.classList.add('dark');\n" +
                "  } else {\n" +
                "    document.body.classList.remove('dark');\n" +
                "  }\n" +
                "}\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    public class JSBridge {
        public void navigateToPath(String pathStr) {
            javafx.application.Platform.runLater(() -> {
                try {
                    FXFilePanel activePanel = mainWindow.getActivePanel();
                    if (activePanel != null) {
                        activePanel.navigateToPath(Paths.get(pathStr));
                    }
                } catch (Exception e) {
                    System.err.println("Navigation error: " + e.getMessage());
                }
            });
        }
    }
}