# Canvas API verification

Checked on 2026-10-29 (America/Indianapolis), using the team Java implementation.
Repeated successfully on 2026-10-29 at 03:53 EDT before committing this branch.

## Automated checks

`gradlew.bat test installDist` passes: 67 tests, including 40 Canvas tests,
26 model tests, and one existing placeholder test. The placeholder does not
prove a feature works. Canvas unit tests use synthetic transport responses.

This review added two regression checks:
- Connecting returns the profile and course list without reading courses twice.
- Cancellation arriving with the last response does not return successful data.

Existing tests cover authentication, URL normalization, course/assignment
pagination, selected courses only, parsing, missing deadlines, timezone
conversion, errors, and interruption.

## Real-school Java smoke check

The built team's CanvasApiService was called through a local console harness,
using its production HttpClient and normal TLS verification. Existing local
credentials were passed only through stdin, never source code, arguments,
repository files, or logs. The harness remains outside this repository.

Verified results:
- Account profile returned successfully.
- 40 accessible student courses returned, including historical courses.
- Selecting CS222 returned 41 assignments.
- Assignment IDs, names, local deadlines, and points matched an independent
  read of Canvas's original API response.
- An intentionally invalid test token was rejected with HTTP 401.
- An in-memory Planner re-import preserved the local ID and completed steps.

These calls read Canvas only. No assignments were changed on Canvas and no
personal task data was saved to this repository.

## Limits and next UI work

- This verifies the Java service, not operation of the desktop controls.
- The current UI reads/displays one course's assignments. It does not import
  selected rows into Planner or save/restore the user's work.
- Desktop interaction, full selection/import/storage integration, and testing
  with three outside target users still require separate verification.
- The live smoke check is separate from the automated, offline unit suite.
