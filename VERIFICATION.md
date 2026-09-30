# Verification — 0.2.0

## Completed before final corrections

- Debug assembly succeeded with Kotlin 2.3.20, AGP 8.13.2, Gradle 8.13 and JDK 17.
- 26 tests passed: 20 StudyEngine tests and 6 Robolectric UI/persistence tests; zero failures, errors or skipped tests. The new test exercises the settings shortcut and persistence of dark, light and system themes.
- The dark home screen was visually inspected using a native Robolectric rendering.
- Asset validation passed: 800 questions, 40 tickets, 310 signs and 806 valid images.
- Lint identified one error: a night-theme XML attribute requiring API 27 while minSdk is 26. It also reported two SDK-version warnings for compile/target SDK 35.

## Final corrections in this archive

- Removed the API-27-only windowLightNavigationBar XML attribute. System-bar appearance is already handled by WindowCompat, and the native dark parent supplies dark defaults.
- Added a consent request generation to the banner composition key. A changed consent response replaces the old banner even if lifecycle-aware collection did not observe the brief busy state while the activity was paused.

The incremental rebuild after these edits reported conflicting declarations despite one source declaration; a clean rebuild was started, but its output and local build tools were removed by workspace maintenance. The final clean rebuild result is unavailable. This archive therefore does not claim a passing build or lint run for the exact final revision.

## Reproduce locally

```bash
./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:generateReleaseBuildConfig
```

Live AdMob serving, UMP consent/geography cases, physical-device behaviour and the minified release build have not been verified. See docs/ADMOB_SETUP.md for setup. Automated UI tests omit the ad controller; they do not validate Google network behaviour. No guarantee of ad fill or absence of defects is made.

## HAS_ADDS master switch — 2026-09-30

Added AppConstants.HAS_ADDS with a shared effective gate combining it with BuildConfig.ADS_ENABLED. Reviewed all ad entry points: activity controller creation, consent refresh/presentation/privacy options, SDK initialization, and banner composition. Consent information is lazy so constructing a disabled controller does not access UMP. No full Android build or device/ad-network test was run for this change.

## Custom logger — 2026-09-30

Adapted the supplied Java Log utility, added PddApplication initialization, and replaced both existing native log calls via the custom import. Checked all supplied public static method names and parameter types, manifest XML, absence of native logging outside the wrapper, and archive integrity. Android compilation and runtime logging tests were not run; no JDK was available on PATH.
