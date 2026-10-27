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
List<CanvasCourse> fetchCourses(String token);
List<CanvasAssignment> fetchAssignmentsForCourses(List<Long> courseIds);

// ========== Zhenye Storage Layer (Task19‑23,28‑29) Package: compass.storage ==========
/*
‑ class LocalStorageService
*/
void saveAllData(AppData data);
AppData loadAllData();
AppData mergeImportedAssignments(List<CanvasAssignment> incomingList, boolean isReplaceMode);

