package com.filecommander.ui.dialogs;

import com.filecommander.localization.LocalizationManager;

import java.nio.file.Path;

public class SearchParams {
    private String criteria;
    private ItemType itemType;
    private Path rootPath;
    private boolean includeSubdirectories;

    public enum ItemType {
        FILES("search.itemType.files"),
        FOLDERS("search.itemType.folders"),
        BOTH("search.itemType.both");

        private final String localizationKey;

        ItemType(String localizationKey) {
            this.localizationKey = localizationKey;
        }

        @Override
        public String toString() {
            return LocalizationManager.getInstance().getString(localizationKey);
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