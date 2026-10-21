package edu.example.library;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FileLibraryTest {
    private static final LocalDate DAY = LocalDate.of(2026, 3, 1);

    @TempDir
    Path directory;

    @Test
    @DisplayName("R18 AC38: memory mode creates no file")
    void memoryModeDoesNotCreateFiles() throws IOException {
        long before;
        try (Stream<Path> files = Files.list(Path.of("."))) {
            before = files.count();
        }
        Library first = new Library();
        Library second = new Library();
        first.addMember(new Undergraduate("S1"));
        first.addBook("B1", 1);
        first.borrow("S1", "B1", DAY);
        assertEquals(1, first.borrowedCount("S1"));
        assertThrowsExactly(IllegalArgumentException.class, () -> second.borrowedCount("S1"));
        try (Stream<Path> files = Files.list(Path.of("."))) {
            assertEquals(before, files.count());
        }
    }

    @Test
    @DisplayName("R19 AC39: creation immediately persists an empty snapshot")
    void createSavesEmptyState() {
        Path path = directory.resolve("library.properties");
        Library library = Library.create(path);
        assertTrue(Files.isRegularFile(path));
        LibraryState loaded = new FileSnapshotStore(path).load();
        assertTrue(loaded.members.isEmpty());
        assertTrue(loaded.books.isEmpty());
        assertTrue(loaded.loans.isEmpty());
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrowedCount("S1"));
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.load(path).borrowedCount("S1"));
    }

    @Test
    @DisplayName("R19 AC40: existing data is never overwritten by create")
    void createDoesNotOverwriteExistingFile() throws IOException {
        Path path = directory.resolve("existing.properties");
        byte[] original = "important original data".getBytes(StandardCharsets.UTF_8);
        Files.write(path, original);
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class,
                () -> Library.create(path));
        assertTrue(error.getMessage().contains("create"));
        assertTrue(error.getMessage().contains(path.toString()));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    @DisplayName("R19 R23 AC41: missing parents and initial-save failure")
    void failedCreationDoesNotPublishTarget() throws IOException {
        Path missing = directory.resolve("absent/library.properties");
        assertThrowsExactly(LibraryStorageException.class, () -> Library.create(missing));
        assertFalse(Files.exists(missing.getParent()));
        Path target = directory.resolve("failed.properties");
        FailingFileStore store = new FailingFileStore(target);
        store.fail = true;
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class,
                () -> store.create(new LibraryState()));
        assertTrue(error.getCause() instanceof IOException);
        assertFalse(Files.exists(target));
        assertNoTemporaryFiles();
    }

    @Test
    @DisplayName("R3 R16: null paths, empty paths and null policies")
    void factoryArgumentsAreValidatedBeforeWriting() {
        Path path = directory.resolve("library.properties");
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.create(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.load(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.create(Path.of("")));
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.load(Path.of(" ")));
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.create(path, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> Library.load(path, null));
        assertFalse(Files.exists(path));
    }

    @Test
    @DisplayName("R20 AC42: missing load is not creation")
    void loadMissingFileFails() {
        Path path = directory.resolve("missing.properties");
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class,
                () -> Library.load(path));
        assertTrue(error.getMessage().contains("load"));
        assertTrue(error.getMessage().contains(path.toString()));
        assertNotNull(error.getCause());
        assertFalse(Files.exists(path));
    }

    @Test
    @DisplayName("R20 AC43: invalid UTF-8 and directory paths are rejected")
    void unreadableDataAndDirectoryAreRejected() throws IOException {
        Path path = directory.resolve("bad-utf8.properties");
        byte[] original = {(byte) 0xc3, (byte) 0x28};
        Files.write(path, original);
        assertThrowsExactly(LibraryStorageException.class, () -> Library.load(path));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertThrowsExactly(LibraryStorageException.class, () -> Library.load(directory));
        assertThrowsExactly(LibraryStorageException.class, () -> Library.create(directory));
        assertTrue(Files.isDirectory(directory));
        Path root = directory.toAbsolutePath().getRoot();
        assertThrowsExactly(LibraryStorageException.class, () -> Library.create(root));
        assertThrowsExactly(LibraryStorageException.class, () -> Library.load(root));
        assertTrue(Files.isDirectory(root));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"version", "missing", "extra", "wrongKey", "badInteger",
            "negativeCount", "hugeCount", "unknownType", "noncanonical", "duplicateMember",
            "duplicateBook", "zeroCopies", "negativeCopies", "overflowCopies", "unknownMember",
            "unknownBook", "duplicateLoan", "tooManyLoans", "badDate", "badPeriod",
            "wrongDueDate", "overflowDueDate", "blankIdentifier"})
    @DisplayName("R20 R21 AC43 AC44 AC47: reject the entire invalid snapshot")
    void malformedSnapshotIsRejected(String corruption) throws IOException {
        Path path = directory.resolve("library.properties");
        Library library = seeded(path);
        library.borrow("S2", "B1", DAY);
        Properties properties = readProperties(path);
        switch (corruption) {
            case "version" -> properties.setProperty("format.version", "99");
            case "missing" -> properties.remove("member.0.type");
            case "extra" -> properties.setProperty("extra.field", "unexpected");
            case "wrongKey" -> {
                properties.remove("member.0.type");
                properties.setProperty("member.9.type", "UNDERGRADUATE");
            }
            case "badInteger" -> properties.setProperty("book.0.total", "many");
            case "negativeCount" -> properties.setProperty("loan.count", "-1");
            case "hugeCount" -> properties.setProperty("member.count", "2147483647");
            case "unknownType" -> properties.setProperty("member.0.type", "STAFF");
            case "noncanonical" -> properties.setProperty("member.0.id", "s1");
            case "duplicateMember" -> properties.setProperty("member.1.id", "S1");
            case "duplicateBook" -> properties.setProperty("book.1.isbn", "B1");
            case "zeroCopies" -> properties.setProperty("book.0.total", "0");
            case "negativeCopies" -> properties.setProperty("book.0.total", "-1");
            case "overflowCopies" -> properties.setProperty("book.0.total", "2147483648");
            case "unknownMember" -> properties.setProperty("loan.0.memberId", "S99");
            case "unknownBook" -> properties.setProperty("loan.0.isbn", "B99");
            case "duplicateLoan" -> properties.setProperty("loan.1.memberId", "S1");
            case "tooManyLoans" -> properties.setProperty("book.0.total", "1");
            case "badDate" -> properties.setProperty("loan.0.borrowedOn", "2026-02-30");
            case "badPeriod" -> properties.setProperty("loan.0.periodDays", "15");
            case "wrongDueDate" -> properties.setProperty("loan.0.dueOn", "2026-03-16");
            case "overflowDueDate" -> properties.setProperty("loan.0.borrowedOn", LocalDate.MAX.toString());
            case "blankIdentifier" -> properties.setProperty("member.0.id", " ");
            default -> throw new AssertionError(corruption);
        }
        writeProperties(path, properties);
        byte[] original = Files.readAllBytes(path);
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class,
                () -> Library.load(path));
        assertTrue(error.getMessage().contains("load"));
        assertTrue(error.getMessage().contains(path.toString()));
        assertNotNull(error.getCause());
        assertArrayEquals(original, Files.readAllBytes(path));
        assertEquals(1, library.borrowedCount("S1"));
        assertNoTemporaryFiles();
    }

    @Test
    @DisplayName("R20 R21: duplicate property keys and truncated files are errors")
    void duplicateKeysAndTruncatedFilesAreRejected() throws IOException {
        Path path = directory.resolve("library.properties");
        seeded(path);
        String original = Files.readString(path);
        for (String invalid : new String[] {
                original + "\nformat.version=1\n",
                original + "\nformat\\.version=1\n",
                original.substring(0, 20),
                ""
        }) {
            Files.writeString(path, invalid);
            assertThrowsExactly(LibraryStorageException.class, () -> Library.load(path));
            assertEquals(invalid, Files.readString(path));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {6, 8})
    @DisplayName("R5 R6 R17 R21 AC14 AC45: rejected update preserves disk and valid update reloads")
    void downgradeWithinLimitSurvivesReload(int initialCount) throws IOException {
        Path path = directory.resolve("library.properties");
        Library library = Library.create(path);
        library.addMember(new Postgraduate("S1"));
        for (int i = 1; i <= initialCount; i++) {
            library.addBook("B" + i, 1);
            library.borrow("S1", "B" + i, DAY);
        }
        byte[] original = Files.readAllBytes(path);
        assertThrowsExactly(BorrowLimitException.class,
                () -> library.updateMember(new Undergraduate("S1")));
        assertEquals(initialCount, library.borrowedCount("S1"));
        assertArrayEquals(original, Files.readAllBytes(path));
        LibraryState stored = new FileSnapshotStore(path).load();
        assertEquals(Postgraduate.class, stored.members.get("S1").getClass());
        assertEquals(initialCount, stored.loans.size());
        assertEquals(0, stored.availableCopies("B1"));
        Library loaded = Library.load(path);
        loaded.addBook("NEW", 1);
        loaded.borrow("S1", "NEW", DAY);
        assertEquals(initialCount + 1, loaded.borrowedCount("S1"));
        assertEquals(0, loaded.returnBook("S1", "NEW", DAY.plusDays(28)));
        for (int i = 1; i <= initialCount - 5; i++) {
            assertEquals(0, loaded.returnBook("S1", "B" + i, DAY.plusDays(28)));
        }
        loaded.updateMember(new Undergraduate("S1"));
        Library updated = Library.load(path);
        assertEquals(5, updated.borrowedCount("S1"));
        assertThrowsExactly(BorrowLimitException.class, () -> updated.borrow("S1", "NEW", DAY));
        assertEquals(0, updated.returnBook("S1", "B" + initialCount, DAY.plusDays(28)));
        updated.borrow("S1", "NEW", DAY);
        assertEquals(5, Library.load(path).borrowedCount("S1"));
        assertEquals(50, updated.returnBook("S1", "NEW", DAY.plusDays(15)));
        assertNoTemporaryFiles();
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNDERGRADUATE", "POSTGRADUATE"})
    @DisplayName("R20 R21 AC45: stored loans cannot exceed the current member limit")
    void memberAboveLimitSnapshotIsRejected(String type) throws IOException {
        Path path = directory.resolve("over-limit.properties");
        Member member = type.equals("UNDERGRADUATE")
                ? new Undergraduate("S1") : new Postgraduate("S1");
        int count = type.equals("UNDERGRADUATE") ? 6 : 11;
        LibraryState invalid = new LibraryState();
        invalid.members.put("S1", member);
        for (int i = 1; i <= count; i++) {
            String isbn = "B" + i;
            invalid.books.put(isbn, 1);
            invalid.loans.put(new LoanKey("S1", isbn),
                    new Loan("S1", isbn, DAY, DAY.plusDays(28), 28));
        }
        new FileSnapshotStore(path).create(invalid);
        byte[] original = Files.readAllBytes(path);
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class,
                () -> Library.load(path));
        assertTrue(error.getCause().getMessage().contains("borrowing limit"));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertNoTemporaryFiles();
    }

    @Test
    @DisplayName("R21 AC46: reloading never recalculates an existing due date")
    void storedDueDateSurvivesReload() {
        Path path = directory.resolve("library.properties");
        Library library = seeded(path);
        library.updateMember(new Postgraduate("S1"));
        Library reloaded = Library.load(path);
        assertEquals(1, reloaded.borrowedCount("S1"));
        assertEquals(50, reloaded.returnBook("S1", "B1", DAY.plusDays(15)));
        reloaded.borrow("S1", "B1", DAY);
        assertEquals(0, reloaded.returnBook("S1", "B1", DAY.plusDays(28)));
    }

    @Test
    @DisplayName("R22 AC48: every mutation is saved without calling save")
    void everyMutationSurvivesReload() {
        Path path = directory.resolve("library.properties");
        Library library = Library.create(path);
        library.addMember(new Undergraduate("S1"));
        library = Library.load(path);
        assertEquals(0, library.borrowedCount("S1"));
        library.addBook("B1", 2);
        library = Library.load(path);
        library.borrow("S1", "B1", DAY);
        library = Library.load(path);
        assertEquals(1, library.borrowedCount("S1"));
        library.updateMember(new Postgraduate("S1"));
        library = Library.load(path);
        library.addBook("B2", 1);
        library.borrow("S1", "B2", DAY);
        library = Library.load(path);
        assertEquals(0, library.returnBook("S1", "B2", DAY.plusDays(28)));
        assertEquals(0, library.returnBook("S1", "B1", DAY.plusDays(14)));
        library = Library.load(path);
        assertEquals(0, library.borrowedCount("S1"));
        library.addMember(new Undergraduate("S2"));
        library.addMember(new Undergraduate("S3"));
        library.borrow("S1", "B1", DAY);
        library.borrow("S2", "B1", DAY);
        Library loaded = library;
        assertThrowsExactly(NoCopyAvailableException.class, () -> loaded.borrow("S3", "B1", DAY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "update", "copies", "borrow", "return"})
    @DisplayName("R17 R23 AC49 AC50: all mutations preserve memory and disk if publication fails")
    void failedSavePreservesMemoryAndFile(String operation) throws IOException {
        Path path = directory.resolve("library.properties");
        seeded(path);
        FailingFileStore store = new FailingFileStore(path);
        LibraryState before = store.load();
        Library library = new Library(before, new CappedDailyFinePolicy(), store);
        byte[] bytes = Files.readAllBytes(path);
        store.fail = true;
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class, () -> {
            switch (operation) {
                case "register" -> library.addMember(new Undergraduate("S3"));
                case "update" -> library.updateMember(new Postgraduate("S1"));
                case "copies" -> library.addBook("B1", 3);
                case "borrow" -> library.borrow("S1", "B2", DAY);
                case "return" -> library.returnBook("S1", "B1", DAY.plusDays(15));
                default -> throw new AssertionError(operation);
            }
        });
        assertTrue(error.getMessage().contains("save"));
        assertTrue(error.getMessage().contains(path.toString()));
        assertTrue(error.getCause() instanceof IOException);
        assertEquals(1, library.borrowedCount("S1"));
        assertEquals(0, library.borrowedCount("S2"));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.borrowedCount("S3"));
        assertThrowsExactly(DuplicateLoanException.class, () -> library.borrow("S1", "B1", DAY));
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertStateEquals(before, store.load());
        assertNoTemporaryFiles();

        store.fail = false;
        library.borrow("S1", "B2", DAY);
        assertEquals(50, library.returnBook("S1", "B2", DAY.plusDays(15)));
        library.borrow("S2", "B1", DAY);
        library.addMember(new Undergraduate("S3"));
        assertThrowsExactly(NoCopyAvailableException.class, () -> library.borrow("S3", "B1", DAY));
        assertEquals(50, library.returnBook("S1", "B1", DAY.plusDays(15)));
    }

    @Test
    @DisplayName("R17 R23 AC49: failed first borrow leaves zero loans and one available copy")
    void failedFirstBorrowPreservesEmptyLoanState() throws IOException {
        Path path = directory.resolve("library.properties");
        Library initial = Library.create(path);
        initial.addMember(new Undergraduate("S1"));
        initial.addBook("B1", 1);
        FailingFileStore store = new FailingFileStore(path);
        Library library = new Library(store.load(), new CappedDailyFinePolicy(), store);
        byte[] original = Files.readAllBytes(path);
        store.fail = true;
        assertThrowsExactly(LibraryStorageException.class, () -> library.borrow("S1", "B1", DAY));
        assertEquals(0, library.borrowedCount("S1"));
        assertArrayEquals(original, Files.readAllBytes(path));
        assertEquals(1, store.load().availableCopies("B1"));
        store.fail = false;
        library.borrow("S1", "B1", DAY);
        assertEquals(1, library.borrowedCount("S1"));
    }

    @Test
    @DisplayName("R23 AC51: unsupported atomic publication never falls back to overwrite")
    void unsupportedAtomicMoveFailsSafely() throws IOException {
        Path path = directory.resolve("library.properties");
        seeded(path);
        FileSnapshotStore store = new FileSnapshotStore(path) {
            @Override
            void publish(Path temporary) throws IOException {
                throw new AtomicMoveNotSupportedException(temporary.toString(), path.toString(), "injected");
            }
        };
        Library library = new Library(store.load(), new CappedDailyFinePolicy(), store);
        byte[] before = Files.readAllBytes(path);
        LibraryStorageException error = assertThrowsExactly(LibraryStorageException.class,
                () -> library.addBook("B1", 1));
        assertInstanceOf(AtomicMoveNotSupportedException.class, error.getCause());
        assertArrayEquals(before, Files.readAllBytes(path));
        assertEquals(1, library.borrowedCount("S1"));
        assertNoTemporaryFiles();
    }

    @Test
    @DisplayName("R17: failure before writing also leaves all state unchanged")
    void failingStoreDoesNotPublishMemory() throws IOException {
        Path path = directory.resolve("library.properties");
        seeded(path);
        FileSnapshotStore disk = new FileSnapshotStore(path);
        SnapshotStore failing = new SnapshotStore() {
            @Override
            public LibraryState load() {
                return disk.load();
            }

            @Override
            public void create(LibraryState state) {
                throw new AssertionError("Creation is not part of this scenario");
            }

            @Override
            public void save(LibraryState state) {
                throw new LibraryStorageException("save", path, new IOException("injected before write"));
            }
        };
        Library library = new Library(disk.load(), new CappedDailyFinePolicy(), failing);
        byte[] before = Files.readAllBytes(path);
        assertThrowsExactly(LibraryStorageException.class, () -> library.borrow("S1", "B2", DAY));
        assertEquals(1, library.borrowedCount("S1"));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    @Test
    @DisplayName("R5 R20 R22 AC11 AC52: reads and same-type updates never save")
    void readsAndNoOpUpdateDoNotWrite() throws IOException {
        Path path = directory.resolve("library.properties");
        seeded(path).updateMember(new Postgraduate("S1"));
        byte[] original = Files.readAllBytes(path);
        Library loaded = Library.load(path);
        assertArrayEquals(original, Files.readAllBytes(path));
        FailingFileStore store = new FailingFileStore(path);
        store.fail = true;
        Library library = new Library(store.load(), new CappedDailyFinePolicy(), store);
        library.updateMember(new Postgraduate(" s1 "));
        library.updateMember(new Postgraduate("s2"));
        for (int i = 0; i < 3; i++) {
            assertEquals(1, library.borrowedCount("S1"));
            assertEquals(1, loaded.borrowedCount("S1"));
        }
        assertEquals(0, store.saveCalls);
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"throw", "negative"})
    @DisplayName("R16 R17 AC36 AC37: policy errors do not save or remove a loan")
    void policyFailurePreservesFile(String failure) throws IOException {
        Path path = directory.resolve("library.properties");
        FinePolicy policy = days -> {
            if (failure.equals("throw")) {
                throw new IllegalStateException("injected policy failure");
            }
            return -1;
        };
        Library library = Library.create(path, policy);
        library.addMember(new Undergraduate("S1"));
        library.addBook("B1", 1);
        library.borrow("S1", "B1", DAY);
        byte[] before = Files.readAllBytes(path);
        assertThrowsExactly(IllegalStateException.class, () -> library.returnBook("S1", "B1", DAY));
        assertEquals(1, library.borrowedCount("S1"));
        assertArrayEquals(before, Files.readAllBytes(path));
        assertEquals(1, Library.load(path).borrowedCount("S1"));
    }

    @Test
    @DisplayName("R24 AC53: caller chooses the fine policy again when loading")
    void customPolicyIsExplicitOnReload() throws IOException {
        Path customPath = directory.resolve("custom.properties");
        FinePolicy discount = days -> (int) days * 20;
        Library library = Library.create(customPath, discount);
        library.addMember(new Undergraduate("S1"));
        library.addBook("B1", 1);
        library.borrow("S1", "B1", DAY);
        Path defaultPath = directory.resolve("default.properties");
        Files.copy(customPath, defaultPath);
        assertEquals(40, Library.load(customPath, discount)
                .returnBook("S1", "B1", DAY.plusDays(16)));
        assertEquals(100, Library.load(defaultPath)
                .returnBook("S1", "B1", DAY.plusDays(16)));
    }

    @Test
    @DisplayName("R2 R21: identifiers survive properties escaping and UTF-8")
    void identifiersSurviveFileRoundTrip() {
        Path path = directory.resolve("library.properties");
        Library library = Library.create(path);
        library.addMember(new Undergraduate(" student=oné: \\ x "));
        library.addBook(" book\nsecond ", 1);
        library.borrow("STUDENT=ONÉ: \\ X", "BOOK\nSECOND", DAY);
        Library loaded = Library.load(path);
        assertEquals(1, loaded.borrowedCount(" student=oné: \\ x "));
        assertEquals(0, loaded.returnBook("STUDENT=ONÉ: \\ X", "BOOK\nSECOND", DAY));
    }

    @Test
    @DisplayName("R17: invalid business operations never touch the file")
    void rejectedOperationsPreserveFileBytes() throws IOException {
        Path path = directory.resolve("library.properties");
        Library library = seeded(path);
        byte[] before = Files.readAllBytes(path);
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addMember(new Postgraduate("S1")));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.updateMember(new Undergraduate("ABSENT")));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.addBook("B1", 0));
        assertThrowsExactly(DuplicateLoanException.class, () -> library.borrow("S1", "B1", DAY));
        assertThrowsExactly(LoanNotFoundException.class, () -> library.returnBook("S1", "B2", DAY));
        assertThrowsExactly(IllegalArgumentException.class, () -> library.returnBook("S1", "B1", DAY.minusDays(1)));
        assertEquals(1, library.borrowedCount("S1"));
        assertArrayEquals(before, Files.readAllBytes(path));
    }

    private Library seeded(Path path) {
        Library library = Library.create(path);
        library.addMember(new Undergraduate("S1"));
        library.addMember(new Postgraduate("S2"));
        library.addBook("B1", 2);
        library.addBook("B2", 1);
        library.borrow("S1", "B1", DAY);
        return library;
    }

    private static Properties readProperties(Path path) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static void writeProperties(Path path, Properties properties) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        }
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".library-")));
        }
    }

    private static void assertStateEquals(LibraryState expected, LibraryState actual) {
        assertEquals(expected.members.keySet(), actual.members.keySet());
        expected.members.forEach((id, member) -> {
            assertEquals(member.getClass(), actual.members.get(id).getClass());
            assertEquals(member.getId(), actual.members.get(id).getId());
        });
        assertEquals(expected.books, actual.books);
        assertEquals(expected.loans, actual.loans);
    }

    private static final class FailingFileStore extends FileSnapshotStore {
        boolean fail;
        int saveCalls;

        FailingFileStore(Path path) {
            super(path);
        }

        @Override
        public void save(LibraryState state) {
            saveCalls++;
            super.save(state);
        }

        @Override
        void publish(Path temporary) throws IOException {
            if (fail) {
                throw new IOException("injected before atomic publication");
            }
            super.publish(temporary);
        }
    }
}
