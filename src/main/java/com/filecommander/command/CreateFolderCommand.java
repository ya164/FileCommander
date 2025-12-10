package com.filecommander.command;

import com.filecommander.localization.LocalizationManager;
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
        LocalizationManager loc = LocalizationManager.getInstance();
        if (Files.exists(folderPath)) {
            validationError = loc.getString("error.folderExists", folderPath.getFileName());
            return false;
        }

        Path parent = folderPath.getParent();
        if (parent == null || !Files.exists(parent)) {
            validationError = loc.getString("error.parentNotExist");
            return false;
        }

        if (!Files.isWritable(parent)) {
            validationError = loc.getString("error.noWriteParent");
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
        LocalizationManager loc = LocalizationManager.getInstance();
        if (Files.exists(folderPath) && Files.isDirectory(folderPath)) {
            if (Files.list(folderPath).findAny().isEmpty()) {
                Files.delete(folderPath);
                System.out.println("Undo: Folder deleted: " + folderPath);
            } else {
                System.err.println("Cannot undo: Folder is not empty: " + folderPath);
                throw new IOException(loc.getString("error.cannotDeleteNonEmpty", folderPath.getFileName()));
            }
        } else {
            System.err.println("Cannot undo: Folder does not exist: " + folderPath);
        }
    }

    @Override
    public String getDescription() {
        return LocalizationManager.getInstance().getString("operation.description.createFolder", folderPath.getFileName());
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