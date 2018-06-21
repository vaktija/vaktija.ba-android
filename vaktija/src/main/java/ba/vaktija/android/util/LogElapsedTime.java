package ba.vaktija.android.util;

import android.util.Log;

/**
 * Created by e on 5/21/15.
 */
public class LogElapsedTime {

    private static StringBuilder sb = new StringBuilder();

    public static void i(String tag, String message, long executionStartTime) {
        sb.setLength(0);
        sb.append("[")
                .append((System.nanoTime() - executionStartTime)/1000.0)
                .append(" us] [")
                .append(Thread.currentThread().getStackTrace()[3].getMethodName())
                .append("] ")
                .append(message);

        Log.i(tag, sb.toString());
    }

    public static void d(String tag, String message, long executionStartTime) {
        sb.setLength(0);
        sb.append("[")
                .append((System.nanoTime() - executionStartTime)/1000.0)
                .append(" us] [")
                .append(Thread.currentThread().getStackTrace()[3].getMethodName())
                .append("] ")
                .append(message);

        Log.d(tag, sb.toString());
    }

    public static void w(String tag, String message, long executionStartTime) {
        sb.setLength(0);
        sb.append("[")
                .append((System.nanoTime() - executionStartTime)/1000.0)
                .append(" us] [")
                .append(Thread.currentThread().getStackTrace()[3].getMethodName())
                .append("] ")
                .append(message);

        Log.w(tag, sb.toString());
    }

    public static void v(String tag, String message, long executionStartTime) {
        sb.setLength(0);
        sb.append("[")
                .append((System.nanoTime() - executionStartTime)/1000.0)
                .append(" us] [")
                .append(Thread.currentThread().getStackTrace()[3].getMethodName())
                .append("] ")
                .append(message);

        Log.v(tag, sb.toString());
    }

    public static void e(String tag, String message, long executionStartTime) {
        sb.setLength(0);
        sb.append("[")
                .append((System.nanoTime() - executionStartTime)/1000.0)
                .append(" us] [")
                .append(Thread.currentThread().getStackTrace()[3].getMethodName())
                .append("] ")
                .append(message);

        Log.e(tag, sb.toString());
    }
}
