package edu.example.library;

import java.time.LocalDate;

/**
 * Library borrowing: the entry point of your design.
 *
 * <p>Keep the public constructor and the five public methods below with these names, parameters
 * and return types; the only change allowed is adding a {@code throws} clause for
 * {@code LibraryException}. You may add fields, other constructors (for example one that takes a
 * {@code FinePolicy}), methods and other classes.
 *
 * <p>Exception conventions:
 * <ul>
 *   <li>Invalid input (an unregistered member or book, a null argument, and so on):
 *       throw {@link IllegalArgumentException};</li>
 *   <li>Valid input that breaks a borrowing rule (for example the limit is reached, or no copy
 *       is available): throw a subclass of your own {@code LibraryException}.</li>
 * </ul>
 */
public class Library {

    /** Creates a library that uses your standard fine rule. */
    public Library() {
    }

    /** Registers a member, e.g. {@code addMember(new Undergraduate("S1"))}. */
    public void addMember(Member member) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Registers a book and how many copies the library holds. {@code copies} is at least 1. */
    public void addBook(String isbn, int copies) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * The member borrows one copy of {@code isbn} on {@code date}.
     * The borrow day is day 0; the last day of the member's loan period is the due date.
     */
    public void borrow(String memberId, String isbn, LocalDate date) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * The member returns {@code isbn} on {@code date}. Returns the fine for this book in cents
     * (1 yuan = 100 cents). Returns 0 when there is no fine.
     */
    public int returnBook(String memberId, String isbn, LocalDate date) {
        throw new UnsupportedOperationException("TODO");
    }

    /** How many books the member currently holds (borrowed and not yet returned). */
    public int borrowedCount(String memberId) {
        throw new UnsupportedOperationException("TODO");
    }
}
