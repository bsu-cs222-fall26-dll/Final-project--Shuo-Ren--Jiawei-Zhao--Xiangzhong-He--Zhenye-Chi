# API_CONTRACT Module Interface Contract
Task02, Xiangzhong. For Jiawei / Shuo / Zhenye reference.
UI layer only invokes the public methods defined below. Business logic is implemented in corresponding modules.

Expected public interfaces for all team modules:
```java
// ========== Jiawei Model Layer (Task03‑10) Package: compass.model ==========
/*
‑ class Assignment: represents assignment, supports manual‑created and canvas‑imported assignments
‑ class StudyStep: represents study step, bound by assignmentId
*/
Assignment createManualAssignment(String name, String courseName, LocalDateTime dueDate);
List<StudyStep> getStepsForAssignment(long assignmentId);
List<Assignment> getAllAssignments();

// ========== Shuo Canvas Service Layer (Task11‑18) Package: compass.canvas ==========
/*
‑ class CanvasApiService
*/
CanvasApiService.ConnectionData connect(); // account() and immutable courses()
List<CanvasCourse> fetchCourses();
List<CanvasAssignment> fetchAssignmentsForCourses(List<CanvasCourse> selectedCourses);

// ========== Zhenye Storage Layer (Task19‑23,28‑29) Package: compass.storage ==========
/*
‑ class LocalStorageService
*/
void saveAllData(AppData data);
AppData loadAllData();
AppData mergeImportedAssignments(List<CanvasAssignment> incomingList, boolean isReplaceMode);

```

## Jiawei Model Implementation and Handoff

Owner: Jiawei Zhao
Verified date: 2026-10-27
Handoff branch: feature/jiawei-10-model-handoff
Package: compass.model

This section documents the implemented model API for Tasks 03-09.
Team integration and review are still pending.

### Data Types

Assignment:
- Constructor: Assignment(long id, String name, String courseName, LocalDateTime dueDate)
- The four-argument constructor uses MANUAL source and a null Canvas assignment ID.
- Constructor: Assignment(long id, String name, String courseName, LocalDateTime dueDate, AssignmentSource source, Long canvasAssignmentId)
- Accessors: getId(), getName(), getCourseName(), getDueDate(), getSource(), getCanvasAssignmentId()
- Assignment fields are immutable.
- Blank or null assignment names and course names are rejected.
- A null due date means no deadline.
- Local IDs and Canvas assignment IDs are separate identifiers.

AssignmentSource:
- MANUAL
- CANVAS

StudyStep:
- Constructor: StudyStep(long id, long assignmentId, String text)
- Accessors: getId(), getAssignmentId(), getText(), isCompleted()
- Update method: setCompleted(boolean completed)
- New steps start incomplete.
- Blank or null step text is rejected.

### Planner Public API

```java
Assignment createManualAssignment(
        String name, String courseName, LocalDateTime dueDate);

List<Assignment> getAllAssignments();

StudyStep addStudyStep(long assignmentId, String text);

List<StudyStep> getStepsForAssignment(long assignmentId);

void deleteStudyStep(long assignmentId, long stepId);

void setStudyStepCompleted(
        long assignmentId, long stepId, boolean completed);

void deleteAssignment(long assignmentId);
```

### Behavior and Error Handling

- Assignments are ordered by earliest due date, with undated assignments last.
- Equal due dates are ordered by local assignment ID.
- Returned lists do not support adding or removing elements.
- Step queries return only steps belonging to the requested assignment.
- Step queries return an empty list when no matching steps exist.
- Adding a step to a missing assignment throws IllegalArgumentException.
- Updating or deleting a missing or unrelated step throws IllegalArgumentException.
- Deleting an assignment also removes its associated steps.
- Deleting an unknown assignment throws IllegalArgumentException.
- Other assignments and their steps remain unchanged.
- The UI should display validation errors to the user.

### Verified Tests

Verification: clean test completed successfully.

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| AssignmentTest | 6 | 0 | 0 | 0 |
| StudyStepTest | 3 | 0 | 0 | 0 |
| PlannerTest | 3 | 0 | 0 | 0 |
| PlannerStudyStepTest | 4 | 0 | 0 | 0 |
| PlannerStepCompletionTest | 3 | 0 | 0 | 0 |
| PlannerAssignmentDeletionTest | 3 | 0 | 0 | 0 |
| Model total | 22 | 0 | 0 | 0 |

The existing AppSmokeTest adds one passing placeholder test.
It is not counted as a model behavior test.
No code coverage percentage has been measured.

### Integration Coordination

- Xiangzhong: connect UI actions to the Planner methods above.
- Shuo: map Canvas data to Assignment while preserving the Canvas assignment ID and nullable due date.
- Zhenye: coordinate persistence and restoration of IDs, sources, dates, steps, and completion states.
- Planner currently manages in-memory data. Saved-state loading still requires integration.
- When restoration is integrated, ID counters must avoid collisions with restored assignment and step IDs.

### Session-only UI / Canvas integration on Shuo's branch

```java
Assignment importCanvasAssignment(
        String name, String courseName, LocalDateTime dueDate,
        long canvasAssignmentId);
```

- New Canvas records receive a local ID separate from the remote Canvas ID.
- Re-importing the same remote ID updates metadata and preserves study steps.
- CourseCompassApp now exposes only a basic Canvas viewer: URL/token entry,
  account connection, one course selection, and assignment display. Network
  calls use a background SwingWorker. It does not call Planner import or save.
- The model, study-step and import APIs remain available for later integration.
- Persistence is not connected on this branch.
- Four additional model tests cover the new import entry point.
- The UI calls CanvasApiService.connect() once to obtain account() and courses().
  checkConnection() remains available for existing callers. See
  docs/CANVAS_VERIFICATION.md for automated and real-school check results.
