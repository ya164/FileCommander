package com.filecommander.observer;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class FileSystemEvent {
    private List<Path> affectedPaths;
    private Type type;
    private LocalDateTime timestamp;

    public enum Type {
        CREATED,
        MODIFIED,
        DELETED,
        SETTINGS_CHANGED
    }

    public FileSystemEvent(List<Path> affectedPaths, Type type) {
        this.affectedPaths = affectedPaths;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    public boolean affectsPath(Path path) {
        if (type == Type.SETTINGS_CHANGED) {
            return true;
        }

        for (Path affectedPath : affectedPaths) {
            if (affectedPath.startsWith(path) || path.startsWith(affectedPath)) {
                return true;
            }
        }
        return false;
    }

    public List<Path> getAffectedPaths() { return affectedPaths; }
    public Type getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
}