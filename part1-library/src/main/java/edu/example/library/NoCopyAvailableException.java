package edu.example.library;

public final class NoCopyAvailableException extends LibraryException {
    public NoCopyAvailableException(String message) {
        super(message);
    }
}
