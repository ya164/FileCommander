package com.filecommander.strategy;

import com.filecommander.model.FileItem;
import com.filecommander.service.SearchService;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class NameSearchStrategy implements SearchStrategy {

    @Override
    public List<FileItem> search(Path rootPath, String criteria) {
        return search(rootPath, criteria, null);
    }

    public List<FileItem> search(Path rootPath, String criteria, SearchService.SearchProgressCallback callback) {
        List<FileItem> results = new ArrayList<>();
        String searchTerm = criteria.trim();
        final int[] filesScanned = {0};

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (SearchService.getInstance().isCancelled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    filesScanned[0]++;
                    if (callback != null && filesScanned[0] % 50 == 0) {
                        callback.onProgress(filesScanned[0], results.size(), file.toString());
                    }

                    if (file.getFileName() != null) {
                        String fileName = file.getFileName().toString();
                        if (containsIgnoreCase(fileName, searchTerm)) {
                            FileItem item = FileItem.from(file);
                            if (item != null) {
                                results.add(item);
                                if (callback != null) {
                                    callback.onProgress(filesScanned[0], results.size(), file.toString());
                                }
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (SearchService.getInstance().isCancelled()) {
                        return FileVisitResult.TERMINATE;
                    }

                    filesScanned[0]++;
                    if (callback != null && filesScanned[0] % 50 == 0) {
                        callback.onProgress(filesScanned[0], results.size(), dir.toString());
                    }

                    if (dir.getFileName() != null) {
                        String dirName = dir.getFileName().toString();
                        if (containsIgnoreCase(dirName, searchTerm)) {
                            FileItem item = FileItem.from(dir);
                            if (item != null) {
                                results.add(item);
                                if (callback != null) {
                                    callback.onProgress(filesScanned[0], results.size(), dir.toString());
                                }
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println("Помилка пошуку: " + e.getMessage());
        }

        return results;
    }

    private boolean containsIgnoreCase(String text, String search) {
        if (text == null || search == null) return false;
        return text.toLowerCase(java.util.Locale.getDefault())
                .contains(search.toLowerCase(java.util.Locale.getDefault()));
    }
}