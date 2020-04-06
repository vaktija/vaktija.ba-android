package ba.vaktija.android.service;

import ba.vaktija.android.BuildConfig;
import ba.vaktija.android.MainActivityHelper;
import ba.vaktija.android.R;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.Utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class NotificationsManager {
    public static final String TAG = NotificationsManager.class.getSimpleName();

    private static final String DEFAULT_CHANNEL = "DEFAULT_CHANNEL";

    private static final int ONGOING_NOTIF = 77;
    static final int NOTIF_UPDATE_INTERVAL = 10 * 1000; //10s

    private static NotificationsManager instance;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPrefs;
    private Context mContext;
    private Prayer mPrayer;
    private boolean mApproaching;
    private boolean mStatusbarNotification;
    private NotificationCompat.Builder mCountdownNotifBuilder;
    private CountDownTimer mCountDownTimer;
    private boolean mSilentModeOn;
    private NotificationCompat.BigTextStyle mBigTextStyle;

    private NotificationsManager(Context context){
        mContext = context;
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotifChannels();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mPrayer = PrayersSchedule.getInstance(mContext).getCurrentPrayer();
        mApproaching = PrayersSchedule.getInstance(mContext).isNextPrayerApproaching();

        mStatusbarNotification = mPrefs.getBoolean(
                Prefs.STATUSBAR_NOTIFICATION,
                Defaults.STATUSBAR_NOTIFICATION);
    }

    private void createNotifChannels(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String desc = mContext.getString(R.string.notif_default_channel_desc);
            createNotifChannel(DEFAULT_CHANNEL, desc);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void  createNotifChannel(String channelId, String desc){

//        https://stackoverflow.com/a/51802085
//        mNotificationManager.deleteNotificationChannel(DEFAULT_CHANNEL);

        NotificationChannel notificationChannel = new NotificationChannel(channelId, desc, NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableVibration(true);
        notificationChannel.setShowBadge(false);
        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    public static NotificationsManager getInstance(Context context){
        if(instance == null){
            instance = new NotificationsManager(context);
        } else {
            instance.mPrayer = PrayersSchedule.getInstance(context).getCurrentPrayer();
            instance.mApproaching = PrayersSchedule.getInstance(context).isNextPrayerApproaching();

            instance.mStatusbarNotification = instance.mPrefs.getBoolean(
                    Prefs.STATUSBAR_NOTIFICATION,
                    Defaults.STATUSBAR_NOTIFICATION);
        }

        return instance;
    }

    @SuppressLint("NewApi")
    public void buildCountDownNotif(boolean showTicker){
        FileLog.d(TAG, "[buildCountdownNotification] showTicker=" + showTicker);

        String city = mPrefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME);

        mSilentModeOn = SilentModeManager.getInstance(mContext).silentShoudBeActive();
        //mSilentModeOn &= SilentModeManager.getInstance(mContext).silentSetByApp();

        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + mPrayer.getId(), false);
        boolean silentNotifDeleted = mPrefs.getBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + mPrayer.getId(), false);

        boolean allPrayersInNotif = mPrefs.getBoolean(Prefs.ALL_PRAYERS_IN_NOTIF, Defaults.ALL_PRAYERS_IN_NOTIF_DEFAULT);

        mApproaching = mApproaching && !approachingNotifDeleted;

        Intent resultIntent = new Intent(mContext, MainActivityHelper.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                mContext,
                (int) System.currentTimeMillis(),
                resultIntent,
                0);

        String title = Prayer.getNextVakatTitle(mPrayer.getId())+" je za ";
        title += FormattingUtils.getTimeString(PrayersSchedule.getInstance(mContext).getTimeTillNextPrayer(), false);

        mBigTextStyle = new NotificationCompat.BigTextStyle();
        mBigTextStyle.bigText(PrayersSchedule.getInstance(mContext).getAllPrayersTimes())
                .setBigContentTitle(title)
                .setSummaryText(mContext.getString(R.string.vaktija_url));

        mCountdownNotifBuilder = new NotificationCompat.Builder(mContext, DEFAULT_CHANNEL)
                .setSmallIcon(R.drawable.ic_notif_default)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(mStatusbarNotification)
                .setContentTitle(title)
                .setContentText(PrayersSchedule.getInstance(mContext).getCurrentAndNextTime());

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

            String silentDeactivationTime = PrayersSchedule.getInstance(mContext).getSilentModeDurationString();

            if(showTicker){
                mCountdownNotifBuilder.setTicker(FormattingUtils.getVakatAnnouncement(mPrayer.getShortTitle()));
            }

            if(SilentModeManager.getInstance(mContext).silentSetByApp()) {

                CharSequence contentTitle = Utils.boldNumbers("Bez zvukova do " + silentDeactivationTime);

                mCountdownNotifBuilder.setContentTitle(contentTitle);
                mBigTextStyle.setBigContentTitle(contentTitle);
            } else {
                mCountdownNotifBuilder.setContentTitle("Zvukovi isključeni");
                mBigTextStyle.setBigContentTitle("Zvukovi isključeni");
            }

            Intent silentOffIntent = VaktijaService.getStartIntent(mContext, "piSilentOff");
            silentOffIntent.setAction(VaktijaService.ACTION_SKIP_SILENT);
            PendingIntent piSilentoff = PendingIntent.getService(
                    mContext,
                    (int) System.currentTimeMillis(),
                    silentOffIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            mCountdownNotifBuilder.setSmallIcon(R.drawable.ic_notif_info);

            mCountdownNotifBuilder.addAction(R.drawable.ic_notif_sound_on, "Uključi zvukove", piSilentoff);

            Intent deleteIntent = VaktijaService.getStartIntent(mContext, "piDeleteIntent");
            deleteIntent.setAction(VaktijaService.ACTION_SILENT_NOTIFICATION_DELETED);

            PendingIntent piDeleteIntent = PendingIntent.getService(
                    mContext,
                    (int) System.currentTimeMillis(),
                    deleteIntent,
                    0);

            if(!mStatusbarNotification){
                mCountdownNotifBuilder.setDeleteIntent(piDeleteIntent);
            }
        }

        if (mApproaching){
            String time = FormattingUtils.getTimeString(PrayersSchedule.getInstance(mContext).getTimeTillNextPrayer(), false);

            if(showTicker){
                mCountdownNotifBuilder.setTicker("Uskoro nastupa "+Prayer.getNextVakatTitle(mPrayer.getId()));
            }

            CharSequence contentText = Utils.boldNumbers("Uskoro je " + Prayer.getNextVakatTitle(mPrayer.getId()) + " (" + time + ")");

            mCountdownNotifBuilder.setContentText(contentText);
            mBigTextStyle.setBigContentTitle(contentText);

            mCountdownNotifBuilder.setSmallIcon(R.drawable.ic_notif_info);

            Prayer nextVakat = PrayersSchedule.getInstance(mContext).getNextPrayer(mPrayer.getId());

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
                                Defaults.getDefaultTone(mContext, false)));

                if(mPrefs.getBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, true)) {
                    notifSoundUri = Uri.parse(Defaults.getDefaultTone(mContext, false));
                }

                mCountdownNotifBuilder.setSound(notifSoundUri);
            }

            //			mCountdownNotifBuilder.setLights(mPrefs.getInt(Prefs.THEME_COLOR, 0xFFFFFF), 1000, 2000);

            Intent deleteIntent = VaktijaService.getStartIntent(mContext, "deleteIntent");
            deleteIntent.setAction(VaktijaService.ACTION_APPROACHING_NOTIFICATION_DELETED);

            PendingIntent di = PendingIntent.getService(
                    mContext,
                    (int) System.currentTimeMillis(),
                    deleteIntent,
                    0);

            if(!mStatusbarNotification){
                mCountdownNotifBuilder.setDeleteIntent(di);
            }
        }

        Notification notif = mCountdownNotifBuilder.build();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
            notif.priority = Notification.PRIORITY_MAX;
        }

        mNotificationManager.notify(ONGOING_NOTIF, notif);
    }

    private void startCountDownTimer(){
        FileLog.d(TAG, "[startCountDownTimer]");

        if(mCountDownTimer != null)
            mCountDownTimer.cancel();

        mCountDownTimer = new CountDownTimer(PrayersSchedule.getInstance(mContext).getTimeTillNextPrayer() * 1000, NOTIF_UPDATE_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {

                if(mCountdownNotifBuilder == null)
                    buildCountDownNotif(true);

                if(!mSilentModeOn)
                    updateCountDownNotif((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                FileLog.d(TAG, "count down timer has finished");
            }
        };
        mCountDownTimer.start();
    }

    private void updateCountDownNotif(int secRemaining){

        CharSequence contentTitle = getTimeTillNext(secRemaining, true);
        mCountdownNotifBuilder.setContentTitle(contentTitle);
        mBigTextStyle.setBigContentTitle(contentTitle);

        mCountdownNotifBuilder.setContentText(PrayersSchedule.getInstance(mContext).getCurrentAndNextTime());

        if(mApproaching){
            String time = FormattingUtils.getTimeString(PrayersSchedule.getInstance(mContext).getTimeTillNextPrayer(), true);
            contentTitle = Utils.boldNumbers("Uskoro je " + Prayer.getNextVakatTitle(mPrayer.getId()) + " (" + time + ")");
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
        mNotificationManager.notify(ONGOING_NOTIF, mCountdownNotifBuilder.build());

        Utils.updateWidget(mContext);
    }

    private CharSequence getTimeTillNext(int seconds, boolean ceil){

        String time = Prayer.getNextVakatTitle(mPrayer.getId())+" je za "+FormattingUtils.getTimeString(seconds, ceil);

        return Utils.boldNumbers(time);
    }

    public void cancelNotification() {
        if(mCountDownTimer != null){
            mCountDownTimer.cancel();
        }

        if(mNotificationManager != null){
            mNotificationManager.cancelAll();
        }
    }

    public void onSilentNotifDeleted() {
        FileLog.d(TAG, "[onSilentNotifDeleted]");

        mPrefs.edit()
                .putBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + mPrayer.getId(), true)
                .commit();
    }

    public void onApproachingNotifDeleted() {
        FileLog.d(TAG, "[onApproachingNotifDeleted]");

        mPrefs.edit()
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + mPrayer.getId(), true)
                .commit();

        cancelExistingNotif();
    }

    public void showApproachingNotification() {
        FileLog.d(TAG, "[showApproachingNotification]");

        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + mPrayer.getId(), false);

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
                .commit();

        if(enabled){
            resetStoredState();
        }

        boolean silentNotifDeleted = mPrefs.getBoolean(Prefs.SILENT_NOTIF_DELETED + "_" + mPrayer.getId(), false);
        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED+"_"+ mPrayer.getId(), false);

        if(enabled){
            buildCountDownNotif(true);
            startCountDownTimer();
        } else {
            cancelExistingNotif();
        }

        if(SilentModeManager.getInstance(mContext).silentShoudBeActive() && !silentNotifDeleted){
            buildCountDownNotif(false);
        }

        mApproaching = PrayersSchedule.getInstance(mContext).isNextPrayerApproaching();

        if(mApproaching && !approachingNotifDeleted){
            buildCountDownNotif(false);
        }
    }

    public void updateNotification() {
        FileLog.d(TAG, "[updateNotification]");

        mApproaching = PrayersSchedule.getInstance(mContext).isNextPrayerApproaching();

        boolean approachingNotifDeleted = mPrefs.getBoolean(Prefs.APPROACHING_NOTIF_DELETED + "_" + mPrayer.getId(), false);

        if(mApproaching && approachingNotifDeleted){
            FileLog.w(TAG, "Not updating notification, approaching notification deleted");
            return;
        }

        boolean statusbarNotif = mPrefs.getBoolean(
                Prefs.STATUSBAR_NOTIFICATION,
                Defaults.STATUSBAR_NOTIFICATION);

        boolean silentNotifDeleted = mPrefs.getBoolean(Prefs.SILENT_NOTIF_DELETED+"_"+ mPrayer.getId(), false);

        FileLog.i(TAG, "statusbar notification on: "+statusbarNotif);
        FileLog.i(TAG, "silent notification deleted: "+silentNotifDeleted);

        if(SilentModeManager.getInstance(mContext).silentShoudBeActive()
                && !silentNotifDeleted){
            buildCountDownNotif(false);
        } else if(!mApproaching && !statusbarNotif){
            cancelExistingNotif();
        } else {
            buildCountDownNotif(false);
            startCountDownTimer();
        }
    }

    private void cancelExistingNotif(){
        if(mNotificationManager != null)
            mNotificationManager.cancelAll();

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
                .commit();
    }
}
