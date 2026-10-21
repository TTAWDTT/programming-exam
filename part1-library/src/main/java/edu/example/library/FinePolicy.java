package edu.example.library;

@FunctionalInterface
public interface FinePolicy {
    /** Returns nonnegative cents for a nonnegative overdue-day count. */
    int calculateFine(long overdueDays);
}
