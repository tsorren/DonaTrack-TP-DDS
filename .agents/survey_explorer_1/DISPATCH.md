# Dispatch: Survey Explorer 1 (Baseline & Script Status)

**Objective**:
Map the baseline health of all verification scripts and acceptance criteria specified in `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.

**Scope & Responsibilities**:
1. Read `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md`.
2. Execute and inspect the output of:
   - `python scripts/validate_docs_links.py` (Broken links report)
   - `node scripts/validate-contracts.js` (Contract validation status)
   - `node scripts/agent-check.js` (Governance check status)
   - `node scripts/tests/run-tests.js` (Script test suite)
   - `mvn spotless:check` (Spotless formatting check across modules)
3. Document exact error outputs, broken links list, contract mismatches, and failures in `c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_1\handoff.md`.
4. Use epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]).
5. Notify orchestrator via `send_message` when complete.

## 2026-09-06T05:12:18Z

You are survey_explorer_1.
Your working directory is c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_1.
Your parent is edbee326-cd86-464a-8638-feb6a5a74249.

Read c:\IdeaProjects\DonaTrack-TP-DDS\.agents\ORIGINAL_REQUEST.md and c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_1\DISPATCH.md.

Task:
Survey the baseline health of all verification scripts and acceptance criteria:
1. Run `python scripts/validate_docs_links.py` and record any broken links.
2. Run `node scripts/validate-contracts.js` and record contract test results.
3. Run `node scripts/agent-check.js` and `node scripts/tests/run-tests.js` and record governance test results.
4. Run `mvn spotless:check` across all modules and record any formatting errors.

Write a complete handoff report using epistemic taxonomy ([OBSERVED], [DOCUMENTED], [VERIFIED]) to c:\IdeaProjects\DonaTrack-TP-DDS\.agents\survey_explorer_1\handoff.md.
When finished, send a message to your parent with your summary and output file path.
