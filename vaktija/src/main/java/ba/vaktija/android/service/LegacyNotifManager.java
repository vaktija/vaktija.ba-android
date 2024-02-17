package ba.vaktija.android.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;

import ba.vaktija.android.AlarmActivity;
import ba.vaktija.android.MainActivityHelper;
import ba.vaktija.android.R;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.receiver.AlarmReceiver;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.Utils;

public class LegacyNotifManager implements NotifManager {
    public static final String TAG = LegacyNotifManager.class.getSimpleName();

    private static LegacyNotifManager instance;
    protected NotificationManager notificationManager;
    private SharedPreferences mPrefs;
    protected Context context;
    protected Prayer mPrayer;
    protected Prayer mNextPrayer;
    protected boolean approaching;
    private boolean mStatusbarNotification;
    private NotificationCompat.Builder mCountdownNotifBuilder;
    private CountDownTimer mCountDownTimer;
    private boolean mSilentModeOn;
    private NotificationCompat.BigTextStyle mBigTextStyle;

    protected LegacyNotifManager(Context context){
        this.context = context;
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mPrayer = PrayersSchedule.getInstance(this.context).getCurrentPrayer();
        mNextPrayer = PrayersSchedule.getInstance(this.context).getNextPrayer();
        approaching = PrayersSchedule.getInstance(this.context).isNextPrayerApproaching();

        mStatusbarNotification = mPrefs.getBoolean(
                Prefs.STATUSBAR_NOTIFICATION,
                Defaults.STATUSBAR_NOTIFICATION);
    }

    public static LegacyNotifManager getInstance(Context context){
        if(instance == null){
            instance = new LegacyNotifManager(context);
        } else {
            instance.mPrayer = PrayersSchedule.getInstance(context).getCurrentPrayer();
            instance.approaching = PrayersSchedule.getInstance(context).isNextPrayerApproaching();

            instance.mStatusbarNotification = instance.mPrefs.getBoolean(
                    Prefs.STATUSBAR_NOTIFICATION,
                    Defaults.STATUSBAR_NOTIFICATION);
        }

        return instance;
    }

    public void buildCountDownNotif(boolean showTicker){
        FileLog.d(TAG, "[buildCountdownNotification] showTicker=" + showTicker);

        Notification notif = getOngoingNotif(showTicker, DEFAULT_CHANNEL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notif.priority = Notification.PRIORITY_MAX;
        }
        notificationManager.notify(ONGOING_NOTIF, notif);
    }

    @SuppressLint("NewApi")
    public Notification getOngoingNotif(boolean showTicker, String channel){
        FileLog.d(TAG, "[buildCountdownNotification] showTicker=" + showTicker+" channel="+channel);

        String city = mPrefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME);

        mSilentModeOn = SilentModeManager.getInstance(context).silentShoudBeActive();

        int nextPrayerId = mPrayer.getId() + 1;

        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + nextPrayerId, false);

        boolean allPrayersInNotif = mPrefs.getBoolean(Prefs.ALL_PRAYERS_IN_NOTIF, Defaults.ALL_PRAYERS_IN_NOTIF_DEFAULT);

        approaching = approaching && !approachingNotifDeleted;

        Intent resultIntent = new Intent(context, MainActivityHelper.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                resultIntent,
                0);

        String title;

        if(mPrefs.getBoolean(Prefs.STATUSBAR_NOTIFICATION, true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            title = Prayer.getNextVakatTitle(mPrayer.getId())+" je za ";
            title += FormattingUtils.getTimeString(PrayersSchedule.getInstance(context).getTimeTillNextPrayer());
        } else {
            title = "Uskoro je "+Prayer.getNextVakatTitle(mPrayer.getId());
        }

        mBigTextStyle = new NotificationCompat.BigTextStyle();
        mBigTextStyle.bigText(PrayersSchedule.getInstance(context).getAllPrayersTimes())
                .setBigContentTitle(title)
                .setSummaryText(context.getString(R.string.vaktija_url));

        mCountdownNotifBuilder = new NotificationCompat.Builder(context, channel)
                .setSmallIcon(R.drawable.ic_notif_default)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(mStatusbarNotification)
                .setContentTitle(title)
                .setContentText(PrayersSchedule.getInstance(context).getCurrentAndNextTime());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            mCountdownNotifBuilder.setSubText(city);
        } else {
            mCountdownNotifBuilder.setContentInfo(city);
        }

        if(allPrayersInNotif){
            mCountdownNotifBuilder.setStyle(mBigTextStyle);
        }

        if(showTicker) {
            mCountdownNotifBuilder.setTicker(title);
        }

        if(mSilentModeOn){

            String silentDeactivationTime = PrayersSchedule.getInstance(context).getSilentModeDurationString();

            if(showTicker){
                mCountdownNotifBuilder.setTicker(FormattingUtils.getVakatAnnouncement(mPrayer.getShortTitle()));
            }

            if(SilentModeManager.getInstance(context).silentSetByApp()) {

                CharSequence contentTitle = Utils.boldNumbers("Bez zvukova do " + silentDeactivationTime);

                mCountdownNotifBuilder.setContentTitle(contentTitle);
                mBigTextStyle.setBigContentTitle(contentTitle);
            } else {
                mCountdownNotifBuilder.setContentTitle("Zvukovi isključeni");
                mBigTextStyle.setBigContentTitle("Zvukovi isključeni");
            }

            Intent silentOffIntent = VaktijaService.getStartIntent(context, "piSilentOff");
            silentOffIntent.setAction(VaktijaService.ACTION_SKIP_SILENT);
            PendingIntent piSilentoff = PendingIntent.getService(
                    context,
                    (int) System.currentTimeMillis(),
                    silentOffIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mCountdownNotifBuilder.setSmallIcon(R.drawable.ic_notif_info);

            mCountdownNotifBuilder.addAction(R.drawable.ic_notif_sound_on, "Uključi zvukove", piSilentoff);

            Intent deleteIntent = VaktijaService.getStartIntent(context, "piDeleteIntent");
            deleteIntent.setAction(VaktijaService.ACTION_SILENT_NOTIFICATION_DELETED);

            PendingIntent piDeleteIntent = PendingIntent.getService(
                    context,
                    (int) System.currentTimeMillis(),
                    deleteIntent,
                    0);

            if(!mStatusbarNotification){
                mCountdownNotifBuilder.setDeleteIntent(piDeleteIntent);
            }
        }

        if (approaching){
            String time = FormattingUtils.getTimeString(PrayersSchedule.getInstance(context).getTimeTillNextPrayer());

            if(showTicker){
                mCountdownNotifBuilder.setTicker("Uskoro nastupa "+Prayer.getNextVakatTitle(mPrayer.getId()));
            }

//            CharSequence contentText = Utils.boldNumbers("Uskoro je " + Prayer.getNextVakatTitle(mPrayer.getId()) + " (" + time + ")");
//
//            mCountdownNotifBuilder.setContentText(contentText);
//            mBigTextStyle.setBigContentTitle(contentText);

            CharSequence contentTitle = Utils.boldNumbers("Uskoro je " + Prayer.getNextVakatTitle(mPrayer.getId()) + " (" + time + ")");
            mCountdownNotifBuilder.setContentTitle(contentTitle);
            mBigTextStyle.setBigContentTitle(contentTitle);

            mCountdownNotifBuilder.setSmallIcon(R.drawable.ic_notif_info);

            Prayer nextVakat = PrayersSchedule.getInstance(context).getNextPrayer(mPrayer.getId());

//            Ringtone defaultRingtone = RingtoneManager.getRingtone(mContext, notifSoundUri);
//            String notifSoundName = defaultRingtone.getTitle(mContext);
//
//            FileLog.i(TAG, "notifSoundName="+notifSoundName);

            if(nextVakat.isNotifVibroOn()){
                mCountdownNotifBuilder.setVibrate(new long[]{0, 200, 200, 200, 200, 200, 200 - 1});
            }

            if(nextVakat.isNotifSoundOn()){

                Uri notifSoundUri = Uri.parse(
                        mPrefs.getString(
                                Prefs.NOTIF_TONE_URI,
                                Defaults.getDefaultTone(context, false)));

                if(mPrefs.getBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, true)) {
                    notifSoundUri = Uri.parse(Defaults.getDefaultTone(context, false));
                }

                mCountdownNotifBuilder.setSound(notifSoundUri);
            }

            //			mCountdownNotifBuilder.setLights(mPrefs.getInt(Prefs.THEME_COLOR, 0xFFFFFF), 1000, 2000);

            Intent deleteIntent = VaktijaService.getStartIntent(context, "deleteIntent");
            deleteIntent.setAction(VaktijaService.ACTION_APPROACHING_NOTIFICATION_DELETED);

            PendingIntent di = PendingIntent.getService(
                    context,
                    (int) System.currentTimeMillis(),
                    deleteIntent,
                    0);

            if(!mStatusbarNotification){
                mCountdownNotifBuilder.setDeleteIntent(di);
            }
        }

        return mCountdownNotifBuilder.build();
    }

    private void startCountDownTimer(){
        FileLog.d(TAG, "[startCountDownTimer]");

        if(mCountDownTimer != null)
            mCountDownTimer.cancel();

        mCountDownTimer = new CountDownTimer(PrayersSchedule.getInstance(context).getTimeTillNextPrayer() * 1000, NOTIF_UPDATE_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {

                if(mCountdownNotifBuilder == null)
                    buildCountDownNotif(true);

                if(!mSilentModeOn)
                    updateCountDownNotif((int) (millisUntilFinished/1000),PrayersSchedule.getInstance(context).getTimeTillNextPrayer(true));
            }

            @Override
            public void onFinish() {
                FileLog.d(TAG, "count down timer has finished");
            }
        };
        mCountDownTimer.start();
    }

    private void updateCountDownNotif(int secRemaining,int secondSecRemaining){

        CharSequence contentTitle = getTimeTillNext(secRemaining,secondSecRemaining);
        mCountdownNotifBuilder.setContentTitle(contentTitle);
        mBigTextStyle.setBigContentTitle(contentTitle);

        mCountdownNotifBuilder.setContentText(PrayersSchedule.getInstance(context).getCurrentAndNextTime());

        if(approaching){
            String time = FormattingUtils.getTimeString(PrayersSchedule.getInstance(context).getTimeTillNextPrayer());
            contentTitle = Utils.boldNumbers("Uskoro je " + Prayer.getNextVakatTitle(mPrayer.getId()) + ": " + time);
            mCountdownNotifBuilder.setContentTitle(contentTitle);
            mBigTextStyle.setBigContentTitle(contentTitle);
        }

        String city = mPrefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            mCountdownNotifBuilder.setSubText(city);
        } else {
            mCountdownNotifBuilder.setContentInfo(city);
        }

        mCountdownNotifBuilder.setWhen(System.currentTimeMillis());
        notificationManager.notify(ONGOING_NOTIF, mCountdownNotifBuilder.build());

        Utils.updateWidget(context);
    }

    private CharSequence getTimeTillNext(int seconds,int secondSeconds){

        String time = Prayer.getNextVakatTitle(mPrayer.getId()) +" za: "+FormattingUtils.getTimeString(seconds);
        if(mPrefs.getBoolean(Prefs.SECOND_VAKAT_IN_NOTIF, true))
            time += " · " + Prayer.getNextVakatTitle(mNextPrayer.getId()) + " za: "+FormattingUtils.getTimeString(secondSeconds);

        return Utils.boldNumbers(time);
    }

    public void cancelNotification() {
        if(mCountDownTimer != null){
            mCountDownTimer.cancel();
        }

        if(notificationManager != null){
            notificationManager.cancelAll();
        }
    }

    @Override
    public void onSilentNotifDeleted() {
        FileLog.d(TAG, "[onSilentNotifDeleted]");

        mPrefs.edit()
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + mPrayer.getId(), true)
                .apply();
    }

    @Override
    public void onApproachingNotifDeleted() {
        FileLog.d(TAG, "[onApproachingNotifDeleted]");

        int nextPrayerId = mPrayer.getId() + 1;

        mPrefs.edit()
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + nextPrayerId, true)
                .apply();

        cancelExistingNotif();
    }

    @Override
    public void showApproachingNotification() {
        FileLog.d(TAG, "[showApproachingNotification]");

        int nextPrayerId = mPrayer.getId() + 1;

        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + nextPrayerId, false);

        if(approachingNotifDeleted){
            FileLog.w(TAG, "Not showing approaching notification, notification deleted");
            return;
        }

        cancelExistingNotif();
        buildCountDownNotif(true);
        startCountDownTimer();
    }

    public void setNotificationsEnabled(boolean enabled) {
        FileLog.d(TAG, "[setNotificationsEnabled] enabled="+enabled);

        mPrefs.edit()
                .putBoolean(Prefs.STATUSBAR_NOTIFICATION, enabled)
                .apply();

        if(enabled){
            resetStoredState();
        }

        int nextPrayerId = mPrayer.getId() + 1;

        boolean silentNotifDeleted = mPrefs.getBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + mPrayer.getId(), false);
        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED+"_"+ nextPrayerId, false);

        if(enabled){
            buildCountDownNotif(true);
            startCountDownTimer();
        } else {
            cancelExistingNotif();
        }

        if(SilentModeManager.getInstance(context).silentShoudBeActive() && !silentNotifDeleted){
            buildCountDownNotif(false);
        }

        approaching = PrayersSchedule.getInstance(context).isNextPrayerApproaching();

        if(approaching && !approachingNotifDeleted){
            buildCountDownNotif(false);
        }
    }

    @Override
    public void updateNotification() {
        FileLog.d(TAG, "[updateNotification]");

        approaching = PrayersSchedule.getInstance(context).isNextPrayerApproaching();

        int nextPrayerId = mPrayer.getId() + 1;

        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + nextPrayerId, false);

        if(approaching && approachingNotifDeleted){
            FileLog.w(TAG, "Not updating notification, approaching notification deleted");
            return;
        }

        boolean statusbarNotif = mPrefs.getBoolean(
                Prefs.STATUSBAR_NOTIFICATION,
                Defaults.STATUSBAR_NOTIFICATION);

        boolean silentNotifDeleted = mPrefs.getBoolean(Prefs.SILENT_NOTIF_DELETED+"_"+ mPrayer.getId(), false);

        FileLog.i(TAG, "statusbar notification on: "+statusbarNotif);
        FileLog.i(TAG, "silent notification deleted: "+silentNotifDeleted);

        if(SilentModeManager.getInstance(context).silentShoudBeActive()
                && !silentNotifDeleted){
            buildCountDownNotif(false);
        } else if(!approaching && !statusbarNotif){
            cancelExistingNotif();
        } else {
            buildCountDownNotif(false);
            startCountDownTimer();
        }
    }

    public Notification getAlarmNotif(Prayer prayer) {

        if(prayer == null) {

            // Prayer is null when notification is needed to satisfy requirement that services started
            // using startForegroundService must show notification. In this case we can just show dummy notification,
            // it is going to be cleared immediately after showing

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(context, ALARMS_CHANNEL)
                            .setSmallIcon(R.drawable.ic_notif_info)
                            .setContentTitle("Alarm")
                            .setContentText("Alarm")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setCategory(NotificationCompat.CATEGORY_ALARM);

            return notificationBuilder.build();
        }

        Intent cancelActionIntent = new Intent(context, AlarmReceiver.class);
        cancelActionIntent.setAction(AlarmReceiver.ACTION_DISMISS_ALARM);

        PendingIntent cancelActionPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) System.currentTimeMillis(),
                cancelActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Action cancelAction = new NotificationCompat.Action(
                R.drawable.ic_sound_off,
                "Ugasi Alarm",
                cancelActionPendingIntent
        );

        Intent fullScreenIntent = new Intent(context, AlarmActivity.class);
        fullScreenIntent.putExtra(AlarmActivity.EXTRA_PRAYER_ID, prayer.getId());
        fullScreenIntent.putExtra(AlarmActivity.EXTRA_PLAY_ALARM_SOUND, false);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context, (int) System.currentTimeMillis(),
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String text = prayer.getTitle() + " je za " + FormattingUtils.getTimeString(
                PrayersSchedule.getInstance(context).getTimeTillNextPrayer());

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, ALARMS_CHANNEL)
                        .setSmallIcon(R.drawable.ic_notif_info)
                        .setContentTitle(prayer.getTitle() + " • Alarm")
                        .setContentText(text)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .addAction(cancelAction)
                        .setFullScreenIntent(fullScreenPendingIntent, true);

        return notificationBuilder.build();
    }

    public void cancelAlarmNotif(){
        notificationManager.cancel(ALARM_NOTIF);
    }

    @Override
    public void cancelApproachingNotif(){
        // no-op
    }

    private void cancelExistingNotif(){
        if(notificationManager != null)
            notificationManager.cancelAll();

        if(mCountDownTimer != null)
            mCountDownTimer.cancel();
    }

    private void resetStoredState(){
        FileLog.d(TAG, "[resetStoredState]");
        mPrefs.edit()
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + Prayer.FAJR, false)
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + Prayer.SUNRISE, false)
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + Prayer.DHUHR, false)
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + Prayer.ASR, false)
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + Prayer.MAGHRIB, false)
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + Prayer.ISHA, false)
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + Prayer.FAJR, false)
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + Prayer.SUNRISE, false)
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + Prayer.DHUHR, false)
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + Prayer.ASR, false)
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + Prayer.MAGHRIB, false)
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + Prayer.ISHA, false)
                .apply();
    }
}
