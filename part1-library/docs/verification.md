# Part 1 verification

Verified on 2026-10-21 against the five-page examination, the approved specification and the actual source and tests. This report records the checks supporting the student-authorized impl commit. It is not a prediction of an examiner's score.

## Build and tests

- Baseline: ./mvnw test passed the starter's 1 test before implementation.
- Final clean build: ./mvnw clean test passed 100 tests, with 0 failures, 0 errors and 0 skips. Run from part1-library. Failure signals are a nonzero exit, missing or zero executed tests, or any reported failure or error.
- LibraryTest: 41 executed cases. FinePolicyTest: 8. FileLibraryTest: 51.
- Runtime: Amazon Corretto 17.0.20.1, build 17.0.20.1+12-LTS, macOS aarch64. Both Maven and all three Surefire reports identify Java 17.0.20.1. The clean build used the unchanged maven.compiler.release=17 setting; all production class files have major version 61. The same 100 cases also passed on Temurin JDK 21 before the Java 17 clean build.
- The JDK came from the [official Corretto download list](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html). Its SHA-256 matched the official checksum: 0452dc114b8b651324f4416489861b84f3085746e4303ffa5349d6531d7f92e5. The archive was extracted under the workspace work/jdk17 directory, outside the submission repository. JAVA_HOME and PATH were set only for verification; the system default was not changed.
- Environment check: ./mvnw -v reported Java version 17.0.20.1 before ./mvnw clean test. Failure signals are a nonzero exit or a Java version other than 17. For another checkout, select an installed JDK 17 through JAVA_HOME before running these commands.
- Project dependencies remain unchanged: JUnit Jupiter 5.10.2 for tests only. Production uses the JDK. Maven's own build-plugin dependencies are not added application dependencies.
- Static checks matched all 53 AC labels to actual executed JUnit methods, checked all 24 rule mappings, and confirmed that Part 2 and pom.xml are unchanged. A nonzero script exit, missing mapping or unexpected diff is a failure.
- git diff --check passed. A nonzero exit or reported whitespace errors is a failure.

## Examination checklist

| Examination requirement | Evidence | Result |
|---|---|---|
| Scope, numbered rules, assumptions and concrete expected values | spec.md contains R1-R24, AC01-AC53 and linked Assumptions | Present |
| Undergraduate 5 books, 14 days; postgraduate 10 books, 28 days | Subclass methods and tests at the fifth/sixth and tenth/eleventh loans | Borrow and update limits pass; see membership changes below |
| Borrow day 0 and due-day boundary | Before/on/after tests for both member types | Pass |
| Replaceable fine policy without modifying Library | FinePolicy injection and a 20-cent replacement test | Pass |
| Abstract Member with two ID-taking subclasses | Source and reflection-based API test | Pass |
| Type differences belong in subclasses | Library calls borrowingLimit and loanPeriodDays; no undergraduate/postgraduate branches in borrowing logic | Pass |
| At least two business exception subclasses | DuplicateLoanException, BorrowLimitException, NoCopyAvailableException, LoanNotFoundException | Pass |
| Invalid arguments use IllegalArgumentException | Null, blank, missing records, invalid copies and date tests | Pass |
| Required public signatures retained | Reflection assertions in memberTypesHaveRequiredLimitsAndPeriods | Pass |
| Suitable collections and reasons | Typed maps in LibraryState and explanations in design.md | Present and used |
| Each rule has tests with exact values and specific exceptions | AC mapping below, assertEquals and assertThrowsExactly | Pass for the agreed specification |
| Task breakdown lists files and checks | Completed tasks.md with actual test names | Present |
| AI decision record | decisions.md records actual requests and choices, including this implementation request | Present; the student remains responsible for reviewing its wording |
| spec precedes implementation | Commit 0c3676d contains only approved documents; its parent is starter | Initial order preserved; revised rule is included with impl |
| Three-commit full-exam sequence | spec completed; the student has authorized impl | review belongs to Part 2 and remains pending |

## Holding limits and membership changes

The revised R5-R6 follow the examination's at-all-times holding limit. Updating a postgraduate with 6 or 8 loans to undergraduate status throws BorrowLimitException and preserves the member, loans, inventory and original file. After returns reduce the count to 5, the update succeeds. New borrowing is blocked at 5 and succeeds at 4.

The student explicitly retained due dates fixed at borrowing. Existing postgraduate loans keep their 28-day dates after a valid update; new undergraduate loans use 14 days. File loading rejects members above their current limit but accepts a valid original period that differs from the current type. Previously generated snapshots with over-limit members are now rejected without rewriting or automatic repair.

Before the code changes, the targeted regression run executed 7 cases and failed all 7: four update cases, two invalid-snapshot cases, and one directory-path case. The directory test exposed a NullPointerException for a filesystem-root target. It now requires LibraryStorageException. The revised implementation passes these cases in the full suite.

File persistence and membership updates are added scope and earn no promised extra marks. The student confirmed the key test expectations and latest decision record as explained in Chinese in the chat. This does not claim personal execution of the tests or a line-by-line source review. Closed-book answers remain the student's responsibility.

The existing spec commit 0c3676d precedes source implementation and contains the earlier approved rule. The student first paused committing to review the discrepancy, then confirmed the Chinese explanation of key test expectations and the revised decision record and authorized impl. The revised rule, implementation and tests are included together. The existing spec commit is not amended.

## Persistence checks and limits

Tests cover creation without overwriting, missing and corrupt file rejection, normalized IDs, due-date preservation after type changes, all effective mutations surviving reload, no writes for queries or same-type updates, and explicit policy selection on reload.

Every mutation is tested with an injected failure before atomic publication. Assertions compare original file bytes, stored state and live behavior. There is also a failure before any write and an unsupported-atomic-move case. Temporary files are checked for cleanup. The implementation does not truncate the original file before publication.

One writer owns a file. Concurrent writers, external file modification and power-loss durability remain out of scope. A custom policy must be supplied again on load; otherwise the default policy applies.

## Acceptance-to-test mapping

This mapping is generated from AC references in test display names and cross-checked against executed Surefire test cases. It is traceability evidence, not a substitute for checking assertion meaning.

| Acceptance criterion | Executed test methods |
|---|---|
| AC01 | `LibraryTest.memberTypesHaveRequiredLimitsAndPeriods` |
| AC02 | `LibraryTest.identifiersIgnoreCaseAndOuterWhitespace` |
| AC03 | `LibraryTest.identifiersIgnoreCaseAndOuterWhitespace` |
| AC04 | `LibraryTest.internalWhitespaceIsPreserved` |
| AC05 | `LibraryTest.invalidArgumentsDoNotChangeState`; `LibraryTest.invalidIdentifiersAreRejected` |
| AC06 | `LibraryTest.unknownReferencesAreRejected` |
| AC07 | `LibraryTest.duplicateRegistrationIsRejected` |
| AC08 | `LibraryTest.typeUpdateDoesNotChangeDueDate` |
| AC09 | `LibraryTest.typeUpdateDoesNotChangeDueDate` |
| AC10 | `LibraryTest.unknownMemberCannotBeUpdated` |
| AC11 | `FileLibraryTest.readsAndNoOpUpdateDoNotWrite` |
| AC12 | `LibraryTest.undergraduateFifthAndSixthLoans` |
| AC13 | `LibraryTest.postgraduateTenthAndEleventhLoans` |
| AC14 | `FileLibraryTest.downgradeWithinLimitSurvivesReload`; `LibraryTest.downgradeRequiresCountWithinNewLimit` |
| AC15 | `LibraryTest.addingCopiesAccumulates` |
| AC16 | `LibraryTest.addingCopiesAccumulatesWithoutOverflow` |
| AC17 | `LibraryTest.copiesAreSharedAndReleasedExactlyOnce` |
| AC18 | `LibraryTest.copiesAreSharedAndReleasedExactlyOnce` |
| AC19 | `LibraryTest.copiesAreSharedAndReleasedExactlyOnce` |
| AC20 | `LibraryTest.copiesAreSharedAndReleasedExactlyOnce` |
| AC21 | `LibraryTest.fineAtDueDateBoundaries` |
| AC22 | `LibraryTest.fineAtDueDateBoundaries` |
| AC23 | `LibraryTest.datesDoNotDependOnSystemClock` |
| AC24 | `LibraryTest.unrepresentableDueDateIsRejected` |
| AC25 | `LibraryTest.duplicatePrecedesLimitAndInventory` |
| AC26 | `LibraryTest.duplicatePrecedesLimitAndInventory` |
| AC27 | `LibraryTest.duplicatePrecedesLimitAndInventory` |
| AC28 | `LibraryTest.repeatReturnIsRejected` |
| AC29 | `LibraryTest.returnBeforeBorrowIsRejected` |
| AC30 | `LibraryTest.returnBeforeBorrowIsRejected` |
| AC31 | `FinePolicyTest.fineAtCapBoundaries`; `LibraryTest.fineAtDueDateBoundaries` |
| AC32 | `LibraryTest.capIsPerReturnedLoan` |
| AC33 | `FinePolicyTest.fineAtCapBoundaries` |
| AC34 | `LibraryTest.overdueLoanDoesNotBlockBorrowing` |
| AC35 | `LibraryTest.customPolicyIsUsed` |
| AC36 | `FileLibraryTest.policyFailurePreservesFile`; `LibraryTest.invalidPolicyDoesNotChangeState` |
| AC37 | `FileLibraryTest.policyFailurePreservesFile` |
| AC38 | `FileLibraryTest.memoryModeDoesNotCreateFiles`; `LibraryTest.memoryInstancesAreIndependent` |
| AC39 | `FileLibraryTest.createSavesEmptyState` |
| AC40 | `FileLibraryTest.createDoesNotOverwriteExistingFile` |
| AC41 | `FileLibraryTest.failedCreationDoesNotPublishTarget` |
| AC42 | `FileLibraryTest.loadMissingFileFails` |
| AC43 | `FileLibraryTest.malformedSnapshotIsRejected`; `FileLibraryTest.unreadableDataAndDirectoryAreRejected` |
| AC44 | `FileLibraryTest.malformedSnapshotIsRejected` |
| AC45 | `FileLibraryTest.downgradeWithinLimitSurvivesReload`; `FileLibraryTest.memberAboveLimitSnapshotIsRejected` |
| AC46 | `FileLibraryTest.storedDueDateSurvivesReload` |
| AC47 | `FileLibraryTest.malformedSnapshotIsRejected` |
| AC48 | `FileLibraryTest.everyMutationSurvivesReload` |
| AC49 | `FileLibraryTest.failedFirstBorrowPreservesEmptyLoanState`; `FileLibraryTest.failedSavePreservesMemoryAndFile` |
| AC50 | `FileLibraryTest.failedSavePreservesMemoryAndFile` |
| AC51 | `FileLibraryTest.unsupportedAtomicMoveFailsSafely` |
| AC52 | `FileLibraryTest.readsAndNoOpUpdateDoNotWrite` |
| AC53 | `FileLibraryTest.customPolicyIsExplicitOnReload` |
