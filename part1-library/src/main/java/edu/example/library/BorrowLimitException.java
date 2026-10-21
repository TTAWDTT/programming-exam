package edu.example.library;

public final class BorrowLimitException extends LibraryException {
    public BorrowLimitException(String message) {
        super(message);
    }
}
