# Implementation Plan - Test Infrastructure and Robustness Update

Review and commit staged (currently unstaged) modifications related to test library upgrades and UI test robustness.

## User Review Required

> [!NOTE]
> No changes were found related to a "drawer" UI component. The modifications are focused on upgrading testing dependencies and improving `MainJourneyTest.kt` reliability. I will proceed with these changes unless you intended for other files to be included.

## Proposed Changes

### Build & Dependencies

#### [MODIFY] [libs.versions.toml](file:///C:/Users/seamo/AndroidStudioProjects/CopyPasteWisdom/gradle/libs.versions.toml)
- Upgrade `junitVersion` to `1.3.0`.
- Upgrade `espressoCore` to `3.7.0`.
- Upgrade `androidxTestRunner` to `1.7.0`.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/seamo/AndroidStudioProjects/CopyPasteWisdom/app/build.gradle.kts)
- Standardize `androidx.test:runner` implementation to use the version catalog (`libs.androidx.androidRunner`).

### Testing

#### [MODIFY] [MainJourneyTest.kt](file:///C:/Users/seamo/AndroidStudioProjects/CopyPasteWisdom/app/src/androidTest/java/com/example/copy_pastewisdom/MainJourneyTest.kt)
- Increase `waitUntil` timeout to 15,000ms for better reliability on slower devices.
- Refactor wait condition to use static import for `hasText`.

### Documentation

#### [MODIFY] [README.md](file:///C:/Users/seamo/AndroidStudioProjects/CopyPasteWisdom/README.md)
- Update the "Testing Suite" section to highlight the focus on robustness and updated dependency stack.

## Git Commit Message

```text
test: upgrade test dependencies and improve UI test robustness

- Upgrade JUnit to 1.3.0, Espresso to 3.7.0, and Test Runner to 1.7.0
- Increase timeout in MainJourneyTest to 15s for better reliability
- Refactor MainJourneyTest to use clean wait conditions
- Standardize test runner dependency in build.gradle.kts
```

## Verification Plan

### Automated Tests
- Run `MainJourneyTest` to ensure the robustness changes work as expected.
- Command: `./gradlew connectedDebugAndroidTest`

### Manual Verification
- Verify `README.md` formatting and content.
- Verify `git commit` and `git push` success.
