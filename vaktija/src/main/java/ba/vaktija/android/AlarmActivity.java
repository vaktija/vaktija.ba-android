package ba.vaktija.android;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.AlarmSoundPlayer;
import ba.vaktija.android.service.OngoingAlarmService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.Utils;
import ba.vaktija.android.widgets.SlidingLayout;
import io.github.inflationx.viewpump.ViewPumpContextWrapper;

import static ba.vaktija.android.service.NotifManager.ALARMS_CHANNEL;

/*
 This activity plays alarm sound
 */

public class AlarmActivity extends AppCompatActivity
        implements SlidingLayout.SlidingLayoutListener {

    public static final String TAG = AlarmActivity.class.getSimpleName();
    public static final String EXTRA_PRAYER_ID = "EXTRA_PRAYER_ID";
    public static final String ACTION_CANCEL_ALARM = "ACTION_CANCEL_ALARM";
    public static final String EXTRA_PLAY_ALARM_SOUND = "EXTRA_PLAY_ALARM_SOUND";

    private static final int ALARM_NOTIF = 1337;
    private static final int ALARM_NOTIF_MISSED = 2337;

    private static final int ALARM_TIMEOUT = 2 /*mins*/ * 60 /*sec*/ * 1000 /*millisec*/;
    private static final int ALARM_SLEEP = 5 /*mins*/ * 60 /*sec*/ * 1000 /*millisec*/;
    public static final String LAUNCH_ALARM = "LAUNCH_ALARM";

    private Prayer prayer;
    private CountDownTimer countDownTimer;
    private SharedPreferences preferences;
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    private App app;
    private AlarmSoundPlayer alarmSoundPlayer;

    public static void cancelAlarm(Activity activity) {
        Intent i = new Intent(activity, AlarmActivity.class);
        i.setAction(ACTION_CANCEL_ALARM);
        activity.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        wakeLock.setReferenceCounted(false);

        alarmSoundPlayer = new AlarmSoundPlayer(this);

        wakeLock.acquire(ALARM_TIMEOUT);

        getWindow().addFlags(
                LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        super.onCreate(savedInstanceState);

        FileLog.d(TAG, "[*** onCreate ***]");

        app = (App) getApplicationContext();

        setContentView(R.layout.activity_alarm);

        TextView mAlarmTimeHrs = findViewById(R.id.activity_alarm_timeHrs);
        TextView mAlarmTimeMins = findViewById(R.id.activity_alarm_timeMins);
        TextView mAlarmTitle = findViewById(R.id.activity_alarmTitle);
        SlidingLayout mSlidingLayout = findViewById(R.id.activity_alarm_slidingLayout);
        ImageView mClockIcon = findViewById(R.id.activity_alarm_icon);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        preferences.edit().putBoolean(Prefs.ALARM_ACTIVE, true).apply();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mSlidingLayout.setSlidingListener(this);

        mClockIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.wiggle));

        int prayerId = getIntent().getIntExtra(EXTRA_PRAYER_ID, -1);

        boolean playAlarmSound = getIntent().getBooleanExtra(EXTRA_PLAY_ALARM_SOUND, true);

        prayer = PrayersSchedule.getInstance(this).getPrayer(prayerId);

        if (prayerId == -1) {
            Log.w(TAG, "Prayer is null");
            preferences.edit().putBoolean(Prefs.ALARM_ACTIVE, false).apply();
            finish();
            return;
        }

        int time = prayer.getPrayerTime() - Utils.getCurrentTimeSec();

        time += 5;

        int minutes = ((time / 60) % 60);
        int hours = (time / 3600) % 24;

        mAlarmTimeHrs.setText(String.valueOf(hours));
        mAlarmTimeMins.setText(String.valueOf(minutes));
        mAlarmTitle.setText(String.format("%s JE ZA", prayer.getTitle().toUpperCase(Locale.getDefault())));

        try {
            if (playAlarmSound) {
                final Uri soundUri = App.app.getAlarmSoundUri();
                alarmSoundPlayer.play(soundUri, true);
                showNotification();
            }
            startCountDownTimer();
        } catch (Exception e) {
            e.printStackTrace();
            FileLog.e(TAG, "Cannot play alarm sound: " + e.getMessage());
            FileLog.e(TAG, e.toString());
            FileLog.w(TAG, "exception: " + e.getClass() + ": " + e);
            Toast.makeText(AlarmActivity.this, "Can't play alarm sound", Toast.LENGTH_LONG).show();
            AlarmActivity.this.finish();
        }

        app.sendScreenView("Alarm");
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        FileLog.d(TAG, "[onNewIntent]");
        String action = intent.getAction();
        FileLog.d(TAG, "action: " + action);

        if (action != null && action.equals(ACTION_CANCEL_ALARM)) {
            cancelAlarm();
            finish();
        }
    }

    void startCountDownTimer() {
        FileLog.d(TAG, "[startCountDownTimer]");

        countDownTimer = new CountDownTimer(ALARM_TIMEOUT, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                int remaining = (int) millisUntilFinished / 1000;

                FileLog.d(TAG, "on tick " + remaining);
            }

            @Override
            public void onFinish() {
                showAlarmMissedNotification();
                cancelAlarm();
                finish();
            }
        };

        countDownTimer.start();
    }

    void rescheduleAlarm() {

        app.sendEvent("Alarm rescheduled", "Rescheduled for " + prayer.getTitle());

        boolean alreadyResheduled = preferences.getBoolean(Prefs.ALARM_RESCHEDULED_ONCE + "_" + prayer.getId(), false);

        if (alreadyResheduled) {
            preferences.edit().putBoolean(Prefs.ALARM_RESCHEDULED_ONCE + "_" + prayer.getId(), false).apply();
            return;
        }

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        long triggerAtMillis = cal.getTimeInMillis() + ALARM_SLEEP;

        am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, Prayer.getAlarmPendingIntent(this, prayer));
        preferences.edit().putBoolean(Prefs.ALARM_RESCHEDULED_ONCE + "_" + prayer.getId(), true).apply();
        FileLog.i(TAG, "alarm resheduled at " + new Date(triggerAtMillis));

        wakeLock.release();
    }

    @Override
    public void onSlidingCompleted() {
        FileLog.d(TAG, "[onSlidingCompleted]");

        cancelAlarm();
        finish();
    }

    private void cancelAlarm() {
        FileLog.d(TAG, "[cancelAlarm]");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(OngoingAlarmService.getStopAlarmIntent(this));
        }

        alarmSoundPlayer.cancel();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        notificationManager.cancel(ALARM_NOTIF);

        preferences.edit().putBoolean(Prefs.ALARM_ACTIVE, false).apply();

        wakeLock.release();
    }

    void showNotification() {
        FileLog.d(TAG, "[showNotification]");
        Intent intent = new Intent(this, AlarmActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, ALARMS_CHANNEL);
        notifBuilder
                .setSmallIcon(R.drawable.ic_notif_alarm)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentIntent(pIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTicker(prayer.getTitle() + " je za " + FormattingUtils.getTimeString(prayer.getPrayerTime() - Utils.getCurrentTimeSec()))
                .setContentTitle(getString(R.string.alarm))
                .setContentText(prayer.getTitle() + " je za " + FormattingUtils.getTimeString(prayer.getPrayerTime() - Utils.getCurrentTimeSec()));

        notificationManager.notify(ALARM_NOTIF, notifBuilder.build());
    }

    void showAlarmMissedNotification() {
        FileLog.d(TAG, "[showAlarmMissedNotification]");
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this, ALARMS_CHANNEL);
        notifBuilder
                .setSmallIcon(R.drawable.ic_notif_warning)
                .setOnlyAlertOnce(true)
                .setOngoing(false)
                .setContentIntent(pIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setTicker(
                        getString(
                                R.string.alarm_missed_for,
                                " " + FormattingUtils.getCaseTitle(
                                        prayer.getId(),
                                        FormattingUtils.Case.AKUZATIV)))

                .setContentTitle(getString(R.string.alarm_missed))
                .setContentText(getString(R.string.alarm_is_missed_for,
                        " " + FormattingUtils.getCaseTitle(
                                prayer.getId(),
                                FormattingUtils.Case.AKUZATIV)));

        notificationManager.notify(ALARM_NOTIF_MISSED, notifBuilder.build());
    }

    @Override
    public void onBackPressed() {
        preferences.edit().putBoolean(Prefs.ALARM_ACTIVE, false).apply();
        cancelAlarm();
        super.onBackPressed();
    }

    // https://developer.android.com/reference/android/app/Activity#onUserLeaveHint()
    @Override
    protected void onUserLeaveHint() {
        Log.d(TAG, "onUserLeaveHint");
        cancelAlarm();
        finish();
        super.onUserLeaveHint();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_POWER) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
