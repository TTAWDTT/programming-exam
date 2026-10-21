# Library borrowing design

Status: reviewed and approved by the student on 2026-10-21. Part 1 is implemented. Verification results are recorded in verification.md.

## Public API

Preserve the examination's signatures:

```java
public Library()
public void addMember(Member member)
public void addBook(String isbn, int copies)
public void borrow(String memberId, String isbn, LocalDate date)
public int returnBook(String memberId, String isbn, LocalDate date)
public int borrowedCount(String memberId)
```

Approved additions:

```java
public Library(FinePolicy finePolicy)
public void updateMember(Member member)
public static Library create(Path path)
public static Library create(Path path, FinePolicy finePolicy)
public static Library load(Path path)
public static Library load(Path path, FinePolicy finePolicy)
```

Constructors create independent in-memory instances. Factories create or load file-backed instances. Overloads without a policy select the default capped policy. No manual save operation is required. Exceptions are unchecked, so the fixed signatures need no additional checked exceptions.

## Classes

Production code belongs to edu.example.library. Support types can be package-private.

| Class or interface | Kind | Responsibility | Relations |
|---|---|---|---|
| Library | class | Validate operations, coordinate loans and publish completed changes | Uses Member, FinePolicy, LibraryState and optional SnapshotStore |
| Member | abstract class | Hold immutable normalized ID; define limit and period accessors | Parent of Undergraduate and Postgraduate |
| Undergraduate | class | Undergraduate(String id); limit 5, period 14 | Extends Member |
| Postgraduate | class | Postgraduate(String id); limit 10, period 28 | Extends Member |
| Identifiers | utility | Normalize and validate IDs | Used by Member, Library and file validation |
| FinePolicy | interface | int calculateFine(long overdueDays); nonnegative input and output | Used by Library |
| CappedDailyFinePolicy | class | Default 50 cents per day, cap 1000 | Implements FinePolicy |
| LoanKey | record | Normalized member ID and ISBN | Key for one active loan |
| Loan | immutable record | IDs, borrow date, due date and period at borrowing | Owned by LibraryState |
| LibraryState | class | Own maps; build an independent candidate copy | Used by Library and file codec |
| SnapshotStore | interface | Create, load and atomically save snapshots | Implemented by FileSnapshotStore; replaceable in tests |
| FileSnapshotStore | class | Own path, encode and validate files, atomically publish snapshots | Implements SnapshotStore using JDK APIs |
| LibraryException and subclasses | unchecked exceptions | Identify borrowing-rule violations | Thrown by Library |
| LibraryStorageException | unchecked exception | Identify storage failure with operation, path and cause | Separate from LibraryException |

Members are immutable. A type update replaces a member object; loans refer to IDs, not the old object. Library gets the limit and period from the member, without hardcoded type checks. The file codec alone maps the supported member classes to stored type codes.

## Why an abstract class for Member and an interface for FinePolicy

Members share ID state and validation, while subclasses provide different limits and periods. An abstract class holds the shared state and requires those differences.

A fine policy describes a calculation capability without needing shared member state. An interface supports replacement through constructor or factory injection. Default construction selects CappedDailyFinePolicy; the return algorithm uses only FinePolicy. No policy setter or serialization of executable objects is needed.

## Exceptions

LibraryException extends RuntimeException. Use concrete subclasses for business violations.

| Exception | Thrown when | Rule |
|---|---|---|
| IllegalArgumentException | Invalid input, unknown referenced ID, duplicate registration, invalid date or stock overflow | R2-R5, R7, R10, R13 |
| LibraryException | Base for the specific borrowing exceptions below | R5, R6, R8, R9, R12 |
| DuplicateLoanException | Same member already holds the ISBN | R8 |
| BorrowLimitException | Borrow at or above the current limit, or update with a count above the new limit | R5, R6 |
| NoCopyAvailableException | Available copies are 0 | R9 |
| LoanNotFoundException | Return has no matching active loan | R12 |
| IllegalStateException | Injected policy returns negative cents | R16 |
| LibraryStorageException | File creation, loading, validation or saving fails | R19-R23 |

Storage failure is operational, not a borrowing violation. Preserve its underlying IOException or parsing cause where available. Identify the operation and path; malformed data errors identify the field or record without dumping the full file.

## Collections

| Data | Collection and generic types | Reason |
|---|---|---|
| Members | Map<String, Member>, backed by HashMap | Unique normalized IDs and direct lookup or replacement |
| Book totals | Map<String, Integer>, backed by HashMap | One total per ISBN; no Book class needed for these fields |
| Active loans | Map<LoanKey, Loan>, backed by HashMap | Direct return lookup and unique member-ISBN pairs |

Count loans for a member to implement borrowedCount. Count loans for an ISBN and subtract from the total to calculate availability. Avoid cached counters that require separate updates and persistence.

Copy maps before mutation. Immutable Member, LoanKey and Loan values can be shared between copies. Do not expose mutable maps. Business behavior does not depend on HashMap iteration order. Sort records by normalized IDs and loan keys when saving for predictable files.

## Operation flow

For borrow, normalize and validate inputs and references, then calculate the candidate due date. Check duplicates, the current limit and availability in that order. Add an immutable Loan with the original period and due date to a candidate state, then commit it.

For updateMember, find the existing normalized ID. If the type is unchanged, return without saving. Otherwise compare the active loan count with the replacement member's borrowingLimit. If the count is greater, throw BorrowLimitException before constructing or saving candidate state. Equality is allowed. Replace the member in a candidate state without changing loans or their original due dates.

For returnBook, validate input and references, find the loan and check the return date. Use the stored due date with ChronoUnit.DAYS to calculate nonnegative overdue days. Call FinePolicy and reject negative output. Remove the loan from a candidate state, commit it, then return the fine.

The default policy uses long arithmetic. At 20 or more overdue days, return 1000 immediately; otherwise return overdueDays * 50 as int cents. No multiplication of a potentially huge long value is needed. Test replacement with a simple alternative FinePolicy; the default implementation need not expose configurable rates or caps.

## File persistence

### Format and validation

Approved format: versioned UTF-8 Java properties, read and written with Reader and Writer. This format is new; the starter does not define one.

| Record | Fields |
|---|---|
| Header | format.version=1; member.count; book.count; loan.count |
| Member i | member.i.id; member.i.type as UNDERGRADUATE or POSTGRADUATE |
| Book i | book.i.isbn; book.i.total |
| Loan i | loan.i.memberId; loan.i.isbn; loan.i.borrowedOn; loan.i.dueOn; loan.i.periodDays |

Indices start at 0. Counts are nonnegative. Stored IDs must already be canonical. Totals are positive int values. Loan references must exist and loan keys must be unique. Loans per ISBN cannot exceed total copies. Dates use ISO text; the original period is 14 or 28, and dueOn equals borrowedOn plus that period.

Reject any member whose active loan count exceeds the current borrowingLimit. Do not compare the original period with the current type: a valid update preserves the terms set at borrowing. A postgraduate who changes to undergraduate status at 5 loans keeps the existing 28-day due dates. New loans use 14 days.

Reject missing or unexpected fields, duplicate property keys, duplicate logical records, unknown types, unsupported versions and malformed values. Standard Properties loading overwrites duplicate keys, so detect repeated keys explicitly. Check declared record counts against actual records before constructing state.

The file stores no fine-policy code or configuration. Reload uses the default unless the caller supplies a replacement. This accepted decision keeps persistence independent of arbitrary policy implementations. File validation checks consistency, not authenticity; untrusted borrowers must not have file write access.

### Creation and loading

Reject null paths or a blank path string with IllegalArgumentException. Resolve a supplied path once to an absolute normalized path; do not apply identifier case conversion to filesystem paths. Require an existing parent. Directory targets and file-operation failures produce LibraryStorageException.

create rejects an existing target, encodes an empty snapshot, and atomically publishes it before returning. load rejects missing files and validates the full snapshot before returning an instance. It never rewrites or repairs the source.

### Automatic saving and failure handling

1. Validate the mutation and prepare an independent candidate state.
2. Encode the complete snapshot without touching the original file.
3. Write and close a temporary file in the same directory.
4. Atomically publish that file using the supported local filesystem.
5. Assign the already prepared candidate as the live state and return success.

No further fallible business computation or I/O occurs after publication. Earlier failures preserve old memory and old file bytes. Attempt to remove temporary files without hiding the original error. Unsupported atomic publication is an error, not a reason to fall back to unsafe overwriting.

Memory mode uses the same candidate-state flow without a store. Queries and same-type updates never save. One active writer owns each path. Concurrent writers, external file edits, process crashes and hardware failures are outside this operating model. Reload checks use the new instance for subsequent mutations or keep it read-only.

This design provides consistency for the stated model; it does not promise database transactions or durability through power loss.

## Tests

Use JUnit 5, fixed LocalDate values and exact cents, counts and exception types. Map tests to R numbers or AC rows.

LibraryTest covers identifiers, registration, updates, both borrowing limits, stock, check priority, returns and failed-operation state. FinePolicyTest covers default-policy boundaries, including overdue days 0, 1, 19, 20, 21 and 2147483648. LibraryTest.customPolicyIsUsed and FileLibraryTest.customPolicyIsExplicitOnReload cover replacement policies. FileLibraryTest uses @TempDir for creation, loading, round trips, invalid files and automatic saving.

The implementation provides a package-private SnapshotStore injection point for deterministic save-failure tests. Also test the real file writer with a failure injected immediately before atomic publication. Do not rely only on permissions, which may behave differently under elevated privileges. After failure, assert the previous file bytes and relevant member, stock and loan state.

## Examination requirements check

| Requirement | Coverage | Current status |
|---|---|---|
| Fixed constructor and five methods | Public API preserves all signatures | Implemented; API test passed |
| Abstract Member and two ID constructors | Classes and member design | Implemented and tested |
| Member differences in subclasses | Limit and period accessors | Implemented and tested |
| FinePolicy and implementation | FinePolicy and CappedDailyFinePolicy | Implemented and tested |
| LibraryException with at least two subclasses | Four borrowing exceptions | Implemented and tested |
| Generic collections and reasons | Collections section | Documented |
| Numbered rules, assumptions and concrete boundaries | spec.md R1-R24, AC01-AC53 | Reviewed and approved |
| Tasks with files and checks | tasks.md | Reviewed and approved |
| Actual AI requests and decisions | decisions.md | Specification decisions approved; student confirmed the Chinese explanation of key test expectations and the latest decision record |
| Java 17, JUnit 5, no added dependencies | Existing pom.xml and JDK-only design | Java 17 clean build and all 100 tests passed |
| spec before impl before review | tasks.md | spec committed before implementation; Part 2 review remains pending |

A clean build executed 100 JUnit test cases on Amazon Corretto 17.0.20.1 with no failures, errors or skips. File tests exercise round trips, invalid snapshots and injected save failures. The tests verify the stated single-writer operating model; they do not establish power-loss durability. See verification.md for the examination checklist and acceptance-to-test mapping.
