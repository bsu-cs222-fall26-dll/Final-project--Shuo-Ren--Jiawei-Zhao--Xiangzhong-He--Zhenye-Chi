# Course Compass

A Java desktop project for organizing Canvas coursework.

Team: Shuo Ren, Jiawei Zhao, Zhenye Chi, Xiangzhong He.

## Current branch

The Swing interface is a basic read-only Canvas viewer. Enter your school's
HTTPS Canvas URL and your own token, connect, choose a course, and load its
assignments. The viewer displays deadlines and points; it does not save data
or modify Canvas. Tokens are kept in memory, never in project files.

The model separately supports manual assignments and study steps, plus
Canvas re-import while retaining local progress. Full model/storage/UI
integration is still in progress. See [verification notes](docs/CANVAS_VERIFICATION.md).

## Build and run

Use JDK 21. The project currently targets Java 17 bytecode and includes
the Gradle Wrapper.

Windows PowerShell, from the repository directory:

```powershell
.\gradlew.bat test installDist
.\gradlew.bat run
```

The generated Windows launcher is
`build/install/CourseCompass/bin/CourseCompass.bat`.

In IntelliJ, open this directory as a Gradle project and run
`compass.CourseCompassApp.main()`. `src/main` is the production source
directory; a class named `Main` is not required.

Unit tests use synthetic responses and do not need a Canvas token.
Real-account checks are documented separately from automated tests and
outside-user acceptance testing.
