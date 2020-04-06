package ba.vaktija.android.prefs;

import ba.vaktija.android.App;
import ba.vaktija.android.LocationActivity;
import ba.vaktija.android.R;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.service.NotificationsManager;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.SettingsManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import androidx.core.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final String TAG = SettingsFragment.class.getSimpleName();
    public static final String EXTRA_FIRST_VISIBLE_ITEM = "EXTRA_FIRST_VISIBLE_ITEM";
    public static final String EXTRA_ITEM_TOP = "EXTRA_ITEM_TOP";

    private static final int REQUEST_NOTIF_TONE = 1;
    private static final int REQUEST_ALARM_TONE = 2;

    SharedPreferences prefs;

    StringBuilder aboutText;

    ListPreference dhuhrCounting;
    Preference mNotificationTonePreference;
    Preference mAlarmTonePreference;

    String selectedTheme;

    App mApp;

    private AlertDialog alertDialog;
    private String mSelectedColor = "";

    private boolean scrolled;
    private  Preference location;

    public static SettingsFragment newInstance(int scrollPosition, int itemTop){
        Bundle args = new Bundle();
        args.putInt(EXTRA_FIRST_VISIBLE_ITEM, scrollPosition);
        args.putInt(EXTRA_ITEM_TOP, itemTop);

        SettingsFragment sf = new SettingsFragment();
        sf.setArguments(args);

        return sf;
    }

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        Log.d(TAG, "onAttach");

        mApp = (App) activity.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

        location = findPreference(Prefs.LOCATION_NAME);
        location.setOnPreferenceClickListener(this);
        location.setSummary(prefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME));

        Preference statusBarNotif = findPreference(Prefs.STATUSBAR_NOTIFICATION);
        statusBarNotif.setOnPreferenceChangeListener(this);

        Preference allPrayersNotif = findPreference(Prefs.ALL_PRAYERS_IN_NOTIF);
        allPrayersNotif.setOnPreferenceChangeListener(this);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN){
            PreferenceCategory category = (PreferenceCategory) findPreference("notifications");
            category.removePreference(allPrayersNotif);
        }

        mNotificationTonePreference = findPreference(Prefs.NOTIF_TONE_URI);
        mNotificationTonePreference.setOnPreferenceClickListener(this);

        String title = Defaults.NOTIF_TONE_TITLE;

        if(!prefs.getBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, true)) {
            String selectedNotifTone = prefs.getString(Prefs.NOTIF_TONE_URI,
                    Defaults.getDefaultTone(getActivity(), false));

            Ringtone ringtone = RingtoneManager
                    .getRingtone(mApp, Uri.parse(selectedNotifTone));

            FileLog.d(TAG, "selectedNotifTone: " + selectedNotifTone);
            title = ringtone.getTitle(mApp);
        }

        mNotificationTonePreference.setSummary(title);

        mAlarmTonePreference = findPreference(Prefs.ALARM_TONE_URI);
        mAlarmTonePreference.setOnPreferenceClickListener(this);

        String selectedAlarmToneTitle = Defaults.ALARM_TONE_TITLE;

        if(!prefs.getBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, true)) {
            String selectedAlarmTone = prefs.getString(
                    Prefs.ALARM_TONE_URI,
                    Defaults.getDefaultTone(getActivity(), true));

            Ringtone alarmRingtone = RingtoneManager
                    .getRingtone(mApp, Uri.parse(selectedAlarmTone));

            FileLog.d(TAG, "selectedAlarmTone: " + selectedAlarmTone);
            selectedAlarmToneTitle = alarmRingtone.getTitle(mApp);
        }

        mAlarmTonePreference.setSummary(selectedAlarmToneTitle);

        dhuhrCounting = (ListPreference)findPreference(Prefs.DHUHR_TIME_COUNTING);
        dhuhrCounting.setOnPreferenceChangeListener(this);

        CheckBoxPreference separateJumaSettings = (CheckBoxPreference)findPreference(Prefs.SEPARATE_JUMA_SETTINGS);
        separateJumaSettings.setOnPreferenceChangeListener(this);

        String dhuhrTime = prefs.getString(Prefs.DHUHR_TIME_COUNTING, "1");
        dhuhrCounting.setSummary(
                dhuhrTime.equals("1")
                        ? getString(R.string.standard_time_12_13)
                        : getString(R.string.real_time));

        dhuhrCounting.setValueIndex(dhuhrTime.equals("1") ? 0 : 1);

        colorPreferenceTitles();
    }

    private void colorPreferenceTitles(){

        int almostBlack = getResources().getColor(R.color.almost_black);

        location.setTitle(FormattingUtils.colorText(
                getString(R.string.location),
                almostBlack));

        Preference statusBarNotif = findPreference(Prefs.STATUSBAR_NOTIFICATION);
        statusBarNotif.setTitle(FormattingUtils.colorText(
                getString(R.string.statusbar_notification),
                almostBlack));

        Preference allPrayersNotif = findPreference(Prefs.ALL_PRAYERS_IN_NOTIF);

        // If preference is not removed from its category
        if(allPrayersNotif != null) {
            allPrayersNotif.setTitle(FormattingUtils.colorText(
                    getString(R.string.all_prayers_in_notif),
                    almostBlack));
        }

        mNotificationTonePreference .setTitle(FormattingUtils.colorText(
                getString(R.string.notification_sound),
                almostBlack));

        mAlarmTonePreference.setTitle(FormattingUtils.colorText(
                getString(R.string.alarm_sound),
                almostBlack));

        dhuhrCounting.setTitle(FormattingUtils.colorText(
                getString(R.string.dhuhr_counting),
                almostBlack));

        Preference separateJumaSettings = findPreference(Prefs.SEPARATE_JUMA_SETTINGS);
        separateJumaSettings.setTitle(FormattingUtils.colorText(
                getString(R.string.separate_juma_settings),
                almostBlack));

        Preference showDate = findPreference(Prefs.SHOW_DATE);
        showDate.setTitle(FormattingUtils.colorText(
                getString(R.string.date_in_subtitle),
                almostBlack));

        /*
        Preference ga = findPreference(Prefs.GA_ENABLED);
        ga.setTitle(FormattingUtils.colorText(
                getString(R.string.google_analytics),
                almostBlack));
                */

        Preference feedback = findPreference(Prefs.FEEDBACK);
        feedback.setTitle(FormattingUtils.colorText(
                getString(R.string.feedback),
                almostBlack));

        Preference about = findPreference(Prefs.ABOUT);
        about.setTitle(FormattingUtils.colorText(
                getString(R.string.about_app),
                almostBlack));
    }

    @Override
    public void onResume(){
        super.onResume();

        if(!scrolled && getArguments() != null && getArguments().containsKey(EXTRA_FIRST_VISIBLE_ITEM)) {
            scrolled = true;
            getListView().post(new Runnable() {
                @Override
                public void run() {
                    int scrollPosition = getArguments().getInt(EXTRA_FIRST_VISIBLE_ITEM);
                    int itemTop = getArguments().getInt(EXTRA_ITEM_TOP);
                    getListView().setSelection(scrollPosition);
                    getListView().smoothScrollBy(-itemTop, 0);
                }
            });
        }

        location.setSummary(prefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME));

        getListView().setDivider(null);
        getListView().setDividerHeight(0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        FileLog.d(TAG, "onPreferenceChange key="+preference.getKey()+" newValue="+newValue);

        if(preference.getKey().equals(Prefs.ALL_PRAYERS_IN_NOTIF)){
            // Wait for onPreferenceChange method to return for change to be saved
            getListView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    NotificationsManager.getInstance(mApp).updateNotification();
                }
            }, 500);
        }

        if(preference.getKey().equals(Prefs.STATUSBAR_NOTIFICATION)){

            boolean enabled = Boolean.parseBoolean(newValue.toString());

            mApp.sendEvent("Settings", enabled ? "Enabled status bar notificatoin" : "Disabled status bar notification");

            Intent service = VaktijaService.getStartIntent(getActivity(), TAG + ":onPreferenceChange");
            service.setAction(enabled ? VaktijaService.ACTION_ENABLE_NOTIFS : VaktijaService.ACTION_DISABLE_NOTIFS);
            getActivity().startService(service);
        }

        if(preference.getKey().equals(Prefs.DHUHR_TIME_COUNTING)){

            Intent service = VaktijaService.getStartIntent(getActivity(), TAG + ":onPreferenceChange");
            service.setAction(VaktijaService.ACTION_UPDATE);
            getActivity().startService(service);

            dhuhrCounting.setSummary(newValue.toString().equals(Prefs.DHUHR_NORMALIZED)
                    ? getString(R.string.standard_time_12_13)
                    : getString(R.string.real_time));

            mApp.sendEvent("Settings", newValue.toString().equals(Prefs.DHUHR_NORMALIZED) ? "Normalized time enabled" : "Actual time enabled");
        }

        if(preference.getKey().equals(Prefs.SEPARATE_JUMA_SETTINGS)){

            Intent service = VaktijaService.getStartIntent(getActivity(), TAG + ":onPreferenceChange");
            service.setAction(VaktijaService.ACTION_UPDATE);
            getActivity().startService(service);

            mApp.sendEvent("Settings", ((boolean)newValue)  ? "Separate settings for juma enabled" : "Separate settings for juma disabled");
        }

        return true;
    }

    private void restart(){
        scrolled = false;
        final int firstVisibleItem = getListView().getFirstVisiblePosition();
        final int firstVisibleItemTop = getListView().getChildAt(0).getTop();

        Log.i(TAG, "firstVisibleItem="+firstVisibleItem);
        Log.i(TAG, "firstVisibleItemTop="+firstVisibleItemTop);

        getActivity().finish();
        Intent i = getActivity().getIntent();
        i.putExtra(SettingsActivity.EXTRA_FIRST_VISIBLE_ITEM, firstVisibleItem);
        i.putExtra(SettingsActivity.EXTRA_ITEM_TOP, firstVisibleItemTop);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(0, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        FileLog.d(TAG, "onActivityResult");

        if (resultCode != Activity.RESULT_OK )
            return;

        if(requestCode == REQUEST_NOTIF_TONE) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            FileLog.d(TAG, "REQUEST_NOTIF_TONE: "+uri);

            if(uri == null){
                uri = Settings.System.DEFAULT_NOTIFICATION_URI;
            }

            Ringtone ringtone = RingtoneManager.getRingtone(mApp, uri);

            if(ringtone != null) {

                prefs.edit()
                        .putString(Prefs.NOTIF_TONE_URI, uri.toString())
                        .putBoolean(Prefs.USE_VAKTIJA_NOTIF_TONE, false)
                        .commit();

                mNotificationTonePreference.setSummary(ringtone.getTitle(mApp));
            } else {
                Toast.makeText(getActivity(), R.string.cant_use_selected_tone, Toast.LENGTH_LONG).show();
            }
        }

        if(requestCode == REQUEST_ALARM_TONE) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if(uri == null)
                uri = Settings.System.DEFAULT_ALARM_ALERT_URI;

            FileLog.d(TAG, "REQUEST_ALARM_TONE: "+uri);

            Ringtone ringtone = RingtoneManager.getRingtone(mApp, uri);

            if(ringtone != null) {
                prefs.edit()
                        .putString(Prefs.ALARM_TONE_URI, uri.toString())
                        .putBoolean(Prefs.USE_VAKTIJA_ALARM_TONE, false)
                        .commit();

                mAlarmTonePreference.setSummary(ringtone.getTitle(mApp));
            } else {
                Toast.makeText(getActivity(), R.string.cant_use_selected_tone, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showToneSelectionDialog(final boolean alarmTone){

        final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        ListView listView = (ListView) LayoutInflater.from(mApp).inflate(R.layout.dialog_list_view, null);
        ArrayAdapter<String> optionsAdapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_list_item_1,
                new String[]{alarmTone
                        ? Defaults.ALARM_TONE_TITLE : Defaults.NOTIF_TONE_TITLE, "Odaberi drugi..."});

        listView.setAdapter(optionsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                alertDialog.dismiss();
                switch (position) {
                    case 0:
                        setDefaultTone(alarmTone);
                        break;
                    case 1:
                        launchToneChooser(alarmTone);
                        break;
                }
            }
        });
        dialog.setView(listView);
        alertDialog = dialog.create();
        alertDialog.show();
    }

    private void setDefaultTone(boolean alarmTone){
        Log.d(TAG, "setDefaultTone alarmTone=" + alarmTone);

        prefs.edit()
                //.putString(alarmTone ? Prefs.ALARM_TONE_URI : Prefs.NOTIF_TONE_URI, Defaults.getDefaultTone(getActivity(), false))
                .putBoolean(alarmTone ? Prefs.USE_VAKTIJA_ALARM_TONE : Prefs.USE_VAKTIJA_NOTIF_TONE, true)
                .commit();

        if(alarmTone)
            mAlarmTonePreference.setSummary(Defaults.ALARM_TONE_TITLE);
        else
            mNotificationTonePreference.setSummary(Defaults.NOTIF_TONE_TITLE);
    }

    private void launchToneChooser(boolean alarmTone){
        String path = prefs.getString(alarmTone ? Prefs.ALARM_TONE_URI : Prefs.NOTIF_TONE_URI, "");

        Uri uri = !TextUtils.isEmpty(path) ? Uri.parse(path) : null;

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, alarmTone ? RingtoneManager.TYPE_ALARM : RingtoneManager.TYPE_NOTIFICATION);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);

        startActivityForResult(intent, alarmTone ? REQUEST_ALARM_TONE : REQUEST_NOTIF_TONE);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if(preference.getKey().equals(Prefs.LOCATION_NAME)){
            showLocationDialog();
        }

        if(preference.getKey().equals(Prefs.NOTIF_TONE_URI)){

            showToneSelectionDialog(false);
        }

        if(preference.getKey().equals(Prefs.ALARM_TONE_URI)){

            showToneSelectionDialog(true);
        }

        if(preference.getKey().equals(Prefs.EXPORT)){
            new SettingsExporter(getActivity(), false).execute();
        }

        if(preference.getKey().equals(Prefs.IMPORT)){
            new SettingsImporter(getActivity()).execute();
        }

        return true;
    }

    void showLocationDialog(){

        mApp.sendEvent(TAG, "Location");

        Intent i = new Intent(getActivity(), LocationActivity.class);
        startActivity(i);
    }

    private void showSettingsExportedDialog(){
        new  AlertDialog.Builder(getActivity())
                .setTitle("Postavke eksportovane")
                .setMessage("Postavke eksportovane u vaktija_settings.dat fajl")
                .setPositiveButton("Uredu", null)
                .show();
    }

    private void showFileExistsDialog(){
        new  AlertDialog.Builder(getActivity())
                .setTitle("Prepisati postavke?")
                .setMessage("Postavke su već prije eksportovane u vaktija_settings.dat datoteku. Da li ih želite zamijeniti sa novim postavkama?")
                .setPositiveButton("Zamijeni", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SettingsExporter(getActivity(), true).execute();
                    }
                })
                .setNegativeButton("Ne", null)
                .show();
    }

    private void showErrorProcessingSettingsDialog(String errorMessage){
        new  AlertDialog.Builder(getActivity())
                .setTitle("Greška")
                .setMessage("Došlo je do greške prilikom procesiranja postavki: "+errorMessage)
                .setPositiveButton("Uredu", null)
                .show();
    }

    private void showFileMissingDialog(){
        new  AlertDialog.Builder(getActivity())
                .setTitle("Datoteka ne postoji")
                .setMessage("Datoteka vaktija_settings.dat nije pronađena")
                .setPositiveButton("Uredu", null)
                .show();
    }

    private class SettingsExporter extends AsyncTask<Void, Void, Integer>{
        private static final int RESULT_OK = 0;
        private static final int RESULT_FILE_EXISTS = 1;
        private static final int RESULT_ERROR = 2;

        private Context mContext;
        private ProgressDialog progressDialog;
        private boolean mOverwriteExisting;
        private String mErrorMessage;

        public SettingsExporter(Context context, boolean overwriteExisting){
            mContext = context;
            mOverwriteExisting = overwriteExisting;
        }

        @Override
        protected void onPreExecute(){
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage("Eksportujem postavke...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {

            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                mErrorMessage = "External storage not mounted";
                return RESULT_ERROR;
            }

            File data = new File(Environment.getExternalStorageDirectory()+"/vaktija_settings.dat");

            if(!mOverwriteExisting && data.exists()){
                return RESULT_FILE_EXISTS;
            }

            try {
                BufferedWriter buf = new BufferedWriter(new FileWriter(data, false));
                buf.write(SettingsManager.getInstance(mContext).getSettings());
                buf.close();
            } catch (Exception e) {
                e.printStackTrace();
                mErrorMessage = e.getMessage();
                return RESULT_ERROR;
            }

            return RESULT_OK;
        }

        @Override
        protected void onPostExecute(Integer result){

            if(progressDialog != null && progressDialog.isShowing())
                progressDialog.cancel();

            switch (result){
                case RESULT_OK:
                    showSettingsExportedDialog();
                    break;
                case RESULT_FILE_EXISTS:
                    showFileExistsDialog();
                    break;
                case RESULT_ERROR:
                    showErrorProcessingSettingsDialog(mErrorMessage);
                    break;
            }
        }
    }

    private class SettingsImporter extends AsyncTask<Void, Void, Integer>{
        private static final int RESULT_OK = 0;
        private static final int RESULT_MISSING_FILE = 1;
        private static final int RESULT_ERROR = 2;

        private Context mContext;
        private ProgressDialog progressDialog;
        private String mErrorMessage;

        public SettingsImporter(Context context){
            mContext = context;
        }

        @Override
        protected void onPreExecute(){
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage("Importujem postavke...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params) {

            if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                mErrorMessage = "External storage not mounted";
                return RESULT_ERROR;
            }

            File data = new File(Environment.getExternalStorageDirectory()+"/vaktija_settings.dat");

            if(!data.exists()){
                return RESULT_MISSING_FILE;
            }

            if(data.length() > (5 * 1024)){
                mErrorMessage = "Fajl ne sadrži postavke Vaktije ili su postavke neispravne";
                return RESULT_ERROR;
            }

            StringBuilder text = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(data));
                String line;

                while ((line = br.readLine()) != null) {
                    text.append(line);
                }
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
                mErrorMessage = e.getMessage();
                return RESULT_ERROR;
            }

            try {
                SettingsManager.getInstance(mContext).restoreSettings(text.toString());
            } catch (SettingsManager.SettingsCorruptedException e) {
                e.printStackTrace();
                mErrorMessage = "Datoteka ne sadrži postavke Vaktije ili su postavke neispravne";
                return RESULT_ERROR;
            } catch (SettingsManager.UnsupportedFormatException e) {
                mErrorMessage = "Datoteka sadrži nepodržane postavke";
                e.printStackTrace();
                return RESULT_ERROR;
            }

            return RESULT_OK;
        }

        @Override
        protected void onPostExecute(Integer result){

            if(progressDialog != null && progressDialog.isShowing())
                progressDialog.cancel();

            switch (result){
                case RESULT_OK:
                    Toast.makeText(mApp, "Postavke importovane", Toast.LENGTH_SHORT).show();

                    PrayersSchedule.getInstance(getActivity()).reset();

                    Intent service = VaktijaService.getStartIntent(getActivity(), TAG + ":setingsImported");
                    service.setAction(VaktijaService.ACTION_UPDATE);
                    getActivity().startService(service);

                    getActivity().finish();
                    getActivity().startActivity(getActivity().getIntent());

                    break;
                case RESULT_MISSING_FILE:
                    showFileMissingDialog();
                    break;
                case RESULT_ERROR:
                    showErrorProcessingSettingsDialog(mErrorMessage);
                    break;
            }
        }
    }
}
