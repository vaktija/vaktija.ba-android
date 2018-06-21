package ba.vaktija.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import ba.vaktija.android.models.Events;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.prefs.SettingsActivity;
import ba.vaktija.android.service.SilentModeManager;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.HijriCalendar;
import ba.vaktija.android.util.Utils;
import ba.vaktija.android.wizard.WizardActivity;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;

public class MainActivity extends BaseActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private SharedPreferences mPrefs;
    private App mApp;
    private boolean mIsDualPane = false;
    private int mLocationId;
    private View mActualEvent;
    private TextView mActualEventMessage;
    private TextView mActualEventAction;
    private TextView mActionBarTitle;
    private TextView mActionBarSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FileLog.d(TAG, "[onCreate]");
        setTheme(TAG);
        super.onCreate(savedInstanceState);

        mApp = (App) getApplicationContext();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mIsDualPane = getResources().getBoolean(R.bool.dual_pane);

        Log.i(TAG, "wizard completed: " + mPrefs.getBoolean(Prefs.WIZARD_COMPLETED, false));

        View customAb = LayoutInflater.from(this).inflate(R.layout.custom_action_bar, null);

        mActionBarTitle = (TextView) customAb.findViewById(R.id.custom_action_bar_title);
        mActionBarSubtitle = (TextView) customAb.findViewById(R.id.custom_action_bar_subtitle);

        mActionBarTitle.setTextColor(getResources().getColor(android.R.color.darker_gray));
        mActionBarSubtitle.setTextColor(getResources().getColor(android.R.color.darker_gray));

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(true);
        ab.setTitle("");
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(customAb);

/*        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if(!mIsDualPane)
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }*/

        if(!mPrefs.getBoolean(Prefs.WIZARD_COMPLETED, false)){
            startWizard();
        } else {
            setupActivity();
        }
    }

    private void setupActivity(){
        Log.d(TAG, "[setupActivity]");
        setContentView(R.layout.activity_main);

        LinearLayout rootView = (LinearLayout) findViewById(R.id.main_root);

        changeActionBarColor(getResources().getColor(R.color.very_light_gray));

        if(!getResources().getBoolean(R.bool.use_cards)){
            rootView.setBackgroundColor(getResources().getColor(R.color.main_activity_bg));
            changeActionBarColor(getResources().getColor(R.color.ab_bg_mdpi));
        }

        mActualEvent = findViewById(R.id.main_actual_event_wrapper);
        mActualEventMessage = (TextView) mActualEvent.findViewById(R.id.main_actual_event_message);
        mActualEventAction = (TextView) mActualEvent.findViewById(R.id.main_actual_event_action);

        mActualEvent.setVisibility(View.GONE);

        mLocationId = mPrefs.getInt(Prefs.SELECTED_LOCATION_ID, 1);
        showActionBarInfo();

        getSupportActionBar().setDisplayShowHomeEnabled(false);

        mApp.sendScreenView(TAG);

        mPrefs.edit().putBoolean(Prefs.USER_CLOSED, false).commit();
        Intent service = VaktijaService.getStartIntent(this, TAG + ":setupActivity()");
        //service.setAction(VaktijaService.ACTION_UPDATE);
        startService(service);

//        showAnalyticsOptOutDialog();
    }

    @Override
    public void onResume(){
        super.onResume();
        FileLog.d(TAG, "[onResume]");

        EventBus.getDefault().register(this);

        if(mLocationId != mPrefs.getInt(Prefs.SELECTED_LOCATION_ID, 1)){
            startService(VaktijaService.getStartIntent(this, TAG + ":onResume()"));
            mLocationId = mPrefs.getInt(Prefs.SELECTED_LOCATION_ID, 1);
        }

        //tintStatusBar();
        showActionBarInfo();

        showActualEventMessage();

        checkDozeModeState();
    }

    private void showActualEventMessage(){
        FileLog.d(TAG, "[showActualEventMessage]");
        boolean silentOn = SilentModeManager.getInstance(this).isSilentOn();
        boolean alarmActive = mPrefs.getBoolean(Prefs.ALARM_ACTIVE, false);
        boolean silentDisabledByUser = mPrefs.getBoolean(Prefs.SILENT_DISABLED_BY_USER, false);
        boolean silentShouldBeActive = SilentModeManager.getInstance(this).silentShoudBeActive();

        FileLog.d(TAG, "alarm active: "+alarmActive);
        FileLog.d(TAG, "silent disabled by user: " + silentDisabledByUser);
        FileLog.d(TAG, "silent should be active: " + silentShouldBeActive);

        if(alarmActive) {
            showAlarmEvent();
        } else if (silentOn && silentShouldBeActive){
            showSilentActive();
        } else if (silentDisabledByUser) {
            showSilentDisabledByUser();
        } else {
            mActualEvent.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause(){
        FileLog.d(TAG, "[onPause]");
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void onStop(){
        super.onStop();
        FileLog.d(TAG, "[onStop]");
    }

    void showActionBarInfo(){

        boolean dateEnabled = mPrefs.getBoolean(Prefs.SHOW_DATE, false);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        String dayName = getString(R.string.monday);
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY:
                dayName = getString(R.string.monday);
                break;
            case Calendar.TUESDAY:
                dayName = getString(R.string.tuesday);
                break;
            case Calendar.WEDNESDAY:
                dayName = getString(R.string.wednesday);
                break;
            case Calendar.THURSDAY:
                dayName = getString(R.string.thursday);
                break;
            case Calendar.FRIDAY:
                dayName = getString(R.string.friday);
                break;
            case Calendar.SATURDAY:
                dayName = getString(R.string.saturday);
                break;
            case Calendar.SUNDAY:
                dayName = getString(R.string.sunday);
                break;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd. MM. yyyy.", Locale.getDefault());
        String date = sdf.format(calendar.getTime());

        mActionBarTitle.setText(mPrefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME).toUpperCase());

        String hijriDate = HijriCalendar.getSimpleDate(Calendar.getInstance());
        mActionBarSubtitle.setText(dayName + ", " + hijriDate);

        // mActionBarSubtitle.setText(dayName + ", " + date);
        mActionBarSubtitle.setVisibility(dateEnabled ? View.VISIBLE : View.GONE);

//        Log.i(TAG, "hijri: "+ HijriCalendar.getSimpleDate(Calendar.getInstance()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_main_date_picker:
                showDatePickerDialog();
                return true;
            case R.id.menu_main_share:
                launchShareIntent();
                return true;
            case R.id.menu_main_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.menu_main_close:
                exit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void launchShareIntent(){

        mApp.sendScreenView("Share");

        SimpleDateFormat sdf = new SimpleDateFormat("dd'. 'MM'. 'yyyy'.'", Locale.getDefault());
        StringBuilder shareText = new StringBuilder("");

        String city = mPrefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME);

        shareText.append(
                getString(R.string.prayer_times_for_date,
                        sdf.format(
                                new Date(Calendar.getInstance(Locale.getDefault())
                                        .getTimeInMillis()))));

        shareText.append(" / ").append(HijriCalendar.getSimpleDate(Calendar.getInstance()));
        shareText.append(" (").append(city).append(")");
        shareText.append("\n");

        for(Prayer v : PrayersSchedule.getInstance(this).getAllPrayers()){
            shareText
                    .append(v.getTitle()).append(": ")
                    .append(FormattingUtils.getFormattedTime(v.getPrayerTime() * 1000, false))
                    .append("\n");
        }

        shareText.append(getResources().getString(R.string.play_url));

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        //		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Hadis dana radija Bir");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        if (sharingIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(sharingIntent, getString(R.string.share_using)), 1);
        } else {
            Toast.makeText(this, "Nije pronađena nijedna aplikacija koja može da prihvati sadržaj", Toast.LENGTH_SHORT).show();
        }
    }

    public void showDatePickerDialog() {
        mApp.sendScreenView("Date picker");

        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        boolean calledOnce = false;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance(Locale.getDefault());
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {

            if(calledOnce) return;

            FileLog.d("DatePickerFragment", "[onDateSet]");

            ArrayList<String> values =  new ArrayList<String>();
            values.add(String.valueOf(day));
            values.add(String.valueOf(month + 1));
            values.add(String.valueOf(year));

            Bundle args = new Bundle();
            args.putStringArrayList(DateFragment.EXTRA_VALUES, values);

            DateFragment dateFragment = new DateFragment();
            dateFragment.setArguments(args);

            dateFragment.show(getActivity().getSupportFragmentManager(), DateFragment.TAG);

            calledOnce = true;
        }
    }

    void exit(){
        FileLog.w(TAG, "### That's it, user is closing me!");
        mApp.sendEvent("Close applicaton", "Close applicaton");
        mPrefs.edit().putBoolean(Prefs.USER_CLOSED, true).commit();

        Intent i = VaktijaService.getStartIntent(this, TAG + ":exit()");
        i.setAction(VaktijaService.ACTION_QUIT);
        startService(i);
        finish();
    }

    private void changeActionBarColor(int color){
        Drawable colorDrawable = new ColorDrawable(color);
        LayerDrawable ld = new LayerDrawable(new Drawable[] { colorDrawable });

        getSupportActionBar().setBackgroundDrawable(ld);

        // http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    private void _tintStatusBar(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;

        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
//		if(themeIndex.equals("0"))
//			tintManager.setNavigationBarTintEnabled(true);
        // Holo light action bar color is #DDDDDD
        int color = getResources().getColor(R.color.theme_gray);

        color = getResources().getColor(android.R.color.white);


        tintManager.setStatusBarTintColor(color);
        //		tintManager.setNavigationBarTintColor(mPrefs.getInt(Prefs.THEME_COLOR, getResources().getColor(R.color.apptheme_color)));

    }

    void startWizard(){
        Intent i = new Intent(this, WizardActivity.class);
        startActivity(i);
        finish();
    }

    /*
    private void showAnalyticsOptOutDialog(){
        if(mPrefs.getBoolean(Prefs.GA_OPT_OUT_SHOWN, false))
            return;

        FileLog.d(TAG, "[showAnalyticsOptOutDialog]");

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_analytics_opt_out_title)
                .setMessage(R.string.dialog_message_analytics_opt_out)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_yes, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPrefs.edit().putBoolean(Prefs.GA_ENABLED, false).commit();
                    }
                })
                .setNegativeButton(R.string.dialog_no, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPrefs.edit().putBoolean(Prefs.GA_ENABLED, true).commit();
                    }
                })
                .show();

        mPrefs.edit().putBoolean(Prefs.GA_OPT_OUT_SHOWN, true).commit();
    }
    */

    private void showActualEvent(){
        FileLog.d(TAG, "[showActualEvent]");

        final boolean silentSetByApp = SilentModeManager.getInstance(this).silentSetByApp();

        String msg = "Bez zvukova do "+PrayersSchedule.getInstance(this).getSilentModeDurationString();
        mActualEventMessage.setText(Utils.boldNumbers(msg));

        if(!silentSetByApp){
            mActualEventMessage.setText(R.string.silent_set_manually);
        }

        mActualEventAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPrefs.edit().putBoolean(Prefs.SILENT_BY_APP, true).commit();

                Intent silentOffIntent = VaktijaService.getStartIntent(MainActivity.this, "Actual event action");
                silentOffIntent.setAction(VaktijaService.ACTION_SKIP_SILENT);
                startService(silentOffIntent);

                /*
                if (silentSetByApp) {
                    Intent silentOffIntent = VaktijaService.getStartIntent(MainActivity.this, "Actual event action");
                    silentOffIntent.setAction(VaktijaService.ACTION_SKIP_SILENT);
                    startService(silentOffIntent);
                } else {
                    SilentModeManager.getInstance(MainActivity.this).disableSilent();
                    //showActualEventMessage();
                }
                */
            }
        });

        mActualEvent.setVisibility(View.VISIBLE);
    }

    private void showSilentActive(){
        FileLog.d(TAG, "[showSilentActive]");

        mActualEventMessage.setText(
                Utils.boldNumbers(
                        getString(
                                R.string.no_sounds_till,
                                PrayersSchedule.getInstance(this)
                                        .getSilentModeDurationString())));

        mActualEventAction.setText(R.string.turn_on);

        mActualEventAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Prayer currentP = PrayersSchedule.getInstance(MainActivity.this).getCurrentPrayer();

                if(currentP.getId() == Prayer.FAJR
                        && SilentModeManager.getInstance(MainActivity.this).isSunriseSilentModeOn()){
                    currentP = PrayersSchedule.getInstance(MainActivity.this).getPrayer(Prayer.SUNRISE);
                }

                currentP.setSkipNextSilent(true);
                currentP.save();

                mApp.sendEvent(currentP.getTitle(), "Turning sounds on");

                Intent service = VaktijaService.getStartIntent(MainActivity.this, TAG + " Turning sounds on");
                service.setAction(VaktijaService.ACTION_SILENT_CHANGED);
                startService(service);

                //mPrefs.edit().putBoolean(Prefs.SILENT_DISABLED_BY_USER, false).commit();

                //showActualEventMessage();
            }
        });

        mActualEvent.setVisibility(View.VISIBLE);
    }

    private void showSilentDisabledByUser(){
        FileLog.d(TAG, "[showSilentDisabledByUser]");

        mActualEventMessage.setText(R.string.silent_disabled_manually);
        mActualEventAction.setText(R.string.ok);

        mActualEventAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mPrefs.edit().putBoolean(Prefs.SILENT_DISABLED_BY_USER, false).commit();

                showActualEventMessage();
            }
        });

        mActualEvent.setVisibility(View.VISIBLE);
    }

    private void showAlarmEvent(){
        FileLog.d(TAG, "[showAlarmEvent]");

        int prayerId = PrayersSchedule.getInstance(this).getNextPrayer().getId();

        String msg = getString(
                R.string.alarm_for_prayer,
                FormattingUtils.getCaseTitle(prayerId, FormattingUtils.Case.AKUZATIV));

        mActualEventMessage.setText(msg);

        mActualEventAction.setText(R.string.turn_off);
        mActualEventAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPrefs.edit().putBoolean(Prefs.ALARM_ACTIVE, false).commit();
                AlarmActivity.cancelAlarm(MainActivity.this);
            }
        });

        mActualEvent.setVisibility(View.VISIBLE);
    }

    public void onEvent(Events.PrayerChangedEvent event){
        showActualEventMessage();
    }

    public void onEvent(Events.RingerModeChanged event){
        showActualEventMessage();
    }

    private void checkDozeModeState(){
        Log.d(TAG, "checkDozeModeState");

        boolean askNoMoreAboutDoze = mPrefs.getBoolean(Prefs.ASK_NO_MORE_DOZE, false);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !askNoMoreAboutDoze){
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            String packageName = getPackageName();
            FileLog.i(TAG, "isIgnoringBatteryOptimizations: " + pm.isIgnoringBatteryOptimizations(packageName));

            if(!pm.isIgnoringBatteryOptimizations(packageName)) {
                showDozeModeDialog();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDozeModeDialog() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.doze_mode)
                .setMessage(R.string.doze_mode_message)
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, null)
                .setNegativeButton(R.string.ask_no_more, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mPrefs.edit().putBoolean(Prefs.ASK_NO_MORE_DOZE, true).commit();
                    }
                })
                .setPositiveButton(R.string.open_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .show();
    }
}
