package com.filecommander.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class RenameCommand extends AbstractFileOperation {
    private Path oldPath;
    private Path newPath;

    public RenameCommand(Path oldPath, Path newPath) {
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.sources = Collections.singletonList(oldPath);
        this.destination = newPath.getParent();
    }

    @Override
    protected boolean validate() {

        if (!Files.exists(oldPath)) {
            validationError = "Source file does not exist: " + oldPath.getFileName();
            return false;
        }

        if (Files.exists(newPath)) {
            validationError = "A file or folder with name '" + newPath.getFileName() + "' already exists";
            return false;
        }

        if (oldPath.getParent() != null && !Files.isWritable(oldPath.getParent())) {
            validationError = "No write permission in parent directory";
            return false;
        }

        return true;
    }

    @Override
    protected void performOperation() throws IOException {
        Files.move(oldPath, newPath);
        System.out.println("Renamed: " + oldPath.getFileName() + " -> " + newPath.getFileName());
    }

    @Override
    public void undo() throws IOException {
        if (Files.exists(newPath)) {
            Files.move(newPath, oldPath);
            System.out.println("Undo rename: " + newPath.getFileName() + " -> " + oldPath.getFileName());
        }
    }

    @Override
    public String getDescription() {
        return "Rename " + oldPath.getFileName() + " to " + newPath.getFileName();
    }
}