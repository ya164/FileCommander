# 📁 File Commander

**File Commander - a dual-panel file manager for Windows featuring asynchronous file operations, full search support, undo functionality, themes, drag & drop, and a Windows 11–inspired UI design.**

---

## 🚀 Features

* Dual-panel layout (Total Commander style)
* Copy, Move, Rename, Delete operations
* Async execution with progress dialogs
* Drag & Drop between panels
* Inline rename (F2)
* Undo support
* Advanced search with separate results mode
* Hidden files toggle
* Light & Dark themes
* Keyboard-driven workflow (F2, F3, F6, F7, Ctrl+Z, Ctrl+F, Tab, etc.)
* Navigation history (Back/Forward)
* Clipboard operations (Ctrl+C / Ctrl+V)

---

## 🔧 File Operations

### Copy

* Recursive directory copying
* Conflict resolution
* Backups for undo support
* Progress tracking

### Move

* Safe validation
* Atomic operations where possible
* Full undo support

### Delete

* Recursive deletion
* Backups for files under 100MB
* Handling protected/readonly files

### Rename

* Inline rename inside the panel
* Validation for duplicate names

### Create Folder

* Undo support

---

## 🔎 Search System

* Asynchronous recursive search
* Real-time progress updates
* “Go to location” action
* Filters: files / folders / both
* Search results open in a special mode

---

## 🎨 Themes

### Light Theme

* Cyan-based palette
* Transparent panels
* Clean Windows-style aesthetic

### Dark Theme

* High-contrast dark UI
* Turquoise accents
* Better for low-light usage

Theme preference is saved in the user’s profile directory.

---

## 💾 Database (SQLite)

Used for storing:

* Operation history (last 50 operations)
* Selected theme

Location:

```
%USERPROFILE%\FileCommander\file_commander.db
```

---

## ⌨ Keyboard Shortcuts

| Action        | Shortcut    |
| ------------- | ----------- |
| Rename        | F2          |
| Copy          | F3 / Ctrl+C |
| Move          | F6          |
| New Folder    | F7          |
| Delete        | F8 / Delete |
| Undo          | Ctrl+Z      |
| Search        | Ctrl+F      |
| Show Hidden   | Ctrl+H      |
| Toggle Theme  | Ctrl+T      |
| Switch Panels | Tab         |

Navigation:

| Action           | Shortcut    |
| ---------------- | ----------- |
| Open             | Enter       |
| Up Directory     | Backspace   |
| Move Left/Right  | ← / →       |
| Multi-select     | Shift + ↑/↓ |
| Toggle selection | Ctrl+Space  |

---

## 🛠 Installation (Windows MSI)

1. Download `FileCommander-1.2.msi`
2. Run the installer
3. Select install directory (`C:\Program Files\FileCommander\`)
4. Launch from Start Menu or Desktop

On first launch:

* Creates `%USERPROFILE%\FileCommander\`
* Initializes the SQLite database
* Opens both panels at `C:\`

---

## 🗑 Uninstalling

Windows Settings → Apps → FileCommander → Uninstall
