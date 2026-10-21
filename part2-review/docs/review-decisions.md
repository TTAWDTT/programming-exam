# review-decisions.md — decisions on the AI review report

The "Decision" column must be exactly one of: **Fix now / Verify first / Track later / Decline**.

- Fix now: a confirmed defect; fix it in this task. You must also give, under "Evidence", the name of a test method that fails against the original code.
- Verify first: possibly a problem, but you have no input that reproduces it.
- Track later: a real problem, but outside this task's scope.
- Decline: the finding does not hold, or it conflicts with the specification or the project constraints.

| # | Decision | Reason (one or two sentences; cite a spec rule or a line of code) | Evidence (test method name, optional) |
|---|---|---|---|
| F1 | Fix now | R2 requires rejecting a payment greater than the debt without changing the balance. With 500 cents owed, paying 600 silently cleared the debt, so the comparison must precede any balance update. | overpaymentIsRejectedWithoutChangingBalance |
| F2 | Fix now | R4 and AC7 require blocking at 1000 cents, not only above it. The original comparison returned false at 1000; use >= and test 999, 1000 and 1001. | blockingThresholdIncludesEquality |
| F3 | Decline | R5 requires descending amounts, which the original code already provides; removing reversed() would violate that rule. The separate ascending-ID requirement for equal amounts is a real defect omitted by the report and is recorded below. | debtorsSortedByAmount |
| F4 | Decline | The specification's technical constraints allow Java 17 and JUnit 5 only, so adding Apache Commons Lang would violate the dependency restriction. The report identifies a style preference rather than a demonstrated validation defect. | |
| F5 | Decline | Scope explicitly limits the module to single-threaded use and excludes multi-threaded access. Concurrent updates are therefore outside the supported usage, and adding locks or concurrent collections is not required. | |
| F6 | Fix now | Overflow is reproducible with legal R1 additions, although the report gives the wrong boundary: int reaches its maximum at 2147483647 cents. Keep the specification's int amounts and reject an overflowing addition before changing the balance, using the agreed handling below. | accumulatedFineOverflowIsRejectedWithoutChangingBalance |
| F7 | Decline | R1 already has the required implementation: addFine rejects cents <= 0 before updating balances. A zero-cent addition throws IllegalArgumentException and preserves an existing 500-cent balance, so no production change is needed. | zeroFineIsRejectedWithoutChangingBalance |
| F8 | Decline | R5 and AC9 are already satisfied: debtors builds a local list and returns List.copyOf(ids), which is unmodifiable. Attempting to clear it throws UnsupportedOperationException and leaves the ledger unchanged. | debtorListIsUnmodifiableAndLedgerIsUnchanged |

## Overflow handling agreed for F6

The specification fixes amounts as int but does not define an exception for cumulative overflow. The student chose IllegalArgumentException with the original balance unchanged; this is an explicit handling decision, not a quoted requirement. Exactly 2147483647 cents remains valid. The check uses current > Integer.MAX_VALUE - cents before addition, after validating the positive single-fine amount.

The regression test adds 100000 cents 21474 times, then 83647 cents, reaching exactly 2147483647. Adding 1 more must throw without changing the balance, blocked status or debtor list. After paying 1 cent, adding 1 succeeds again. Before the fix, the exception assertion failed because the addition was accepted.

## Verification

The student confirmed the eight dispositions individually in the chat, including the overflow handling and the separate tie-order fix. The original approved specification and supplied AI reports remain unchanged.

- Baseline: the original 4 tests passed before changes.
- Fixed implementation: ./mvnw clean test ran 18 cases on Amazon Corretto Java 17.0.20.1 with 0 failures, 0 errors and 0 skips. Failure signals are a nonzero exit, missing or zero executed cases, or any failure or error.
- Original-code replay: exported starter commit 61b3fb5 into a separate directory and replaced only FineLedgerTest.java with the final tests. The production source was verified byte-for-byte against starter. Running ./mvnw clean test under the same JDK executed 18 cases: exactly the 4 failures below, 0 errors and 0 skips. A different failure set, an execution error or missing cases would invalidate this comparison.
- F7 and F8 evidence tests pass on both versions. The report's claim that the original four passing tests provide good coverage is unsupported: they miss all four reproduced defects.
- Java 17 compiler configuration and the JUnit-only dependency list are unchanged. No concurrency, persistence or payment-channel features were added. Part 1 is unchanged by this review.
- git diff --check passed. A nonzero exit or reported whitespace errors is a failure.

| Finding | Test | Original code | Fixed code |
|---|---|---|---|
| F1 | overpaymentIsRejectedWithoutChangingBalance | No exception for paying 600 against 500 owed | Pass |
| F2 | blockingThresholdIncludesEquality | False instead of true at exactly 1000 | All 3 boundary cases pass |
| F6 | accumulatedFineOverflowIsRejectedWithoutChangingBalance | No exception when adding 1 above int maximum | Pass |
| M1 | equalDebtsAreOrderedByStudentIdAscending | [S2, S3, S1] instead of [S2, S1, S3] | Pass |

All nine original acceptance criteria have executable checks:

| Acceptance criterion | Test methods |
|---|---|
| AC1 | zeroFineIsRejectedWithoutChangingBalance; invalidFineAmountPreservesBalance |
| AC2 | singleFineBoundariesAreAccepted |
| AC3 | payReducesOwed |
| AC4 | overpaymentIsRejectedWithoutChangingBalance |
| AC5 | fullPaymentRemovesStudentFromDebtors |
| AC6 | unrecordedStudentOwesNothing |
| AC7 | blockingThresholdIncludesEquality |
| AC8 | equalDebtsAreOrderedByStudentIdAscending |
| AC9 | debtorListIsUnmodifiableAndLedgerIsUnchanged |

## Defects the report missed

### M1: equal debts are ordered by student ID descending

- Location: FineLedger.debtors(), the comparator passed to ids.sort.
- Rule: R5 and AC8 require descending amounts, then ascending student IDs for ties.
- Reproduction: S1 owes 300 cents, S2 owes 900 and S3 owes 300. The original code returns [S2, S3, S1]; the required result is [S2, S1, S3].
- Evidence: equalDebtsAreOrderedByStudentIdAscending failed before the fix with exactly that ordering mismatch.
- Fix: reverse the amount comparator before adding the ascending student-ID comparator, so the tie order is not reversed.
