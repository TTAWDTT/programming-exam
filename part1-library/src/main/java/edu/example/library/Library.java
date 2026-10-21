package edu.example.library;

import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Library {
    private LibraryState state;
    private final FinePolicy finePolicy;
    private final SnapshotStore store;

    public Library() {
        this(new CappedDailyFinePolicy());
    }

    public Library(FinePolicy finePolicy) {
        this(new LibraryState(), finePolicy, null);
    }

    Library(LibraryState state, FinePolicy finePolicy, SnapshotStore store) {
        requireArgument(finePolicy, "Fine policy");
        this.state = new LibraryState(state);
        this.finePolicy = finePolicy;
        this.store = store;
    }

    public static Library create(Path path) {
        return create(path, new CappedDailyFinePolicy());
    }

    public static Library create(Path path, FinePolicy finePolicy) {
        requireArgument(finePolicy, "Fine policy");
        SnapshotStore store = new FileSnapshotStore(path);
        LibraryState state = new LibraryState();
        Library library = new Library(state, finePolicy, store);
        store.create(state);
        return library;
    }

    public static Library load(Path path) {
        return load(path, new CappedDailyFinePolicy());
    }

    public static Library load(Path path, FinePolicy finePolicy) {
        requireArgument(finePolicy, "Fine policy");
        SnapshotStore store = new FileSnapshotStore(path);
        return new Library(store.load(), finePolicy, store);
    }

    public void addMember(Member member) {
        requireArgument(member, "Member");
        if (state.members.containsKey(member.getId())) {
            throw new IllegalArgumentException("Member already registered: " + member.getId());
        }
        LibraryState candidate = new LibraryState(state);
        candidate.members.put(member.getId(), member);
        commit(candidate);
    }

    public void updateMember(Member member) {
        requireArgument(member, "Member");
        Member existing = findMember(member.getId());
        if (existing.getClass() == member.getClass()) {
            return;
        }
        if (state.borrowedCount(member.getId()) > member.borrowingLimit()) {
            throw new BorrowLimitException("Existing loans exceed the new borrowing limit for " + member.getId());
        }
        LibraryState candidate = new LibraryState(state);
        candidate.members.put(member.getId(), member);
        commit(candidate);
    }

    public void addBook(String isbn, int copies) {
        String bookId = Identifiers.normalize(isbn);
        if (copies < 1) {
            throw new IllegalArgumentException("Copies must be positive");
        }
        long total = (long) state.books.getOrDefault(bookId, 0) + copies;
        if (total > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Total copies exceed " + Integer.MAX_VALUE);
        }
        LibraryState candidate = new LibraryState(state);
        candidate.books.put(bookId, (int) total);
        commit(candidate);
    }

    public void borrow(String memberId, String isbn, LocalDate date) {
        String id = Identifiers.normalize(memberId);
        String bookId = Identifiers.normalize(isbn);
        requireArgument(date, "Borrow date");
        Member member = findMember(id);
        requireBook(bookId);
        LocalDate dueDate;
        try {
            dueDate = date.plusDays(member.loanPeriodDays());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Due date is outside the supported date range", e);
        }
        LoanKey key = new LoanKey(id, bookId);
        if (state.loans.containsKey(key)) {
            throw new DuplicateLoanException("Member already holds this ISBN: " + bookId);
        }
        if (state.borrowedCount(id) >= member.borrowingLimit()) {
            throw new BorrowLimitException("Borrowing limit reached for " + id);
        }
        if (state.availableCopies(bookId) == 0) {
            throw new NoCopyAvailableException("No copy available for " + bookId);
        }
        LibraryState candidate = new LibraryState(state);
        candidate.loans.put(key, new Loan(id, bookId, date, dueDate, member.loanPeriodDays()));
        commit(candidate);
    }

    public int returnBook(String memberId, String isbn, LocalDate date) {
        String id = Identifiers.normalize(memberId);
        String bookId = Identifiers.normalize(isbn);
        requireArgument(date, "Return date");
        findMember(id);
        requireBook(bookId);
        LoanKey key = new LoanKey(id, bookId);
        Loan loan = state.loans.get(key);
        if (loan == null) {
            throw new LoanNotFoundException("No active loan for " + id + " and " + bookId);
        }
        if (date.isBefore(loan.borrowedOn())) {
            throw new IllegalArgumentException("Return date precedes borrow date");
        }
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(loan.dueOn(), date));
        int fine = finePolicy.calculateFine(overdueDays);
        if (fine < 0) {
            throw new IllegalStateException("Fine policy returned negative cents");
        }
        LibraryState candidate = new LibraryState(state);
        candidate.loans.remove(key);
        commit(candidate);
        return fine;
    }

    public int borrowedCount(String memberId) {
        String id = Identifiers.normalize(memberId);
        findMember(id);
        return state.borrowedCount(id);
    }

    private Member findMember(String id) {
        Member member = state.members.get(id);
        if (member == null) {
            throw new IllegalArgumentException("Unknown member: " + id);
        }
        return member;
    }

    private void requireBook(String isbn) {
        if (!state.books.containsKey(isbn)) {
            throw new IllegalArgumentException("Unknown ISBN: " + isbn);
        }
    }

    private static void requireArgument(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    private void commit(LibraryState candidate) {
        if (store != null) {
            store.save(candidate);
        }
        state = candidate;
    }
}
