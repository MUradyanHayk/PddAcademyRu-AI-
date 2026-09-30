package ru.pdd.academy.util;

import android.content.Context;
import androidx.annotation.NonNull;
import ru.pdd.academy.AppConstants;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Log {
    private static final Object syncObj = new Object();
    private static volatile File logDirectory;
    private static final long MAX_LOG_BYTES = 2 * 1024 * 1024;
    private static final ExecutorService writer = new ThreadPoolExecutor(
            0, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256),
            runnable -> { Thread thread = new Thread(runnable, "PddLogWriter");
                thread.setDaemon(true); return thread; },
            new ThreadPoolExecutor.DiscardPolicy());

    /** Called once from Application. No storage permission is required. */
    public static void init(Context context) {
        if (!isDebugMode()) return;
        logDirectory = new File(context.getApplicationContext().getNoBackupFilesDir(), "logs");
        writer.execute(Log::deleteBigLogs);
        writer.execute(Log::deleteTomorrowLog);
    }

    private Log() {
    }

    /**
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
        if(!isDebugMode()) {
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        writeToFile(tag, msg);
        return android.util.Log.d("AppLog " + tag, lineNumber + " " + msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        if(!isDebugMode()) {
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        writeToFile(tag, String.valueOf(msg) + "\n" + android.util.Log.getStackTraceString(tr));
        return android.util.Log.d("AppLog " + tag, lineNumber + " " + msg, tr);
    }

    public static int e(String tag, String msg) {
        if(!isDebugMode()) {
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        if (msg == null) {
            msg = "NullPointerException";
        }
        writeToFile(tag, msg);
        return android.util.Log.e("AppLog " + tag, lineNumber + " " + msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        if(!isDebugMode()){
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        if (msg == null) {
            msg = "NullPointerException";
        }
        writeToFile(tag, String.valueOf(msg) + "\n" + android.util.Log.getStackTraceString(tr));
        return android.util.Log.e("AppLog " + tag, lineNumber + " " + msg, tr);
    }

    public static int w(String tag, String msg, Throwable tr) {
        if(!isDebugMode()){
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        if (msg == null) {
            msg = "NullPointerException";
        }
        writeToFile(tag, String.valueOf(msg) + "\n" + android.util.Log.getStackTraceString(tr));
        return android.util.Log.w("AppLog " + tag, lineNumber + " " + msg, tr);
    }

    public static int w(String tag, String msg) {
        if(!isDebugMode()){
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        writeToFile(tag, msg);
        return android.util.Log.w("AppLog " + tag, lineNumber + " " + msg);
    }

    public static int i(String tag, String msg) {
        if(!isDebugMode()){
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        if (msg == null) {
            msg = "NullPointerException";
        }
        writeToFile(tag, msg);
        return android.util.Log.i("AppLog " + tag, lineNumber + " " + msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        if(!isDebugMode()) {
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        writeToFile(tag, String.valueOf(msg) + "\n" + android.util.Log.getStackTraceString(tr));
        return  android.util.Log.i("AppLog " + tag, lineNumber + " " + msg, tr);
    }

    public static int v(String tag, String msg) {
        if(!isDebugMode()) {
            return 0;
        }
        String lineNumber = "";
        try {
            final StackTraceElement stackTrace = new Exception().getStackTrace()[1];

            String fileName = stackTrace.getFileName();
            if (fileName == null) {
                fileName = "";  // It is necessary if you want to use proguard obfuscation.
            }

            lineNumber = stackTrace.getMethodName() + " (" + fileName + ":" + stackTrace.getLineNumber() + ")";
        } catch (Exception ignore) {
        }

        writeToFile(tag, msg);
        return android.util.Log.v("AppLog " + tag, lineNumber + " " + msg);
    }

    public static void writeToFile(String tag, String message) {
        if (!isDebugMode() || logDirectory == null) return;
        final String entry = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH)
                .format(new Date()) + " " + tag + " " + String.valueOf(message) + "\n";
        final String path = getTodayLogName();
        writer.execute(() -> {
            synchronized (syncObj) {
                try {
                    File file = new File(path);
                    File parent = file.getParentFile();
                    if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) return;
                    byte[] bytes = entry.getBytes(StandardCharsets.UTF_8);
                    // Keep a bounded tail even for an unusually large message.
                    int length = (int) Math.min(bytes.length, MAX_LOG_BYTES);
                    boolean append = file.length() + length <= MAX_LOG_BYTES;
                    try (FileOutputStream output = new FileOutputStream(file, append)) {
                        output.write(bytes, bytes.length - length, length);
                    }
                } catch (IOException | SecurityException ignored) {
                    // Logging must never crash the app or recurse into this logger.
                }
            }
        });
    }

    public static String getTodayLogName() { return logName(new Date(), ""); }

    public static String getTodayLogName(String suffix) {
        // Do not allow a suffix to escape the private log directory.
        String safe = suffix == null ? "" : suffix.replaceAll("[^A-Za-z0-9_-]", "_");
        return logName(new Date(), safe.isEmpty() ? "" : safe + "_");
    }

    private static String logName(Date date, String prefix) {
        File directory = logDirectory;
        if (directory == null) return "";
        return new File(directory, "log_" + prefix +
                new SimpleDateFormat("EEE", Locale.ENGLISH).format(date) + ".txt").getAbsolutePath();
    }

    public static void deleteBigLogs() {
        synchronized (syncObj) {
            File[] files = logFiles();
            if (files != null) for (File file : files)
                if (needToDeleteLog(file.getAbsolutePath())) deleteLog(file.getAbsolutePath());
        }
    }

    public static void deleteAllLogsFile() { deleteLogsFile(); }

    public static void deleteTomorrowLog() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        deleteLog(logName(calendar.getTime(), ""));
    }

    public static void deleteTodayLogIfItsBig() {
        synchronized (syncObj) {
            String path = getTodayLogName();
            if (needToDeleteLog(path)) deleteLog(path);
        }
    }

    public static boolean needToDeleteLog(String path) {
        File file = ownedLog(path);
        return file != null && file.length() > MAX_LOG_BYTES;
    }

    public static void deleteLog(String path) {
        synchronized (syncObj) {
            File file = ownedLog(path);
            if (file != null) file.delete();
        }
    }

    public static void deleteLogsFile() {
        synchronized (syncObj) {
            File[] files = logFiles();
            if (files != null) for (File file : files) deleteLog(file.getAbsolutePath());
        }
    }

    private static File[] logFiles() {
        File directory = logDirectory;
        return directory == null ? null : directory.listFiles((dir, name) ->
                name.startsWith("log_") && name.endsWith(".txt"));
    }

    private static File ownedLog(String path) {
        if (logDirectory == null || path == null || path.isEmpty()) return null;
        try {
            File file = new File(path).getCanonicalFile();
            return logDirectory.getCanonicalFile().equals(file.getParentFile()) &&
                    file.getName().startsWith("log_") && file.getName().endsWith(".txt") ? file : null;
        } catch (IOException | SecurityException ignored) { return null; }
    }

    public static void nativeLog(byte[] log) {
        if (isDebugMode() && log != null && log.length > 0)
            writeToFile("ProjectCore", new String(log, StandardCharsets.UTF_8));
    }

    /**
     * This method printing Log Structurally mode ex.` params(Class -- Method) LOG: Log printing in info level..
     *
     * @param tag    it’s a Log what you want print
     * @param params its a varargs for printing Class name method name and else....
     */
    public static void verbose(@NonNull String tag, @NonNull String... params) {
        String msg = getLogMsg(params);
        v(tag, msg);
    }

    public static void info(@NonNull String tag, @NonNull String... params) {
        String msg = getLogMsg(params);
        i(tag, msg);
    }

    public static void debug(@NonNull String tag, @NonNull String... params) {
        String msg = getLogMsg(params);
        d(tag, msg);
    }

    public static void warn(@NonNull String tag, @NonNull String... params) {
        String msg = getLogMsg(params);
        w(tag, msg);
    }

    public static void error(@NonNull String tag, @NonNull String... params) {
        String msg = getLogMsg(params);
        e(tag, msg);
    }

    private static String getLogMsg(@NonNull String... params) {
        StringBuilder format = new StringBuilder();
        for (String param : params) {
            format.append(param).append(" ");
        }
        return format.toString();
    }

    public static Boolean isDebugMode() {
        return AppConstants.IS_DEBUG;
    }
}
