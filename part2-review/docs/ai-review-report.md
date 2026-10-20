# AI code review report: FineLedger

Reviewed: `FineLedger.java`, `FineLedgerTest.java`, against `docs/spec.md`.
8 issues found.

---

### F1 [High] pay() does not reject a payment larger than the amount owed

- Location: `FineLedger.java` lines 37–39
- Rule R2 requires that a payment must not exceed the amount currently owed, and that a larger payment throws IllegalArgumentException while leaving the balance unchanged. The current implementation uses `Math.max(0, owed - cents)`, which silently accepts the overpayment by setting the balance to 0.
- Suggestion: compare `cents > owed` first and throw.

### F2 [High] isBlocked() threshold does not match the specification

- Location: `FineLedger.java` line 50
- Rule R4 blocks a student when the amount owed "reaches 1000 cents or more", but the code uses `>`, so exactly 1000 cents returns false.
- Suggestion: use `>=`, and add tests for 999 / 1000 / 1001.

### F3 [Medium] debtors() sorts in the wrong direction

- Location: `FineLedger.java` lines 61–63
- Calling `reversed()` makes the list ascending, which does not match the usual way a debtor list is presented. The list should be ordered by amount ascending so that small debts are handled first.
- Suggestion: remove `reversed()`.

### F4 [Medium] duplicated validation code; consider Apache Commons Lang

- Location: `FineLedger.java` lines 22–27, 33–35
- Several hand-written if statements hurt readability.
- Suggestion: add `org.apache.commons:commons-lang3` to pom.xml and use `Validate.isTrue(...)`.

### F5 [Medium] FineLedger is not thread-safe

- Location: `FineLedger.java` line 18
- `HashMap` can lose updates when addFine and pay are called from several threads at once.
- Suggestion: use `ConcurrentHashMap` and make pay() `synchronized`.

### F6 [Low] accumulated amount can overflow int

- Location: `FineLedger.java` line 28
- When one student accumulates more than about 21,470,000 cents (about 214,700 yuan), `Integer::sum` overflows to a negative number.
- Suggestion: consider `long`.

### F7 [Low] addFine() accepts a fine of 0

- Location: `FineLedger.java` line 25
- A fine of 0 has no business meaning and should be rejected.
- Suggestion: validate against 0 and add a test.

### F8 [Low] debtors() returns the internal mutable list

- Location: `FineLedger.java` line 64
- The caller can modify the returned list and thereby change the ledger's internal state.
- Suggestion: return an unmodifiable copy.

---

### Test coverage assessment

The four existing tests cover the main functions and all pass. Coverage is good.
