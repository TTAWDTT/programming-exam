package edu.example.library;

public final class DuplicateLoanException extends LibraryException {
    public DuplicateLoanException(String message) {
        super(message);
    }
}
