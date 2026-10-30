# Course Compass

A Java desktop application for viewing Canvas coursework and organizing assignment study steps.

**Team:** Shuo Ren, Jiawei Zhao, Zhenye Chi, Xiangzhong He.

## Current Version

The main branch includes Canvas account connection, course selection, and assignment display with deadlines and points.

The viewer is read-only and does not save assignments or modify Canvas. The model supports manual assignments and study steps; storage and study-step interface integration are separate work.

## Build and Run

Use JDK 21. The project includes the Gradle Wrapper and targets Java 17 bytecode.

```powershell
.\gradlew.bat test installDist
.\gradlew.bat run
```

Application entry point: `compass.CourseCompassApp`.

Never commit Canvas access tokens.
