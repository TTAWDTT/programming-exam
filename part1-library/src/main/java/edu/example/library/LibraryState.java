package edu.example.library;

import java.util.HashMap;
import java.util.Map;

final class LibraryState {
    final Map<String, Member> members;
    final Map<String, Integer> books;
    final Map<LoanKey, Loan> loans;

    LibraryState() {
        members = new HashMap<>();
        books = new HashMap<>();
        loans = new HashMap<>();
    }

    LibraryState(LibraryState source) {
        members = new HashMap<>(source.members);
        books = new HashMap<>(source.books);
        loans = new HashMap<>(source.loans);
    }

    int borrowedCount(String memberId) {
        return (int) loans.values().stream()
                .filter(loan -> loan.memberId().equals(memberId)).count();
    }

    int availableCopies(String isbn) {
        long borrowed = loans.values().stream()
                .filter(loan -> loan.isbn().equals(isbn)).count();
        return books.get(isbn) - (int) borrowed;
    }
}
