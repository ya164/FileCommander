package com.filecommander.command;

import com.filecommander.localization.LocalizationManager;
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
        LocalizationManager loc = LocalizationManager.getInstance();

        if (!Files.exists(oldPath)) {
            validationError = loc.getString("error.fileNotExist", oldPath.getFileName());
            return false;
        }

        if (Files.exists(newPath)) {
            validationError = loc.getString("error.fileExistsDestination", newPath.getFileName());
            return false;
        }

        if (oldPath.getParent() != null && !Files.isWritable(oldPath.getParent())) {
            validationError = loc.getString("error.noWriteParent");
            return false;
        }

        return true;
    }

    @Override
    protected void performOperation() throws IOException {
        Files.move(oldPath, newPath);
    }

    @Override
    public void undo() throws IOException {
        if (Files.exists(newPath)) {
            Files.move(newPath, oldPath);
        }
    }

    @Override
    public String getDescription() {
        return LocalizationManager.getInstance().getString("operation.description.rename", oldPath.getFileName(), newPath.getFileName());
    }
}