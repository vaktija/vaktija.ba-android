package ba.vaktija.android.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import ba.vaktija.android.App;
import ba.vaktija.android.models.Events;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.Utils;

import de.greenrobot.event.EventBus;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

public class VaktijaService extends Service {

    public static final String TAG = VaktijaService.class.getSimpleName();

    private static final String STARTED_FROM = "STARTED_FROM";

    public static final String ACTION_SKIP_SILENT = "ACTION_SKIP_SILENT";
    public static final String ACTION_DISABLE_NOTIFS = "ACTION_DISABLE_NOTIFS";
    public static final String ACTION_ENABLE_NOTIFS = "ACTION_ENABLE_NOTIFS";
    public static final String ACTION_SHOW_APPROACHING_NOTIFICATION = "ACTION_SHOW_APPROACHING_NOTIFICATION";
    public static final String ACTION_PRAYER_CHANGE = "ACTION_PRAYER_CHANGE";
    public static final String ACTION_DEACTIVATE_SILENT = "ACTION_DEACTIVATE_SILENT";
    public static final String ACTION_NEW_DAY = "ACTION_NEW_DAY";
    public static final String ACTION_QUIT = "ACTION_QUIT";
    public static final String ACTION_SILENT_NOTIFICATION_DELETED = "ACTION_SILENT_NOTIFICATION_DELETED";
    public static final String ACTION_APPROACHING_NOTIFICATION_DELETED = "ACTION_APPROACHING_NOTIFICATION_DELETED";
    public static final String ACTION_ALARM_CHANGED = "ACTION_ALARM_CHANGED";
    public static final String ACTION_SILENT_CHANGED = "ACTION_SILENT_CHANGED";
    public static final String ACTION_NOTIF_CHANGED = "ACTION_NOTIF_CHANGED";
    public static final String ACTION_BOOT_COMPLETED = "ACTION_BOOT_COMPLETED";
    public static final String ACTION_LOCK_CHANGED = "ACTION_LOCK_CHANGED";
    public static final String ACTION_TIME_CHANGED = "ACTION_TIME_CHANGED";
    public static final String ACTION_SILENT_ACTIVATED = "ACTION_SILENT_ACTIVATED";
    public static final String ACTION_UPDATE = "ACTION_UPDATE";
    public static final String ACTION_VOLUME_CHANGED = "ACTION_VOLUME_CHANGED";

    private static final int NEW_DAY_ALARM_ID = 101010;

    private SharedPreferences mPrefs;

    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private AlarmManager mAlarmManager;

    private App mApp;
    private Prayer mPrayer;

    private EventBus mEventBus = EventBus.getDefault();

    private BroadcastReceiver mScreenOnReceiver;

    @Override
    public void onCreate() {
        FileLog.d(TAG, "[onCreate]");

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        FileLog.i(TAG, "wizard completed: "+mPrefs.getBoolean(Prefs.WIZARD_COMPLETED, false));

        if(!mPrefs.getBoolean(Prefs.WIZARD_COMPLETED, false)){
            return;
        }

        mApp = (App) getApplicationContext();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        acquireWakeLock();

        registerScreenOnReceiver();
    }

    private void acquireWakeLock(){
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.setReferenceCounted(false);
    }

    private void resetStoredState(){
        FileLog.d(TAG, "[resetStoredState]");

        SharedPreferences.Editor editor = mPrefs.edit();

        for(Prayer p : PrayersSchedule.getInstance(this).getAllPrayers()){
            editor.putBoolean(Prefs.APPROACHING_NOTIF_DELETED+"_"+p.getId(), false);
            editor.putBoolean(Prefs.SILENT_NOTIF_DELETED+"_"+p.getId(), false);
        }

        editor.putBoolean(Prefs.ACTUAL_EVENT_MESSAGE_SHOWN, false);
        editor.putBoolean(Prefs.SILENT_DISABLED_BY_USER, false);
        editor.commit();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        FileLog.d(TAG, "[onStartCommand] startId=" + startId);

        if(!mPrefs.getBoolean(Prefs.WIZARD_COMPLETED, false)){
            return START_NOT_STICKY;
        }

        boolean userQuit = mPrefs.getBoolean(Prefs.USER_CLOSED, false);

        if(userQuit){
            FileLog.w(TAG, "App is userQuit");
            shutdown(startId);
            return START_NOT_STICKY;
        }

        if(mWakeLock == null){
            acquireWakeLock();
        }

        mWakeLock.acquire(); // TODO can be null?

        processStartCommand(intent, startId);

        mWakeLock.release();

        return START_STICKY;
    }

    private void processStartCommand(Intent intent, int startId){

        boolean summerTime = Prayer.isSummerTime();
        FileLog.i(TAG, "summer time active: "+summerTime);

        if(mPrefs.getBoolean(Prefs.SUMMER_TIME, false) != summerTime){
            FileLog.i(TAG, "summer time changed");
            mPrefs.edit()
                    .putBoolean(Prefs.SUMMER_TIME, summerTime)
                    .commit();

            PrayersSchedule.getInstance(this).reset();
            mEventBus.post(new Events.PrayerChangedEvent());
        }

        mPrayer = PrayersSchedule.getInstance(this).getCurrentPrayer();

        //FileLog.i(TAG, "mPrayer: "+ mPrayer);

        String action = "";

        if(intent != null){

            if(intent.getAction() != null)
                action = intent.getAction();

            String startedFrom = intent.getStringExtra(STARTED_FROM);
            FileLog.d(TAG, "started from: "+startedFrom);
        }

        FileLog.i(TAG, "action: " + action);

        if(action.startsWith(ACTION_SHOW_APPROACHING_NOTIFICATION))
            action = ACTION_SHOW_APPROACHING_NOTIFICATION;

        if(action.startsWith(ACTION_DEACTIVATE_SILENT))
            action = ACTION_DEACTIVATE_SILENT;

        if(action.startsWith(ACTION_PRAYER_CHANGE))
            action = ACTION_PRAYER_CHANGE;

        switch (action) {

            case ACTION_UPDATE:
            case ACTION_BOOT_COMPLETED:
            case ACTION_TIME_CHANGED:
                resetDay();
                resetStoredState();
                scheduleAllEvents();
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                break;
            case ACTION_LOCK_CHANGED:
                NotificationsManager.getInstance(this).updateNotification();
                break;
            case ACTION_SILENT_CHANGED:
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                mEventBus.post(new Events.PrayerChangedEvent());
                scheduleSilentActivationEvents();
                break;
            case ACTION_SILENT_ACTIVATED:
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                break;
            case ACTION_NOTIF_CHANGED:
                NotificationsManager.getInstance(this).updateNotification();
                scheduleAllNotifications();
                break;
            case ACTION_ALARM_CHANGED:
                scheduleAlarms();
                break;
            case ACTION_NEW_DAY:
                resetDay();
                scheduleAllEvents();
                break;
            case ACTION_VOLUME_CHANGED:
            case ACTION_SKIP_SILENT:
                skipSilent();
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                mEventBus.post(new Events.PrayerChangedEvent());
                break;
            case ACTION_PRAYER_CHANGE:
                resetPrevoiusPrayerSkips();
                resetStoredState();
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                mEventBus.post(new Events.PrayerChangedEvent());
                Utils.updateWidget(this);
                break;
            case ACTION_DEACTIVATE_SILENT:
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                mEventBus.post(new Events.PrayerChangedEvent());
                break;
            case ACTION_SHOW_APPROACHING_NOTIFICATION:
                NotificationsManager.getInstance(this).showApproachingNotification();
                break;
            case ACTION_DISABLE_NOTIFS:
                enableNotificaion(false);
                break;
            case ACTION_ENABLE_NOTIFS:
                enableNotificaion(true);
                break;
            case ACTION_APPROACHING_NOTIFICATION_DELETED:
                onApproachingNotifDeleted();
                break;
            case ACTION_SILENT_NOTIFICATION_DELETED:
                onSilentNotifDeleted();
                break;
            case ACTION_QUIT:
                shutdown(startId);
                break;
            default:
                scheduleAllEvents();
                SilentModeManager.getInstance(this).updateSilentMode(this);
                NotificationsManager.getInstance(this).updateNotification();
                break;
        }

        //dumpEventsTimeline();
    }

    private void enableNotificaion(final boolean enabled){
        NotificationsManager.getInstance(this).setNotificationsEnabled(enabled);
    }

    private void dumpEventsTimeline(){
        FileLog.newLine(1);

        FileLog.i(TAG, "*** ALARMS ***");

        for(Prayer p : PrayersSchedule.getInstance(this).getAllPrayers()){
            FileLog.i(TAG, "alarm on for "+p.getTitle()+" "+p.isAlarmOn()+", activation at: "+p.getAlarmActivationTime());
        }

        FileLog.i(TAG, "*** NOTIFICATIONS ***");

        for(Prayer p : PrayersSchedule.getInstance(this).getAllPrayers()){
            FileLog.i(TAG, "notification on for "+p.getTitle()+" "+p.isNotifOn()+", activation at: "+p.getNotificationTime());
        }

        FileLog.i(TAG, "*** SILENT DEACTIVATION ***");

        for(Prayer p : PrayersSchedule.getInstance(this).getAllPrayers()){
            FileLog.i(TAG, "silent on for "+p.getTitle()+" "+p.isSilentOn()+", activation at: "+p.getSilentDeactivationTime());
        }

        FileLog.newLine(1);
    }

    private void registerScreenOnReceiver(){
        FileLog.d(TAG, "[registerScreenOnReceiver]");

        mScreenOnReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                FileLog.d(TAG, "mScreenOnReceiver onReceive");

                NotificationsManager.getInstance(VaktijaService.this).updateNotification();
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenOnReceiver, filter);
    }

    private void onSilentNotifDeleted(){
        FileLog.d(TAG, "[onSilentNotifDeleted]");

        NotificationsManager.getInstance(VaktijaService.this).onSilentNotifDeleted();
        mApp.sendEvent("Silent notification deleted", "Deleted for " + mPrayer.getTitle());

    }

    private void onApproachingNotifDeleted(){
        FileLog.d(TAG, "[onApproachingNotifDeleted]");

        NotificationsManager.getInstance(this).onApproachingNotifDeleted();

        mApp.sendEvent("Approaching notification deleted", "Deleted for " + mPrayer.getTitle());
    }

    private void shutdown(int startId){
        FileLog.d(TAG, "[### shutdown]");

        NotificationsManager.getInstance(this).cancelNotification();

        for(Prayer p : PrayersSchedule.getInstance(this).getAllPrayers()){
            p.cancelAllAlarms(this, mAlarmManager);
        }

        resetStoredState();
        mWakeLock.release();
        stopSelf(startId);
    }

    private void resetDay(){

        PrayersSchedule.getInstance(this).reset();
        mPrayer = PrayersSchedule.getInstance(this).getCurrentPrayer();
        mEventBus.post(new Events.PrayerChangedEvent());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void resetPrevoiusPrayerSkips(){
        FileLog.d(TAG, "[resetPrevoiusPrayerSkips]");

        Prayer prevPrayer = PrayersSchedule.getInstance(this).getPreviousPrayerIgnoringDate(mPrayer.getId());
        prevPrayer.resetSkips();
        //PrayersSchedule.getInstance(this).reset();
        mEventBus.post(new Events.PrayerUpdatedEvent(prevPrayer.getId()));
    }

    private void scheduleAlarms(){
        FileLog.d(TAG, "[scheduleAlarms]");

        List<Prayer> prayers = new ArrayList<>();

        prayers.addAll(PrayersSchedule.getInstance(this).getAllPrayers());

        if(PrayersSchedule.getInstance(this).isJumaDay()) {
            prayers.remove(Prayer.DHUHR);

            Prayer juma = PrayersSchedule.getInstance(this).getPrayer(Prayer.JUMA);

            juma.scheduleAlarms(this, mAlarmManager);
        }

        for(Prayer v : prayers){
            v.scheduleAlarms(this, mAlarmManager);
        }

        scheduleNewDayAlarm();
    }

    private void scheduleSilentActivationEvents(){
        FileLog.d(TAG, "[scheduleSilentActivationEvents]");

        List<Prayer> prayers = new ArrayList<>();

        prayers.addAll(PrayersSchedule.getInstance(this).getAllPrayers());

        if(PrayersSchedule.getInstance(this).isJumaDay()) {
            prayers.remove(Prayer.DHUHR);

            Prayer juma = PrayersSchedule.getInstance(this).getPrayer(Prayer.JUMA);
            juma.schedulePrayerChangeAlarm(this, mAlarmManager);
        }

        for(Prayer v : prayers){
            v.schedulePrayerChangeAlarm(this, mAlarmManager);

            if(v.getId() == Prayer.SUNRISE){
                v.scheduleSunriseSilent(this, mAlarmManager);
            }
        }

        scheduleNewDayAlarm();
    }

    private void scheduleAllEvents(){
        FileLog.d(TAG, "[scheduleAllEvents]");

        final List<Prayer> prayers = new ArrayList<>();

        prayers.addAll(PrayersSchedule.getInstance(this).getAllPrayers());

        if(PrayersSchedule.getInstance(this).isJumaDay()) {
            prayers.remove(Prayer.DHUHR);

            Prayer juma = PrayersSchedule.getInstance(this).getPrayer(Prayer.JUMA);

            juma.scheduleAlarms(this, mAlarmManager);
            juma.scheduleNotifications(this, mAlarmManager);
            juma.schedulePrayerChangeAlarm(this, mAlarmManager);
        }



        for(Prayer v : prayers){
            v.scheduleAlarms(this, mAlarmManager);
            v.scheduleNotifications(this, mAlarmManager);
            v.schedulePrayerChangeAlarm(this, mAlarmManager);

            if(v.getId() == Prayer.SUNRISE){
                v.scheduleSunriseSilent(this, mAlarmManager);
            }
        }

        /*
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < 550; i++){
                    for(Prayer v : prayers){
                        v.scheduleAlarms(VaktijaService.this, mAlarmManager);
                        v.scheduleNotifications(VaktijaService.this, mAlarmManager);
                        v.schedulePrayerChangeAlarm(VaktijaService.this, mAlarmManager);

                        if(v.getId() == Prayer.SUNRISE){
                            v.scheduleSunriseSilent(VaktijaService.this, mAlarmManager);
                        }
                    }

                    Log.i(TAG, "pass: "+i);
                }
            }
        });

        thread.start();
        */

        scheduleNewDayAlarm();
    }

    private void scheduleAllNotifications(){
        FileLog.d(TAG, "[scheduleAllEvents]");
        List<Prayer> prayers = new ArrayList<>();

        prayers.addAll(PrayersSchedule.getInstance(this).getAllPrayers());

        if(PrayersSchedule.getInstance(this).isJumaDay()) {
            prayers.remove(Prayer.DHUHR);

            Prayer juma = PrayersSchedule.getInstance(this).getPrayer(Prayer.JUMA);

            juma.scheduleNotifications(this, mAlarmManager);
        }

        for(Prayer v : prayers){
            v.scheduleNotifications(this, mAlarmManager);
        }
    }

    void skipSilent(){

        if(SilentModeManager.getInstance(this).isSunriseSilentModeOn()){
            Prayer sunrise = PrayersSchedule.getInstance(this).getPrayer(Prayer.SUNRISE);
            sunrise.setSkipNextSilent(true);
            sunrise.save();
            //PrayersSchedule.getInstance(this).reset();
            mEventBus.post(new Events.SkipSilentEvent(sunrise.getId()));
        } else {
            mPrayer.setSkipNextSilent(true);
            mPrayer.save();
            //PrayersSchedule.getInstance(this).reset();
            mEventBus.post(new Events.SkipSilentEvent(mPrayer.getId()));
        }
    }

    @Override
    public void onDestroy(){
        FileLog.d(TAG, "[onDestroy]");

        NotificationsManager.getInstance(this).cancelNotification();
        unregisterReceiver(mScreenOnReceiver);

        super.onDestroy();
    }

    private void scheduleNewDayAlarm(){

        Calendar mCalendar = Calendar.getInstance(TimeZone.getDefault());
        mCalendar.add(Calendar.DATE, 1);
        mCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 1);

        long atMillis = mCalendar.getTimeInMillis();

        FileLog.i(TAG, "new day alarm at " + new Date(atMillis));

        mAlarmManager.cancel(getNewDayPendingIntent());

        mAlarmManager.set(
                AlarmManager.RTC_WAKEUP,
                atMillis,
                getNewDayPendingIntent());
    }

    private PendingIntent getNewDayPendingIntent(){
        Intent intent = getStartIntent(this, "NewDayPendingIntent");
        intent.setAction(ACTION_NEW_DAY);

        return PendingIntent.getService(
                this,
                NEW_DAY_ALARM_ID,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static Intent getStartIntent(Context context, String startedFrom) {
        Intent startIntent = new Intent(context, VaktijaService.class);
        startIntent.putExtra(STARTED_FROM, startedFrom);
        return startIntent;
    }
}
