package com.filecommander.command;

import com.filecommander.localization.LocalizationManager;
import com.filecommander.service.FileOperationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveCommand extends AbstractFileOperation {
    private Map<Path, Path> movedFiles = new HashMap<>();

    public MoveCommand(List<Path> sources, Path destination) {
        this.sources = sources;
        this.destination = destination;
    }

    @Override
    protected boolean validate() {
        LocalizationManager loc = LocalizationManager.getInstance();
        for (Path source : sources) {
            if (!Files.exists(source)) {
                validationError = loc.getString("error.fileNotExist", source.getFileName());
                return false;
            }

            Path sourceParent = source.getParent();
            if (sourceParent != null && sourceParent.equals(destination)) {
                validationError = loc.getString("error.cannotMoveSameFolder");
                return false;
            }

            Path target = destination.resolve(source.getFileName());
            if (Files.exists(target)) {
                validationError = loc.getString("error.fileExistsDestination", source.getFileName());
                return false;
            }
        }

        if (!Files.exists(destination)) {
            validationError = loc.getString("error.destinationNotExist", destination);
            return false;
        }

        if (!Files.isDirectory(destination)) {
            validationError = loc.getString("error.destinationNotFolder", destination.getFileName());
            return false;
        }

        if (!Files.isWritable(destination)) {
            validationError = loc.getString("error.noWritePermission", destination.getFileName());
            return false;
        }

        return true;
    }

    @Override
    protected void performOperation() throws IOException {
        FileOperationService service = FileOperationService.getInstance();
        LocalizationManager loc = LocalizationManager.getInstance();
        service.notifyStatus(loc.getString("operation.moving"));

        int total = sources.size();
        int current = 0;

        for (Path source : sources) {
            if (service.isCancelled()) {
                throw new IOException(loc.getString("operation.cancelled"));
            }

            Path target = destination.resolve(source.getFileName());

            if (source.equals(target)) {
                continue;
            }

            movedFiles.put(target, source);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            current++;
            service.notifyProgress(current, total, source.getFileName().toString());
        }
    }

    @Override
    public void undo() throws IOException {
        for (Map.Entry<Path, Path> entry : movedFiles.entrySet()) {
            if (Files.exists(entry.getKey())) {
                Files.move(entry.getKey(), entry.getValue(), StandardCopyOption.ATOMIC_MOVE);
            }
        }
    }

    @Override
    public String getDescription() {
        return LocalizationManager.getInstance().getString("operation.description.move", sources.size(), destination);
    }
}