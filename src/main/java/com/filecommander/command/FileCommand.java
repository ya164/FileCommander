package com.filecommander.command;

import com.filecommander.model.OperationResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface FileCommand {
    OperationResult execute() throws IOException;
    void undo() throws IOException;
    String getDescription();
    List<Path> getAffectedPaths();
}