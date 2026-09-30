# Logging

Use `import ru.pdd.academy.util.Log`. This Java utility preserves the supplied d/e/w/i/v overloads, structured helpers, file helpers and nativeLog API. PddApplication initializes storage before activities start. Both existing StudyViewModel error calls now use the utility. Native Logcat calls remain only inside the wrapper; SDK internals are unchanged.

AppConstants.IS_DEBUG follows BuildConfig.DEBUG. Release builds do not emit messages or write log files, including direct writeToFile/nativeLog calls. HAS_ADDS remains independent.

Debug logs go to Logcat with the AppLog prefix and caller location, and to app-private `no_backup/logs/log_<weekday>.txt`. No storage permission or external PathManager is needed. File entries contain timestamps and throwable stack traces.

File writes use a background worker with a bounded 256-entry queue; new file entries are dropped when full. Logging is best-effort; queued entries can be lost when the process exits. Each file is capped at 2 MiB, replacing older contents when full. Startup deletes tomorrow's weekday file and oversized logs. Cleanup helpers delete only matching files inside the private log directory. Null messages are safe.

```kotlin
Log.d("PddAcademy", "Screen opened")
Log.e("PddAcademy", "Loading failed", exception)
```
