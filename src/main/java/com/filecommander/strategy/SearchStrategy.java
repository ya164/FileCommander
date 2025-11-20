package com.filecommander.strategy;

import com.filecommander.model.FileItem;
import java.nio.file.Path;
import java.util.List;

public interface SearchStrategy {
    List<FileItem> search(Path rootPath, String criteria);
}