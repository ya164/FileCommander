package com.filecommander.ui.dialogs;

import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import netscape.javascript.JSObject;

import java.io.File;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

public class WebViewSearchDialog extends Stage {
    private WebView webView;
    private WebEngine webEngine;
    private JSBridge jsBridge;
    private BiConsumer<SearchParams, PanelChoice> onSearch;
    private boolean isDarkTheme;

    public enum PanelChoice {
        LEFT, RIGHT, ACTIVE
    }

    public WebViewSearchDialog(BiConsumer<SearchParams, PanelChoice> onSearch, boolean isDarkTheme) {
        this.onSearch = onSearch;
        this.isDarkTheme = isDarkTheme;
        initializeWebView();
        setTitle("Search Files and Folders");
        initStyle(StageStyle.UNDECORATED);
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

                if (isDarkTheme) {
                    webEngine.executeScript("document.body.classList.add('dark');");
                }
            }
        });

        BorderPane root = new BorderPane();
        root.setCenter(webView);

        Scene scene = new Scene(root, 600, 580);
        setScene(scene);
    }

    private String getHTMLContent() {
        String userHome = System.getProperty("user.home").replace("\\", "\\\\");

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<meta charset=\"UTF-8\">\n" +
                "<style>\n" +
                "* { margin:0; padding:0; box-sizing:border-box; user-select:none; }\n" +
                ":root {\n" +
                "  --bg-primary:#f8fafc;\n" +
                "  --bg-secondary:#ffffff;\n" +
                "  --bg-input:#f1f5f9;\n" +
                "  --bg-hover:rgba(0,153,168,0.08);\n" +
                "  --text-primary:#1e293b;\n" +
                "  --text-secondary:#64748b;\n" +
                "  --accent:#0099a8;\n" +
                "  --accent-hover:#00b8cc;\n" +
                "  --accent-light:rgba(0,153,168,0.1);\n" +
                "  --border:#e2e8f0;\n" +
                "  --shadow:0 20px 25px -5px rgba(0,0,0,0.1),0 8px 10px -6px rgba(0,0,0,0.1);\n" +
                "  --shadow-sm:0 1px 2px 0 rgba(0,0,0,0.05);\n" +
                "  --glow:0 0 0 3px rgba(0,153,168,0.15);\n" +
                "}\n" +
                "body {\n" +
                "  font-family:'Segoe UI Variable Display','Segoe UI',system-ui,-apple-system,sans-serif;\n" +
                "  background:linear-gradient(135deg,#e0f7fa 0%,#b2ebf2 50%,#80deea 100%);\n" +
                "  height:100vh;\n" +
                "  display:flex;\n" +
                "  align-items:center;\n" +
                "  justify-content:center;\n" +
                "  overflow:hidden;\n" +
                "  position:relative;\n" +
                "}\n" +
                "body::before {\n" +
                "  content:'';\n" +
                "  position:absolute;\n" +
                "  inset:0;\n" +
                "  background:radial-gradient(circle at 30% 50%,rgba(0,188,212,0.15) 0%,transparent 50%),\n" +
                "              radial-gradient(circle at 70% 50%,rgba(0,151,167,0.15) 0%,transparent 50%);\n" +
                "  animation:pulse 8s ease-in-out infinite;\n" +
                "  pointer-events:none;\n" +
                "}\n" +
                "@keyframes pulse {\n" +
                "  0%,100% { opacity:1; }\n" +
                "  50% { opacity:0.7; }\n" +
                "}\n" +
                "body.dark {\n" +
                "  --bg-primary:#0f172a;\n" +
                "  --bg-secondary:#1e293b;\n" +
                "  --bg-input:#334155;\n" +
                "  --bg-hover:rgba(0,153,168,0.15);\n" +
                "  --text-primary:#f1f5f9;\n" +
                "  --text-secondary:#94a3b8;\n" +
                "  --accent:#00d4e6;\n" +
                "  --accent-hover:#00e8ff;\n" +
                "  --border:#334155;\n" +
                "  --shadow:0 20px 25px -5px rgba(0,0,0,0.5),0 8px 10px -6px rgba(0,0,0,0.5);\n" +
                "  --glow:0 0 0 3px rgba(0,212,230,0.2);\n" +
                "  background:linear-gradient(135deg,#0f172a 0%,#1e293b 50%,#334155 100%);\n" +
                "}\n" +
                "body.dark::before { display:none; }\n" +
                ".dialog-container {\n" +
                "  width:100%;\n" +
                "  height:100%;\n" +
                "  background:rgba(255,255,255,0.95);\n" +
                "  backdrop-filter:blur(40px) saturate(180%);\n" +
                "  -webkit-backdrop-filter:blur(40px) saturate(180%);\n" +
                "  border-radius:20px;\n" +
                "  box-shadow:var(--shadow);\n" +
                "  display:flex;\n" +
                "  flex-direction:column;\n" +
                "  overflow:hidden;\n" +
                "  border:1px solid rgba(255,255,255,0.3);\n" +
                "  animation:dialogIn 0.4s cubic-bezier(0.34,1.56,0.64,1);\n" +
                "}\n" +
                "body.dark .dialog-container {\n" +
                "  background:rgba(30,41,59,0.98);\n" +
                "  border-color:rgba(51,65,85,0.5);\n" +
                "}\n" +
                "@keyframes dialogIn {\n" +
                "  from { opacity:0; transform:scale(0.9) translateY(-20px); }\n" +
                "  to { opacity:1; transform:scale(1) translateY(0); }\n" +
                "}\n" +
                ".header {\n" +
                "  background:linear-gradient(135deg,var(--accent) 0%,var(--accent-hover) 100%);\n" +
                "  padding:32px 32px 28px;\n" +
                "  position:relative;\n" +
                "  overflow:hidden;\n" +
                "}\n" +
                ".header::before {\n" +
                "  content:'';\n" +
                "  position:absolute;\n" +
                "  inset:0;\n" +
                "  background:linear-gradient(135deg,transparent 0%,rgba(255,255,255,0.1) 100%);\n" +
                "}\n" +
                ".header-content { position:relative; z-index:1; }\n" +
                ".title {\n" +
                "  font-size:28px;\n" +
                "  font-weight:700;\n" +
                "  color:white;\n" +
                "  margin-bottom:6px;\n" +
                "  letter-spacing:-0.5px;\n" +
                "  text-shadow:0 2px 4px rgba(0,0,0,0.1);\n" +
                "}\n" +
                ".subtitle {\n" +
                "  font-size:14px;\n" +
                "  color:rgba(255,255,255,0.9);\n" +
                "  font-weight:500;\n" +
                "}\n" +
                ".content {\n" +
                "  flex:1;\n" +
                "  padding:32px;\n" +
                "  overflow-y:auto;\n" +
                "  background:var(--bg-primary);\n" +
                "}\n" +
                ".form-group {\n" +
                "  margin-bottom:24px;\n" +
                "  animation:slideUp 0.5s ease-out backwards;\n" +
                "}\n" +
                ".form-group:nth-child(1) { animation-delay:0.1s; }\n" +
                ".form-group:nth-child(2) { animation-delay:0.15s; }\n" +
                ".form-group:nth-child(3) { animation-delay:0.2s; }\n" +
                ".form-group:nth-child(4) { animation-delay:0.25s; }\n" +
                ".form-group:nth-child(5) { animation-delay:0.3s; }\n" +
                "@keyframes slideUp {\n" +
                "  from { opacity:0; transform:translateY(20px); }\n" +
                "  to { opacity:1; transform:translateY(0); }\n" +
                "}\n" +
                ".label {\n" +
                "  display:block;\n" +
                "  font-size:13px;\n" +
                "  font-weight:600;\n" +
                "  color:var(--text-secondary);\n" +
                "  margin-bottom:10px;\n" +
                "  text-transform:uppercase;\n" +
                "  letter-spacing:0.8px;\n" +
                "}\n" +
                ".input-wrapper {\n" +
                "  position:relative;\n" +
                "}\n" +
                ".input {\n" +
                "  width:100%;\n" +
                "  padding:14px 18px;\n" +
                "  border:2px solid var(--border);\n" +
                "  border-radius:12px;\n" +
                "  font-size:15px;\n" +
                "  font-family:inherit;\n" +
                "  background:var(--bg-input);\n" +
                "  color:var(--text-primary);\n" +
                "  transition:all 0.3s cubic-bezier(0.4,0,0.2,1);\n" +
                "  font-weight:500;\n" +
                "}\n" +
                ".input:focus {\n" +
                "  outline:none;\n" +
                "  border-color:var(--accent);\n" +
                "  background:var(--bg-secondary);\n" +
                "  box-shadow:var(--glow);\n" +
                "  transform:translateY(-1px);\n" +
                "}\n" +
                ".input::placeholder {\n" +
                "  color:var(--text-secondary);\n" +
                "  opacity:0.6;\n" +
                "}\n" +
                ".location-group {\n" +
                "  display:flex;\n" +
                "  gap:10px;\n" +
                "}\n" +
                ".location-group .input { flex:1; }\n" +
                ".btn-browse {\n" +
                "  padding:14px 24px;\n" +
                "  background:var(--bg-secondary);\n" +
                "  border:2px solid var(--border);\n" +
                "  border-radius:12px;\n" +
                "  font-size:14px;\n" +
                "  font-weight:600;\n" +
                "  color:var(--text-primary);\n" +
                "  cursor:pointer;\n" +
                "  transition:all 0.3s cubic-bezier(0.4,0,0.2,1);\n" +
                "  white-space:nowrap;\n" +
                "}\n" +
                ".btn-browse:hover {\n" +
                "  background:var(--bg-hover);\n" +
                "  border-color:var(--accent);\n" +
                "  transform:translateY(-2px);\n" +
                "  box-shadow:var(--shadow-sm);\n" +
                "}\n" +
                ".btn-browse:active {\n" +
                "  transform:translateY(0) scale(0.98);\n" +
                "}\n" +
                ".radio-group {\n" +
                "  display:flex;\n" +
                "  gap:16px;\n" +
                "  padding:14px;\n" +
                "  background:var(--bg-input);\n" +
                "  border-radius:12px;\n" +
                "  border:2px solid var(--border);\n" +
                "}\n" +
                ".radio-option {\n" +
                "  flex:1;\n" +
                "  display:flex;\n" +
                "  align-items:center;\n" +
                "  gap:10px;\n" +
                "  padding:12px 16px;\n" +
                "  background:var(--bg-secondary);\n" +
                "  border-radius:8px;\n" +
                "  cursor:pointer;\n" +
                "  transition:all 0.2s;\n" +
                "  border:2px solid transparent;\n" +
                "}\n" +
                ".radio-option:hover {\n" +
                "  background:var(--bg-hover);\n" +
                "  border-color:var(--accent);\n" +
                "}\n" +
                ".radio-option input[type=\"radio\"] {\n" +
                "  width:18px;\n" +
                "  height:18px;\n" +
                "  cursor:pointer;\n" +
                "  accent-color:var(--accent);\n" +
                "}\n" +
                ".radio-option label {\n" +
                "  font-size:14px;\n" +
                "  font-weight:600;\n" +
                "  color:var(--text-primary);\n" +
                "  cursor:pointer;\n" +
                "  user-select:none;\n" +
                "}\n" +
                ".checkbox-wrapper {\n" +
                "  display:flex;\n" +
                "  align-items:center;\n" +
                "  gap:12px;\n" +
                "  padding:14px 18px;\n" +
                "  background:var(--bg-input);\n" +
                "  border-radius:12px;\n" +
                "  cursor:pointer;\n" +
                "  transition:all 0.2s;\n" +
                "  border:2px solid var(--border);\n" +
                "}\n" +
                ".checkbox-wrapper:hover {\n" +
                "  background:var(--bg-hover);\n" +
                "  border-color:var(--accent);\n" +
                "}\n" +
                ".checkbox {\n" +
                "  width:20px;\n" +
                "  height:20px;\n" +
                "  cursor:pointer;\n" +
                "  accent-color:var(--accent);\n" +
                "}\n" +
                ".checkbox-label {\n" +
                "  font-size:14px;\n" +
                "  font-weight:500;\n" +
                "  color:var(--text-primary);\n" +
                "  cursor:pointer;\n" +
                "}\n" +
                ".footer {\n" +
                "  padding:24px 32px;\n" +
                "  background:var(--bg-primary);\n" +
                "  border-top:1px solid var(--border);\n" +
                "  display:flex;\n" +
                "  gap:12px;\n" +
                "  justify-content:flex-end;\n" +
                "}\n" +
                ".btn {\n" +
                "  padding:14px 32px;\n" +
                "  border-radius:12px;\n" +
                "  font-size:15px;\n" +
                "  font-weight:700;\n" +
                "  cursor:pointer;\n" +
                "  border:none;\n" +
                "  transition:all 0.3s cubic-bezier(0.4,0,0.2,1);\n" +
                "  position:relative;\n" +
                "  overflow:hidden;\n" +
                "  letter-spacing:0.3px;\n" +
                "}\n" +
                ".btn::after {\n" +
                "  content:'';\n" +
                "  position:absolute;\n" +
                "  top:50%;\n" +
                "  left:50%;\n" +
                "  width:0;\n" +
                "  height:0;\n" +
                "  border-radius:50%;\n" +
                "  background:rgba(255,255,255,0.3);\n" +
                "  transform:translate(-50%,-50%);\n" +
                "  transition:width 0.6s,height 0.6s;\n" +
                "}\n" +
                ".btn:active::after { width:300px; height:300px; }\n" +
                ".btn-primary {\n" +
                "  background:linear-gradient(135deg,var(--accent) 0%,var(--accent-hover) 100%);\n" +
                "  color:white;\n" +
                "  box-shadow:0 4px 12px rgba(0,153,168,0.3);\n" +
                "}\n" +
                ".btn-primary:hover {\n" +
                "  transform:translateY(-3px);\n" +
                "  box-shadow:0 8px 20px rgba(0,153,168,0.4);\n" +
                "}\n" +
                ".btn-primary:active {\n" +
                "  transform:translateY(-1px) scale(0.98);\n" +
                "}\n" +
                ".btn-secondary {\n" +
                "  background:var(--bg-secondary);\n" +
                "  color:var(--text-primary);\n" +
                "  border:2px solid var(--border);\n" +
                "}\n" +
                ".btn-secondary:hover {\n" +
                "  background:var(--bg-hover);\n" +
                "  border-color:var(--accent);\n" +
                "  transform:translateY(-2px);\n" +
                "}\n" +
                ".btn-secondary:active {\n" +
                "  transform:translateY(0) scale(0.98);\n" +
                "}\n" +
                "::-webkit-scrollbar { width:10px; }\n" +
                "::-webkit-scrollbar-track { background:transparent; }\n" +
                "::-webkit-scrollbar-thumb {\n" +
                "  background:linear-gradient(135deg,rgba(0,153,168,0.3),rgba(0,184,204,0.3));\n" +
                "  border-radius:5px;\n" +
                "}\n" +
                "::-webkit-scrollbar-thumb:hover {\n" +
                "  background:linear-gradient(135deg,rgba(0,153,168,0.5),rgba(0,184,204,0.5));\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"dialog-container\">\n" +
                "  <div class=\"header\">\n" +
                "    <div class=\"header-content\">\n" +
                "      <div class=\"title\">Search Files and Folders</div>\n" +
                "      <div class=\"subtitle\">Find what you need quickly</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"content\">\n" +
                "    <div class=\"form-group\">\n" +
                "      <label class=\"label\">Search Query</label>\n" +
                "      <input type=\"text\" class=\"input\" id=\"searchInput\" placeholder=\"*.txt, *.pdf, filename, etc.\" autofocus>\n" +
                "    </div>\n" +
                "    <div class=\"form-group\">\n" +
                "      <label class=\"label\">Search Location</label>\n" +
                "      <div class=\"location-group\">\n" +
                "        <input type=\"text\" class=\"input\" id=\"locationInput\" value=\"" + userHome + "\" placeholder=\"C:\\Users\\YourName\">\n" +
                "        <button class=\"btn-browse\" onclick=\"browseFolder()\">Browse</button>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"form-group\">\n" +
                "      <label class=\"label\">Item Type</label>\n" +
                "      <div class=\"radio-group\">\n" +
                "        <div class=\"radio-option\">\n" +
                "          <input type=\"radio\" name=\"itemType\" id=\"bothRadio\" value=\"BOTH\" checked>\n" +
                "          <label for=\"bothRadio\">Both</label>\n" +
                "        </div>\n" +
                "        <div class=\"radio-option\">\n" +
                "          <input type=\"radio\" name=\"itemType\" id=\"filesRadio\" value=\"FILES\">\n" +
                "          <label for=\"filesRadio\">Files Only</label>\n" +
                "        </div>\n" +
                "        <div class=\"radio-option\">\n" +
                "          <input type=\"radio\" name=\"itemType\" id=\"foldersRadio\" value=\"FOLDERS\">\n" +
                "          <label for=\"foldersRadio\">Folders Only</label>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"form-group\">\n" +
                "      <label class=\"label\">Display Results In</label>\n" +
                "      <div class=\"radio-group\">\n" +
                "        <div class=\"radio-option\">\n" +
                "          <input type=\"radio\" name=\"panelChoice\" id=\"leftPanel\" value=\"LEFT\">\n" +
                "          <label for=\"leftPanel\">Left Panel</label>\n" +
                "        </div>\n" +
                "        <div class=\"radio-option\">\n" +
                "          <input type=\"radio\" name=\"panelChoice\" id=\"rightPanel\" value=\"RIGHT\">\n" +
                "          <label for=\"rightPanel\">Right Panel</label>\n" +
                "        </div>\n" +
                "        <div class=\"radio-option\">\n" +
                "          <input type=\"radio\" name=\"panelChoice\" id=\"activePanel\" value=\"ACTIVE\" checked>\n" +
                "          <label for=\"activePanel\">Active Panel</label>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"form-group\">\n" +
                "      <label class=\"checkbox-wrapper\">\n" +
                "        <input type=\"checkbox\" class=\"checkbox\" id=\"subdirCheck\" checked>\n" +
                "        <span class=\"checkbox-label\">Include subdirectories</span>\n" +
                "      </label>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"footer\">\n" +
                "    <button class=\"btn btn-secondary\" onclick=\"closeDialog()\">Cancel</button>\n" +
                "    <button class=\"btn btn-primary\" onclick=\"startSearch()\">Search</button>\n" +
                "  </div>\n" +
                "</div>\n" +
                "<script>\n" +
                "document.getElementById('searchInput').addEventListener('keydown', function(e) {\n" +
                "    if (e.key === 'Enter') { startSearch(); }\n" +
                "});\n" +
                "function browseFolder() {\n" +
                "    javaBridge.browseFolder();\n" +
                "}\n" +
                "function startSearch() {\n" +
                "    const q = document.getElementById('searchInput').value.trim();\n" +
                "    const l = document.getElementById('locationInput').value.trim();\n" +
                "    const s = document.getElementById('subdirCheck').checked;\n" +
                "    const t = document.querySelector('input[name=\"itemType\"]:checked').value;\n" +
                "    const p = document.querySelector('input[name=\"panelChoice\"]:checked').value;\n" +
                "    javaBridge.search(q, l, s, t, p);\n" +
                "}\n" +
                "function closeDialog() { javaBridge.close(); }\n" +
                "function setLocation(p) { document.getElementById('locationInput').value = p; }\n" +
                "</script>\n" +
                "</body>\n" +
                "</html>";
    }

    public class JSBridge {
        public void browseFolder() {
            javafx.application.Platform.runLater(() -> {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setTitle("Select Search Location");
                try {
                    String currentPath = webEngine.executeScript("document.getElementById('locationInput').value").toString();
                    File initialDir = new File(currentPath);
                    if (initialDir.exists() && initialDir.isDirectory()) {
                        chooser.setInitialDirectory(initialDir);
                    }
                } catch (Exception e) {
                    chooser.setInitialDirectory(new File(System.getProperty("user.home")));
                }
                File selectedDir = chooser.showDialog(WebViewSearchDialog.this);
                if (selectedDir != null) {
                    String path = selectedDir.getAbsolutePath().replace("\\", "\\\\");
                    webEngine.executeScript("setLocation('" + path + "');");
                }
            });
        }

        public void search(String criteria, String location, boolean includeSubdirs, String itemType, String panelChoice) {
            javafx.application.Platform.runLater(() -> {
                if (criteria.isEmpty() || location.isEmpty()) {
                    return;
                }
                SearchParams params = new SearchParams();
                params.setCriteria(criteria);
                params.setRootPath(Paths.get(location));
                params.setIncludeSubdirectories(includeSubdirs);
                params.setItemType(SearchParams.ItemType.valueOf(itemType));

                PanelChoice choice = PanelChoice.valueOf(panelChoice);
                onSearch.accept(params, choice);
                close();
            });
        }

        public void close() {
            javafx.application.Platform.runLater(() -> WebViewSearchDialog.this.close());
        }
    }
}