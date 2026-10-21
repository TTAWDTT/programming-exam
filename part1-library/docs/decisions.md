# AI use and key decisions

## Tools used

- AI tool: Codex desktop chat.
- Model: the exact model identifier was not verified in the chat.
- Current stage: specification and design approved by the student on 2026-10-21, including the technical decisions. Implementation has not started. Initial Maven test commands were started, but successful results have not been confirmed.

## Key requests

| # | My request | What the AI produced | What I accepted or changed, and why |
|---|---|---|---|
| 1 | Read the examination and starter project, explain the background, and discuss one decision at a time. | An explanation of borrowing concepts, required interfaces, documents and tests, followed by individual choices. | I asked for more background before deciding and limited the discussion to one question at a time so I could understand each choice. |
| 2 | Explain the fine and ordinary borrowing rules. | The AI suggested 50 cents per overdue day without a cap, one active copy of each ISBN per member, additive copies and rejection of duplicate members. | I chose 50 cents per day with a 1000-cent cap per loan. I accepted the duplicate-loan restriction and additive copies. I kept duplicate registration invalid but raised the need to change an undergraduate to a postgraduate. See R4, R7, R8 and R14. |
| 3 | Support member type changes while preserving existing loans. | A separate update method and fixed existing due dates. The AI initially suggested refusing a downgrade above the new borrowing limit. | I accepted the separate method and preserved due dates. I challenged the downgrade restriction because the recorded identity should match the real status. I chose to allow updates and apply the new limit to future borrowing. Unknown IDs are rejected; same-type updates do nothing. See R5 and R6. |
| 4 | Clarify returns, identifier handling, date authority and validation order. | Alternatives for missing loans, inconsistent dates, whitespace and case, overdue borrowers, dates and error precedence. | I selected LoanNotFoundException for missing loans, rejected returns before borrowing, allowed same-day returns and required unchanged state on failure. I chose case-insensitive IDs with outer whitespace removed and allowed overdue members to borrow within ordinary limits. I raised the risk of borrowers choosing false dates; dates must come from trusted callers. Borrow checks run in input, duplicate, limit, inventory order. See R2, R10-R13, R15 and R17. |
| 5 | Preserve data in files and prepare English documents for review before implementation. | The AI initially recommended no persistence, then described automatic file saving, failure handling, explicit creation and loading, and caller-selected paths. | I requested file persistence so records survive a restart. I chose automatic saving, whole-operation failure on save errors, separate create and load operations, rejection of corrupt files and explicit paths. I accepted an in-memory no-argument constructor. I requested English submission documents and help recording the actual decisions from this conversation. I reviewed the documents and the explanation of the added technical decisions, confirmed that they match my expectations, and authorized updating their status to accepted. Implementation and test verification are not complete. See R18-R24. |

Update this record after the next significant step with actual requests, outputs, decisions and checks. Document approval is not an implementation or test result.
