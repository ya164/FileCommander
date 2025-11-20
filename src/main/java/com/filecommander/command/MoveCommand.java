package com.filecommander.command;

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
        for (Path source : sources) {
            if (!Files.exists(source)) {
                validationError = "Source file does not exist: " + source.getFileName();
                return false;
            }

            Path sourceParent = source.getParent();
            if (sourceParent != null && sourceParent.equals(destination)) {
                validationError = "Cannot move files to the same location";
                return false;
            }

            Path target = destination.resolve(source.getFileName());
            if (Files.exists(target)) {
                validationError = "A file or folder with name '" + source.getFileName() + "' already exists in destination";
                return false;
            }
        }

        if (!Files.exists(destination)) {
            validationError = "Destination folder does not exist: " + destination;
            return false;
        }

        if (!Files.isDirectory(destination)) {
            validationError = "Destination is not a folder: " + destination.getFileName();
            return false;
        }

        if (!Files.isWritable(destination)) {
            validationError = "No write permission for destination folder: " + destination.getFileName();
            return false;
        }

        return true;
    }

    @Override
    protected void performOperation() throws IOException {
        FileOperationService service = FileOperationService.getInstance();
        service.notifyStatus("Moving files...");

        int total = sources.size();
        int current = 0;

        for (Path source : sources) {
            if (service.isCancelled()) {
                throw new IOException("Operation cancelled by user");
            }

            Path target = destination.resolve(source.getFileName());

            if (source.equals(target)) {
                System.out.println("⚠️ Skipping - source and target are the same: " + source);
                continue;
            }

            movedFiles.put(target, source);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            current++;
            service.notifyProgress(current, total, source.getFileName().toString());
            System.out.println("✅ Moved: " + source + " -> " + target);
        }
    }

    @Override
    public void undo() throws IOException {
        for (Map.Entry<Path, Path> entry : movedFiles.entrySet()) {
            if (Files.exists(entry.getKey())) {
                Files.move(entry.getKey(), entry.getValue(), StandardCopyOption.ATOMIC_MOVE);
                System.out.println("Undo move: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
    }

    @Override
    public String getDescription() {
        return "Move " + sources.size() + " file(s) to " + destination;
    }
}