package ba.vaktija.android.util;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by e on 2/9/15.
 */
public class LogAppender {
    private static LogAppender instance;
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    private Executor mExecutor = Executors.newFixedThreadPool(1);
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    public static LogAppender getInstance() {
        if (instance == null)
            instance = new LogAppender();

        return instance;
    }

    public synchronized void append(final String type, final String tag, final String message) {

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {

                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    return;
                }

                File logFile = new File(Environment.getExternalStorageDirectory(), "vaktija_log.txt");

                if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                    File prevLogFile = new File(Environment.getExternalStorageDirectory(), "vaktija_log.1.txt");

                    if (prevLogFile.exists()) {
                        prevLogFile.delete();
                    }

                    logFile.renameTo(new File(Environment.getExternalStorageDirectory(), "vaktija_log.1.txt"));

                    logFile = new File(Environment.getExternalStorageDirectory(), "vaktija_log.txt");
                }

                if (!logFile.exists()) {
                    try {
                        logFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                    String log = mSimpleDateFormat.format(new Date()) + " [" + type + "] [" + tag + "] " + message;
                    buf.append(log);
                    buf.newLine();
                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public synchronized void appendNewLines(final int n) {

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {

                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    return;
                }

                File logFile = new File(Environment.getExternalStorageDirectory(), "vaktija_log.txt");

                if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                    File prevLogFile = new File(Environment.getExternalStorageDirectory(), "vaktija_log.1.txt");

                    if (prevLogFile.exists()) {
                        prevLogFile.delete();
                    }

                    logFile.renameTo(new File(Environment.getExternalStorageDirectory(), "vaktija_log.1.txt"));

                    logFile = new File(Environment.getExternalStorageDirectory(), "vaktija_log.txt");
                }

                if (!logFile.exists()) {
                    try {
                        logFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                    for (int i = 0; i < n; i++)
                        buf.newLine();

                    buf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
