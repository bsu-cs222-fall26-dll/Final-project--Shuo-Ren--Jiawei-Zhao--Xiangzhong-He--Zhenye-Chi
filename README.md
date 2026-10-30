# Course Compass

A Java desktop application for viewing Canvas coursework and organizing assignment study steps.

**Team:** Shuo Ren, Jiawei Zhao, Zhenye Chi, Xiangzhong He.

## Current Status

`main` contains the models and basic Swing interface.

The Canvas connection and assignment viewer are available on:
`feature/shuo-11-canvas-auth`

## Build and Run

Use JDK 21. The current `main` branch requires Gradle 9.0:

```powershell
gradle test installDist
gradle run
```

Application entry point: `compass.CourseCompassApp`.

Never commit Canvas access tokens.
