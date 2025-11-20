package com.filecommander.factory;

import com.filecommander.command.*;
import java.nio.file.Path;
import java.util.List;

public class OperationFactory {

    public FileCommand createCopyCommand(List<Path> sources, Path destination, boolean addCopySuffix) {
        return new CopyCommand(sources, destination, addCopySuffix);
    }

    public FileCommand createMoveCommand(List<Path> sources, Path destination) {
        return new MoveCommand(sources, destination);
    }

    public FileCommand createDeleteCommand(List<Path> sources) {
        return new DeleteCommand(sources);
    }

    public FileCommand createFolderCommand(Path folderPath) {
        return new CreateFolderCommand(folderPath);
    }

    public FileCommand createRenameCommand(Path oldPath, Path newPath) {
        return new RenameCommand(oldPath, newPath);
    }
}

