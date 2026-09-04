# Project structure

This archive restores the uploaded files to the directory layout expected by
Android Gradle Plugin and the included GitHub Actions workflow.

- `build.gradle.kts` contains the root plugin versions.
- `app/build.gradle.kts` configures the Android application.
- `app/src/main/java/com/alexdo/todo/` contains the Kotlin source.
- `app/src/main/res/` contains Android resources.
- `.github/workflows/build.yml` builds `app-debug.apk` and publishes it as the
  `todo-widget-apk` workflow artifact.

The workflow installs Gradle 8.10.2 itself, so a Gradle wrapper is not required
for the GitHub-hosted build.
