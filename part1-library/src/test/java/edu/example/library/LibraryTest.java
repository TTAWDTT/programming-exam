package edu.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LibraryTest {
    private static final LocalDate DAY = LocalDate.of(2026, 3, 1);
    private Library library;

    @BeforeEach
    void setUp() {
        library = new Library();
        library.addMember(new Undergraduate("S1"));
        library.addMember(new Postgraduate("S2"));
        library.addBook("B1", 2);
        library.addBook("B2", 1);
    }

    @Test
    @DisplayName("R1 AC01: member classes and required public API")
    void memberTypesHaveRequiredLimitsAndPeriods() throws Exception {
        Member undergraduate = new Undergraduate("S1");
        Member postgraduate = new Postgraduate("S2");
        assertEquals(5, undergraduate.borrowingLimit());
        assertEquals(14, undergraduate.loanPeriodDays());
        assertEquals(10, postgraduate.borrowingLimit());
        assertEquals(28, postgraduate.loanPeriodDays());
        assertTrue(Modifier.isAbstract(Member.class.getModifiers()));
        assertTrue(FinePolicy.class.isInterface());
        assertNotNull(Library.class.getConstructor());
        assertEquals(void.class, Library.class.getMethod("addMember", Member.class).getReturnType());
        assertEquals(void.class, Library.class.getMethod("addBook", String.class, int.class).getReturnType());
        assertEquals(void.class, Library.class.getMethod("borrow", String.class, String.class,
                LocalDate.class).getReturnType());
        assertEquals(int.class, Library.class.getMethod("returnBook", String.class, String.class,
                LocalDate.class).getReturnType());
        assertEquals(int.class, Library.class.getMethod("borrowedCount", String.class).getReturnType());
        assertTrue(LibraryException.class.isAssignableFrom(DuplicateLoanException.class));
        assertTrue(LibraryException.class.isAssignableFrom(BorrowLimitException.class));
        assertTrue(LibraryException.class.isAssignableFrom(NoCopyAvailableException.class));
        assertTrue(LibraryException.class.isAssignableFrom(LoanNotFoundException.class));
    }

    @Test
    @DisplayName("R2 R4 R9 AC02 AC03: normalization applies to every operation")
    void identifiersIgnoreCaseAndOuterWhitespace() {
        Library normalized = new Library();
        normalized.addMember(new Undergraduate(" s1 "));
        normalized.addBook(" b1 ", 1);
        normalized.addBook("B1", 1);
        assertThrowsExactly(IllegalArgumentException.class,
                () -> normalized.addMember(new Postgraduate("S1")));
        normalized.borrow(" s1 ", "B1", DAY);
        assertEquals(1, normalized.borrowedCount("s1"));
        assertThrowsExactly(DuplicateLoanException.class,
                () -> normalized.borrow("S1", " b1 ", DAY));
        normalized.updateMember(new Postgraduate(" s1 "));
        assertEquals(50, normalized.returnBook("s1", " b1 ", DAY.plusDays(15)));
        assertEquals(0, normalized.borrowedCount(" S1 "));
    }

    @Test
    @DisplayName("R2 AC04: internal whitespace is preserved")
    void internalWhitespaceIsPreserved() {
        library.addMember(new Undergraduate("S 1"));
        assertEquals(0, library.borrowedCount("S 1"));
        library.borrow("S1", "B1", DAY);
        assertEquals(0, library.borrowedCount("S 1"));
        assertEquals(1, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R2: normalization is independent of the default locale")
    void normalizationUsesRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            library.addMember(new Undergraduate(" i "));
            assertEquals(0, library.borrowedCount("I"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\u2003"})
    @DisplayName("R2 R3 R17 AC05: invalid identifiers are rejected consistently")
    void invalidIdentifiersAreRejected(String invalid) {
        assertThrowsExactly(IllegalArgumentException.class, () -> new Undergraduate(invalid));
        assertThrowsExactly(IllegalArgumentException.class, () -> new Postgraduate(invalid));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrowedCount(invalid));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addBook(invalid, 1));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow(invalid, "B1", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S1", invalid, DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.returnBook(invalid, "B1", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.returnBook("S1", invalid, DAY));
        assertEquals(0, library.borrowedCount("S1"));
        library.borrow("S1", "B2", DAY);
        assertEquals(1, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R3 R17 AC05: null arguments and nonpositive copies")
    void invalidArgumentsDoNotChangeState() {
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addMember(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.updateMember(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S1", "B1", null));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.returnBook("S1", "B1", null));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addBook("B1", 0));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addBook("B1", -1));
        assertEquals(0, library.borrowedCount("S1"));
        library.borrow("S1", "B1", DAY);
        library.borrow("S2", "B1", DAY);
        library.addMember(new Undergraduate("S3"));
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S3", "B1", DAY));
    }

    @Test
    @DisplayName("R3 AC06: unknown references are invalid input")
    void unknownReferencesAreRejected() {
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrowedCount("S99"));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S99", "B1", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S1", "B99", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.returnBook("S99", "B1", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.returnBook("S1", "B99", DAY));
        assertEquals(0, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R4 R17 AC07: duplicate registration preserves the original member")
    void duplicateRegistrationIsRejected() {
        assertEquals(0, library.borrowedCount("S1"));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> library.addMember(new Undergraduate("S1")));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> library.addMember(new Postgraduate("S1")));
        library.borrow("S1", "B1", DAY);
        assertEquals(50, library.returnBook("S1", "B1", DAY.plusDays(15)));
    }

    @Test
    @DisplayName("R5 R10 AC08 AC09: old dates stay fixed and new loans use the new period")
    void typeUpdateDoesNotChangeDueDate() {
        library.borrow("S1", "B1", DAY);
        library.updateMember(new Postgraduate("S1"));
        assertEquals(1, library.borrowedCount("S1"));
        library.borrow("S1", "B2", LocalDate.of(2026, 3, 10));
        assertEquals(50, library.returnBook("S1", "B1", LocalDate.of(2026, 3, 16)));
        assertEquals(0, library.returnBook("S1", "B2", LocalDate.of(2026, 4, 7)));
    }

    @Test
    @DisplayName("R5 AC10: update must not create an unknown member")
    void unknownMemberCannotBeUpdated() {
        assertThrowsExactly(IllegalArgumentException.class,
                () -> library.updateMember(new Postgraduate("S99")));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrowedCount("S99"));
        library.addMember(new Undergraduate("S99"));
        assertEquals(0, library.borrowedCount("S99"));
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 8})
    @DisplayName("R5 R6 R17 AC14: reject updates above the new limit and allow equality")
    void downgradeRequiresCountWithinNewLimit(int initialCount) {
        borrowBooks("S2", initialCount);
        assertThrowsExactly(BorrowLimitException.class,
                () -> library.updateMember(new Undergraduate(" s2 ")));
        assertEquals(initialCount, library.borrowedCount("S2"));
        assertThrowsExactly(NoCopyAvailableException.class,
                () -> library.borrow("S1", "L1", DAY));
        library.addBook("NEW", 1);
        library.borrow("S2", "NEW", DAY);
        assertEquals(initialCount + 1, library.borrowedCount("S2"));
        assertEquals(0, library.returnBook("S2", "NEW", DAY.plusDays(28)));
        for (int i = 1; i <= initialCount - 5; i++) {
            assertEquals(0, library.returnBook("S2", "L" + i, DAY.plusDays(28)));
        }
        library.updateMember(new Undergraduate("S2"));
        assertEquals(5, library.borrowedCount("S2"));
        assertThrowsExactly(BorrowLimitException.class, () -> library.borrow("S2", "NEW", DAY));
        assertEquals(0, library.returnBook("S2", "L" + initialCount, DAY.plusDays(28)));
        assertEquals(4, library.borrowedCount("S2"));
        library.borrow("S2", "NEW", DAY);
        assertEquals(5, library.borrowedCount("S2"));
        assertEquals(50, library.returnBook("S2", "NEW", DAY.plusDays(15)));
    }

    @Test
    @DisplayName("R6 AC12: undergraduate fifth succeeds and sixth fails")
    void undergraduateFifthAndSixthLoans() {
        borrowBooks("S1", 4);
        library.addBook("FIFTH", 1);
        library.addBook("SIXTH", 1);
        library.borrow("S1", "FIFTH", DAY);
        assertEquals(5, library.borrowedCount("S1"));
        assertThrowsExactly(BorrowLimitException.class, () -> library.borrow("S1", "SIXTH", DAY));
        assertEquals(5, library.borrowedCount("S1"));
        library.borrow("S2", "SIXTH", DAY);
    }

    @Test
    @DisplayName("R6 AC13: postgraduate tenth succeeds and eleventh fails")
    void postgraduateTenthAndEleventhLoans() {
        borrowBooks("S2", 9);
        library.addBook("TENTH", 1);
        library.addBook("ELEVENTH", 1);
        library.borrow("S2", "TENTH", DAY);
        assertEquals(10, library.borrowedCount("S2"));
        assertThrowsExactly(BorrowLimitException.class, () -> library.borrow("S2", "ELEVENTH", DAY));
        assertEquals(10, library.borrowedCount("S2"));
        library.borrow("S1", "ELEVENTH", DAY);
    }

    @Test
    @DisplayName("R7 AC15: adding copies preserves existing loans")
    void addingCopiesAccumulates() {
        library.borrow("S1", "B1", DAY);
        library.addBook(" b1 ", 3);
        for (int i = 3; i <= 6; i++) {
            library.addMember(new Undergraduate("S" + i));
            library.borrow("S" + i, "B1", DAY);
        }
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S2", "B1", DAY));
        assertEquals(1, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R3 R7 R17 AC16: inventory addition cannot overflow")
    void addingCopiesAccumulatesWithoutOverflow() {
        library.addBook("MAX", Integer.MAX_VALUE);
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addBook("MAX", 1));
        library.borrow("S1", "MAX", DAY);
        assertEquals(1, library.borrowedCount("S1"));
        assertThrowsExactly(IllegalArgumentException.class,
                () -> library.addBook("B2", Integer.MAX_VALUE));
        library.borrow("S1", "B2", DAY);
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S2", "B2", DAY));
    }

    @Test
    @DisplayName("R8 R9 AC17 AC18 AC19 AC20: duplicate, shared stock and reuse after return")
    void copiesAreSharedAndReleasedExactlyOnce() {
        library.borrow("S1", "B1", DAY);
        assertThrowsExactly(DuplicateLoanException.class, () -> library.borrow("S1", "B1", DAY));
        library.borrow("S2", "B1", DAY);
        assertEquals(1, library.borrowedCount("S1"));
        assertEquals(1, library.borrowedCount("S2"));
        assertEquals(0, library.returnBook("S1", "B1", DAY));
        library.borrow("S1", "B1", DAY);
        library.borrow("S1", "B2", DAY);
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S2", "B2", DAY));
        library.returnBook("S1", "B2", DAY);
        library.borrow("S2", "B2", DAY);
        assertEquals(2, library.borrowedCount("S2"));
    }

    @ParameterizedTest
    @CsvSource({
        "U,2026-03-14,0", "U,2026-03-15,0", "U,2026-03-16,50",
        "P,2026-03-28,0", "P,2026-03-29,0", "P,2026-03-30,50",
        "U,2026-04-03,950", "U,2026-04-04,1000", "U,2026-04-05,1000"
    })
    @DisplayName("R10 R14 AC21 AC22 AC31: exact due-date and cap boundaries")
    void fineAtDueDateBoundaries(String type, LocalDate returned, int expected) {
        String id = type.equals("U") ? "S1" : "S2";
        library.borrow(id, "B1", DAY);
        assertEquals(expected, library.returnBook(id, "B1", returned));
        assertEquals(0, library.borrowedCount(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2000-01-01", "2099-01-01"})
    @DisplayName("R10 AC23: trusted historical and future dates")
    void datesDoNotDependOnSystemClock(LocalDate borrowed) {
        library.borrow("S1", "B1", borrowed);
        assertEquals(0, library.returnBook("S1", "B1", borrowed.plusDays(14)));
    }

    @Test
    @DisplayName("R10 R17 AC24: date overflow preserves state")
    void unrepresentableDueDateIsRejected() {
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S1", "B2", LocalDate.MAX));
        assertEquals(0, library.borrowedCount("S1"));
        library.borrow("S2", "B2", DAY);
    }

    @Test
    @DisplayName("R11 AC25 AC26 AC27: input, duplicate, limit, inventory precedence")
    void duplicatePrecedesLimitAndInventory() {
        borrowBooks("S1", 5);
        assertThrowsExactly(DuplicateLoanException.class, () -> library.borrow("S1", "L1", DAY));
        library.borrow("S2", "B2", DAY);
        assertThrowsExactly(BorrowLimitException.class, () -> library.borrow("S1", "B2", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S1", "B99", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrow("S1", "L1", LocalDate.MAX));
        assertEquals(5, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R12 R17 AC28: wrong and repeated returns never add stock")
    void repeatReturnIsRejected() {
        assertThrowsExactly(LoanNotFoundException.class, () -> library.returnBook("S1", "B2", DAY));
        library.borrow("S1", "B2", DAY);
        library.returnBook("S1", "B2", DAY);
        assertThrowsExactly(LoanNotFoundException.class, () -> library.returnBook("S1", "B2", DAY));
        library.borrow("S2", "B2", DAY);
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S1", "B2", DAY));
        assertEquals(0, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R13 R14 R17 AC29 AC30: return date ordering")
    void returnBeforeBorrowIsRejected() {
        LocalDate borrowed = LocalDate.of(2026, 3, 10);
        library.borrow("S1", "B2", borrowed);
        assertThrowsExactly(IllegalArgumentException.class,
                () -> library.returnBook("S1", "B2", borrowed.minusDays(1)));
        assertEquals(1, library.borrowedCount("S1"));
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S2", "B2", borrowed));
        assertEquals(0, library.returnBook("S1", "B2", borrowed));
        assertEquals(0, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R14 AC32: fine cap applies to each loan")
    void capIsPerReturnedLoan() {
        library.borrow("S1", "B1", DAY);
        library.borrow("S1", "B2", DAY);
        assertEquals(1000, library.returnBook("S1", "B1", LocalDate.of(2026, 4, 5)));
        assertEquals(1000, library.returnBook("S1", "B2", LocalDate.of(2026, 4, 5)));
    }

    @Test
    @DisplayName("R15 AC34: overdue loan alone does not block borrowing")
    void overdueLoanDoesNotBlockBorrowing() {
        library.addBook("B3", 1);
        library.borrow("S1", "B1", DAY);
        library.borrow("S1", "B2", DAY);
        library.borrow("S1", "B3", LocalDate.of(2026, 3, 20));
        assertEquals(3, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R16 AC35: injected policy replaces the calculation")
    void customPolicyIsUsed() {
        AtomicLong received = new AtomicLong(-1);
        Library custom = new Library(days -> {
            received.set(days);
            return (int) days * 20;
        });
        custom.addMember(new Undergraduate("S1"));
        custom.addBook("B1", 1);
        custom.borrow("S1", "B1", DAY);
        assertEquals(40, custom.returnBook("S1", "B1", DAY.plusDays(16)));
        assertEquals(2, received.get());
        custom.borrow("S1", "B1", DAY);
        assertEquals(0, custom.returnBook("S1", "B1", DAY));
        assertEquals(0, received.get());
    }

    @Test
    @DisplayName("R16 R17 AC36: invalid policies cannot complete a return")
    void invalidPolicyDoesNotChangeState() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new Library(null));
        Library invalid = new Library(days -> -1);
        invalid.addMember(new Undergraduate("S1"));
        invalid.addBook("B1", 1);
        invalid.borrow("S1", "B1", DAY);
        assertThrowsExactly(IllegalStateException.class, () -> invalid.returnBook("S1", "B1", DAY));
        assertEquals(1, invalid.borrowedCount("S1"));
        assertThrowsExactly(DuplicateLoanException.class, () -> invalid.borrow("S1", "B1", DAY));
    }

    @Test
    @DisplayName("R18 AC38: memory instances do not share data")
    void memoryInstancesAreIndependent() {
        Library other = new Library();
        assertThrowsExactly(IllegalArgumentException.class, () -> other.borrowedCount("S1"));
        assertEquals(0, library.borrowedCount("S1"));
    }

    private void borrowBooks(String memberId, int count) {
        for (int i = 1; i <= count; i++) {
            library.addBook("L" + i, 1);
            library.borrow(memberId, "L" + i, DAY);
        }
    }
}
