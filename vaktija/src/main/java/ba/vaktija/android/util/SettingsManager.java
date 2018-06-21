package ba.vaktija.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;

/**
 * Created by e on 2/13/15.
 */
public class SettingsManager {

    private static final String PRAYERS_SETTINGS = "prayersSettings";
    private static final String APP_VERSION_NAME = "appVersionName";
    private static final String APP_VERSION_CODE = "appVersionCode";
    private static final int MIN_VERSION_CODE = 24;

    private static SettingsManager instance;
    private Context mContext;
    private SharedPreferences prefs;

    public static class SettingsCorruptedException extends Exception{ }

    public static class UnsupportedFormatException extends Exception{ }

    public static SettingsManager getInstance(Context context){
        if(instance == null)
            instance = new SettingsManager(context);

        return instance;
    }

    private SettingsManager(Context context){
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getSettings(){

        PrayersSchedule schedule = PrayersSchedule.getInstance(mContext);

        JsonArray prayersSettings = new JsonArray();
        prayersSettings.add(schedule.getPrayer(Prayer.FAJR).getSettingsAsJson());
        prayersSettings.add(schedule.getPrayer(Prayer.SUNRISE).getSettingsAsJson());
        prayersSettings.add(schedule.getPrayer(Prayer.DHUHR).getSettingsAsJson());
        prayersSettings.add(schedule.getPrayer(Prayer.ASR).getSettingsAsJson());
        prayersSettings.add(schedule.getPrayer(Prayer.MAGHRIB).getSettingsAsJson());
        prayersSettings.add(schedule.getPrayer(Prayer.ISHA).getSettingsAsJson());
        prayersSettings.add(schedule.getPrayer(Prayer.JUMA).getSettingsAsJson());

        String appVersionName = "unknown";
        int appVersionCode = -1;

        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            appVersionName = pInfo.versionName;
            appVersionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        JsonObject settings = new JsonObject();

        settings.addProperty(APP_VERSION_NAME, appVersionName);
        settings.addProperty(APP_VERSION_CODE, appVersionCode);
        settings.addProperty(Prefs.ALARM_TONE_URI, prefs.getString(Prefs.ALARM_TONE_URI, ""));
        settings.addProperty(Prefs.NOTIF_TONE_URI, prefs.getString(Prefs.NOTIF_TONE_URI, ""));
        settings.addProperty(Prefs.DHUHR_TIME_COUNTING, prefs.getString(Prefs.DHUHR_TIME_COUNTING, "0"));
        settings.addProperty(Prefs.SELECTED_LOCATION_ID, prefs.getInt(Prefs.SELECTED_LOCATION_ID, Defaults.LOCATION_ID));
        settings.addProperty(Prefs.SEPARATE_JUMA_SETTINGS, prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true));
        settings.addProperty(Prefs.GA_ENABLED, prefs.getBoolean(Prefs.GA_ENABLED, true));
        settings.addProperty(Prefs.STATUSBAR_NOTIFICATION, prefs.getBoolean(Prefs.STATUSBAR_NOTIFICATION, true));
        settings.addProperty(Prefs.USE_VAKTIJA_ALARM_TONE, prefs.getBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, true));
        settings.addProperty(Prefs.USE_VAKTIJA_NOTIF_TONE, prefs.getBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, true));
        settings.addProperty(Prefs.SHOW_DATE, prefs.getBoolean(Prefs.SHOW_DATE, false));

        settings.add(PRAYERS_SETTINGS, prayersSettings);

        return settings.toString();
    }

    public void restoreSettings(String data) throws SettingsCorruptedException, UnsupportedFormatException{

        Gson gson = new Gson();

        SharedPreferences.Editor editor;

        editor = prefs.edit();

        try {
            JsonObject settings = gson.fromJson(data, JsonObject.class);
            String versionName = settings.get(APP_VERSION_NAME).getAsString();
            int versionCode = settings.get(APP_VERSION_CODE).getAsInt();

            if(versionCode < MIN_VERSION_CODE){
                throw new UnsupportedFormatException();
            }

            try {
                editor.putString(Prefs.ALARM_TONE_URI, settings.get(Prefs.ALARM_TONE_URI).getAsString());
                editor.putString(Prefs.NOTIF_TONE_URI, settings.get(Prefs.NOTIF_TONE_URI).getAsString());
            } catch (UnsupportedOperationException uoe){
                uoe.printStackTrace();
                editor.putString(Prefs.ALARM_TONE_URI, "");
                editor.putString(Prefs.NOTIF_TONE_URI, "");
            }

            editor.putString(Prefs.DHUHR_TIME_COUNTING, settings.get(Prefs.DHUHR_TIME_COUNTING).getAsString());
            editor.putInt(Prefs.SELECTED_LOCATION_ID, settings.get(Prefs.SELECTED_LOCATION_ID).getAsInt());
            editor.putBoolean(Prefs.SEPARATE_JUMA_SETTINGS, settings.get(Prefs.SEPARATE_JUMA_SETTINGS).getAsBoolean());
            editor.putBoolean(Prefs.GA_ENABLED, settings.get(Prefs.GA_ENABLED).getAsBoolean());
            editor.putBoolean(Prefs.STATUSBAR_NOTIFICATION, settings.get(Prefs.STATUSBAR_NOTIFICATION).getAsBoolean());
            editor.putBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, settings.get(Prefs.USE_VAKTIJA_ALARM_TONE).getAsBoolean());
            editor.putBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, settings.get(Prefs.USE_VAKTIJA_NOTIF_TONE).getAsBoolean());
            editor.putBoolean(Prefs.SHOW_DATE, settings.get(Prefs.SHOW_DATE).getAsBoolean());

            JsonArray prayersSettings = settings.get(PRAYERS_SETTINGS).getAsJsonArray();

            for(int i = Prayer.FAJR; i <= Prayer.ISHA; i++){
                PrayersSchedule.getInstance(mContext).getPrayer(i).initFromJson(prayersSettings.get(i).getAsJsonObject()).save();
            }

            PrayersSchedule.getInstance(mContext).getPrayer(Prayer.JUMA).initFromJson(prayersSettings.get(Prayer.JUMA).getAsJsonObject()).save();

            editor.commit();

        } catch (JsonSyntaxException jse){
            throw new SettingsCorruptedException();
        } catch (NullPointerException npe){
            throw new SettingsCorruptedException();
        }

    }
}
