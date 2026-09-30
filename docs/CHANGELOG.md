# 0.2.0

- Added a settings shortcut to the home screen and an accessible appearance selector.
- Refined light/dark palettes, native night resources, hero card and surface colors. Theme choice persists in DataStore.
- Integrated Google AdMob adaptive banners on home and completed-session screens, UMP consent and privacy options, lifecycle cleanup, and refresh after privacy choices change.
- Debug uses Google test inventory. Release ads default to disabled; enable with your own app and banner IDs as documented in ADMOB_SETUP.md.
- Upgraded to Kotlin 2.3.20, AGP 8.13.2 and Gradle 8.13 for AdMob 25.4.0 metadata compatibility.
- Added a UI test covering the appearance shortcut and persistence of all three modes.

See VERIFICATION.md for the exact validation status and remaining checks.
