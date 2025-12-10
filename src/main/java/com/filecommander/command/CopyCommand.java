package com.filecommander.command;

import com.filecommander.localization.LocalizationManager;
import com.filecommander.service.FileOperationService;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CopyCommand extends AbstractFileOperation {
    private final Map<Path, byte[]> backupData = new HashMap<>();
    private final Map<Path, Path> copiedFiles = new HashMap<>();
    private final boolean addCopySuffix;
    private int totalFiles = 0;
    private int processedFiles = 0;

    public CopyCommand(List<Path> sources, Path destination, boolean addCopySuffix) {
        this.sources = sources;
        this.destination = destination;
        this.addCopySuffix = addCopySuffix;
    }

    @Override
    protected boolean validate() {
        LocalizationManager loc = LocalizationManager.getInstance();
        for (Path source : sources) {
            if (!Files.exists(source)) {
                validationError = loc.getString("error.fileNotExist", source.getFileName());
                return false;
            }
            if (Files.isDirectory(source)) {
                try {
                    if (destination.startsWith(source)) {
                        validationError = loc.getString("error.folderSame", source.getFileName());
                        return false;
                    }
                } catch (Exception e) {
                }
            }
        }
        if (!Files.exists(destination)) {
            validationError = loc.getString("error.destNotExist", destination);
            return false;
        }
        if (!Files.isDirectory(destination)) {
            validationError = loc.getString("error.destNotFolder", destination.getFileName());
            return false;
        }
        if (!Files.isWritable(destination)) {
            validationError = loc.getString("error.noWriteAccess", destination.getFileName());
            return false;
        }
        return true;
    }

    @Override
    protected void prepare() {
        super.prepare();
        try {
            FileOperationService.getInstance().notifyStatus(LocalizationManager.getInstance().getString("operation.countingFiles"));
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
        });
        return count[0];
    }

    @Override
    protected void performOperation() throws IOException {
        FileOperationService service = FileOperationService.getInstance();
        service.notifyStatus(LocalizationManager.getInstance().getString("operation.copying"));

        for (Path source : sources) {
            if (service.isCancelled()) {
                throw new IOException(LocalizationManager.getInstance().getString("operation.cancelled"));
            }

            Path targetPath = getUniqueTargetPathWithCopySuffix(source);
            copiedFiles.put(source, targetPath);

            if (Files.isDirectory(source)) {
                copyDirectory(source, targetPath);
            } else {
                copyFile(source, targetPath);
                processedFiles++;
                service.notifyProgress(processedFiles, totalFiles, source.getFileName().toString());
            }
        }
    }

    private Path getUniqueTargetPathWithCopySuffix(Path source) {
        String fileName = source.getFileName().toString();
        String baseName;
        String extension = "";

        if (!Files.isDirectory(source)) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                baseName = fileName.substring(0, dotIndex);
                extension = fileName.substring(dotIndex);
            } else {
                baseName = fileName;
            }
        } else {
            baseName = fileName;
        }

        String suffix = " - Copy";
        if (LocalizationManager.getInstance().getCurrentLanguage().equals("uk")) {
            suffix = " - Копія";
        }

        Path targetPath = destination.resolve(baseName + suffix + extension);
        if (!Files.exists(targetPath)) {
            return targetPath;
        }

        int counter = 2;
        while (counter < 10000) {
            targetPath = destination.resolve(baseName + suffix + " (" + counter + ")" + extension);
            if (!Files.exists(targetPath)) {
                return targetPath;
            }
            counter++;
        }

        long timestamp = System.currentTimeMillis();
        return destination.resolve(baseName + suffix + " (" + timestamp + ")" + extension);
    }

    private void copyFile(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            backupData.put(target, Files.readAllBytes(target));
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        FileOperationService service = FileOperationService.getInstance();

        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (service.isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (service.isCancelled()) {
                    return FileVisitResult.TERMINATE;
                }
                Path targetFile = target.resolve(source.relativize(file));
                copyFile(file, targetFile);
                processedFiles++;
                service.notifyProgress(processedFiles, totalFiles, file.getFileName().toString());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                System.err.println("Failed to copy: " + file + " - " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public String getDescription() {
        LocalizationManager loc = LocalizationManager.getInstance();
        if (addCopySuffix && sources.size() == 1) {
            return loc.getString("operation.description.copyOne", sources.get(0).getFileName());
        }
        return loc.getString("operation.description.copy", sources.size(), destination);
    }

    @Override
    public void undo() throws IOException {
        System.out.println("Undoing copy operation...");
        for (Path targetPath : copiedFiles.values()) {
            if (Files.exists(targetPath)) {
                if (Files.isDirectory(targetPath)) {
                    deleteDirectory(targetPath);
                } else {
                    forceDelete(targetPath);
                }
            }
        }
        for (Map.Entry<Path, byte[]> entry : backupData.entrySet()) {
            Files.write(entry.getKey(), entry.getValue());
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                forceDelete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                forceDelete(dir);
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
                        throw new IOException(LocalizationManager.getInstance().getString("error.deleteLocked", path), e);
                    }
                } catch (Exception ex2) {
                    throw e;
                }
            }
        }
    }
}