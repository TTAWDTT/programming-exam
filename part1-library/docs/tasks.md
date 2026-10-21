# Part 1 tasks

Status: documentation reviewed and approved by the student on 2026-10-21. Implementation tasks are not complete. Source and test filenames below are in the edu/example/library package under src/main/java and src/test/java.

- [ ] T1: Complete the spec commit before implementation. Document review is complete; the commit is pending.
  - Files: docs/spec.md, docs/design.md, docs/tasks.md, docs/decisions.md.
  - Check: each R1-R24 rule has an acceptance example; each open point has an Assumptions entry; required signatures match the examination. Run git log --oneline from the project root after committing. Failure signals: missing mapping, signature mismatch or no spec commit above starter. Student review is complete; the spec commit remains pending.
- [ ] T2: Implement normalized immutable members, loan records and state collections.
  - Files: Member.java, Undergraduate.java, Postgraduate.java, Identifiers.java, Loan.java, LoanKey.java, LibraryState.java; LibraryTest.java.
  - Check: memberTypesHaveRequiredLimitsAndPeriods; identifiersIgnoreCaseAndOuterWhitespace. Failure signals: failed assertions or named tests absent from the report.
- [ ] T3: Implement registration, type updates and additive inventory.
  - Files: Library.java, LibraryState.java; LibraryTest.java.
  - Check: duplicateRegistrationIsRejected; unknownMemberCannotBeUpdated; downgradePreservesExistingLoans; sameTypeUpdateIsNoOp; addingCopiesAccumulatesWithoutOverflow. Failure signals: failed assertions or named tests absent.
- [ ] T4: Implement borrowing and business exceptions.
  - Files: Library.java, LibraryException.java, DuplicateLoanException.java, BorrowLimitException.java, NoCopyAvailableException.java, LoanNotFoundException.java; LibraryTest.java.
  - Check: undergraduateFifthAndSixthLoans; postgraduateTenthAndEleventhLoans; duplicatePrecedesLimitAndInventory; overdueLoanDoesNotBlockBorrowing. Failure signals: wrong count, date or exception assertion, or named tests absent.
- [ ] T5: Implement policies and returns.
  - Files: FinePolicy.java, CappedDailyFinePolicy.java, Library.java; FinePolicyTest.java, LibraryTest.java.
  - Check: fineAtDueDateBoundaries; fineAtCapBoundaries; fineHandlesLargeOverdueDays; customPolicyIsUsed; returnBeforeBorrowIsRejected; repeatReturnIsRejected; typeUpdateDoesNotChangeDueDate. Failure signals: wrong cents, exception or state assertion, or named tests absent.
- [ ] T6: Implement snapshot encoding, validation, creation and loading.
  - Files: SnapshotStore.java, FileSnapshotStore.java, LibraryStorageException.java, Library.java; FileLibraryTest.java.
  - Check: createDoesNotOverwriteExistingFile; loadMissingFileFails; malformedSnapshotIsRejected; downgradedMemberAboveLimitCanReload; storedDueDateSurvivesReload. Failure signals: invalid file accepted, changed original bytes, incorrect restored values or named tests absent.
- [ ] T7: Implement atomic automatic saving and failure recovery.
  - Files: FileSnapshotStore.java, Library.java; FileLibraryTest.java.
  - Check: everyMutationSurvivesReload; failedSavePreservesMemoryAndFile; unsupportedAtomicMoveFailsSafely; readsAndNoOpUpdateDoNotWrite. Test every mutation separately. Failure signals: incorrect live state or file bytes after failure, missing saved changes, unwanted writes or named tests absent.
- [ ] T8: Check code, tests and documents together, then make the impl commit.
  - Files: all Part 1 source, tests and docs; README.md if actual file-mode usage needs documenting.
  - Check: run ./mvnw test in part1-library. Require BUILD SUCCESS, a nonzero executed-test count and zero failures and errors. Failure signals: nonzero exit, missing or zero executed tests, or any failure or error. Verify AC01-AC53 coverage and exact exception types. Run git log --oneline to confirm impl follows spec. The starter's one creation test is not enough.
  - Update docs/decisions.md with the actual implementation requests, decisions and checks.

After Part 1, complete Part 2 and commit review. Before submission, preserve .git and confirm chronological order spec, impl, review. The student reviews the final record and prepares to explain the submitted work. This list does not claim completed implementation, passed tests or new commits.
