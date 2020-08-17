package ba.vaktija.android.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.io.IOException;

import ba.vaktija.android.App;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.util.FileLog;

public class OngoingAlarmService extends Service {
    private static final String TAG = "OngoingAlarmService";

    private static final int ALARM_TIMEOUT = 2 /*mins*/ * 60 /*sec*/ * 1000 /*millisec*/;

    public static final String ACTION_START_ALARM = "ACTION_START_ALARM";
    public static final String ACTION_STOP_ALARM = "ACTION_STOP_ALARM";

    private static final String EXTRA_PRAYER_ID = "EXTRA_PRAYER_ID";

    private AlarmSoundPlayer alarmSoundPlayer;
    private CountDownTimer countDownTimer;
    private NotifManager notificationsManager;
    private int startId;

    public static Intent getStartAlarmIntent(Context context, int prayerId){
        Intent intent = new Intent(context, OngoingAlarmService.class);
        intent.setAction(ACTION_START_ALARM);
        intent.putExtra(EXTRA_PRAYER_ID, prayerId);
        return intent;
    }

    public static Intent getStopAlarmIntent(Context context){
        Intent intent = new Intent(context, OngoingAlarmService.class);
        intent.setAction(ACTION_STOP_ALARM);
        return intent;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        FileLog.d(TAG,"onStartCommand");

        this.startId = startId;

        String action = intent.getAction();

        if(TextUtils.isEmpty(action)) {
            FileLog.e(TAG,"Action is required");
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        FileLog.d(TAG,"action=" + action);

        notificationsManager = NotifManagerFactory.getNotifManager(this);

        if(action.equals(ACTION_START_ALARM)) {

            int prayerId = intent.getIntExtra(EXTRA_PRAYER_ID, -1);

            if (prayerId == -1) {
                FileLog.w(TAG, "Required EXTRA_PRAYER_ID not passed to service");
                stopSelf(startId);
            } else {
                Prayer prayer = PrayersSchedule.getInstance(this).getPrayer(prayerId);
                startForeground(
                        NotifManager.ALARM_NOTIF,
                        notificationsManager.getAlarmNotif(prayer));

                final Uri soundUri = App.app.getAlarmSoundUri();

                alarmSoundPlayer = new AlarmSoundPlayer(this);
                try {
                    alarmSoundPlayer.play(soundUri, true);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                startCountDownTimer();
            }

        } else if(action.equals(ACTION_STOP_ALARM)) {
            if(countDownTimer != null) {
                countDownTimer.cancel();
            }
            quit();
        }

        return START_NOT_STICKY;
    }

    void startCountDownTimer(){
        FileLog.d(TAG, "startCountDownTimer");

        countDownTimer = new CountDownTimer(ALARM_TIMEOUT, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                int remaining = (int) millisUntilFinished / 1000;
                FileLog.d(TAG, "on tick " + remaining);
            }

            @Override
            public void onFinish() {
                quit();
            }
        };

        countDownTimer.start();
    }

    private void quit(){
        FileLog.d(TAG, "quit");
        if(alarmSoundPlayer != null) {
            alarmSoundPlayer.cancel();
        }
        notificationsManager.cancelAlarmNotif();
        stopSelf(startId);
    }
}
