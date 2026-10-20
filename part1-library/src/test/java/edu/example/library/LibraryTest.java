package edu.example.library;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Replace this with tests that follow your spec.md. A typical test looks like:
 *
 * <pre>
 * Library library = new Library();
 * library.addMember(new Undergraduate("S1"));
 * library.addBook("B1", 2);
 * library.borrow("S1", "B1", LocalDate.of(2026, 3, 1));
 * assertEquals(1, library.borrowedCount("S1"));
 * </pre>
 */
class LibraryTest {

    @Test
    void libraryCanBeCreated() {
        assertNotNull(new Library());
    }
}
