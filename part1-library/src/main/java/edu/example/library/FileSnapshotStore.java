package edu.example.library;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class FileSnapshotStore implements SnapshotStore {
    private final Path path;

    FileSnapshotStore(Path path) {
        if (path == null || path.toString().isBlank()) {
            throw new IllegalArgumentException("File path must not be blank");
        }
        this.path = path.toAbsolutePath().normalize();
    }

    @Override
    public void create(LibraryState state) {
        write(state, true);
    }

    @Override
    public void save(LibraryState state) {
        write(state, false);
    }

    @Override
    public LibraryState load() {
        try {
            if (!Files.isRegularFile(path)) {
                throw new IOException("Expected an existing regular file");
            }
            Properties properties = new StrictProperties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return decode(properties);
        } catch (IOException | IllegalArgumentException | DateTimeException | SecurityException e) {
            throw new LibraryStorageException("load", path, e);
        }
    }

    private void write(LibraryState state, boolean creating) {
        Path temporary = null;
        String operation = creating ? "create" : "save";
        try {
            Path parent = path.getParent();
            if (parent == null || !Files.isDirectory(parent)) {
                throw new IOException("Parent directory must already exist");
            }
            if (creating && Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Target already exists");
            }
            if (!creating && !Files.isRegularFile(path)) {
                throw new IOException("Expected an existing regular file");
            }
            String content = encode(state);
            temporary = Files.createTempFile(parent, ".library-", ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            publish(temporary);
            temporary = null;
        } catch (IOException | IllegalArgumentException | SecurityException e) {
            throw new LibraryStorageException(operation, path, e);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException | SecurityException ignored) {
                }
            }
        }
    }

    void publish(Path temporary) throws IOException {
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
    }

    private static String encode(LibraryState state) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("format.version", "1");
        properties.setProperty("member.count", Integer.toString(state.members.size()));
        properties.setProperty("book.count", Integer.toString(state.books.size()));
        properties.setProperty("loan.count", Integer.toString(state.loans.size()));
        int i = 0;
        for (String id : state.members.keySet().stream().sorted().toList()) {
            Member member = state.members.get(id);
            String type;
            if (member instanceof Undergraduate) {
                type = "UNDERGRADUATE";
            } else if (member instanceof Postgraduate) {
                type = "POSTGRADUATE";
            } else {
                throw new IllegalArgumentException("Unsupported member type: " + member.getClass().getName());
            }
            properties.setProperty("member." + i + ".id", id);
            properties.setProperty("member." + i + ".type", type);
            i++;
        }
        i = 0;
        for (String isbn : state.books.keySet().stream().sorted().toList()) {
            properties.setProperty("book." + i + ".isbn", isbn);
            properties.setProperty("book." + i + ".total", state.books.get(isbn).toString());
            i++;
        }
        i = 0;
        for (Loan loan : state.loans.values().stream()
                .sorted(Comparator.comparing(Loan::memberId).thenComparing(Loan::isbn)).toList()) {
            String prefix = "loan." + i + ".";
            properties.setProperty(prefix + "memberId", loan.memberId());
            properties.setProperty(prefix + "isbn", loan.isbn());
            properties.setProperty(prefix + "borrowedOn", loan.borrowedOn().toString());
            properties.setProperty(prefix + "dueOn", loan.dueOn().toString());
            properties.setProperty(prefix + "periodDays", Integer.toString(loan.periodDays()));
            i++;
        }
        StringWriter writer = new StringWriter();
        properties.store(writer, null);
        return writer.toString();
    }

    private static LibraryState decode(Properties properties) {
        Map<String, String> fields = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            fields.put(key, properties.getProperty(key));
        }
        if (!take(fields, "format.version").equals("1")) {
            throw new IllegalArgumentException("Unsupported format.version");
        }
        int members = count(fields, "member.count");
        int books = count(fields, "book.count");
        int loans = count(fields, "loan.count");
        long expected = 2L * members + 2L * books + 5L * loans;
        if (fields.size() != expected) {
            throw new IllegalArgumentException("Record counts do not match snapshot fields");
        }
        LibraryState state = new LibraryState();
        for (int i = 0; i < members; i++) {
            String prefix = "member." + i + ".";
            String id = canonicalId(fields, prefix + "id");
            String type = take(fields, prefix + "type");
            Member member = switch (type) {
                case "UNDERGRADUATE" -> new Undergraduate(id);
                case "POSTGRADUATE" -> new Postgraduate(id);
                default -> throw new IllegalArgumentException("Unknown member type at " + prefix + "type");
            };
            if (state.members.putIfAbsent(id, member) != null) {
                throw new IllegalArgumentException("Duplicate member ID at " + prefix + "id");
            }
        }
        for (int i = 0; i < books; i++) {
            String prefix = "book." + i + ".";
            String isbn = canonicalId(fields, prefix + "isbn");
            int total = number(fields, prefix + "total");
            if (total < 1) {
                throw new IllegalArgumentException("Nonpositive copies at " + prefix + "total");
            }
            if (state.books.putIfAbsent(isbn, total) != null) {
                throw new IllegalArgumentException("Duplicate ISBN at " + prefix + "isbn");
            }
        }
        Map<String, Integer> loanCounts = new HashMap<>();
        for (int i = 0; i < loans; i++) {
            String prefix = "loan." + i + ".";
            String memberId = canonicalId(fields, prefix + "memberId");
            String isbn = canonicalId(fields, prefix + "isbn");
            LocalDate borrowed = date(fields, prefix + "borrowedOn");
            LocalDate due = date(fields, prefix + "dueOn");
            int period = number(fields, prefix + "periodDays");
            if (!state.members.containsKey(memberId) || !state.books.containsKey(isbn)) {
                throw new IllegalArgumentException("Unknown member or ISBN at " + prefix);
            }
            if ((period != 14 && period != 28) || !borrowed.plusDays(period).equals(due)) {
                throw new IllegalArgumentException("Inconsistent period or due date at " + prefix);
            }
            Loan loan = new Loan(memberId, isbn, borrowed, due, period);
            if (state.loans.putIfAbsent(new LoanKey(memberId, isbn), loan) != null) {
                throw new IllegalArgumentException("Duplicate loan at " + prefix);
            }
            int borrowedCopies = loanCounts.merge(isbn, 1, Integer::sum);
            if (borrowedCopies > state.books.get(isbn)) {
                throw new IllegalArgumentException("More loans than total copies at " + prefix);
            }
        }
        if (!fields.isEmpty()) {
            throw new IllegalArgumentException("Unexpected snapshot fields: " + fields.keySet());
        }
        for (Member member : state.members.values()) {
            if (state.borrowedCount(member.getId()) > member.borrowingLimit()) {
                throw new IllegalArgumentException("Loans exceed the borrowing limit for " + member.getId());
            }
        }
        return state;
    }

    private static String take(Map<String, String> fields, String key) {
        String value = fields.remove(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return value;
    }

    private static int number(Map<String, String> fields, String key) {
        String value = take(fields, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer at " + key, e);
        }
    }

    private static int count(Map<String, String> fields, String key) {
        int value = number(fields, key);
        if (value < 0) {
            throw new IllegalArgumentException("Negative count at " + key);
        }
        return value;
    }

    private static String canonicalId(Map<String, String> fields, String key) {
        String value = take(fields, key);
        if (!value.equals(Identifiers.normalize(value))) {
            throw new IllegalArgumentException("Noncanonical identifier at " + key);
        }
        return value;
    }

    private static LocalDate date(Map<String, String> fields, String key) {
        String value = take(fields, key);
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date at " + key, e);
        }
    }

    private static final class StrictProperties extends Properties {
        @Override
        public synchronized Object put(Object key, Object value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException("Duplicate property key: " + key);
            }
            return super.put(key, value);
        }
    }
}
