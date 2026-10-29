# Canvas Service Handoff

Owner: Shuo Ren

## Implemented

- School URL normalization and Bearer authentication.
- Current user connection check.
- Student course and term discovery.
- Historical course status.
- Course pagination with server validation and page limits.
- Safe HTTP, JSON, timeout and network error messages.

## Public API

Create a service with:

    new CanvasApiService(canvasUrl, token)

Available methods:

    CanvasApiService.ConnectionData connect()
    CanvasApiService.ConnectionCheck checkConnection()
    List<CanvasCourse> fetchCourses()

These methods may throw IOException or InterruptedException.
connect() returns account() and courses() from one profile/course read flow.
The UI uses connect() to avoid fetching the courses twice.

ConnectionCheck accessors:

    name()
    userId()
    courseCount()

CanvasCourse accessors:

    id()
    name()
    term()
    concluded()

The original draft fetchCourses(String token) is refined to
fetchCourses(): credentials are supplied once in the constructor.

Course count means accessible student courses, including historical
courses. It is not a current-term course count.

## Integration

Run network calls on a background worker, outside the Swing event thread.
Create a new service and invalidate the previous check when URL/token changes.
Do not save tokens, service instances or request headers.

Assignment fetching is implemented. The current simplified Swing entry point
is a read-only Canvas viewer, not a Planner import or persistence workflow.
The Java service has passed a real-school smoke check; desktop interaction
and full import/storage integration remain pending. See docs/CANVAS_VERIFICATION.md.

## Tests

Connection/course tests use synthetic transport responses.
They do not prove that a real school account or TLS connection works.

## Selected Assignment Fetching

Implemented method:

    List<CanvasAssignment> fetchAssignmentsForCourses(
        List<CanvasCourse> selectedCourses
    )

Pass the checked CanvasCourse objects returned by fetchCourses().
An empty selection sends no requests.

CanvasAssignment accessors:

    canvasAssignmentId()
    courseId()
    name()
    courseName()
    dueDate()
    pointsPossible()

canvasAssignmentId is the remote Canvas ID, not Planner's local ID.
Dates are converted into the computer's local time.
Missing dates remain null. Missing points use zero.
Points remain available in the Canvas DTO; the current Planner Assignment
model does not yet contain a points field.

Course and assignment pagination validate the server and endpoint.
Repeated courses and repeated assignment records are deduplicated.
A failed request throws an exception instead of returning partial results.

The current viewer supplies one selected course to the service and displays
its assignments. It does not modify Planner or saved data.

## UI integration update

- At Shuo's request, CourseCompassApp is now a minimal, standard Swing viewer:
  URL/token inputs, Connect, one course dropdown, Load assignments, and a table.
- Connect checks the account and loads its courses. Load assignments reads
  only the selected course and displays course/name/deadline/points.
- Manual-entry, search, study-step, progress and multi-selection UI controls
  are not exposed in this version. The existing backend code remains intact.
- Canvas calls run on a SwingWorker. Inputs are disabled during a request.
- Tokens stay in memory and are cleared when the viewer closes.
- Planner.importCanvasAssignment(name, courseName, dueDate, canvasAssignmentId)
  maps remote IDs to local IDs. Re-import updates metadata while preserving
  the local ID and existing study steps. Four new behavior tests cover this.
- The viewer only displays responses; it does not import into Planner or save
  data. Earlier full session UI code was backed up outside the team repository.
- Verified: Gradle test/installDist. The existing model and Canvas tests remain.
- The real-school Java smoke check is recorded separately from unit tests.
  Neither it nor UI wiring proves outside-user acceptance testing is complete.

## Remaining API and import work

- Tasks 11-16: URL/authentication, profile/course discovery, selected-course
  reads, pagination and parsing are implemented and covered by synthetic tests.
- Task 17: status-code, timeout, malformed-response and interruption handling
  have tests, including cancellation arriving with the last response. Whole
  GUI interaction still needs verification. Credentials cannot be edited
  during requests.
- Task 18: 40 Canvas tests use an injected synthetic transport. A separate
  real-school run of the production Java HttpClient/TLS flow passed on
  2026-10-29. Verification records contain no credentials or response bodies.
- The basic viewer intentionally omits term filters, course/assignment checks,
  name search and past-due filtering from the full reference workflow.
- Full Planner import, persistence/restoration and the confirmed, backed-up
  replacement/cleanup workflow still require integration.
  The complete import user story must not be marked finished yet.
