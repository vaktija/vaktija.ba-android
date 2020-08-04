package ba.vaktija.android.models;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import ba.vaktija.android.AlarmActivity;
import ba.vaktija.android.App;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;

/**
 * Created by e on 2/6/15.
 */
public class Prayer implements Parcelable {

    public static final String TAG = Prayer.class.getSimpleName();

    public static final int FAJR = 0;
    public static final int SUNRISE = 1;
    public static final int DHUHR = 2;
    public static final int ASR = 3;
    public static final int MAGHRIB = 4;
    public static final int ISHA = 5;
    public static final int JUMA = 6;
    public static final String FIELD_ALARM = "alarm";
    public static final String FIELD_NOTIF = "notif";
    public static final String FIELD_SILENT = "silent";
    public static final String FIELD_SKIP_NEXT_ALARM = "skipNextAlarm";
    public static final String FIELD_SKIP_NEXT_SILENT = "skipNextSilent";
    public static final String FIELD_ALARM_ON = "alarmOn";
    public static final String FIELD_SILENT_ON = "silentOn";
    public static final String FIELD_ALARM_ON_MINS = "alarmMins";
    public static final String FIELD_ALARM_SOUND = "alarmSound";
    public static final String FIELD_SILENT_TIMEOUT = "silentTimeout";
    public static final String FIELD_NOTIF_ON = "notifOn";
    public static final String FIELD_NOTIF_SOUND_ON = "notifSoundOn";
    public static final String FIELD_NOTIF_SOUND = "notifSound";
    public static final String FIELD_NOTIF_VIBRO_ON = "notifVibroOn";
    public static final String FIELD_NOTIF_ON_MINS = "notifTime";
    public static final String FIELD_SKIP_NEXT_NOTIF = "skipNextNotif";
    public static final String FIELD_SILENT_VIBRO_OFF = "silentVibroOff";
    public static final Parcelable.Creator<Prayer> CREATOR = new Parcelable.Creator<Prayer>() {
        public Prayer createFromParcel(Parcel in) {
            return new Prayer(in);
        }

        public Prayer[] newArray(int size) {
            return new Prayer[size];
        }
    };
    protected static final int ALARM_PENDING_INTENT_ID = 13579;
    protected static final int NOTIF_PENDING_INTET_ID = 24680;
    protected static final int PRAYER_CHANGE_PENDING_INTENT_ID = 86420;
    protected static final int SILENT_OFF_PENDING_INTENT_ID = 97531;
    protected static final int SILENT_ON_PENDING_INTENT_ID = 97000;
    protected int id = -1;
    protected boolean skipNextAlarm;
    protected boolean skipNextSilent;
    protected boolean alarmOn;
    protected boolean silentOn;
    protected int alarmMins;
    protected String alarmSound = "";
    protected int soundOnMins;
    protected Calendar mCalendar;
    protected boolean skipNextNotif;
    protected boolean notifOn;
    protected int notifMins;
    protected String notifSound = "";
    protected boolean notifSoundOn = true;
    protected boolean notifVibroOn = true;
    protected boolean notifLedOn;
    protected boolean silentVibrationOff;
    protected int prayerTime;
    protected SimpleDateFormat mDateFormat = new SimpleDateFormat("HH'h' mm'm'", Locale.getDefault());

    public Prayer(Parcel in) {
        id = in.readInt();
        skipNextAlarm = in.readInt() == 1;
        skipNextSilent = in.readInt() == 1;
        alarmOn = in.readInt() == 1;
        silentOn = in.readInt() == 1;
        alarmMins = in.readInt();
        alarmSound = in.readString();
        soundOnMins = in.readInt();
        notifOn = in.readInt() == 1;
        notifMins = in.readInt();
        notifSound = in.readString();
        notifSoundOn = in.readInt() == 1;
        notifVibroOn = in.readInt() == 1;
        notifLedOn = in.readInt() == 1;
        setNextNotifOff(in.readInt() == 1);

        silentVibrationOff = in.readInt() == 1;
        prayerTime = in.readInt();

        initCalendar();

        initFromPreference();
    }

    public Prayer() {
    }

    public Prayer(int time, int prayerTimeId) {
        FileLog.d(TAG, "[<init> id: " + prayerTimeId + ", time: " + FormattingUtils.getTimeStringDots(time) + "]");
        prayerTime = time;
        id = prayerTimeId;

        initCalendar();

        initFromPreference();
    }

    public static PendingIntent getPrayerChangePendingIntent(Context context, int vakatId) {
        Intent vakatChangeIntent = VaktijaService.getStartIntent(context, "PrayerChangePendingIntent");
        vakatChangeIntent.setAction(VaktijaService.ACTION_PRAYER_CHANGE + "_" + vakatId);

        return PendingIntent.getService(
                context,
                PRAYER_CHANGE_PENDING_INTENT_ID,
                vakatChangeIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getSilentOnPendingIntent(Context context, int id) {
        Intent silentOnIntent = VaktijaService.getStartIntent(context, "SilentOnPendingIntent");
        silentOnIntent.setAction(VaktijaService.ACTION_SILENT_ACTIVATED + "_" + id);

        return PendingIntent.getService(
                context,
                SILENT_ON_PENDING_INTENT_ID,
                silentOnIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getSilentOffPendingIntent(Context context, int id) {
        Intent silentOffIntent = VaktijaService.getStartIntent(context, "SilentOffPendingIntent");
        silentOffIntent.setAction(VaktijaService.ACTION_DEACTIVATE_SILENT + "_" + id);

        return PendingIntent.getService(
                context,
                SILENT_OFF_PENDING_INTENT_ID,
                silentOffIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getAlarmPendingIntent(Context context, Prayer prayer) {
        Intent activateAlarm = new Intent(context, AlarmActivity.class);
        activateAlarm.setAction(AlarmActivity.LAUNCH_ALARM + "_" + prayer.getId());
        activateAlarm.putExtra(AlarmActivity.EXTRA_PRAYER_ID, prayer.getId());

        return PendingIntent.getActivity(
                context,
                ALARM_PENDING_INTENT_ID,
                activateAlarm,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getNotifPendingIntent(Context context, int vakatId) {
        Intent notifIntent = VaktijaService.getStartIntent(context, "NotifPendingIntent");
        notifIntent.setAction(VaktijaService.ACTION_SHOW_APPROACHING_NOTIFICATION + "_" + vakatId);

        return PendingIntent.getService(
                context,
                NOTIF_PENDING_INTET_ID,
                notifIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static boolean isSummerTime() {
        return TimeZone.getDefault().inDaylightTime(new Date());
    }

    public static String getNextVakatTitle(int currentVakatId) {

        boolean friday = Calendar.getInstance(Locale.getDefault()).get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        boolean respectJuma = App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);

        if (friday && respectJuma && currentVakatId == Prayer.SUNRISE)
            return "Džuma";

        switch (currentVakatId) {
            case Prayer.FAJR:
                return "Izlazak sunca";
            case Prayer.SUNRISE:
                return "Podne";
            case Prayer.DHUHR:
            case Prayer.JUMA:
                return "Ikindija";
            case Prayer.ASR:
                return "Akšam";
            case Prayer.MAGHRIB:
                return "Jacija";
            case Prayer.ISHA:
                return "Zora";
        }

        return "";
    }

    public JsonElement getSettingsAsJson() {
        JsonObject settings = new JsonObject();
        settings.addProperty(FIELD_ALARM_ON, alarmOn);
        settings.addProperty(FIELD_SILENT_ON, silentOn);
        settings.addProperty(FIELD_NOTIF_ON, notifOn);
        settings.addProperty(FIELD_ALARM_ON_MINS, alarmMins);
        settings.addProperty(FIELD_SILENT_TIMEOUT, soundOnMins);
        settings.addProperty(FIELD_NOTIF_SOUND_ON, notifSoundOn);
        settings.addProperty(FIELD_NOTIF_VIBRO_ON, notifVibroOn);
        settings.addProperty(FIELD_NOTIF_ON_MINS, notifMins);
        settings.addProperty(FIELD_SILENT_VIBRO_OFF, silentVibrationOff);

        return settings;
    }

    protected void initCalendar() {
        mCalendar = Calendar.getInstance(TimeZone.getDefault());

        mCalendar.set(Calendar.HOUR_OF_DAY, getHours());
        mCalendar.set(Calendar.MINUTE, getMinutes());
        mCalendar.set(Calendar.SECOND, 0);
    }

    protected void initFromPreference() {
//        FileLog.d(TAG, "initFromPreference "+getTitle(true));

        Gson gson = new Gson();

        JsonObject defData = new JsonObject();
        defData.addProperty(FIELD_SKIP_NEXT_ALARM, Defaults.getBooleanDefault(id, FIELD_SKIP_NEXT_ALARM));
        defData.addProperty(FIELD_SKIP_NEXT_NOTIF, Defaults.getBooleanDefault(id, FIELD_SKIP_NEXT_NOTIF));
        defData.addProperty(FIELD_SKIP_NEXT_SILENT, Defaults.getBooleanDefault(id, FIELD_SKIP_NEXT_SILENT));

        defData.addProperty(FIELD_ALARM_ON, Defaults.getBooleanDefault(id, FIELD_ALARM_ON));
        defData.addProperty(FIELD_NOTIF_ON, Defaults.getBooleanDefault(id, FIELD_NOTIF_ON));
        defData.addProperty(FIELD_SILENT_ON, Defaults.getBooleanDefault(id, FIELD_SILENT_ON));

        defData.addProperty(FIELD_ALARM_ON_MINS, Defaults.getIntDefault(id, FIELD_ALARM_ON_MINS));
        defData.addProperty(FIELD_SILENT_TIMEOUT, Defaults.getIntDefault(id, FIELD_SILENT_TIMEOUT));
        defData.addProperty(FIELD_NOTIF_ON_MINS, Defaults.getIntDefault(id, FIELD_NOTIF_ON_MINS));

        defData.addProperty(FIELD_NOTIF_SOUND, "");
        defData.addProperty(FIELD_ALARM_SOUND, "");

        defData.addProperty(FIELD_NOTIF_SOUND_ON, Defaults.getBooleanDefault(id, FIELD_NOTIF_SOUND_ON));
        defData.addProperty(FIELD_NOTIF_VIBRO_ON, Defaults.getBooleanDefault(id, FIELD_NOTIF_VIBRO_ON));
        defData.addProperty(FIELD_SILENT_VIBRO_OFF, Defaults.getBooleanDefault(id, FIELD_SILENT_VIBRO_OFF));

        String saved = App.vakatPrefs.getString(String.valueOf(id), null);

//        FileLog.i(TAG, "saved: "+saved);

        if (saved == null)
            saved = defData.toString();

        JsonObject data = gson.fromJson(saved, JsonElement.class).getAsJsonObject();

        initFromJson(data);
        //		FileLog.i(TAG, "after init: "+toString());
    }

    public Prayer initFromJson(JsonObject data) {
        if (data.has(FIELD_SKIP_NEXT_ALARM))
            skipNextAlarm = data.get(FIELD_SKIP_NEXT_ALARM).getAsBoolean();

        if (data.has(FIELD_SKIP_NEXT_NOTIF))
            skipNextNotif = data.get(FIELD_SKIP_NEXT_NOTIF).getAsBoolean();

        if (data.has(FIELD_SKIP_NEXT_SILENT))
            skipNextSilent = data.get(FIELD_SKIP_NEXT_SILENT).getAsBoolean();

        alarmOn = data.get(FIELD_ALARM_ON).getAsBoolean();
        silentOn = data.get(FIELD_SILENT_ON).getAsBoolean();
        notifOn = data.get(FIELD_NOTIF_ON).getAsBoolean();

        alarmMins = data.get(FIELD_ALARM_ON_MINS).getAsInt();
        soundOnMins = data.get(FIELD_SILENT_TIMEOUT).getAsInt();
        notifMins = data.get(FIELD_NOTIF_ON_MINS).getAsInt();

        if (data.has(FIELD_NOTIF_SOUND))
            notifSound = data.get(FIELD_NOTIF_SOUND).getAsString();

        if (data.has(FIELD_ALARM_SOUND))
            alarmSound = data.get(FIELD_ALARM_SOUND).getAsString();

        notifSoundOn = data.get(FIELD_NOTIF_SOUND_ON).getAsBoolean();
        notifVibroOn = data.get(FIELD_NOTIF_VIBRO_ON).getAsBoolean();
        silentVibrationOff = data.get(FIELD_SILENT_VIBRO_OFF).getAsBoolean();

        return this;
    }

    public void save() {
        FileLog.d(TAG, "save " + "id=" + id + " " + toString());
        long start = System.nanoTime();
        JsonObject data = new JsonObject();

        data.addProperty(FIELD_SKIP_NEXT_ALARM, skipNextAlarm);
        data.addProperty(FIELD_SKIP_NEXT_SILENT, skipNextSilent);
        data.addProperty(FIELD_SKIP_NEXT_NOTIF, skipNextNotif);

        data.addProperty(FIELD_ALARM_ON, alarmOn);
        data.addProperty(FIELD_SILENT_ON, silentOn);
        data.addProperty(FIELD_NOTIF_ON, notifOn);

        data.addProperty(FIELD_ALARM_ON_MINS, alarmMins);
        data.addProperty(FIELD_NOTIF_ON_MINS, notifMins);
        data.addProperty(FIELD_SILENT_TIMEOUT, soundOnMins);

        data.addProperty(FIELD_ALARM_SOUND, alarmSound);
        data.addProperty(FIELD_NOTIF_SOUND, notifSound);

        data.addProperty(FIELD_NOTIF_SOUND_ON, notifSoundOn);
        data.addProperty(FIELD_NOTIF_VIBRO_ON, notifVibroOn);
        data.addProperty(FIELD_SILENT_VIBRO_OFF, silentVibrationOff);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            App.vakatPrefs.edit().putString(String.valueOf(id), data.toString()).apply();
        } else {
            App.vakatPrefs.edit().putString(String.valueOf(id), data.toString()).commit();
        }

        long end = System.nanoTime();
        FileLog.i(TAG, "save data " + data.toString());
        FileLog.i(TAG, "save data done in " + ((end - start) / 1000.0) + " us");
    }

    // Getters and setters

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(skipNextAlarm ? 1 : 0);
        dest.writeInt(skipNextSilent ? 1 : 0);
        dest.writeInt(alarmOn ? 1 : 0);
        dest.writeInt(silentOn ? 1 : 0);
        dest.writeInt(alarmMins);
        dest.writeString(alarmSound);
        dest.writeInt(soundOnMins);
        dest.writeInt(notifOn ? 1 : 0);
        dest.writeInt(notifMins);
        dest.writeString(notifSound);
        dest.writeInt(notifSoundOn ? 1 : 0);
        dest.writeInt(notifVibroOn ? 1 : 0);
        dest.writeInt(notifLedOn ? 1 : 0);
        dest.writeInt(skipNextNotif() ? 1 : 0);
        dest.writeInt(silentVibrationOff ? 1 : 0);
        dest.writeInt(prayerTime);
    }

    public String getTitle() {
        switch (id) {
            case FAJR:
                return "Zora";
            case SUNRISE:
                return "Izlazak sunca";
            case DHUHR:
                return "Podne";
            case ASR:
                return "Ikindija";
            case MAGHRIB:
                return "Akšam";
            case ISHA:
                return "Jacija";
            case JUMA:
                return "Džuma";
        }

        return "No title";
    }

    public String getShortTitle() {
        switch (id) {
            case FAJR:
                return "Zora";
            case SUNRISE:
                return "I. sunca";
            case DHUHR:
                return "Podne";
            case ASR:
                return "Ikindija";
            case MAGHRIB:
                return "Akšam";
            case ISHA:
                return "Jacija";
            case JUMA:
                return "Džuma";
        }

        return "No title";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean skipNextAlarm() {
        return skipNextAlarm;
    }

    public boolean isAlarmOn() {
        return alarmOn;
    }

    public void setAlarmOn(boolean alarmOn) {
        this.alarmOn = alarmOn;

    }

    public boolean isSilentOn() {
        return silentOn;
    }

    public void setSilentOn(boolean silentOn) {
        this.silentOn = silentOn;
    }

    public int getAlarmMins() {
        return alarmMins;
    }

    public void setAlarmMins(int alarmMins) {
        this.alarmMins = alarmMins;
    }

    public String getAlarmSound() {
        return alarmSound;
    }

    public void setAlarmSound(String alarmSound) {
        this.alarmSound = alarmSound;
    }

    public int getSoundOnMins() {
        return soundOnMins;
    }

    public void setSoundOnMins(int soundOnMins) {
        this.soundOnMins = soundOnMins;
    }

    public int getPrayerTime() {

        boolean timeNormalized = App.prefs.getString(Prefs.DHUHR_TIME_COUNTING, "1").equals("1");
        boolean summerTime = isSummerTime();

        if ((id == DHUHR || id == JUMA) && timeNormalized) {
            return summerTime ? (13 * 3600) : (12 * 3600);
        }

        return prayerTime;
    }

    public String getPrayerTimeString() {
        return getHrsString() + ":" + getMinsString();
    }

    public int getRawPrayerTime() {
        return prayerTime;
    }

    public int getHours() {
        return (getPrayerTime() / (3600)) % 24;
    }

    public int getMinutes() {
        return (getPrayerTime() / 60) % 60;
    }

    @SuppressLint("DefaultLocale")
    public String getHrsString() {
        return String.format("%02d", ((getPrayerTime() / (3600)) % 24));
    }

    @SuppressLint("DefaultLocale")
    public String getMinsString() {
        return String.format("%02d", ((getPrayerTime() / 60) % 60));
    }

    public boolean isNotifOn() {
        return notifOn;
    }


    public void setNotifOn(boolean notifOn) {
        this.notifOn = notifOn;
    }

    public int getNotifMins() {
        return notifMins;
    }

    public void setNotifMins(int notifMins) {
        this.notifMins = notifMins;
    }

    public boolean isNotifSoundOn() {
        return notifSoundOn;
    }

    public void setNotifSoundOn(boolean notifSoundOn) {
        this.notifSoundOn = notifSoundOn;
    }

    public boolean isNotifVibroOn() {
        return notifVibroOn;
    }

    public void setNotifVibroOn(boolean notifVibroOn) {
        this.notifVibroOn = notifVibroOn;
    }

    public boolean skipNextNotif() {
        return skipNextNotif;
    }

    public void setSkipNextAlarm(boolean skipNextAlarm) {
        this.skipNextAlarm = skipNextAlarm;
    }

    public void setNextNotifOff(boolean skipNextNotif) {
        this.skipNextNotif = skipNextNotif;
    }

    public boolean isSilentVibrationOff() {
        return silentVibrationOff;
    }

    public void setSilentVibrationOff(boolean silentVibrationOff) {
        this.silentVibrationOff = silentVibrationOff;
    }

    @Override
    public String toString() {
        return "Vakat [id=" + id + ", skipNextAlarm=" + skipNextAlarm
                + ", skipNexSoundOff=" + skipNextSilent + ", skipNextNotif="
                + skipNextNotif + ", alarmOn=" + alarmOn + ", silentOn="
                + silentOn + ", alarmMins=" + alarmMins + ", alarmSound="
                + alarmSound + ", soundOnMins=" + soundOnMins + ", notifOn="
                + notifOn + ", notifMins=" + notifMins + ", notifSound="
                + notifSound + ", notifSoundOn=" + notifSoundOn
                + ", notifVibroOn=" + notifVibroOn + ", notifLedOn="
                + notifLedOn + ", silentVibrationOff=" + silentVibrationOff
                + ", prayerTime=" + prayerTime + ", previousTime="
                + ", mDateFormat="
                + mDateFormat + "]";
    }

    public void cancelAllAlarms(Context context, AlarmManager alarmManager) {
        FileLog.d(TAG, "cancelling all alarms for " + getTitle());

        alarmManager.cancel(getAlarmPendingIntent(context, this));
        alarmManager.cancel(getNotifPendingIntent(context, getId()));
        alarmManager.cancel(getSilentOffPendingIntent(context, getId()));
        alarmManager.cancel(getPrayerChangePendingIntent(context, getId()));
    }

    @SuppressLint("NewApi")
    public void scheduleSunriseSilent(Context conext, AlarmManager alarmManager) {
        FileLog.d(TAG, "scheduleSunriseSilent for " + getTitle());
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

        int prayerH = getHours();
        int prayerM = getMinutes();

        calendar.set(Calendar.HOUR_OF_DAY, prayerH);
        calendar.set(Calendar.MINUTE, prayerM);
        calendar.set(Calendar.SECOND, 0);

        FileLog.i(TAG, "sunrise sound off mins: " + getSoundOffMins());

        calendar.add(Calendar.MINUTE, getSoundOffMins());

        long silentOnAtMillis = calendar.getTimeInMillis();

        Date d = new Date(silentOnAtMillis);
        String alarmActivationTime = mDateFormat.format(d);

        calendar = Calendar.getInstance(TimeZone.getDefault());
        long currentTime = calendar.getTimeInMillis();

        alarmManager.cancel(getSilentOnPendingIntent(conext, Prayer.SUNRISE));

        FileLog.i(TAG, "alarm for " + getTitle() + " enabled: " + isAlarmOn());
        FileLog.i(TAG, "alarm for " + getTitle() + " skipped: " + skipNextAlarm);

        if (isSilentOn() && !skipNextSilent) {

            if (silentOnAtMillis < currentTime) {
                FileLog.w(TAG, "alarm activation time has passed for " + getTitle() + ", not setting alarm");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        silentOnAtMillis,
                        getSilentOnPendingIntent(conext, Prayer.SUNRISE));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        silentOnAtMillis,
                        getSilentOnPendingIntent(conext, Prayer.SUNRISE));
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        silentOnAtMillis,
                        getSilentOnPendingIntent(conext, Prayer.SUNRISE));
            }

            FileLog.i(TAG, "alarm set for " + getTitle() + ", activation time: " + alarmActivationTime);
        }
    }

    @SuppressLint("NewApi")
    public void scheduleAlarms(Context conext, AlarmManager alarmManager) {
//        FileLog.d(TAG, "scheduleAlarms for "+getTitle(true));
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

        int prayerH = getHours();
        int prayerM = getMinutes();

        calendar.set(Calendar.HOUR_OF_DAY, prayerH);
        calendar.set(Calendar.MINUTE, prayerM);
        calendar.set(Calendar.SECOND, 0);

        calendar.add(Calendar.MINUTE, -getAlarmMins());

        long alarmOnAtMillis = calendar.getTimeInMillis();// - getAlarmMins() * 60 * 1000;

        Date d = new Date(alarmOnAtMillis);
        String alarmActivationTime = mDateFormat.format(d);

        calendar = Calendar.getInstance(TimeZone.getDefault());
        long currentTime = calendar.getTimeInMillis();

        alarmManager.cancel(getAlarmPendingIntent(conext, this));

        FileLog.i(TAG, "alarm for " + getTitle() + " enabled: " + isAlarmOn());
        FileLog.i(TAG, "alarm for " + getTitle() + " skipped: " + skipNextAlarm);

        if (isAlarmOn() && !skipNextAlarm) {

            if (alarmOnAtMillis < currentTime) {
                FileLog.w(TAG, "alarm activation time has passed for " + getTitle() + ", not setting alarm");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmOnAtMillis,
                        getAlarmPendingIntent(conext, this));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmOnAtMillis,
                        getAlarmPendingIntent(conext, this));
                /*
            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent intent = new Intent(conext, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent pi = PendingIntent.getActivity(conext, 1221, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(alarmOnAtMillis, pi);
                alarmManager.setAlarmClock(alarmClockInfo, getAlarmPendingIntent(conext, this));
            */
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        alarmOnAtMillis,
                        getAlarmPendingIntent(conext, this));
            }

            FileLog.i(TAG, "alarm set for " + getTitle() + ", activation time: " + alarmActivationTime);
        }
    }

    @SuppressLint("NewApi")
    public void scheduleNotifications(Context context, AlarmManager alarmManager) {
        //FileLog.d(TAG, "scheduleNotifications for "+getTitle(true));
        Calendar mCalendar = Calendar.getInstance(TimeZone.getDefault());

        int vakatH = getHours();
        int vakatM = getMinutes();

        mCalendar.set(Calendar.HOUR_OF_DAY, vakatH);
        mCalendar.set(Calendar.MINUTE, vakatM);
        mCalendar.set(Calendar.SECOND, 0);

        mCalendar.add(Calendar.MINUTE, -getNotifMins());

        long notifOnAtMillis = mCalendar.getTimeInMillis();// - getNotifMins() * 60 * 1000;

        Date d = new Date(notifOnAtMillis);
        String notifActivationTime = mDateFormat.format(d);

        mCalendar = Calendar.getInstance(TimeZone.getDefault());
        long currentTime = mCalendar.getTimeInMillis();

        alarmManager.cancel(Prayer.getNotifPendingIntent(context, getId()));

        if (isNotifOn() && (notifOnAtMillis > currentTime)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notifOnAtMillis,
                        Prayer.getNotifPendingIntent(context, getId()));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        notifOnAtMillis,
                        Prayer.getNotifPendingIntent(context, getId()));
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        notifOnAtMillis,
                        Prayer.getNotifPendingIntent(context, getId()));
            }

            FileLog.i(TAG, "notif set for " + getTitle() + ", activation time: " + notifActivationTime);
        }
    }

    @SuppressLint("NewApi")
    public void schedulePrayerChangeAlarm(Context context, AlarmManager alarmManager) {
        //.d(TAG, "schedulePrayerChangeAlarm for "+getTitle(true));
        Calendar mCalendar = Calendar.getInstance(TimeZone.getDefault());

        long currentTime = mCalendar.getTimeInMillis();

        mCalendar.set(Calendar.HOUR_OF_DAY, getHours());
        mCalendar.set(Calendar.MINUTE, getMinutes());
        mCalendar.set(Calendar.SECOND, 0);

        long vakatChangeTime = mCalendar.getTimeInMillis();

        FileLog.i(TAG, "prayer change time for " + getTitle() + ": " + new Date(vakatChangeTime));

        Date d = new Date(vakatChangeTime);
        String activationTime = mDateFormat.format(d);

        alarmManager.cancel(getPrayerChangePendingIntent(context, getId()));

        if (vakatChangeTime > currentTime) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        vakatChangeTime,
                        Prayer.getPrayerChangePendingIntent(context, getId()));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        vakatChangeTime,
                        Prayer.getPrayerChangePendingIntent(context, getId()));
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        vakatChangeTime,
                        Prayer.getPrayerChangePendingIntent(context, getId()));


            }

            FileLog.i(TAG, "vakat change set, activationTime: " + activationTime);
        }
    }

    @SuppressLint("NewApi")
    public void scheduleSilentOffAlarm(Context context, AlarmManager alarmManager) {
        //FileLog.d(TAG, "scheduleSilentOffAlarm for "+getTitle(true));
        Calendar mCalendar = Calendar.getInstance(TimeZone.getDefault());

        int vakatH = getHours();
        int vakatM = getMinutes();

        long currentTime = mCalendar.getTimeInMillis();

        mCalendar.set(Calendar.HOUR_OF_DAY, vakatH);
        mCalendar.set(Calendar.MINUTE, vakatM);
        mCalendar.set(Calendar.SECOND, 0);

        mCalendar.add(Calendar.MINUTE, getSoundOnMins());

        long soundOnFutureTime = mCalendar.getTimeInMillis();// + getSoundOnMins() * 60 * 1000;

        FileLog.i(TAG, "silent deactivation for " + getTitle() + ": " + mDateFormat.format(new Date(soundOnFutureTime)));

        alarmManager.cancel(getSilentOffPendingIntent(context, getId()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    soundOnFutureTime,
                    Prayer.getSilentOffPendingIntent(context, getId()));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    soundOnFutureTime,
                    Prayer.getSilentOffPendingIntent(context, getId()));
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    soundOnFutureTime,
                    Prayer.getSilentOffPendingIntent(context, getId()));

        }
    }

    public String getAlarmActivationTime() {

        long alarmOnAtMillis = mCalendar.getTimeInMillis() - getAlarmMins() * 60 * 1000;

        Date d = new Date(alarmOnAtMillis);
        return mDateFormat.format(d);
    }

    public String getNotificationTime() {

        long notifOnAtMillis = mCalendar.getTimeInMillis() - getNotifMins() * 60 * 1000;

        Date d = new Date(notifOnAtMillis);
        return mDateFormat.format(d);
    }

    public String getSilentDeactivationTime() {

        long soundOnFutureTime = mCalendar.getTimeInMillis() + getSoundOnMins() * 60 * 1000;

        Date d = new Date(soundOnFutureTime);
        return mDateFormat.format(d);
    }

    public boolean isSummerTime(int month, int day) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());

        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_WEEK, day);

        return TimeZone.getDefault().inDaylightTime(new Date(calendar.getTimeInMillis()));
    }

    public boolean skipNextSilent() {
        return skipNextSilent;
    }

    public void setSkipNextSilent(boolean skipNextSilent) {
        this.skipNextSilent = skipNextSilent;
    }

    public void setSkipNextNotif(boolean skipNextNotif) {
        this.skipNextNotif = skipNextNotif;
    }

    public int getSoundOffMins() {

        if (id == SUNRISE)
            return soundOnMins;

        return 0;
    }

    public boolean anyEventsOn() {
        return isAlarmOn() || isSilentOn() || isNotifOn();
    }

    public void resetSkips() {
        skipNextNotif = false;
        skipNextAlarm = false;
        skipNextSilent = false;
        save();
    }
}
