package com.filecommander.ui.dialogs;

import java.nio.file.Path;

public class SearchParams {
    private String criteria;
    private ItemType itemType;
    private Path rootPath;
    private boolean includeSubdirectories;

    public enum ItemType {
        FILES("Files only"),
        FOLDERS("Folders only"),
        BOTH("Files and Folders");

        private final String displayName;

        ItemType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public SearchParams() {
    }

    public SearchParams(String criteria, ItemType itemType,
                        Path rootPath, boolean includeSubdirectories) {
        this.criteria = criteria;
        this.itemType = itemType;
        this.rootPath = rootPath;
        this.includeSubdirectories = includeSubdirectories;
    }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public Path getRootPath() { return rootPath; }
    public void setRootPath(Path rootPath) { this.rootPath = rootPath; }

    public boolean isIncludeSubdirectories() { return includeSubdirectories; }
    public void setIncludeSubdirectories(boolean includeSubdirectories) {
        this.includeSubdirectories = includeSubdirectories;
    }
}