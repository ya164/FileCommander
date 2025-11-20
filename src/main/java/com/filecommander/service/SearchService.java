package com.filecommander.service;

import com.filecommander.model.FileItem;
import com.filecommander.strategy.*;

import java.nio.file.Path;
import java.util.List;

public class SearchService {
    private static SearchService instance;
    private SearchStrategy currentStrategy;
    private volatile boolean cancelled = false;

    public interface SearchProgressCallback {
        void onProgress(int filesScanned, int filesFound, String currentPath);
    }

    private SearchService() {
        this.currentStrategy = new NameSearchStrategy();
    }

    public static synchronized SearchService getInstance() {
        if (instance == null) {
            instance = new SearchService();
        }
        return instance;
    }

    public void cancelCurrentSearch() {
        this.cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public List<FileItem> search(Path rootPath, String criteria) {
        cancelled = false;
        return currentStrategy.search(rootPath, criteria);
    }

    public List<FileItem> search(Path rootPath, String criteria, SearchProgressCallback callback) {
        cancelled = false;
        if (currentStrategy instanceof NameSearchStrategy) {
            return ((NameSearchStrategy) currentStrategy).search(rootPath, criteria, callback);
        }
        return currentStrategy.search(rootPath, criteria);
    }
}