package com.filecommander.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateFolderCommand extends AbstractFileOperation {
    private Path folderPath;

    public CreateFolderCommand(Path folderPath) {
        this.folderPath = folderPath;
        this.sources = new ArrayList<>();
    }

    @Override
    protected boolean validate() {
        if (Files.exists(folderPath)) {
            validationError = "Folder already exists: " + folderPath.getFileName();
            return false;
        }

        Path parent = folderPath.getParent();
        if (parent == null || !Files.exists(parent)) {
            validationError = "Parent folder does not exist";
            return false;
        }

        if (!Files.isWritable(parent)) {
            validationError = "No write permission for parent folder";
            return false;
        }

        return true;
    }

    @Override
    protected void performOperation() throws IOException {
        Files.createDirectory(folderPath);
        System.out.println("Folder created: " + folderPath);
    }

    @Override
    public void undo() throws IOException {
        if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
            if (Files.list(folderPath).findAny().isEmpty()) {
                Files.delete(folderPath);
                System.out.println("Undo: Folder deleted: " + folderPath);
            } else {
                System.err.println("Cannot undo: Folder is not empty: " + folderPath);
                throw new IOException("Cannot delete non-empty folder: " + folderPath.getFileName());
            }
        } else {
            System.err.println("Cannot undo: Folder does not exist: " + folderPath);
        }
    }

    @Override
    public String getDescription() {
        return "Create folder: " + folderPath.getFileName();
    }

    @Override
    public List<Path> getAffectedPaths() {
        return Collections.singletonList(folderPath);
    }

    public void updatePath(Path newPath) {
        this.folderPath = newPath;
        System.out.println("CreateFolderCommand path updated to: " + newPath);
    }
}