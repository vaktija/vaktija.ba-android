package ba.vaktija.android;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;

import ba.vaktija.android.db.Database;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;

/*
@ReportsCrashes(
		formKey = "", 
		mailTo = "android@vaktija.ba",
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.crash_toast_text)
		*/
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
}
