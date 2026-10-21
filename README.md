# Programming Exam starter project

- `part1-library/` — Part 1, library borrowing. Write your spec, tasks, design and decisions in `docs/`, and your implementation and tests in `src/`.
- `part2-review/` — Part 2, review and fix AI-written code. Read the three files in `docs/` first, then fill in `docs/review-decisions.md`.

Running the tests needs JDK 17 or newer:

```bash
cd part1-library        # or part2-review
./mvnw test             # Windows: mvnw.cmd test
```

The first run downloads Maven and JUnit and needs a network connection (about 1 minute). If you installed the offline kit beforehand, it will work without a network.

This folder is itself a git repository. Commits are stored only on your computer, in the hidden `.git` folder; no GitHub account is needed. See "Submission" in the exam paper for how to commit and zip.

## Part 1 usage

The public classes are in `edu.example.library`. `new Library()` creates an empty in-memory library and does not access a data file. Member IDs and ISBNs ignore case and outer whitespace.

```java
import edu.example.library.*;
import java.time.LocalDate;

Library library = new Library();
library.addMember(new Undergraduate("S1"));
library.addBook("B1", 2);
library.borrow("S1", "B1", LocalDate.of(2026, 3, 1));
int fine = library.returnBook("S1", "B1", LocalDate.of(2026, 3, 16));
// fine is 50 cents: the due date was March 15.
```

`updateMember(new Postgraduate("S1"))` changes an existing member's type. Existing loan dates remain fixed; new loans use the new period and limit. If the active loan count exceeds the new type's limit, the update throws BorrowLimitException without changing state or saving. Updating at exactly the new limit is allowed, but another borrow requires returning a book first.

## Part 1 file mode

Use an explicit path whose parent directory already exists. `create` refuses an existing target; `load` requires an existing valid file. Replace the old instance after loading so that one writer owns a file.

```java
import edu.example.library.*;
import java.nio.file.Path;
import java.time.LocalDate;

Path file = Path.of("library.properties");
Library library = Library.create(file); // First use only; the target must not exist.
library.addMember(new Undergraduate("S1"));
library.addBook("B1", 1);
library.borrow("S1", "B1", LocalDate.of(2026, 3, 1));

library = Library.load(file); // For subsequent use, load instead of creating again.
int fine = library.returnBook("S1", "B1", LocalDate.of(2026, 3, 16));
// fine is 50 cents; the loan was restored with its original due date.
```

Each effective mutation is saved automatically. Queries and same-type member updates do not write. A failed save throws `LibraryStorageException` and preserves the previous in-memory state and original file. Invalid or incomplete files are rejected entirely. Do not edit the file while a library instance is using it.

Files contain versioned UTF-8 properties for members, book totals and active loans. A local filesystem supporting atomic replacement is required. There is no non-atomic fallback, concurrent-writer support or guarantee against power loss.

The default fine policy charges 50 cents per overdue day, capped at 1000 cents per loan. To replace it, supply a `FinePolicy` to the constructor or `create` or `load` overload. Policies are not stored in files: `load(path)` always uses the default, while `load(path, policy)` uses the supplied policy.

Dates are supplied by a trusted calling system or administrator. There is no login, payment system or database. Part 2 remains independent. See the English specification and design under `part1-library/docs` for the complete contract and acceptance examples.
