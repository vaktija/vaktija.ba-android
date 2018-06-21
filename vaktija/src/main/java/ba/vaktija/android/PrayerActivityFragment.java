package ba.vaktija.android;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.Utils;

import java.util.Locale;

/**
 * Created by e on 1/28/15.
 */
public class PrayerActivityFragment extends Fragment implements SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {

    public static final String TAG = PrayerActivityFragment.class.getSimpleName();

    private static final String EXTRA_PRAYER_ID = "EXTRA_PRAYER_ID";
    private static final String EXTRA_RESPECT_JUMA = "EXTRA_RESPECT_JUMA";
    //	private static final int ALARM_AUDIO_CHOOSER = 1;
    //	private static final int NOTIF_AUDIO_CHOOSER = 2;

    // User post JELLY_BEAN androids
    SwitchCompat alarmButton;
    SwitchCompat silentButton;
    SwitchCompat notifButton;

    // User pre JELLY_BEAN androids
    CheckBox alarmCheckBox;
    CheckBox silentCheckBox;
    CheckBox notifCheckBox;

    TextView alarmTime;
    TextView soundOffTime;
    SeekBar alarmSeekBar;
    SeekBar silentOffSeekBar;
    View alarmOptionsWrapper;
    View soundOptionsWrapper;
    View notifOptionsWrapper;
    CheckBox notifUseSound;
    CheckBox notifUseVibro;
    SeekBar notifTime;

    TextView notifOnTime;
    TextView soundLabel;
    CheckBox vibroOff;

    CardView alarm;
    CardView notif;
    CardView silent;

    boolean invertValues = false;
    Prayer mPrayer;
    App app;

    private boolean respectJuma;

    private boolean mUseCheckBoxes;

    float cardElevEnabled;
    float cardElev;

    int colorEnabled;
    int colorDisabled;

    public static PrayerActivityFragment newInstance(int prayerId, boolean respectJuma){
        PrayerActivityFragment fragment = new PrayerActivityFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_PRAYER_ID, prayerId);
        args.putBoolean(EXTRA_RESPECT_JUMA, respectJuma);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_activity_prayer, container, false);

        alarm = (CardView) view.findViewById(R.id.activity_vakat_alarm);
        notif = (CardView) view.findViewById(R.id.activity_vakat_notif);
        silent = (CardView) view.findViewById(R.id.activity_vakat_silent);

        alarmButton = (SwitchCompat) view.findViewById(R.id.activity_vakat_alarmSwitch);
        silentButton = (SwitchCompat) view.findViewById(R.id.activity_vakat_silentSwitch);
        notifButton = (SwitchCompat) view.findViewById(R.id.activity_vakat_notifSwitch);

        alarmCheckBox = (CheckBox) view.findViewById(R.id.activity_vakat_alarmCheckBox);
        silentCheckBox = (CheckBox) view.findViewById(R.id.activity_vakat_silentCheckBox);
        notifCheckBox = (CheckBox) view.findViewById(R.id.activity_vakat_notifCheckBox);

        alarmSeekBar = (SeekBar) view.findViewById(R.id.activity_vakat_alarmSeekBar);
        silentOffSeekBar = (SeekBar) view.findViewById(R.id.activity_vakat_silentOffSeekBar);
        notifTime = (SeekBar) view.findViewById(R.id.activity_vakat_notifTime);

        alarmOptionsWrapper = view.findViewById(R.id.activity_vakat_alarmOptionsWrapper);
        soundOptionsWrapper = view.findViewById(R.id.activity_vakat_soundOptionsWrapper);
        notifOptionsWrapper = view.findViewById(R.id.activity_vakat_notifOptionsWrapper);

        alarmTime = (TextView) view.findViewById(R.id.fragment_activity_prayer_alarmTime);

        notifUseSound = (CheckBox) view.findViewById(R.id.fragment_activity_prayer_notifUseSound);
        notifUseVibro = (CheckBox) view.findViewById(R.id.fragment_activity_prayer_notifUseVibro);
        notifOnTime = (TextView) view.findViewById(R.id.fragment_activity_prayer_notifTime);

        soundOffTime = (TextView) view.findViewById(R.id.fragment_activity_prayer_silentTime);
        soundLabel = (TextView) view.findViewById(R.id.fragment_activity_prayer_silentOffLabel);
        vibroOff = (CheckBox) view.findViewById(R.id.fragment_activity_prayer_silentVibroOff);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");

        app = (App) getActivity().getApplicationContext();

        respectJuma = getArguments().getBoolean(EXTRA_RESPECT_JUMA, false);

        int prayerId = getArguments().getInt(EXTRA_PRAYER_ID);

        mPrayer = PrayersSchedule.getInstance(app).getPrayer(prayerId);

        colorEnabled = getResources().getColor(R.color.theme_gray);
        colorDisabled = getResources().getColor(R.color.text_disabled);

        if(mPrayer.getId() == Prayer.JUMA && !respectJuma){
            mPrayer = PrayersSchedule.getInstance(app).getPrayer(Prayer.DHUHR);
        }

        if (mPrayer.getId() == Prayer.DHUHR && respectJuma) {
            mPrayer = PrayersSchedule.getInstance(app).getPrayer(Prayer.JUMA);
        }

        alarmSeekBar.setMax(Defaults.getMaxValue(mPrayer.getId(), Prayer.FIELD_ALARM));
        notifTime.setMax(Defaults.getMaxValue(mPrayer.getId(), Prayer.FIELD_NOTIF));
        silentOffSeekBar.setMax(Defaults.getMaxValue(mPrayer.getId(), Prayer.FIELD_SILENT));

        mUseCheckBoxes = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN;

        invertValues = mPrayer.getId() == Prayer.SUNRISE;

        soundLabel.setText(invertValues ? "ISKLJUČIVANJE ZVUKOVA" : "UKLJUČIVANJE ZVUKOVA");

        TextView silentDesc = (TextView) getView().findViewById(R.id.fragment_activity_prayer_silentDescription);
        silentDesc.setText(mPrayer.getId() == Prayer.SUNRISE ? R.string.silent_description_sunrise : R.string.silent_description);

        app.sendScreenView(mPrayer.getTitle());

        FileLog.i(TAG, "mPrayer=" + mPrayer);
        FileLog.i(TAG, "respectJuma=" + respectJuma);

        showActionBarTitle();

        int soundOnMins = Math.abs(mPrayer.getSoundOnMins());
        int notifMins = mPrayer.getNotifMins();
        int alarmMins = mPrayer.getAlarmMins();

        long soundOnMillis = soundOnMins * 60 * 1000;

        alarmTime.setText(FormattingUtils.getFormattedTime(alarmMins * 60000, false)+" prije nastupa");// ("+ mPrayer.getAlarmActivationTime()+")");

        soundOffTime.setText(FormattingUtils.getFormattedTime(soundOnMillis, false)
                + (invertValues ? " prije nastupa" : " nakon nastupa"));// ("+ mPrayer.getSilentDeactivationTime()+")"));

        notifOnTime.setText(
                FormattingUtils.getFormattedTime(notifMins * 60000, false)+" prije nastupa");// ("+ mPrayer.getNotificationTime()+")");

        notifUseSound.setChecked(mPrayer.isNotifSoundOn());
        notifUseVibro.setChecked(mPrayer.isNotifVibroOn());
        vibroOff.setChecked(mPrayer.isSilentVibrationOff());

        alarmSeekBar.setProgress(alarmMins);
        notifTime.setProgress(notifMins);
        silentOffSeekBar.setProgress(soundOnMins);

        alarmOptionsWrapper.setEnabled(mPrayer.isAlarmOn());
        soundOptionsWrapper.setEnabled(mPrayer.isSilentOn());
        notifOptionsWrapper.setEnabled(mPrayer.isNotifOn());

        cardElevEnabled = getResources().getDimension(R.dimen.card_elevation_current);
        cardElev = getResources().getDimension(R.dimen.card_elevation);

        alarm.setCardElevation(mPrayer.isAlarmOn() ? cardElevEnabled : cardElev);
        notif.setCardElevation(mPrayer.isNotifOn() ? cardElevEnabled : cardElev);
        silent.setCardElevation(mPrayer.isSilentOn() ? cardElevEnabled : cardElev);

        if(mUseCheckBoxes) {
            alarmCheckBox.setChecked(mPrayer.isAlarmOn());
            silentCheckBox.setChecked(mPrayer.isSilentOn());
            notifCheckBox.setChecked(mPrayer.isNotifOn());

            alarmCheckBox.setOnCheckedChangeListener(this);
            silentCheckBox.setOnCheckedChangeListener(this);
            notifCheckBox.setOnCheckedChangeListener(this);

            alarmButton.setVisibility(View.GONE);
            silentButton.setVisibility(View.GONE);
            notifButton.setVisibility(View.GONE);
        } else {
            alarmButton.setChecked(mPrayer.isAlarmOn());
            silentButton.setChecked(mPrayer.isSilentOn());
            notifButton.setChecked(mPrayer.isNotifOn());

            alarmButton.setOnCheckedChangeListener(this);
            silentButton.setOnCheckedChangeListener(this);
            notifButton.setOnCheckedChangeListener(this);

            alarmCheckBox.setVisibility(View.GONE);
            silentCheckBox.setVisibility(View.GONE);
            notifCheckBox.setVisibility(View.GONE);
        }

        notifUseSound.setOnCheckedChangeListener(this);
        notifUseVibro.setOnCheckedChangeListener(this);
        vibroOff.setOnCheckedChangeListener(this);

        silentOffSeekBar.setOnSeekBarChangeListener(this);
        notifTime.setOnSeekBarChangeListener(this);
        alarmSeekBar.setOnSeekBarChangeListener(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            vibroOff.setTextColor(colorDisabled);
            vibroOff.setDuplicateParentStateEnabled(false);
            vibroOff.setEnabled(false);
        }

        updateCheckBoxColor();
    }

    private void updateCheckBoxColor(){

        notifUseSound.setTextColor(mPrayer.isNotifOn() ? colorEnabled : colorDisabled);
        notifUseVibro.setTextColor(mPrayer.isNotifOn() ? colorEnabled : colorDisabled);

        if(vibroOff.isEnabled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            vibroOff.setTextColor(mPrayer.isSilentOn() ? colorEnabled : colorDisabled);
        }
    }

    void showActionBarTitle(){

        String title = mPrayer.getTitle().toUpperCase(Locale.getDefault());

        boolean respect = App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);

        boolean hideTitle = mPrayer.getId() == Prayer.DHUHR;
        hideTitle |= mPrayer.getId() == Prayer.JUMA;

        if(respect && hideTitle){
            title = "";
        }

        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(false);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.getId() == R.id.activity_vakat_alarmSeekBar){
            mPrayer.setAlarmMins(seekBar.getProgress());

            alarmTime.setText(FormattingUtils.getFormattedTime(progress * 60 * 1000, false)+" prije nastupa");// ("+ mPrayer.getAlarmActivationTime()+")");
        }

        if(seekBar.getId() == R.id.activity_vakat_silentOffSeekBar){
            mPrayer.setSoundOnMins((seekBar.getProgress() * (invertValues ? -1 : 1)));

            soundOffTime.setText(
                    FormattingUtils.getFormattedTime(progress * 60 * 1000, false)+
                            (invertValues ? " prije" : " nakon")+
                            " nastupa");// ("+ mPrayer.getSilentDeactivationTime()+")");
        }

        if(seekBar.getId() == R.id.activity_vakat_notifTime){
            mPrayer.setNotifMins(seekBar.getProgress());

            notifOnTime.setText(FormattingUtils.getFormattedTime(progress * 60 * 1000, false)+" prije nastupa");// ("+ mPrayer.getNotificationTime()+")");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {	}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        FileLog.d(TAG, "onStopTrackingTouch");

        //		String message = null;
        String startedFrom = "";
        String action = "";

        switch (seekBar.getId()) {
            case R.id.activity_vakat_alarmSeekBar:

                mPrayer.setAlarmMins(seekBar.getProgress());

                app.sendEvent(mPrayer.getTitle() + " settings", "alarm time set to " + seekBar.getProgress());
                //			message = mPrayer.getAlarmActivationTime();
                action = VaktijaService.ACTION_ALARM_CHANGED;
                startedFrom = "activity_vakat_alarmSeekBar";

                break;
            case R.id.activity_vakat_silentOffSeekBar:

                mPrayer.setSoundOnMins((seekBar.getProgress() * (invertValues ? -1 : 1)));

                app.sendEvent(mPrayer.getTitle() + " settings", "silent off time set to " + seekBar.getProgress());
                //			message = mPrayer.getSilentDeactivationTime();
                action = VaktijaService.ACTION_SILENT_CHANGED;
                startedFrom = "activity_vakat_silentOffSeekBar";

                break;
            case R.id.activity_vakat_notifTime:

                mPrayer.setNotifMins(seekBar.getProgress());

                app.sendEvent(mPrayer.getTitle() + " settings", "notification time set to " + seekBar.getProgress());
                //			message = mPrayer.getNotificationTime();
                action = VaktijaService.ACTION_NOTIF_CHANGED;
                startedFrom = "activity_vakat_notifTime";

                break;
        }

        mPrayer.save();

        PrayersSchedule.getInstance(app).reset();

        app.prefs.edit()
                .putBoolean(Prefs.SILENT_NOTIF_DELETED+"_"+ mPrayer.getId(), false)
                .putBoolean(Prefs.APPROACHING_NOTIF_DELETED+"_"+(mPrayer.getId() - 1), false)
                .commit();

        Intent service = VaktijaService.getStartIntent(app, TAG + ":" + startedFrom);
        service.setAction(action);
        getActivity().startService(service);

        Utils.updateWidget(getActivity());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        FileLog.d(TAG, "onCheckedChanged");

        long start = System.nanoTime();

        String startedFrom = "";
        String action = "";
        final String gaEventCategory = mPrayer.getTitle()+" settings";
        String gaEventAction = "";

        switch (buttonView.getId()){
            case R.id.fragment_activity_prayer_notifUseSound:
                mPrayer.setNotifSoundOn(isChecked);

                startedFrom = "onCheckedChanged-activity_vakat_notifUseSound";
                action = VaktijaService.ACTION_NOTIF_CHANGED;
                gaEventAction = isChecked
                        ? "Enabled sound for notifications"
                        : "Disabled sound for notifications";
                break;

            case R.id.fragment_activity_prayer_notifUseVibro:
                mPrayer.setNotifVibroOn(isChecked);

                startedFrom = "onCheckedChanged-activity_vakat_notifUseVibro";
                action = VaktijaService.ACTION_NOTIF_CHANGED;

                gaEventAction = isChecked ? "Enabled vibration for notifications"
                        : "Disabled vibration for notifications";

                break;

            case R.id.fragment_activity_prayer_silentVibroOff:
                mPrayer.setSilentVibrationOff(isChecked);

                startedFrom = "onCheckedChanged-activity_vakat_vibroOff";
                action = VaktijaService.ACTION_SILENT_CHANGED;

                gaEventAction = isChecked ? "Disabled vibration in silent mode"
                        : "Enabled vibration in silent mode";
                break;

            case R.id.activity_vakat_alarmSwitch:
            case R.id.activity_vakat_alarmCheckBox:

                alarm.setCardElevation(isChecked ? cardElevEnabled : cardElev);
                mPrayer.setSkipNextAlarm(false);
                mPrayer.setAlarmOn(isChecked);
                alarmOptionsWrapper.setEnabled(isChecked);

                startedFrom = "onCheckedChanged-activity_vakat_alarmSwitch";
                action = VaktijaService.ACTION_ALARM_CHANGED;

                gaEventAction = isChecked ? "Enabled alarm" : "Disabled alarm";

                break;

            case R.id.activity_vakat_notifSwitch:
            case R.id.activity_vakat_notifCheckBox:

                notif.setCardElevation(isChecked ? cardElevEnabled : cardElev);

                //notifUseSound.setTextColor(isChecked ? colorEnabled : colorDisabled);
                //notifUseVibro.setTextColor(isChecked ? colorEnabled : colorDisabled);

                mPrayer.setSkipNextNotif(false);
                mPrayer.setNotifOn(isChecked);
                notifOptionsWrapper.setEnabled(isChecked);

                startedFrom = "onCheckedChanged-activity_vakat_notifSwitch";
                action = VaktijaService.ACTION_NOTIF_CHANGED;

                gaEventAction = isChecked ? "Enabled notification" : "Disabled notification";

                break;

            case R.id.activity_vakat_silentSwitch:
            case R.id.activity_vakat_silentCheckBox:

                vibroOff.setTextColor(isChecked ? colorEnabled : colorDisabled);
                silent.setCardElevation(isChecked ? cardElevEnabled : cardElev);

                soundOptionsWrapper.setEnabled(isChecked);

                mPrayer.setSkipNextSilent(false);
                mPrayer.setSilentOn(isChecked);

                startedFrom = "onCheckedChanged-activity_vakat_silentSwitch";
                action = VaktijaService.ACTION_SILENT_CHANGED;

                gaEventAction = isChecked ? "Enabled silent mode" : "Disabled silent mode";

                break;
        }

        final String serviceAction = action;
        final String serviceStartedFrom = startedFrom;
        final String eventAction = gaEventAction;

        mPrayer.save();

        getView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent service = VaktijaService.getStartIntent(getActivity(), TAG + ":" + serviceStartedFrom);
                service.setAction(serviceAction);
                getActivity().startService(service);

                Utils.updateWidget(getActivity());

                app.sendEvent(gaEventCategory, eventAction);
            }
        }, 500);

        long endTime = System.nanoTime();

        Log.i(TAG, "onChecked changed done in "+(endTime - start)/1000.0+" us");
    }
}
