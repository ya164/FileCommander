package com.filecommander.localization;

import java.util.HashMap;
import java.util.Map;

public class LocalizationManager {
    private static LocalizationManager instance;
    private String currentLanguage = "uk"; // за замовчуванням українська
    private Map<String, Map<String, String>> translations;

    private LocalizationManager() {
        initializeTranslations();
    }

    public static synchronized LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }
        return instance;
    }

    private void initializeTranslations() {
        translations = new HashMap<>();

        Map<String, String> uk = new HashMap<>();

        uk.put("app.title", "File Commander");

        uk.put("toolbar.copy", "Копіювати");
        uk.put("toolbar.move", "Перемістити");
        uk.put("toolbar.delete", "Видалити");
        uk.put("toolbar.newFolder", "Нова папка");
        uk.put("toolbar.rename", "Перейменувати");
        uk.put("toolbar.search", "Пошук");
        uk.put("toolbar.undo", "Скасувати");
        uk.put("toolbar.history", "Історія");
        uk.put("toolbar.showHidden", "Показати приховані");
        uk.put("toolbar.hideHidden", "Приховати приховані");
        uk.put("toolbar.toggleHidden", "Приховані файли");
        uk.put("toolbar.theme", "Тема");
        uk.put("toolbar.language", "Мова");

        uk.put("sidebar.quickAccess", "Швидкий доступ");
        uk.put("sidebar.desktop", "Робочий стіл");
        uk.put("sidebar.documents", "Документи");
        uk.put("sidebar.pictures", "Зображення");
        uk.put("sidebar.downloads", "Завантаження");
        uk.put("sidebar.music", "Музика");
        uk.put("sidebar.videos", "Відео");
        uk.put("sidebar.users", "Користувачі");
        uk.put("sidebar.drives", "Пристрої");
        uk.put("sidebar.localDisk", "Локальний диск");

        uk.put("panel.tooltip.back", "Назад");
        uk.put("panel.tooltip.forward", "Вперед");
        uk.put("panel.tooltip.up", "Вгору");
        uk.put("panel.tooltip.go", "Перейти");
        uk.put("panel.tooltip.theme", "Змінити тему");
        uk.put("panel.path.placeholder", "Введіть шлях...");
        uk.put("panel.column.name", "Назва");
        uk.put("panel.column.size", "Розмір");
        uk.put("panel.column.modified", "Дата зміни");
        uk.put("panel.status.selected", "Вибрано: {0} | Папок: {1} | Файлів: {2}");
        uk.put("panel.disk.free", "Вільно: {0} ГБ");
        uk.put("panel.search.results", "Результати пошуку: \"{0}\" ({1} елементів)");

        uk.put("type.folder", "Папка");
        uk.put("type.textFile", "Текстовий файл");
        uk.put("type.pdfDocument", "PDF документ");
        uk.put("type.image", "Зображення");
        uk.put("type.audio", "Аудіо");
        uk.put("type.video", "Відео");
        uk.put("type.archive", "Архів");
        uk.put("type.program", "Програма");
        uk.put("type.javaCode", "Java код");
        uk.put("type.dataFile", "Файл даних");
        uk.put("type.file", "Файл");

        uk.put("size.folder", "<ПАПКА>");
        uk.put("size.bytes", "Б");
        uk.put("size.kb", "КБ");
        uk.put("size.mb", "МБ");
        uk.put("size.gb", "ГБ");

        uk.put("context.open", "Відкрити");
        uk.put("context.copy", "Копіювати (F3)");
        uk.put("context.move", "Перемістити (F6)");
        uk.put("context.delete", "Видалити (F8)");
        uk.put("context.newFolder", "Нова папка (F7)");
        uk.put("context.rename", "Перейменувати (F2)");
        uk.put("context.copyToClipboard", "Копіювати в буфер (Ctrl+C)");
        uk.put("context.paste", "Вставити (Ctrl+V)");
        uk.put("context.gotoLocation", "Перейти до розташування");
        uk.put("context.openFolder", "Відкрити папку");
        uk.put("context.openFile", "Відкрити файл");
        uk.put("context.copyToOther", "Копіювати в іншу панель");

        uk.put("popup.copy.title", "Копіювати {0} елементів до:");
        uk.put("popup.copy.current", "Поточна панель: '{0}' (C)");
        uk.put("popup.copy.other", "Інша панель: '{0}' (O)");
        uk.put("popup.action.title", "Що ви хочете зробити з '{0}'?");
        uk.put("popup.action.goto", "Перейти до розташування (G)");
        uk.put("popup.action.openFolder", "Відкрити папку (O)");
        uk.put("popup.action.openFile", "Відкрити файл (O)");
        uk.put("popup.openFolder.title", "Відкрити папку в якій панелі?");
        uk.put("popup.openFolder.current", "Поточна панель (C)");
        uk.put("popup.openFolder.other", "Інша панель (O)");
        uk.put("popup.gotoLocation.title", "Перейти до розташування в якій панелі?");
        uk.put("popup.gotoLocation.current", "Поточна панель (C)");
        uk.put("popup.gotoLocation.other", "Інша панель (O)");
        uk.put("popup.cancel", "Скасувати");

        uk.put("dialog.confirmDelete", "Підтвердження видалення");
        uk.put("dialog.confirmDeleteMessage", "Ви впевнені, що хочете видалити {0} елементів?");
        uk.put("dialog.error", "Помилка");
        uk.put("dialog.warning", "Попередження");
        uk.put("dialog.info", "Інформація");

        uk.put("error.path.title", "Помилка шляху");
        uk.put("error.path.notExists", "Шлях не існує або це не папка");
        uk.put("error.path.invalid", "Невірний формат шляху");
        uk.put("error.searchMode.title", "Обмеження режиму пошуку");
        uk.put("error.searchMode.message", "Ця операція недоступна в режимі результатів пошуку.\nДоступні тільки копіювання та перегляд.");
        uk.put("error.searchModeRestriction", "Ця операція недоступна в режимі пошуку.");
        uk.put("error.operationUnavailable", "Операція недоступна");

        uk.put("error.accessDenied", "Доступ заборонено");
        uk.put("error.accessDeniedDetails", "Немає прав доступу до: {0}. Спробуйте запустити програму від імені адміністратора.");
        uk.put("error.fileAlreadyExists", "Файл вже існує: {0}");
        uk.put("error.fileNotFound", "Файл не знайдено: {0}");
        uk.put("error.notEmpty", "Папка не порожня: {0}");
        uk.put("error.io", "Помилка вводу/виводу: {0}");
        uk.put("error.unknown", "Невідома помилка: {0}");

        uk.put("error.fileNotExist", "Файл не існує: {0}");
        uk.put("error.folderSame", "Неможливо скопіювати папку саму в себе: {0}");
        uk.put("error.destNotExist", "Папка призначення не існує: {0}");
        uk.put("error.destNotFolder", "Шлях призначення не є папкою: {0}");
        uk.put("error.noWriteAccess", "Немає прав на запис: {0}");
        uk.put("error.folderExists", "Папка вже існує: {0}");
        uk.put("error.parentNotExist", "Батьківська папка не існує");
        uk.put("error.moveSameFolder", "Неможливо перемістити файли в ту саму папку");
        uk.put("error.fileExists", "Файл або папка з ім'ям '{0}' вже існує");
        uk.put("error.fileExistsDestination", "Файл або папка з іменем '{0}' вже існує в місці призначення");
        uk.put("error.deleteLocked", "Неможливо видалити заблокований файл: {0}");
        uk.put("error.deleteNotEmpty", "Неможливо видалити непорожню папку: {0}");
        uk.put("error.destinationNotExist", "Папка призначення не існує: {0}");
        uk.put("error.destinationNotFolder", "Шлях призначення не є папкою: {0}");
        uk.put("error.noWritePermission", "Немає прав на запис до папки призначення: {0}");
        uk.put("error.noWriteParent", "Немає прав на запис до батьківської папки");
        uk.put("error.cannotMoveSameFolder", "Неможливо перемістити файли в ту саму папку");
        uk.put("error.cannotDeleteNonEmpty", "Неможливо видалити непорожню папку: {0}");

        uk.put("warning.noSelection", "Не вибрано файлів");
        uk.put("warning.cannotMoveSameFolder", "Неможливо перемістити файли в ту саму папку");
        uk.put("warning.fileExists", "Файл або папка з такою назвою вже існує");
        uk.put("warning.bufferEmpty", "Буфер обміну порожній");

        uk.put("operation.copying", "Копіювання файлів");
        uk.put("operation.moving", "Переміщення файлів");
        uk.put("operation.deleting", "Видалення файлів");
        uk.put("operation.undoing", "Скасування операції");
        uk.put("operation.preparing", "Підготовка...");
        uk.put("operation.undoSuccess", "Операцію успішно скасовано");
        uk.put("operation.undoFailed", "Не вдалося скасувати");
        uk.put("operation.createFolderFailed", "Не вдалося створити папку");
        uk.put("operation.failed", "Операція не виконана");
        uk.put("operation.success", "Операцію успішно виконано");
        uk.put("operation.renameFailed", "Не вдалося перейменувати");
        uk.put("operation.countingFiles", "Підрахунок файлів...");
        uk.put("operation.deletingFiles", "Видалення файлів...");
        uk.put("operation.cancelled", "Операцію скасовано користувачем");
        uk.put("operation.restoring", "Підготовка до відновлення файлів...");
        uk.put("operation.restoringFolder", "Відновлення папки:");
        uk.put("operation.restoringFile", "Відновлення файлу:");

        uk.put("operation.description.copy", "Скопіювати {0} елементів до {1}");
        uk.put("operation.description.copyOne", "Створити та скопіювати {0}");
        uk.put("operation.description.move", "Перемістити {0} елементів до {1}");
        uk.put("operation.description.delete", "Видалити {0} елементів");
        uk.put("operation.description.rename", "Перейменувати {0} на {1}");
        uk.put("operation.description.createFolder", "Створити папку: {0}");

        uk.put("search.title", "Пошук файлів та папок");
        uk.put("search.subtitle", "Швидко знайдіть те, що потрібно");
        uk.put("search.query", "Запит пошуку");
        uk.put("search.queryPlaceholder", "*.txt, *.pdf, назва файлу...");
        uk.put("search.location", "Місце пошуку");
        uk.put("search.browse", "Огляд");
        uk.put("search.browseTitle", "Виберіть папку для пошуку");
        uk.put("search.itemTypeLabel", "Тип елементів");
        uk.put("search.itemType.both", "Всі");
        uk.put("search.itemType.files", "Тільки файли");
        uk.put("search.itemType.folders", "Тільки папки");
        uk.put("search.showResultsIn", "Показати результати в");
        uk.put("search.leftPanel", "Ліва панель");
        uk.put("search.rightPanel", "Права панель");
        uk.put("search.activePanel", "Активна панель");
        uk.put("search.includeSubdirs", "Включити підпапки");
        uk.put("search.cancel", "Скасувати");
        uk.put("search.search", "Пошук");
        uk.put("search.enterCriteria", "Введіть критерій пошуку");
        uk.put("search.cannotDetermine", "Не вдалося визначити цільову панель");

        uk.put("progress.preparing", "Підготовка...");
        uk.put("progress.cancel", "Скасувати");
        uk.put("progress.found", "Знайдено");
        uk.put("progress.scanned", "Проскановано");

        uk.put("history.title", "Історія операцій");
        uk.put("history.loading", "Завантаження...");
        uk.put("history.refresh", "Оновити");
        uk.put("history.close", "Закрити");
        uk.put("history.empty", "Операцій ще немає");
        uk.put("history.emptySubtext", "Історія операцій з'явиться тут");
        uk.put("history.noOperations", "Немає операцій");
        uk.put("history.recentOperations", "Останні операції");
        uk.put("history.total", "всього");

        uk.put("operation.type.Copy", "Копіювання");
        uk.put("operation.type.Move", "Переміщення");
        uk.put("operation.type.Delete", "Видалення");
        uk.put("operation.type.CreateFolder", "Створення папки");
        uk.put("operation.type.Rename", "Перейменування");
        uk.put("operation.type.Paste", "Вставка");

        uk.put("history.status.SUCCESS", "УСПІХ");
        uk.put("history.status.FAILED", "ПОМИЛКА");

        uk.put("history.keyword.created", "Створено папку");
        uk.put("history.keyword.deleted", "Видалити");
        uk.put("history.keyword.copied", "Скопіювати");
        uk.put("history.keyword.moved", "Перемістити");
        uk.put("history.keyword.renamed", "Перейменувати");
        uk.put("history.keyword.items", "елементів");
        uk.put("history.keyword.to", "до");
        uk.put("history.keyword.on", "на");
        uk.put("error.validation", "Помилка перевірки");
        uk.put("error.accessDeniedSystem", "Помилка доступу: Неможливо записати до системної папки {0}. Виберіть іншу папку або запустіть від імені адміністратора.");
        uk.put("error.accessDeniedFolder", "Помилка доступу: Неможливо записати до папки {0}. Виберіть іншу папку або перевірте права доступу.");
        uk.put("error.accessDeniedGeneral", "Доступ заборонено. Перевірте права доступу до файлу або папки.");
        uk.put("error.unknownOperation", "Невідома помилка під час виконання операції");

        translations.put("uk", uk);


        Map<String, String> en = new HashMap<>();

        en.put("app.title", "File Commander");

        en.put("toolbar.copy", "Copy");
        en.put("toolbar.move", "Move");
        en.put("toolbar.delete", "Delete");
        en.put("toolbar.newFolder", "New Folder");
        en.put("toolbar.rename", "Rename");
        en.put("toolbar.search", "Search");
        en.put("toolbar.undo", "Undo");
        en.put("toolbar.history", "History");
        en.put("toolbar.showHidden", "Show Hidden");
        en.put("toolbar.hideHidden", "Hide Hidden");
        en.put("toolbar.toggleHidden", "Toggle Hidden");
        en.put("toolbar.theme", "Theme");
        en.put("toolbar.language", "Language");

        en.put("sidebar.quickAccess", "Quick Access");
        en.put("sidebar.desktop", "Desktop");
        en.put("sidebar.documents", "Documents");
        en.put("sidebar.pictures", "Pictures");
        en.put("sidebar.downloads", "Downloads");
        en.put("sidebar.music", "Music");
        en.put("sidebar.videos", "Videos");
        en.put("sidebar.users", "Users");
        en.put("sidebar.drives", "Drives");
        en.put("sidebar.localDisk", "Local Disk");

        en.put("panel.tooltip.back", "Back");
        en.put("panel.tooltip.forward", "Forward");
        en.put("panel.tooltip.up", "Up");
        en.put("panel.tooltip.go", "Go");
        en.put("panel.tooltip.theme", "Change Theme");
        en.put("panel.path.placeholder", "Enter path...");
        en.put("panel.column.name", "Name");
        en.put("panel.column.size", "Size");
        en.put("panel.column.modified", "Date Modified");
        en.put("panel.status.selected", "Selected: {0} | Folders: {1} | Files: {2}");
        en.put("panel.disk.free", "Free: {0} GB");
        en.put("panel.search.results", "Search results: \"{0}\" ({1} items)");

        en.put("type.folder", "Folder");
        en.put("type.textFile", "Text File");
        en.put("type.pdfDocument", "PDF Document");
        en.put("type.image", "Image");
        en.put("type.audio", "Audio");
        en.put("type.video", "Video");
        en.put("type.archive", "Archive");
        en.put("type.program", "Program");
        en.put("type.javaCode", "Java Code");
        en.put("type.dataFile", "Data File");
        en.put("type.file", "File");

        en.put("size.folder", "<DIR>");
        en.put("size.bytes", " B");
        en.put("size.kb", " KB");
        en.put("size.mb", " MB");
        en.put("size.gb", " GB");

        en.put("context.open", "Open");
        en.put("context.copy", "Copy (F3)");
        en.put("context.move", "Move (F6)");
        en.put("context.delete", "Delete (F8)");
        en.put("context.newFolder", "New Folder (F7)");
        en.put("context.rename", "Rename (F2)");
        en.put("context.copyToClipboard", "Copy to Clipboard (Ctrl+C)");
        en.put("context.paste", "Paste (Ctrl+V)");
        en.put("context.gotoLocation", "Go to Location");
        en.put("context.openFolder", "Open Folder");
        en.put("context.openFile", "Open File");
        en.put("context.copyToOther", "Copy to Other Panel");

        en.put("popup.copy.title", "Copy {0} items to:");
        en.put("popup.copy.current", "Current Panel: '{0}' (C)");
        en.put("popup.copy.other", "Other Panel: '{0}' (O)");
        en.put("popup.action.title", "What do you want to do with '{0}'?");
        en.put("popup.action.goto", "Go to Location (G)");
        en.put("popup.action.openFolder", "Open Folder (O)");
        en.put("popup.action.openFile", "Open File (O)");
        en.put("popup.openFolder.title", "Open folder in which panel?");
        en.put("popup.openFolder.current", "Current Panel (C)");
        en.put("popup.openFolder.other", "Other Panel (O)");
        en.put("popup.gotoLocation.title", "Go to location in which panel?");
        en.put("popup.gotoLocation.current", "Current Panel (C)");
        en.put("popup.gotoLocation.other", "Other Panel (O)");
        en.put("popup.cancel", "Cancel");

        en.put("dialog.confirmDelete", "Confirm Deletion");
        en.put("dialog.confirmDeleteMessage", "Are you sure you want to delete {0} items?");
        en.put("dialog.error", "Error");
        en.put("dialog.warning", "Warning");
        en.put("dialog.info", "Information");

        en.put("error.path.title", "Path Error");
        en.put("error.path.notExists", "Path does not exist or is not a folder");
        en.put("error.path.invalid", "Invalid path format");
        en.put("error.searchMode.title", "Search Mode Restriction");
        en.put("error.searchMode.message", "This operation is not available in search results mode.\nOnly copy and view are available.");
        en.put("error.searchModeRestriction", "This operation is not available in search mode.");
        en.put("error.operationUnavailable", "Operation Unavailable");

        en.put("error.accessDenied", "Access denied");
        en.put("error.fileAlreadyExists", "File already exists: {0}");
        en.put("error.fileNotFound", "File not found: {0}");
        en.put("error.notEmpty", "Directory is not empty: {0}");
        en.put("error.io", "I/O Error: {0}");
        en.put("error.unknown", "Unknown error: {0}");

        en.put("error.fileNotExist", "File does not exist: {0}");
        en.put("error.folderSame", "Cannot copy folder into itself: {0}");
        en.put("error.destNotExist", "Destination folder does not exist: {0}");
        en.put("error.destNotFolder", "Destination path is not a folder: {0}");
        en.put("error.noWriteAccess", "No write permission: {0}");
        en.put("error.folderExists", "Folder already exists: {0}");
        en.put("error.parentNotExist", "Parent folder does not exist");
        en.put("error.moveSameFolder", "Cannot move files to the same folder");
        en.put("error.fileExists", "File or folder named '{0}' already exists");
        en.put("error.fileExistsDestination", "File or folder named '{0}' already exists in destination");
        en.put("error.deleteLocked", "Cannot delete locked file: {0}");
        en.put("error.deleteNotEmpty", "Cannot delete non-empty folder: {0}");
        en.put("error.destinationNotExist", "Destination folder does not exist: {0}");
        en.put("error.destinationNotFolder", "Destination path is not a folder: {0}");
        en.put("error.noWritePermission", "No write permission to destination folder: {0}");
        en.put("error.noWriteParent", "No write permission to parent folder");
        en.put("error.cannotMoveSameFolder", "Cannot move files to the same folder");
        en.put("error.cannotDeleteNonEmpty", "Cannot delete non-empty folder: {0}");

        en.put("warning.noSelection", "No files selected");
        en.put("warning.cannotMoveSameFolder", "Cannot move files to the same folder");
        en.put("warning.fileExists", "File or folder with this name already exists");
        en.put("warning.bufferEmpty", "Clipboard is empty");
        en.put("error.accessDeniedSystem", "Access error: Cannot write to system folder {0}. Choose another folder or run as administrator.");
        en.put("error.accessDeniedDetails", "No permission to access: {0}. Check permissions.");

        en.put("operation.copying", "Copying Files");
        en.put("operation.moving", "Moving Files");
        en.put("operation.deleting", "Deleting Files");
        en.put("operation.undoing", "Undoing Operation");
        en.put("operation.preparing", "Preparing...");
        en.put("operation.undoSuccess", "Operation undone successfully");
        en.put("operation.undoFailed", "Failed to undo");
        en.put("operation.createFolderFailed", "Failed to create folder");
        en.put("operation.failed", "Operation failed");
        en.put("operation.success", "Operation completed successfully");
        en.put("operation.renameFailed", "Failed to rename");
        en.put("operation.countingFiles", "Counting files...");
        en.put("operation.deletingFiles", "Deleting files...");
        en.put("operation.cancelled", "Operation cancelled by user");
        en.put("operation.restoring", "Preparing to restore files...");
        en.put("operation.restoringFolder", "Restoring folder:");
        en.put("operation.restoringFile", "Restoring file:");

        en.put("operation.description.copy", "Copy {0} items to {1}");
        en.put("operation.description.copyOne", "Create and copy {0}");
        en.put("operation.description.move", "Move {0} items to {1}");
        en.put("operation.description.delete", "Delete {0} items");
        en.put("operation.description.rename", "Rename {0} to {1}");
        en.put("operation.description.createFolder", "Create folder: {0}");

        en.put("search.title", "Search Files and Folders");
        en.put("search.subtitle", "Quickly find what you need");
        en.put("search.query", "Search Query");
        en.put("search.queryPlaceholder", "*.txt, *.pdf, file name...");
        en.put("search.location", "Search Location");
        en.put("search.browse", "Browse");
        en.put("search.browseTitle", "Select Folder to Search");
        en.put("search.itemTypeLabel", "Item Type");
        en.put("search.itemType.both", "Both");
        en.put("search.itemType.files", "Files Only");
        en.put("search.itemType.folders", "Folders Only");
        en.put("search.showResultsIn", "Show Results In");
        en.put("search.leftPanel", "Left Panel");
        en.put("search.rightPanel", "Right Panel");
        en.put("search.activePanel", "Active Panel");
        en.put("search.includeSubdirs", "Include Subdirectories");
        en.put("search.cancel", "Cancel");
        en.put("search.search", "Search");
        en.put("search.enterCriteria", "Enter search criteria");
        en.put("search.cannotDetermine", "Cannot determine target panel");

        en.put("progress.preparing", "Preparing...");
        en.put("progress.cancel", "Cancel");
        en.put("progress.found", "Found");
        en.put("progress.scanned", "Scanned");

        en.put("history.title", "Operation History");
        en.put("history.loading", "Loading...");
        en.put("history.refresh", "Refresh");
        en.put("history.close", "Close");
        en.put("history.empty", "No operations yet");
        en.put("history.emptySubtext", "Operation history will appear here");
        en.put("history.noOperations", "No operations");
        en.put("history.recentOperations", "Recent operations");
        en.put("history.total", "total");

        en.put("operation.type.Copy", "Copy");
        en.put("operation.type.Move", "Move");
        en.put("operation.type.Delete", "Delete");
        en.put("operation.type.CreateFolder", "Create Folder");
        en.put("operation.type.Rename", "Rename");
        en.put("operation.type.Paste", "Paste");

        en.put("history.status.SUCCESS", "SUCCESS");
        en.put("history.status.FAILED", "FAILED");

        en.put("history.keyword.created", "Created folder");
        en.put("history.keyword.deleted", "Delete");
        en.put("history.keyword.copied", "Copy");
        en.put("history.keyword.moved", "Move");
        en.put("history.keyword.renamed", "Rename");
        en.put("history.keyword.items", "items");
        en.put("history.keyword.to", "to");
        en.put("history.keyword.on", "to");
        en.put("error.validation", "Validation error");
        en.put("error.accessDeniedFolder", "Access error: Cannot write to folder {0}. Choose another folder or check permissions.");
        en.put("error.accessDeniedGeneral", "Access denied. Check file or folder permissions.");
        en.put("error.unknownOperation", "Unknown error during operation");

        translations.put("en", en);
    }

    public void setLanguage(String language) {
        if (translations.containsKey(language)) {
            this.currentLanguage = language;
        }
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public String getString(String key) {
        Map<String, String> currentTranslations = translations.get(currentLanguage);
        if (currentTranslations != null && currentTranslations.containsKey(key)) {
            return currentTranslations.get(key);
        }
        return key;
    }

    public String getString(String key, Object... args) {
        String template = getString(key);
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return template;
    }
}