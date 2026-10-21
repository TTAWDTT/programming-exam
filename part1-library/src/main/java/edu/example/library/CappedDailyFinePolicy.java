package edu.example.library;

public final class CappedDailyFinePolicy implements FinePolicy {
    @Override
    public int calculateFine(long overdueDays) {
        if (overdueDays < 0) {
            throw new IllegalArgumentException("Overdue days must not be negative");
        }
        return overdueDays >= 20 ? 1000 : (int) overdueDays * 50;
    }
}
