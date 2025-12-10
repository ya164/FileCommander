package com.filecommander.model;

import com.filecommander.localization.LocalizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class FileItem implements Comparable<FileItem> {
    private Path path;
    private String name;
    private long size;
    private LocalDateTime lastModified;
    private boolean isDirectory;
    private boolean isHidden;

    public static FileItem from(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            boolean hidden = false;
            try {
                hidden = Files.isHidden(path);
            } catch (IOException ignored) {
            }

            return new FileItem(
                    path,
                    path.getFileName() != null ? path.getFileName().toString() : path.toString(),
                    attrs.size(),
                    LocalDateTime.ofInstant(
                            attrs.lastModifiedTime().toInstant(),
                            ZoneId.systemDefault()
                    ),
                    attrs.isDirectory(),
                    hidden
            );
        } catch (IOException e) {
            return null;
        }
    }

    public FileItem(Path path, String name, long size,
                    LocalDateTime lastModified, boolean isDirectory, boolean isHidden) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
        this.isHidden = isHidden;
    }

    public String getType() {
        LocalizationManager loc = LocalizationManager.getInstance();

        if (isDirectory) {
            return loc.getString("type.folder");
        }

        String fileName = name.toLowerCase();
        if (fileName.endsWith(".txt")) return loc.getString("type.textFile");
        if (fileName.endsWith(".pdf")) return loc.getString("type.pdfDocument");
        if (fileName.endsWith(".jpg") || fileName.endsWith(".png") ||
                fileName.endsWith(".gif") || fileName.endsWith(".bmp")) return loc.getString("type.image");
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") ||
                fileName.endsWith(".flac")) return loc.getString("type.audio");
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") ||
                fileName.endsWith(".mkv")) return loc.getString("type.video");
        if (fileName.endsWith(".zip") || fileName.endsWith(".rar") ||
                fileName.endsWith(".7z") || fileName.endsWith(".tar")) return loc.getString("type.archive");
        if (fileName.endsWith(".exe") || fileName.endsWith(".msi")) return loc.getString("type.program");
        if (fileName.endsWith(".java")) return loc.getString("type.javaCode");
        if (fileName.endsWith(".xml") || fileName.endsWith(".json")) return loc.getString("type.dataFile");

        return loc.getString("type.file");
    }

    public String getIcon() {
        if (isDirectory) return "\uD83D\uDCC1";

        String fileName = name.toLowerCase();
        if (fileName.endsWith(".txt")) return "\uD83D\uDCC4";
        if (fileName.endsWith(".pdf")) return "\uD83D\uDCD5";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) return "\uD83D\uDDBC";
        if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) return "\uD83C\uDFB5";
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi")) return "\uD83C\uDFAC";
        if (fileName.endsWith(".zip") || fileName.endsWith(".rar")) return "\uD83D\uDCE6";
        if (fileName.endsWith(".exe")) return "\u2699\uFE0F";
        if (fileName.endsWith(".java")) return "\u2615";

        return "\uD83D\uDCC4";
    }

    public String getFormattedSize() {
        LocalizationManager loc = LocalizationManager.getInstance();

        if (isDirectory) return loc.getString("size.folder");

        if (size < 1024) return size + " " + loc.getString("size.bytes");

        if (size < 1024 * 1024) {
            return String.format("%.1f %s", size / 1024.0, loc.getString("size.kb"));
        }

        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f %s", size / (1024.0 * 1024), loc.getString("size.mb"));
        }

        return String.format("%.1f %s", size / (1024.0 * 1024 * 1024), loc.getString("size.gb"));
    }

    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return lastModified.format(formatter);
    }

    @Override
    public int compareTo(FileItem other) {
        if (this.isDirectory && !other.isDirectory) return -1;
        if (!this.isDirectory && other.isDirectory) return 1;
        return this.name.compareToIgnoreCase(other.name);
    }

    public Path getPath() { return path; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public LocalDateTime getLastModified() { return lastModified; }
    public boolean isDirectory() { return isDirectory; }
    public boolean isHidden() { return isHidden; }
}