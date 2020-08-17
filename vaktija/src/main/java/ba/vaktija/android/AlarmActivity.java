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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.AlarmSoundPlayer;
import ba.vaktija.android.service.OngoingAlarmService;
import ba.vaktija.android.service.VaktijaServiceHelper;
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

    private TextView mAlarmTimeHrs;
    private TextView mAlarmTimeMins;
    private TextView mAlarmTitle;
    private SlidingLayout mSlidingLayout;
    private ImageView mClockIcon;
    private Prayer mPrayer;
    private CountDownTimer mAlarmTimer;
    private SharedPreferences mPrefs;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private NotificationManager mNotificationManager;
    private App mApp;
    private AlarmSoundPlayer alarmSoundPlayer;

    public static void cancelAlarm(Activity activity) {
        Intent i = new Intent(activity, AlarmActivity.class);
        i.setAction(ACTION_CANCEL_ALARM);
        //i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        mWakeLock.setReferenceCounted(false);

        alarmSoundPlayer = new AlarmSoundPlayer(this);

        mWakeLock.acquire();

        getWindow().addFlags(
                LayoutParams.FLAG_DISMISS_KEYGUARD
                        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | LayoutParams.FLAG_TURN_SCREEN_ON
                        | LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        super.onCreate(savedInstanceState);

        FileLog.d(TAG, "[*** onCreate ***]");

        mApp = (App) getApplicationContext();

        setContentView(R.layout.activity_alarm);

        mAlarmTimeHrs = (TextView) findViewById(R.id.activity_alarm_timeHrs);
        mAlarmTimeMins = (TextView) findViewById(R.id.activity_alarm_timeMins);
        mAlarmTitle = (TextView) findViewById(R.id.activity_alarmTitle);
        mSlidingLayout = (SlidingLayout) findViewById(R.id.activity_alarm_slidingLayout);
        mClockIcon = (ImageView) findViewById(R.id.activity_alarm_icon);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mPrefs.edit().putBoolean(Prefs.ALARM_ACTIVE, true).commit();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mSlidingLayout.setSlidingListener(this);

        mClockIcon.startAnimation(AnimationUtils.loadAnimation(this, R.anim.wiggle));

        int prayerId = getIntent().getIntExtra(EXTRA_PRAYER_ID, -1);

        boolean playAlarmSound = getIntent().getBooleanExtra(EXTRA_PLAY_ALARM_SOUND, true);

        mPrayer = PrayersSchedule.getInstance(this).getPrayer(prayerId);

        if (prayerId == -1) {
            Log.w(TAG, "Prayer is null");
            mPrefs.edit().putBoolean(Prefs.ALARM_ACTIVE, false).commit();
            finish();
            return;
        }

        int time = mPrayer.getPrayerTime() - Utils.getCurrentTimeSec();

        time += 5;

        int minutes = ((time / 60) % 60);
        int hours = (time / 3600) % 24;

        mAlarmTimeHrs.setText(String.valueOf(hours));
        mAlarmTimeMins.setText(String.valueOf(minutes));
        mAlarmTitle.setText(mPrayer.getTitle().toUpperCase(Locale.getDefault()) + " JE ZA");

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

        mApp.sendScreenView("Alarm");
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
            cancelAlarmAndFinish();
        }
    }

    void startCountDownTimer() {
        FileLog.d(TAG, "[startCountDownTimer]");

        mAlarmTimer = new CountDownTimer(ALARM_TIMEOUT, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                int remaining = (int) millisUntilFinished / 1000;

                FileLog.d(TAG, "on tick " + remaining);
            }

            @Override
            public void onFinish() {
                showAlarmMissedNotification();
                cancelAlarmAndFinish();
            }
        };

        mAlarmTimer.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FileLog.d(TAG, "[onDestroy]");

        cancelAlarmAndFinish();
    }

    void rescheduleAlarm() {

        mApp.sendEvent("Alarm rescheduled", "Rescheduled for " + mPrayer.getTitle());

        boolean alreadyResheduled = mPrefs.getBoolean(Prefs.ALARM_RESCHEDULED_ONCE + "_" + mPrayer.getId(), false);

        if (alreadyResheduled) {
            mPrefs.edit().putBoolean(Prefs.ALARM_RESCHEDULED_ONCE + "_" + mPrayer.getId(), false).commit();
            return;
        }

        Calendar cal = Calendar.getInstance(Locale.getDefault());
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        long triggerAtMillis = cal.getTimeInMillis() + ALARM_SLEEP;

        am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, Prayer.getAlarmPendingIntent(this, mPrayer));
        mPrefs.edit().putBoolean(Prefs.ALARM_RESCHEDULED_ONCE + "_" + mPrayer.getId(), true).commit();
        FileLog.i(TAG, "alarm resheduled at " + new Date(triggerAtMillis));

        mWakeLock.release();
    }

    @Override
    public void onSlidingCompleted() {
        FileLog.d(TAG, "[onSlidingCompleted]");

        cancelAlarmAndFinish();
    }

    private void cancelAlarmAndFinish() {
        FileLog.d(TAG, "[cancelAlarmAndFinish]");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VaktijaServiceHelper.startService(this, OngoingAlarmService.getStopAlarmIntent(this));
        }

        alarmSoundPlayer.cancel();

        if (mAlarmTimer != null) {
            mAlarmTimer.cancel();
        }

        mNotificationManager.cancel(ALARM_NOTIF);

        mPrefs.edit().putBoolean(Prefs.ALARM_ACTIVE, false).commit();

        //mPrefs.edit().putBoolean(Prefs.ALARM_RESCHEDULED_ONCE+"_"+ mPrayer.getId(), false).commit();
        mWakeLock.release();
        finish();
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
                .setTicker(mPrayer.getTitle() + " je za " + FormattingUtils.getTimeString(mPrayer.getPrayerTime() - Utils.getCurrentTimeSec()))
                .setContentTitle(getString(R.string.alarm))
                .setContentText(mPrayer.getTitle() + " je za " + FormattingUtils.getTimeString(mPrayer.getPrayerTime() - Utils.getCurrentTimeSec()));

        mNotificationManager.notify(ALARM_NOTIF, notifBuilder.build());
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
                                        mPrayer.getId(),
                                        FormattingUtils.Case.AKUZATIV)))

                .setContentTitle(getString(R.string.alarm_missed))
                .setContentText(getString(R.string.alarm_is_missed_for,
                        " " + FormattingUtils.getCaseTitle(
                                mPrayer.getId(),
                                FormattingUtils.Case.AKUZATIV)));

        mNotificationManager.notify(ALARM_NOTIF_MISSED, notifBuilder.build());
    }

    @Override
    public void onBackPressed() {
        mPrefs.edit().putBoolean(Prefs.ALARM_ACTIVE, false).commit();
        super.onBackPressed();
    }

    @Override
    protected void onUserLeaveHint() {
        Log.d(TAG, "onUserLeaveHint");
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
