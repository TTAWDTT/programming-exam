package edu.example.library;

import java.nio.file.Path;

public final class LibraryStorageException extends RuntimeException {
    LibraryStorageException(String operation, Path path, Throwable cause) {
        super("Cannot " + operation + " library at " + path + ": " + cause.getMessage(), cause);
    }
}
