package com.filecommander.ui;

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

    private String getDesktopPath() {
        String userHome = System.getProperty("user.home");

        String[] desktopPaths = {
                userHome + "\\OneDrive\\Рабочий стол",
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
                    .append("</span><span class=\"nav-text\">Local Disk (")
                    .append(driveName)
                    .append(")</span></div>");
        }

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>*{margin:0;padding:0;box-sizing:border-box;user-select:none;}:root{--bg-primary:#f3f3f3;--bg-secondary:#ffffff;--bg-hover:rgba(0,153,168,0.1);--bg-active:rgba(0,153,168,0.2);--text-primary:#1c1c1c;--text-secondary:#707070;--accent:#0099a8;--accent-hover:#00b8cc;--border:rgba(0,0,0,0.08);--shadow:rgba(0,0,0,0.05);}body{font-family:'Segoe UI Variable','Segoe UI','Segoe UI Emoji','Noto Color Emoji','Apple Color Emoji',system-ui,sans-serif;background:var(--bg-primary);height:100vh;overflow:hidden;color:var(--text-primary);}body.dark{--bg-primary:#202020;--bg-secondary:#2b2b2b;--bg-hover:rgba(0,153,168,0.15);--bg-active:rgba(0,153,168,0.25);--text-primary:#e8e8e8;--text-secondary:#a0a0a0;--border:rgba(255,255,255,0.08);--shadow:rgba(0,0,0,0.3);}.sidebar{height:100%;background:var(--bg-secondary);display:flex;flex-direction:column;overflow-y:auto;overflow-x:hidden;border-right:1px solid var(--border);}.section{margin:20px 0 8px 16px;font-size:11px;font-weight:600;color:var(--text-secondary);letter-spacing:0.5px;text-transform:uppercase;opacity:0;animation:fadeIn 0.4s ease-out forwards;}.section:nth-child(1){animation-delay:0.1s;}.section:nth-child(9){animation-delay:0.2s;}.nav-item{display:flex;align-items:center;padding:10px 16px;margin:2px 8px;border-radius:6px;cursor:pointer;transition:all 0.2s cubic-bezier(0.4,0,0.2,1);position:relative;overflow:hidden;opacity:0;animation:slideIn 0.4s ease-out forwards;}.nav-item:nth-child(2){animation-delay:0.15s;}.nav-item:nth-child(3){animation-delay:0.18s;}.nav-item:nth-child(4){animation-delay:0.21s;}.nav-item:nth-child(5){animation-delay:0.24s;}.nav-item:nth-child(6){animation-delay:0.27s;}.nav-item:nth-child(7){animation-delay:0.30s;}.nav-item:nth-child(8){animation-delay:0.33s;}.nav-item::before{content:'';position:absolute;left:0;top:0;bottom:0;width:3px;background:var(--accent);transform:scaleY(0);transition:transform 0.2s ease;}.nav-item:hover{background:var(--bg-hover);transform:translateX(2px);}.nav-item:hover::before{transform:scaleY(1);}.nav-item:active{background:var(--bg-active);transform:translateX(2px) scale(0.98);}.nav-icon{font-size:20px;margin-right:12px;flex-shrink:0;filter:drop-shadow(0 1px 2px var(--shadow));transition:transform 0.2s ease;}.nav-item:hover .nav-icon{transform:scale(1.1);}.nav-text{font-size:14px;font-weight:500;color:var(--text-primary);flex:1;}::-webkit-scrollbar{width:8px;}::-webkit-scrollbar-track{background:transparent;}::-webkit-scrollbar-thumb{background:rgba(0,153,168,0.3);border-radius:4px;transition:background 0.2s;}::-webkit-scrollbar-thumb:hover{background:rgba(0,153,168,0.5);}@keyframes fadeIn{from{opacity:0;transform:translateY(-10px);}to{opacity:1;transform:translateY(0);}}@keyframes slideIn{from{opacity:0;transform:translateX(-20px);}to{opacity:1;transform:translateX(0);}}</style></head><body><div class=\"sidebar\"><div class=\"section\">Quick Access</div><div class=\"nav-item\" onclick=\"navigate('" + desktopPath + "')\"><span class=\"nav-icon\">&#x1F5A5;</span><span class=\"nav-text\">Desktop</span></div><div class=\"nav-item\" onclick=\"navigate('" + documentsPath + "')\"><span class=\"nav-icon\">&#x1F4C1;</span><span class=\"nav-text\">Documents</span></div><div class=\"nav-item\" onclick=\"navigate('" + picturesPath + "')\"><span class=\"nav-icon\">&#x1F5BC;</span><span class=\"nav-text\">Pictures</span></div><div class=\"nav-item\" onclick=\"navigate('" + downloadsPath + "')\"><span class=\"nav-icon\">&#x1F4E5;</span><span class=\"nav-text\">Downloads</span></div><div class=\"nav-item\" onclick=\"navigate('" + userHome + "\\\\\\\\Music')\"><span class=\"nav-icon\">&#x1F3B5;</span><span class=\"nav-text\">Music</span></div><div class=\"nav-item\" onclick=\"navigate('" + userHome + "\\\\\\\\Videos')\"><span class=\"nav-icon\">&#x1F3AC;</span><span class=\"nav-text\">Videos</span></div><div class=\"nav-item\" onclick=\"navigate('C:\\\\\\\\\\\\\\\\Users')\"><span class=\"nav-icon\">&#x1F465;</span><span class=\"nav-text\">Users</span></div><div class=\"section\">Devices</div>" + drivesHtml.toString() + "</div><script>let isDarkTheme=false;function navigate(path){console.log('Navigating to:',path);javaBridge.navigateToPath(path);}function toggleTheme(){isDarkTheme=!isDarkTheme;if(isDarkTheme){document.body.classList.add('dark');}else{document.body.classList.remove('dark');}}</script></body></html>";
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