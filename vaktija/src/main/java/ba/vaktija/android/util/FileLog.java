package ba.vaktija.android.util;

import android.util.Log;

import ba.vaktija.android.prefs.Prefs;

public class FileLog {

    public static void i(String tag, String message) {
        if (Prefs.DEBUG) Log.i(tag, message);
        if (Prefs.FILE_LOG) LogAppender.getInstance().append("D", tag, message);
    }

    public static void d(String tag, String message) {
        if (Prefs.DEBUG) Log.d(tag, message);
        if (Prefs.FILE_LOG) LogAppender.getInstance().append("I", tag, message);
    }

    public static void w(String tag, String message) {
        if (Prefs.DEBUG) Log.w(tag, message);
        if (Prefs.FILE_LOG) LogAppender.getInstance().append("W", tag, message);
    }

    public static void e(String tag, String message) {
        if (Prefs.DEBUG) Log.e(tag, message);
        if (Prefs.FILE_LOG) LogAppender.getInstance().append("E", tag, message);
    }

    public static void newLine(int n) {
        if (Prefs.FILE_LOG) LogAppender.getInstance().appendNewLines(n);
    }
}
