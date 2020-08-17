package ba.vaktija.android;

import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;

import ba.vaktija.android.db.Database;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.receiver.DndChangeReceiver;
import ba.vaktija.android.receiver.RingerChangeReceiver;
import ba.vaktija.android.util.FileLog;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;

public class App extends Application {
    public static final String TAG = App.class.getSimpleName();

    public static final String VAKAT_PREFS = "VAKAT_PREFS";

    public static Typeface robotoCondensedLight;
    public static Typeface robotoCondensedRegular;

    public static SharedPreferences prefs;
//	Tracker mTracker;

    public static SharedPreferences vakatPrefs;

    public static App app;

    public Database db;

    public NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        FileLog.newLine(3);
        FileLog.d(TAG, "[>>> onCreate <<<]");

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            FileLog.i(TAG, "version code: " + pInfo.versionCode + ", version name: " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        //ACRA.init(this);

        app = this;

        db = new Database(this);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int locationId = prefs.getInt(Prefs.SELECTED_LOCATION_ID, Defaults.LOCATION_ID);
        prefs.edit().putString(Prefs.LOCATION_NAME, db.getLocationName(locationId)).commit();

//		initGoogleAnalytics();

        robotoCondensedLight = Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-Light.ttf");
        robotoCondensedRegular = Typeface.createFromAsset(getAssets(), "fonts/RobotoCondensed-Regular.ttf");

        vakatPrefs = getSharedPreferences(VAKAT_PREFS, Context.MODE_PRIVATE);

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/RobotoCondensed-Regular.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());

        checkNotifTone();
        checkAlarmTone();
        applySettingsChanges();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerDndChangeReceiver();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            ComponentName componentName = new ComponentName(this, RingerChangeReceiver.class);
            getPackageManager().setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            registerRingerChangeReceiver();
        }

        FileLog.d(TAG, "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT);
        FileLog.d(TAG, "Build.DEVICE=" + Build.DEVICE);
        FileLog.d(TAG, "Build.BOARD=" + Build.BOARD);
        FileLog.d(TAG, "Build.BRAND=" + Build.BRAND);
        FileLog.d(TAG, "Build.MODEL=" + Build.MODEL);
        FileLog.d(TAG, "Build.PRODUCT=" + Build.PRODUCT);
    }

	/*
	void initGoogleAnalytics(){
		GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
		mTracker = analytics.newTracker(R.xml.global_tracker);
	}
    */

    public void sendScreenView(String screenName) {
        /*
		if(!prefs.getBoolean(Prefs.GA_ENABLED, true))
			return;
		
		mTracker.setScreenName(screenName);
		mTracker.send(new HitBuilders.AppViewBuilder().build());
		*/
    }

    public void sendEvent(String category, String action) {
        /*
		if(!prefs.getBoolean(Prefs.GA_ENABLED, true))
			return;

		mTracker.send(new HitBuilders.EventBuilder()
		.setCategory(category)
		.setAction(action)
		.setLabel("")
		.build());
		*/
    }

    private void checkNotifTone() {
        if (!prefs.getBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, true)) {
            String selectedNotifTone = prefs.getString(Prefs.NOTIF_TONE_URI,
                    Defaults.getDefaultTone(this, false));

            Ringtone ringtone = RingtoneManager
                    .getRingtone(this, Uri.parse(selectedNotifTone));

            if (ringtone == null) {
                prefs.edit().putBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, true).commit();
            }
        }
    }

    private void checkAlarmTone() {
        if (!prefs.getBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, true)) {
            String selectedAlarmTone = prefs.getString(
                    Prefs.ALARM_TONE_URI,
                    Defaults.getDefaultTone(this, true));

            Ringtone alarmRingtone = RingtoneManager
                    .getRingtone(this, Uri.parse(selectedAlarmTone));

            if (alarmRingtone == null) {
                prefs.edit().putBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, true).commit();
            }
        }
    }

    private void applySettingsChanges() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            boolean silentVibrationSettingsAdjusted = prefs.getBoolean(Prefs.SILENT_VIBRATION_SETTINGS_ADJUSTED, false);

            if (!silentVibrationSettingsAdjusted) {
                try {
                    for (Prayer prayer : PrayersSchedule.getInstance(this).getAllPrayers()) {
                        prayer.setSilentVibrationOff(false);
                        prayer.save();
                    }
                    prefs.edit().putBoolean(Prefs.SILENT_VIBRATION_SETTINGS_ADJUSTED, true).commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Uri getAlarmSoundUri() {

        String selectedAlarmTonePath = prefs.getString(
                Prefs.ALARM_TONE_URI,
                Defaults.getDefaultTone(this, true));

        if (prefs.getBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, true)) {
            selectedAlarmTonePath = Defaults.getDefaultTone(this, true);
        }

        FileLog.d(TAG, "selected alarm tone path: " + selectedAlarmTonePath);

        return Uri.parse(selectedAlarmTonePath);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void registerDndChangeReceiver() {
        IntentFilter intentFilter = new IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
        registerReceiver(new DndChangeReceiver(), intentFilter);
    }

    private void registerRingerChangeReceiver() {
        IntentFilter intentFilter = new IntentFilter("android.media.RINGER_MODE_CHANGED");
        registerReceiver(new RingerChangeReceiver(), intentFilter);
    }
}
