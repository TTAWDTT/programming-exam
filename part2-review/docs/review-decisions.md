# review-decisions.md — decisions on the AI review report

The "Decision" column must be exactly one of: **Fix now / Verify first / Track later / Decline**.

- Fix now: a confirmed defect; fix it in this task. You must also give, under "Evidence", the name of a test method that fails against the original code.
- Verify first: possibly a problem, but you have no input that reproduces it.
- Track later: a real problem, but outside this task's scope.
- Decline: the finding does not hold, or it conflicts with the specification or the project constraints.

| # | Decision | Reason (one or two sentences; cite a spec rule or a line of code) | Evidence (test method name, optional) |
|---|---|---|---|
| F1 |  |  |  |
| F2 |  |  |  |
| F3 |  |  |  |
| F4 |  |  |  |
| F5 |  |  |  |
| F6 |  |  |  |
| F7 |  |  |  |
| F8 |  |  |  |

## Defects the report missed

If you found a defect the report does not mention, record it here: location, the spec rule it violates, an input that reproduces it, and the test method name.

(Write "none" if you found nothing.)
