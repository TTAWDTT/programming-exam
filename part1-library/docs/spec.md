# Library borrowing specification

Status: reviewed and approved by the student on 2026-10-21, including the technical decisions. Implementation has not started.

## Goal

Manage library members, book copies and loans, calculate return fines, and optionally preserve the current state in a local file.

## Scope

In scope: undergraduate and postgraduate members; registration and type updates; adding copies; borrowing, returning and current loan counts; replaceable fine policies; independent in-memory instances; explicit creation and loading of file-backed instances; automatic saving after effective changes.

Out of scope: a user interface, authentication, databases, payments, debt accounts, reservations, renewals, notifications, historical reports, concurrent access, multiple writers to one file and power-loss durability. Part 2's FineLedger is a separate project.

The caller is a trusted system or administrator and controls access and business dates. Borrowers must not be allowed to choose dates directly. Library does not compare supplied dates with the system clock.

Use Java 17 and JUnit 5 with no other dependencies. Preserve the examination's public Library constructor and five method signatures. File storage uses the Java standard library.

## Rules

- R1: Undergraduate has a limit of 5 active loans and a period of 14 days. Postgraduate has a limit of 10 and a period of 28 days. Each subclass has a constructor taking the member ID.
- R2: Normalize every member ID and ISBN by removing outer whitespace and converting to uppercase using Locale.ROOT. Preserve internal whitespace. IDs are opaque strings; ISBN checksum validation is not required.
- R3: Null required arguments, empty normalized IDs, unknown referenced members and books, and copies outside 1 through 2147483647 cause IllegalArgumentException. Registration may introduce new IDs; queries, borrowing, returning and updating require the relevant existing records.
- R4: addMember registers a member with 0 active loans. Reject any already registered normalized ID with IllegalArgumentException, including the same member type.
- R5: updateMember changes the type of an existing normalized member ID and preserves every loan and due date. An unknown ID causes IllegalArgumentException. Updating to the same type succeeds without changing state.
- R6: A new borrow requires an active count below the member's current limit; otherwise throw BorrowLimitException. A downgrade may leave the count above the new limit. Existing loans remain valid; new borrowing must wait until the count is below the limit.
- R7: addBook adds copies to the total, including an already registered ISBN. Preserve active loans. If the resulting total would exceed 2147483647, throw IllegalArgumentException without changing the total.
- R8: Each member may have at most 1 active loan for an ISBN. Repeating it causes DuplicateLoanException. Borrowing it again after return is allowed; different members may borrow different copies.
- R9: Available copies equal total copies minus active loans for that ISBN. A successful borrow consumes 1 copy; a successful return releases 1. With 0 available copies, borrow causes NoCopyAvailableException. borrowedCount returns the member's active loan count.
- R10: Borrowing day is day 0. Calculate and store the due date as the supplied borrow date plus the member's period at borrowing. Store the original period too. Accept past and future dates; reject a borrow whose due date cannot fit LocalDate with IllegalArgumentException.
- R11: For borrow, check valid arguments and referenced records, including a representable due date, then duplicate borrowing, the member limit, and inventory, in that order. There is no precedence promise between two invalid arguments; both yield IllegalArgumentException.
- R12: A return with a registered member and ISBN but no matching active loan causes LoanNotFoundException. This includes a repeated return.
- R13: A return before its loan's borrow date causes IllegalArgumentException. Same-day return is valid. Return checks arguments and referenced records, finds the loan, then checks the date against that loan.
- R14: Default fines are 50 cents per overdue day, capped at 1000 cents per returned loan. Before or on the due date, the fine is 0. Calculate min(overdueDays * 50, 1000) without overflow, where overdueDays is a nonnegative calendar-day difference. A successful return removes the loan and returns int cents; it does not record a payment or debt.
- R15: An overdue active loan does not itself block another borrow. The ordinary input, duplicate, limit and inventory checks still apply.
- R16: Library depends on FinePolicy. A replacement policy can be supplied at construction, creation or loading without editing Library. Policies receive nonnegative long overdue days and return nonnegative int cents. A null policy causes IllegalArgumentException; a negative result causes IllegalStateException before changing return state.
- R17: Any failed mutation preserves all members, book totals and loans. In file mode it also preserves the original file. This includes validation, business-rule, policy and saving failures.
- R18: new Library() creates an empty independent in-memory instance with the default policy and performs no file I/O. File-backed instances require a caller-supplied path.
- R19: create(path) requires a nonexistent target and an existing parent directory. It saves a valid empty snapshot before returning. It never overwrites an existing target. Failure produces LibraryStorageException and no usable instance; any pre-existing file is unchanged.
- R20: load(path) reads an existing file without rewriting it. Missing, unreadable, unsupported or invalid files cause LibraryStorageException identifying the operation, path and cause. Never substitute empty or partially loaded data; preserve the original file.
- R21: Save normalized member IDs and types, ISBNs and total copies, and each loan's member ID, ISBN, borrow date, due date and original period. Reject malformed fields, duplicate records, invalid references, nonpositive totals, more active loans than copies and inconsistent dates. Allow counts above the member's current limit and original periods different from the current type.
- R22: In file mode, automatically save each effective registration, type change, copy addition, borrow and return. Publish candidate memory state only after saving succeeds. Queries, loading and same-type updates do not write. Report a successful return's fine only after saving succeeds.
- R23: Saving failure causes LibraryStorageException. Write a full candidate to a temporary file in the same directory, then publish it atomically. Never truncate the original first or use a non-atomic fallback. Unsupported atomic publication fails without changing the live state or original file. This guarantee assumes one writer and a supported local filesystem; hardware loss and crash durability are outside scope.
- R24: Save business data, not executable policy objects. create(path) and load(path) select the default policy. Explicit overloads accept a custom policy, which the caller must supply again on reload. Policy selection never changes stored due dates.

## Acceptance criteria

Each row is independent unless it describes a sequence. U means Undergraduate, P means Postgraduate. Dates are ISO LocalDate values. For ordinary borrowing and return scenarios, first register the referenced members and books with sufficient copies unless the row specifies otherwise. Registration, missing-record and file-creation scenarios use their explicit setup instead. Each exception assertion uses the exact type. Every failed mutation also checks R17. Inventory can be verified by subsequent borrowing; no extra public inventory getter is required.

| AC | Rule | Concrete input or setup | Expected result |
|---|---|---|---|
| AC01 | R1 | Construct U("S1") and P("S2") | Limits 5 and 10; periods 14 and 28 |
| AC02 | R2, R4 | Register U(" s1 "), then P("S1") | Second call throws IllegalArgumentException; original member remains |
| AC03 | R2, R9 | Register U("S1"); addBook(" b1 ", 1); borrow(" s1 ", "B1", 2026-03-01) | borrowedCount("s1") is 1 |
| AC04 | R2 | Register U("S 1") and U("S1") | Two distinct members, each with count 0 |
| AC05 | R3 | Separately pass a null member, null date, null ID, ID "   ", ISBN "", copies 0 or -1 | IllegalArgumentException in each case |
| AC06 | R3 | Query or borrow with absent S99; borrow or return absent B99; other arguments valid | IllegalArgumentException in each case |
| AC07 | R4 | Register U("S1") twice | First call gives count 0; second throws IllegalArgumentException |
| AC08 | R5, R10 | U S1 borrows 2026-03-01; update to P; return on 2026-03-16 | Existing due date remains 2026-03-15; fine 50 |
| AC09 | R5, R10 | Register U S1; add B2 with 1 copy; update S1 to P; borrow B2 on 2026-03-10; return 2026-04-07 | New due date 2026-04-07; fine 0 |
| AC10 | R5 | updateMember(P("S99")) with S99 absent | IllegalArgumentException; S99 is not created |
| AC11 | R5, R22 | Update P S1 to P S1 with 1 loan in file mode | Count, dates and file bytes unchanged; no write or error |
| AC12 | R6 | U S1 holds 4 distinct books; borrow available B5, then B6 | B5 succeeds, count 5; B6 throws BorrowLimitException, count still 5 |
| AC13 | R6 | P S2 holds 9 distinct books; borrow available B10, then B11 | B10 succeeds, count 10; B11 throws BorrowLimitException |
| AC14 | R5, R6 | P S1 has 8 loans; update to U; try a new ISBN at counts 8, 5 and 4 after intervening returns | Update succeeds; attempts at 8 and 5 fail with BorrowLimitException; attempt at 4 succeeds, count 5 |
| AC15 | R7 | Add B1 with 2 copies; borrow 1; add B1 with 3 copies | Total 5, active 1, available 4 |
| AC16 | R3, R7 | Add B1 with 1 copy; add B2 with 2147483647; add B2 with 1 more | First two succeed; last throws IllegalArgumentException; B2 total unchanged |
| AC17 | R8 | B1 has 2 copies; S1 borrows B1 twice | Second call throws DuplicateLoanException; count 1 |
| AC18 | R8, R9 | B1 has 2 copies; S1 and S2 each borrow 1 | Both succeed; each count 1; available 0 |
| AC19 | R8, R9 | Borrow B1, return the same day, then borrow B1 again | Fine 0; second borrow succeeds; final count 1 |
| AC20 | R9 | B1 has 1 copy; S1 borrows it; S2 tries before and after S1 returns it | First attempt throws NoCopyAvailableException; later attempt succeeds |
| AC21 | R10, R14 | U borrows 2026-03-01; separate returns on 2026-03-14, 2026-03-15, 2026-03-16 | Due 2026-03-15; fines 0, 0, 50 |
| AC22 | R10, R14 | P borrows 2026-03-01; separate returns on 2026-03-28, 2026-03-29, 2026-03-30 | Due 2026-03-29; fines 0, 0, 50 |
| AC23 | R10 | U borrows 2000-01-01 or 2099-01-01 in separate libraries | Both accepted; due 2000-01-15 and 2099-01-15, independent of the clock |
| AC24 | R10, R17 | Valid U and book; borrow on LocalDate.MAX | IllegalArgumentException; count 0; copy remains available |
| AC25 | R11 | U holds 5 loans including B1; B1 has no stock; request B1 | DuplicateLoanException before limit and stock checks |
| AC26 | R11 | U holds 5 loans; request different B6, registered but with all copies borrowed | BorrowLimitException before stock check |
| AC27 | R11 | U holds 5 loans; request unregistered B99 | IllegalArgumentException before limit check |
| AC28 | R12 | Return registered B1 without a loan; separately, return an already returned loan | LoanNotFoundException; no extra available copy |
| AC29 | R13 | Borrow 2026-03-10; return 2026-03-09 | IllegalArgumentException; loan and inventory unchanged |
| AC30 | R13, R14 | Borrow and return 2026-03-10 | Fine 0; count changes from 1 to 0 |
| AC31 | R14 | U borrows 2026-03-01; separate returns 2026-04-03, 2026-04-04, 2026-04-05 | 19, 20, 21 overdue days; fines 950, 1000, 1000 |
| AC32 | R14 | Two U loans borrowed 2026-03-01; return each on 2026-04-05 | Each costs 1000; cap is per loan |
| AC33 | R14 | Default policy receives 2147483648 overdue days | Exactly 1000 cents; no int overflow |
| AC34 | R15 | U has 2 loans, one due 2026-03-15; borrow a new available ISBN on 2026-03-20 | Success; count 3 |
| AC35 | R16 | Inject 20-cent-per-day policy; U borrows 2026-03-01, returns 2026-03-17 | Fine 40 without changing Library |
| AC36 | R16, R17 | Null policy; separately, a policy returning -1 during return | IllegalArgumentException for null; IllegalStateException for -1; active loan preserved |
| AC37 | R17 | A policy throws IllegalStateException during a file-backed return | Exception propagates; all live state and file bytes unchanged |
| AC38 | R18 | Create two new Library() instances; register S1 only in the first | First returns count 0; second rejects S1 as unknown; no file I/O |
| AC39 | R19 | create a nonexistent library.properties in an existing temporary directory | Empty valid file created; load restores 0 members, 0 books and 0 loans |
| AC40 | R19 | create at an existing file | LibraryStorageException; original bytes unchanged |
| AC41 | R19, R23 | Missing parent directory or injected initial-save failure | LibraryStorageException; no successful instance or published target snapshot |
| AC42 | R20 | load nonexistent library.properties | LibraryStorageException identifying load and path; no file created |
| AC43 | R20 | File version 99, or unreadable file | LibraryStorageException with useful cause; original file unchanged |
| AC44 | R20, R21 | Duplicate S1; loan referencing absent S99; or 2 B1 loans with total 1 | Each entire load rejected with LibraryStorageException; no rewrite |
| AC45 | R21 | P S1 has 8 loans; update to U, save and reload | Load succeeds; count 8; new borrowing throws BorrowLimitException |
| AC46 | R21 | U borrows 2026-03-01, upgrades to P, reloads and returns 2026-03-16 | Stored due date 2026-03-15; fine 50 |
| AC47 | R21 | Stored borrow date 2026-03-01, period 14, due date 2026-03-16 | Load rejected; expected due date is 2026-03-15 |
| AC48 | R22 | Register, add 2 B1 copies, borrow 1, change type, return; reload after each successful mutation | Every completed mutation survives reload; final count 0 and available copies 2 |
| AC49 | R17, R23 | S1 has 0 loans and B1 has 1 copy; inject borrow-save failure | LibraryStorageException; live count 0, available 1, original bytes unchanged |
| AC50 | R17, R23 | Inject saving failure separately for registration, update, adding copies and return | LibraryStorageException; all live state and original file bytes unchanged; return does not report success |
| AC51 | R23 | Inject AtomicMoveNotSupportedException before publication | LibraryStorageException; no unsafe fallback; old memory and file unchanged |
| AC52 | R20, R22 | Load valid snapshot; query borrowedCount 3 times | Identical count each time; file bytes unchanged |
| AC53 | R24 | Same pre-return snapshot, custom policy 20 per day versus default; return 2 days late in independent runs | Custom fine 40; default fine 100; same stored due date |

## Assumptions

Confirmed entries record decisions made during the discussion. Accepted after review entries were initially technical proposals and were approved by the student on 2026-10-21.

| Open point | Decision | Reason | Rule | Status |
|---|---|---|---|---|
| Fine rate and cap | 50 cents per day, maximum 1000 per loan | Limit long-overdue charges | R14 | Confirmed |
| Repeated ISBN borrowing | One active copy per member | Return identifies a loan by member and ISBN | R8 | Confirmed |
| Repeated book addition | Accumulate copies | Support restocking without replacing loans | R7 | Confirmed |
| Duplicate member registration | Reject duplicate normalized ID | Avoid accidental replacement | R4 | Confirmed |
| Type changes | Separate updateMember | Support status changes explicitly | R5 | Confirmed |
| Existing loans after updates | Preserve original due dates | Do not change agreed loan terms retroactively | R5, R10 | Confirmed |
| Downgrade above new limit | Allow update; constrain new borrowing | Keep identity accurate without cancelling loans | R6 | Confirmed |
| Unknown update ID | Reject; do not create | Separate registration and modification | R5 | Confirmed |
| Same-type update | Succeed without changes | Repeated updates have no extra effect | R5, R22 | Confirmed |
| Return without a loan | LoanNotFoundException; unchanged state | Detect wrong or repeated returns | R12, R17 | Confirmed |
| Return before borrowing | Reject; permit same-day return | Keep dates consistent | R13 | Confirmed |
| Whitespace and case | Strip outer whitespace; ignore case | Treat equivalent inputs consistently | R2 | Confirmed |
| Overdue members | Allow further borrowing under ordinary checks | No extra suspension rule | R15 | Confirmed |
| Date authority | Trusted caller provides dates | Caller controls borrower permissions | R10, Scope | Confirmed |
| Simultaneous borrow violations | Input, duplicate, limit, inventory | Deterministic errors | R11 | Confirmed |
| Failed operations | Preserve all state | Avoid partial changes | R17 | Confirmed |
| Persistence | Local files, no database | Restore records using only the JDK | R18-R24 | Confirmed |
| Automatic saving | Save effective mutations; saving failure fails operation | Avoid unsaved successful changes | R17, R22, R23 | Confirmed |
| Missing file | Explicit create and load; missing load is an error | Avoid silently replacing lost data | R19, R20 | Confirmed |
| Invalid file | Reject entirely; preserve original | Avoid dropping records | R20, R21 | Confirmed |
| File location and default constructor | Explicit paths; no-argument constructor in memory | Make storage explicit and tests independent | R18 | Confirmed |
| Numeric and date limits | Reject stock overflow and unrepresentable due dates | Prevent wrapped totals or dates | R3, R7, R10 | Accepted after review |
| Exact ID normalization | String.strip, Locale.ROOT uppercase, preserve internal spaces, no ISBN checksum | Define reproducible comparisons | R2 | Accepted after review |
| Creation details | Save empty file immediately; require existing parent; never overwrite existing target | Establish usable storage safely | R19 | Accepted after review |
| Storage exception type | LibraryStorageException separate from business exceptions | Distinguish operational failures | R20, R23 | Accepted after review |
| Snapshot validation and file ownership | Validate whole snapshot; one writer; atomic publication | Keep memory and disk consistent | R21, R23 | Accepted after review |
| Custom policy after restart | Do not serialize policies; caller re-supplies custom policy | Avoid coupling storage to executable implementations | R16, R24 | Accepted after review |
| Invalid custom policy | Reject null or negative results; preserve state on policy failure | Maintain the fine contract | R16, R17 | Accepted after review |

## Boundary review

All R1-R24 rules have concrete examples. The 8 applicable edge categories are numeric boundaries, equality, empty inputs, text normalization, check ordering, overflow, repeated operations and storage effect order. All have explicit accepted contracts and acceptance examples: 8 applicable, 8 resolved in the specification, 0 unspecified, 0 backstop tags and 0 judgment tags. The student has approved the technical decisions.

The prohibition review retains the trusted-date boundary in Scope and R10. This internal utility adds no other user-facing value or fairness policy. Authentication, concurrent writers and crash-durable storage are explicit scope exclusions.
