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

    CanvasApiService.ConnectionCheck checkConnection()
    List<CanvasCourse> fetchCourses()

Both methods may throw IOException or InterruptedException.

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

Assignment fetching is implemented. Planner import integration is still pending.

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

Only checked assignments should later be passed to the local import flow.
Fetching does not modify Planner or saved data.
