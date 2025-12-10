package com.filecommander.command;

import com.filecommander.localization.LocalizationManager;
import com.filecommander.service.FileOperationService;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class DeleteCommand extends AbstractFileOperation {
    private Map<Path, byte[]> deletedFilesBackup = new HashMap<>();
    private List<Path> deletedDirectories = new ArrayList<>();
    private int totalFiles = 0;
    private int processedFiles = 0;

    public DeleteCommand(List<Path> sources) {
        this.sources = sources;
    }

    @Override
    protected boolean validate() {
        LocalizationManager loc = LocalizationManager.getInstance();
        for (Path source : sources) {
            if (!Files.exists(source)) {
                validationError = loc.getString("error.fileNotExist", source.getFileName());
                return false;
            }

            if (source.getParent() != null && !Files.isWritable(source.getParent())) {
                validationError = loc.getString("error.noWriteAccess", source.getFileName());
                return false;
            }
        }

        return true;
    }

    @Override
    protected void prepare() {
        super.prepare();
        FileOperationService service = FileOperationService.getInstance();
        try {
            service.notifyStatus(LocalizationManager.getInstance().getString("operation.countingFiles"));
            totalFiles = countFiles();
        } catch (IOException e) {
            totalFiles = sources.size();
        }
    }

    private int countFiles() throws IOException {
        int count = 0;
        for (Path source : sources) {
            if (Files.isDirectory(source)) {
                count += countFilesInDirectory(source);
            } else {
                count++;
            }
        }
        return count;
    }

    private int countFilesInDirectory(Path directory) throws IOException {
        final int[] count = {0};
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                count[0]++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }

    @Override
    protected void performOperation() throws IOException {
        FileOperationService service = FileOperationService.getInstance();
        LocalizationManager loc = LocalizationManager.getInstance();
        service.notifyStatus(loc.getString("operation.deletingFiles"));

        for (Path source : sources) {
            if (service.isCancelled()) {
                throw new IOException(loc.getString("operation.cancelled"));
            }

            if (Files.isDirectory(source)) {
                backupAndDeleteDirectory(source);
            } else {
                backupAndDeleteFile(source);
            }
        }
    }

    private void backupAndDeleteFile(Path file) throws IOException {
        FileOperationService service = FileOperationService.getInstance();

        try {
            long size = Files.size(file);
            if (size < 100 * 1024 * 1024) {
                deletedFilesBackup.put(file, Files.readAllBytes(file));
            } else {
                System.out.println("Skipping backup for large file: " + file);
            }
        } catch (IOException e) {
            System.err.println("Failed to backup file: " + file);
        }

        forceDelete(file);
        processedFiles++;
        service.notifyProgress(processedFiles, totalFiles, file.getFileName().toString());
    }

    private void backupAndDeleteDirectory(Path dir) throws IOException {
        FileOperationService service = FileOperationService.getInstance();
        deletedDirectories.add(dir);

        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path subDir, BasicFileAttributes attrs) {
                if (!subDir.equals(dir)) {
                    deletedDirectories.add(subDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (service.isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }

                try {
                    long size = Files.size(file);
                    if (size < 100 * 1024 * 1024) {
                        deletedFilesBackup.put(file, Files.readAllBytes(file));
                    }
                } catch (IOException e) {
                    System.err.println("Failed to backup file: " + file);
                }

                forceDelete(file);
                processedFiles++;
                service.notifyProgress(processedFiles, totalFiles, file.getFileName().toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path subDir, IOException exc)
                    throws IOException {
                if (exc == null) {
                    forceDelete(subDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Failed to delete: " + file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void forceDelete(Path path) throws IOException {
        try {
            Files.delete(path);
        } catch (AccessDeniedException e) {
            try {
                Files.setAttribute(path, "dos:readonly", false);
                Files.delete(path);
            } catch (Exception ex) {
                try {
                    path.toFile().setWritable(true);
                    if (!path.toFile().delete()) {
                        throw e;
                    }
                } catch (Exception ex2) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void undo() throws IOException {
        System.out.println("Undoing delete operation...");
        FileOperationService service = FileOperationService.getInstance();
        LocalizationManager loc = LocalizationManager.getInstance();

        service.notifyStatus(loc.getString("operation.restoring"));

        int totalToRestore = deletedDirectories.size() + deletedFilesBackup.size();
        int restoredCount = 0;

        Collections.reverse(deletedDirectories);
        for (Path dir : deletedDirectories) {
            if (service.isCancelled()) break;

            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            restoredCount++;
            service.notifyProgress(restoredCount, totalToRestore, loc.getString("operation.restoringFolder") + " " + dir.getFileName());
        }

        for (Map.Entry<Path, byte[]> entry : deletedFilesBackup.entrySet()) {
            if (service.isCancelled()) break;

            Path path = entry.getKey();
            byte[] content = entry.getValue();

            restoredCount++;
            service.notifyProgress(restoredCount, totalToRestore, loc.getString("operation.restoringFile") + " " + path.getFileName());

            if (!Files.exists(path)) {
                Path parent = path.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.write(path, content);
            }
        }
    }

    @Override
    public String getDescription() {
        return LocalizationManager.getInstance().getString("operation.description.delete", sources.size());
    }
}