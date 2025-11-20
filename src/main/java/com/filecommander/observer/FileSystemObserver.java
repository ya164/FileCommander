package com.filecommander.observer;

public interface FileSystemObserver {
    void onFileSystemChanged(FileSystemEvent event);
}