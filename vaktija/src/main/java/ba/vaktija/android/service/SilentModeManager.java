package ba.vaktija.android.service;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.Utils;

public class SilentModeManager {
    private static final String TAG = SilentModeManager.class.getSimpleName();

    private static SilentModeManager instance;
    private AudioManager mAudioManager;
    private AlarmManager mAlarmManager;
    private SharedPreferences mPrefs;
    private Context mContext;

    private SilentModeManager(Context context) {
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SilentModeManager getInstance(Context context) {
        if (instance == null)
            instance = new SilentModeManager(context);

        return instance;
    }

    public boolean isSilentOn() {
        return mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
    }

    public void disableSilent() {
        mPrefs.edit()
                .putBoolean(Prefs.SILENT_BY_APP, false)
                .putBoolean(Prefs.GOING_SILENT, false)
                .putBoolean(Prefs.COMMING_FROM_SILENT, true)
                .commit();
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    }

    public boolean silentSetByApp() {
        return mPrefs.getBoolean(Prefs.SILENT_BY_APP, false);
    }

    public void updateSilentMode(Context context) {
        //FileLog.d(TAG, "updateSilentMode");

        Prayer currentVakat = PrayersSchedule.getInstance(context).getCurrentPrayer();
        boolean silentSetByApp = mPrefs.getBoolean(Prefs.SILENT_BY_APP, false);
        boolean silentShouldBeActive = silentShoudBeActive();
        boolean deviceInSilentMode = mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;

        FileLog.i(TAG, "silent set by app: " + silentSetByApp);
        FileLog.i(TAG, "silent should be on: " + silentShouldBeActive);
        FileLog.i(TAG, "deviceInSilentMode: " + deviceInSilentMode);

        if (!silentSetByApp && deviceInSilentMode) {
            return;
        }

        if (silentShouldBeActive) {
            mPrefs.edit()
                    .putBoolean(Prefs.SILENT_BY_APP, true)
                    .putBoolean(Prefs.GOING_SILENT, true)
                    .putBoolean(Prefs.COMMING_FROM_SILENT, false)
                    .commit();

            // because... https://code.google.com/p/android/issues/detail?id=78652
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                FileLog.i(TAG, "set ringer mode to RINGER_MODE_VIBRATE");
            } else {
                mAudioManager.setRingerMode(
                        isVibrationOff()
                                ? AudioManager.RINGER_MODE_SILENT
                                : AudioManager.RINGER_MODE_VIBRATE);
                FileLog.i(TAG, "set ringer mode to " +
                        (isVibrationOff()
                                ? "RINGER_MODE_SILENT"
                                : "RINGER_MODE_VIBRATE"));
            }

            if (isSunriseSilentModeOn()) {
                //				currentVakat.scheduleAlternativeSilentOffAlarm(context, mAlarmManager);
            } else {
                currentVakat.scheduleSilentOffAlarm(context, mAlarmManager);
            }

        } else if (silentSetByApp) {

            int ringerMode = mAudioManager.getRingerMode();

            mPrefs.edit()
                    .putBoolean(Prefs.SILENT_BY_APP, false)
                    .putBoolean(Prefs.GOING_SILENT, false)
                    .putBoolean(Prefs.COMMING_FROM_SILENT, ringerMode != AudioManager.RINGER_MODE_NORMAL)
                    .commit();

            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            FileLog.i(TAG, "set ringer mode to RINGER_MODE_NORMAL");
        }
    }

    private boolean isVibrationOff() {
        //FileLog.d(TAG, "isVribrationOff");

        Prayer currentVakat = PrayersSchedule.getInstance(mContext).getCurrentPrayer();
        int currentTime = Utils.getCurrentTimeSec();
        boolean standardVibrationOff = false;
        boolean alternativeVibrationOff = false;

        if (!currentVakat.isSilentVibrationOff()) {
            standardVibrationOff = false;
        } else {
            int silentTimeout = currentVakat.getPrayerTime() + currentVakat.getSoundOnMins() * 60;

            if (currentVakat.getId() == Prayer.ISHA) {
                standardVibrationOff = (currentVakat.getPrayerTime() <= currentTime
                        && currentTime < silentTimeout);
            } else {
                standardVibrationOff = currentTime < silentTimeout;
            }
        }

        FileLog.i(TAG, "standard vibration off: " + standardVibrationOff);

        if (standardVibrationOff) {
            return true;
        } else if (currentVakat.getId() == Prayer.FAJR) {
            Prayer sunrise = PrayersSchedule.getInstance(mContext).getPrayer(Prayer.SUNRISE);

            if (!sunrise.isSilentVibrationOff()) {
                alternativeVibrationOff = false;
            } else {

                int silentActivationTime = sunrise.getPrayerTime() + sunrise.getSoundOffMins() * 60;

                alternativeVibrationOff = silentActivationTime <= currentTime;
            }
        }
        FileLog.i(TAG, "alternative vibration off: " + alternativeVibrationOff);

        return alternativeVibrationOff;
    }

    public boolean silentShoudBeActive() {
        //FileLog.d(TAG, "silentShoudBeActive");

        Prayer currentVakat = PrayersSchedule.getInstance(mContext).getCurrentPrayer();
        int currentTime = Utils.getCurrentTimeSec();
        boolean standardSilentOn = false;

        if (!currentVakat.isSilentOn() || currentVakat.skipNextSilent()) {
            standardSilentOn = false;
        } else {
            int silentTimeout = currentVakat.getPrayerTime() + currentVakat.getSoundOnMins() * 60;

            if (currentVakat.getId() == Prayer.ISHA) {
                standardSilentOn = (currentVakat.getPrayerTime() <= currentTime
                        && currentTime < silentTimeout);
            } else {
                standardSilentOn = currentTime < silentTimeout;
            }
        }

        FileLog.i(TAG, "standard silent on: " + standardSilentOn);

        if (standardSilentOn)
            return true;

        return isSunriseSilentModeOn();
    }

    public boolean isSunriseSilentModeOn() {
        //FileLog.d(TAG, "isSunriseSilentModeOn");

        Prayer currentVakat = PrayersSchedule.getInstance(mContext).getCurrentPrayer();
        int currentTime = Utils.getCurrentTimeSec();
        boolean alternativeSilentOn = false;

        if (currentVakat.getId() == Prayer.FAJR) {
            Prayer sunrise = PrayersSchedule.getInstance(mContext).getPrayer(Prayer.SUNRISE);

            if (!sunrise.isSilentOn() || sunrise.skipNextSilent()) {
                alternativeSilentOn = false;
            } else {

                int silentActivationTime = sunrise.getPrayerTime() + sunrise.getSoundOffMins() * 60;

                alternativeSilentOn = silentActivationTime <= currentTime;
            }
        }
        FileLog.i(TAG, "alternative silent on: " + alternativeSilentOn);

        return alternativeSilentOn;
    }
}
