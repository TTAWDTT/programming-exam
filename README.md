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
