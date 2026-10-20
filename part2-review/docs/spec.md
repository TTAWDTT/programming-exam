# spec.md — Fine ledger (FineLedger)

Status: approved by the requirements owner.

## Scope

- In scope: recording fines a student owes, paying them, querying the amount owed and the list of debtors.
- Out of scope: persistence, payment channels, multi-threaded access (this module is used from a single thread only).
- Technical constraints: Java 17 and JUnit 5; no other dependencies. Amounts are always `int` cents.

## Rules

- R1: `addFine(studentId, cents)` records a fine. `studentId` must not be blank; `cents` must be between 1 and 100000, otherwise `IllegalArgumentException`.
- R2: `pay(studentId, cents)` pays a fine. `cents` must be ≥ 1 and **must not exceed the amount currently owed**; a larger payment throws `IllegalArgumentException` and leaves the balance unchanged. Returns the remaining amount owed.
- R3: `owed(studentId)` returns the amount currently owed; a student with no records returns 0.
- R4: `isBlocked(studentId)` is true when the amount owed **reaches** 1000 cents (10 yuan) or more.
- R5: `debtors()` returns every student who owes more than 0, ordered by amount owed, largest first; **students owing the same amount are ordered by student ID ascending**. The returned list is unmodifiable.

## Acceptance criteria

| AC | Rule | Input | Expected result |
|---|---|---|---|
| AC1 | R1 | addFine 0 or 100001 | IllegalArgumentException |
| AC2 | R1 | addFine 1, 100000 | recorded |
| AC3 | R2 | owed 500, pay 300 | returns 200, owed is 200 |
| AC4 | R2 | owed 500, pay 600 | IllegalArgumentException, owed is still 500 |
| AC5 | R2 | owed 500, pay 500 | returns 0, the student no longer appears in debtors() |
| AC6 | R3 | S9 never recorded | owed is 0 |
| AC7 | R4 | owed 999 / 1000 / 1001 | false / true / true |
| AC8 | R5 | S1 owes 300, S2 owes 900, S3 owes 300 | [S2, S1, S3] |
| AC9 | R5 | modifying the list returned by debtors() | UnsupportedOperationException |
