package ba.vaktija.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;

import ba.vaktija.android.models.Events;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import de.greenrobot.event.EventBus;

//import ba.vaktija.android.util.FileLog;

public class FragmentPrayer extends Fragment {
    public static final String TAG = FragmentPrayer.class.getSimpleName();

    public static final String EXTRA_ID = "EXTRA_ID";

    private Prayer mPrayer;

    private SharedPreferences mPrefs;
    private AppCompatActivity mActivity;

    private App mApp;

    private CountDownTimer mCountDownTimer;

    int mVakatId = -1;

    boolean mLandscapeMode = false;

    private EventBus mEventBus = EventBus.getDefault();

    View root;
    TextView mPrayerTitle;
    TextView mPrayerTimer;
    TextView mTimeText;
    TextView mNotificationOnText;
    TextView mAlarmOnText;
    TextView mSoundOnText;
    View mAlarmDetailsWrapper;
    View mSoundDetailsWrapper;
    View mNotifsDetailsWrapper;
    ImageView mSoundOffIcon;
    ImageView mAlarmIcon;
    ImageView mNotifIcon;
    ImageView mMoreIcon;

    private boolean mRespectJuma;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //		FileLog.d(TAG, "onCreateView");

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        if(getTag() != null){
            mVakatId = Integer.valueOf(getTag());
        }

        View view = inflater.inflate(R.layout.fragment_prayer, container, false);

        root = view.findViewById(R.id.vakat_container);
        mPrayerTimer = (TextView) view.findViewById(R.id.fragment_prayer_timer);
        mPrayerTitle = (TextView) view.findViewById(R.id.fragment_prayer_title);
        mTimeText = (TextView) view.findViewById(R.id.vakat_time);
        mNotificationOnText = (TextView)view.findViewById(R.id.vakat_notif_on_mins);
        mAlarmOnText = (TextView) view.findViewById(R.id.vakat_alarm_on_mins);
        mSoundOnText = (TextView) view.findViewById(R.id.vakat_sound_on_mins);
        mAlarmDetailsWrapper = view.findViewById(R.id.vakat_alarm_details_container);
        mSoundDetailsWrapper = view.findViewById(R.id.vakat_silent_details_container);
        mNotifsDetailsWrapper =  view.findViewById(R.id.vakat_notif_details_container);
        mSoundOffIcon = (ImageView) view.findViewById(R.id.vakat_sound_off_icon);
        mAlarmIcon = (ImageView) view.findViewById(R.id.vakat_alarm_icon);
        mNotifIcon = (ImageView) view.findViewById(R.id.vakat_notif_icon);
        mMoreIcon = (ImageView) view.findViewById(R.id.vakat_more);

        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//		FileLog.d(TAG, "onActivityCreated");

        mActivity = (AppCompatActivity) getActivity();
        mApp = (App) mActivity.getApplicationContext();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        if(getArguments() != null && getArguments().containsKey(EXTRA_ID))
            mVakatId = getArguments().getInt(EXTRA_ID);

//		FileLog.i(TAG, "mPrayer: "+mPrayer);

        mLandscapeMode = getResources().getBoolean(R.bool.dual_pane);

        mRespectJuma = mPrefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);

        mMoreIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(mActivity, v);
                popup.inflate(R.menu.prayer_events_popup);

                MenuItem skipAlarm = popup.getMenu().findItem(R.id.prayer_events_popup_skip_alarm);
                MenuItem skipNotif = popup.getMenu().findItem(R.id.prayer_events_popup_skip_notif);
                MenuItem skipSilent = popup.getMenu().findItem(R.id.prayer_events_popup_skip_silent);

                skipAlarm.setTitle(!mPrayer.skipNextAlarm()
                        ? R.string.skip_alarm
                        : R.string.turn_alarm_on);

                skipNotif.setTitle(!mPrayer.skipNextNotif()
                        ? R.string.skip_notification
                        : R.string.turn_notification_on);

                skipSilent.setTitle(!mPrayer.skipNextSilent()
                        ? R.string.skip_silent
                        : R.string.turn_silent_on);

                skipAlarm.setEnabled(mPrayer.isAlarmOn());
                skipAlarm.setVisible(mPrayer.isAlarmOn());

                skipNotif.setEnabled(mPrayer.isNotifOn());
                skipNotif.setVisible(mPrayer.isNotifOn());

                skipSilent.setEnabled(mPrayer.isSilentOn());
                skipSilent.setVisible(mPrayer.isSilentOn());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        String action = "N/A";

                        switch (menuItem.getItemId()) {
                            case R.id.prayer_events_popup_skip_alarm:
                                mPrayer.setSkipNextAlarm(!mPrayer.skipNextAlarm());
                                mApp.sendEvent(mPrayer.getTitle(), mPrayer.skipNextAlarm() ? "Skipping next alarm" : "Restoring next alarm");

                                action = VaktijaService.ACTION_ALARM_CHANGED;
                                break;
                            case R.id.prayer_events_popup_skip_notif:
                                mPrayer.setSkipNextNotif(!mPrayer.skipNextNotif());
                                mApp.sendEvent(mPrayer.getTitle(), mPrayer.skipNextNotif() ? "Skipping next notification" : "Restoring next notification");

                                action = VaktijaService.ACTION_NOTIF_CHANGED;
                                break;
                            case R.id.prayer_events_popup_skip_silent:
                                mPrayer.setSkipNextSilent(!mPrayer.skipNextSilent());
                                mApp.sendEvent(mPrayer.getTitle(), mPrayer.skipNextSilent() ? "Skipping next silent" : "Restoring next silent");

                                if (!mPrayer.skipNextSilent()) {
                                    mPrefs.edit().putBoolean(Prefs.SILENT_DISABLED_BY_USER, false).commit();
                                }

                                action = VaktijaService.ACTION_SILENT_CHANGED;
                                break;
                        }

                        updateIndicators();

                        mPrayer.save();

                        PrayersSchedule.getInstance(mApp).reset();
                        Intent service = VaktijaService.getStartIntent(mActivity, TAG + " onClick()");
                        service.setAction(action);
                        mActivity.startService(service);
                        return true;
                    }
                });

                popup.show();
            }
        });
    }

    void showPrayer(){
        FileLog.d(TAG, "[showPrayer]");

        mPrayer = PrayersSchedule.getInstance(mApp).getPrayer(mVakatId);

        if(PrayersSchedule.getInstance(mActivity).isJumaDay() && mVakatId == Prayer.DHUHR){
            mPrayer = PrayersSchedule.getInstance(mActivity).getPrayer(Prayer.JUMA);
        }

        FileLog.i(TAG, "mPrayer="+mPrayer);

        boolean isNext = PrayersSchedule.getInstance(mApp).isNextPrayer(mPrayer.getId());
        boolean current = mPrayer.getId() == PrayersSchedule.getInstance(mApp).getCurrentPrayer().getId();

        mPrayerTitle.setText(
                isNext
                        ? mPrayer.getShortTitle().toUpperCase(Locale.getDefault())
                        : mPrayer.getTitle().toUpperCase(Locale.getDefault())
        );

        mTimeText.setText(mPrayer.getHrsString()+":"+ mPrayer.getMinsString());

        mSoundOffIcon.setVisibility(mPrayer.isSilentOn() ? View.VISIBLE : View.GONE);
        mSoundDetailsWrapper.setVisibility(mPrayer.isSilentOn() ? View.VISIBLE : View.GONE);

        mNotifsDetailsWrapper.setVisibility(mPrayer.isNotifOn() ? View.VISIBLE : View.GONE);

        mAlarmIcon.setVisibility(mPrayer.isAlarmOn() ? View.VISIBLE : View.GONE);
        mAlarmDetailsWrapper.setVisibility(mPrayer.isAlarmOn() ? View.VISIBLE : View.GONE);

        mPrayerTimer.setVisibility(View.GONE);

        if(current){
            mTimeText.setTypeface(App.robotoCondensedRegular);
            mTimeText.setTextColor(getResources().getColor(R.color.curent_prayer_highlight));

            if(root instanceof CardView) {
//                ((CardView)root).setCardElevation(getResources().getDimension(R.dimen.card_elevation_current));
            }
        } else {
            mTimeText.setTypeface(App.robotoCondensedLight);
            mTimeText.setTextColor(getResources().getColor(android.R.color.darker_gray));

            if(root instanceof CardView) {
//                ((CardView) root).setCardElevation(getResources().getDimension(R.dimen.card_elevation));
            }
        }

        root.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PrayerActivity.getLaunchIntent(mActivity, mPrayer));
            }
        });

        mNotificationOnText.setText("-"+ mPrayer.getNotifMins()+"'");
        mAlarmOnText.setText("-"+ mPrayer.getAlarmMins()+"'");
        mSoundOnText.setText(""+ mPrayer.getSoundOnMins()+"'");

        updateIndicators();

        if(isNext){
            startTimer();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
		FileLog.d(TAG, "onStart");
        showPrayer();
        mEventBus.register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
		FileLog.d(TAG, "onResume");

        boolean respectJuma = mPrefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);

        if(mPrayer.getId() == Prayer.DHUHR && (respectJuma != mRespectJuma)){
            showPrayer();
            mRespectJuma = mPrefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
		FileLog.d(TAG, "onStop");

        if(mCountDownTimer != null)
            mCountDownTimer.cancel();

        mEventBus.unregister(this);
    }

    void startTimer(){
        //FileLog.d(TAG, "startTimer, day is ending: "+mDayEnd);

        mPrayerTimer.setVisibility(View.VISIBLE);

        long secondsInFuture = PrayersSchedule.getInstance(mActivity).getTimeTillNextPrayer();

        if(mCountDownTimer != null){
            mCountDownTimer.cancel();
        }

        mCountDownTimer = new CountDownTimer(secondsInFuture * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                mPrayerTimer.setText(FormattingUtils.getFormattedTime(millisUntilFinished, true));
            }

            @Override
            public void onFinish() {
                mPrayerTimer.setVisibility(View.GONE);
            }
        };
        mCountDownTimer.start();
    }

    private void updateIndicators(){
        int colorLight = getResources().getColor(R.color.event_skipped);

        mMoreIcon.setVisibility(mPrayer.anyEventsOn() ? View.VISIBLE : View.GONE);

        mNotificationOnText.setTextColor(mPrayer.skipNextNotif() ? colorLight : Color.GRAY);
        mAlarmOnText.setTextColor(mPrayer.skipNextAlarm() ? colorLight : Color.GRAY);
        mSoundOnText.setTextColor(mPrayer.skipNextSilent() ? colorLight : Color.GRAY);

        mNotificationOnText.setBackgroundResource(mPrayer.skipNextNotif() ? R.drawable.tip_bg_disabled : R.drawable.tip_bg);
        mAlarmOnText.setBackgroundResource(mPrayer.skipNextAlarm() ? R.drawable.tip_bg_disabled : R.drawable.tip_bg);
        mSoundOnText.setBackgroundResource(mPrayer.skipNextSilent() ? R.drawable.tip_bg_disabled : R.drawable.tip_bg);

        mAlarmIcon.getDrawable().mutate().setAlpha(mPrayer.skipNextAlarm() ? 80 : 255);
        mNotifIcon.getDrawable().mutate().setAlpha(mPrayer.skipNextNotif() ? 80 : 255);
        mSoundOffIcon.getDrawable().mutate().setAlpha(mPrayer.skipNextSilent() ? 80 : 255);
    }

    public void onEventMainThread(Events.SkipSilentEvent event){
		FileLog.d(TAG, "onEvent SilentDisabledEvent");

        if(event.getPrayerId() == mPrayer.getId())
            showPrayer();
    }

    public void onEventMainThread(Events.PrayerChangedEvent event){
		FileLog.d(TAG, "onEvent VakatChangedEvent");

        showPrayer();
    }

    public void onEventMainThread(Events.PrayerUpdatedEvent event){
		FileLog.d(TAG, "onEventMainThread PrayerUpdatedEvent");
        if(event.getPrayerId() == mPrayer.getId()
                || (mPrayer.getId() == Prayer.JUMA && event.getPrayerId() == Prayer.DHUHR)) {

            showPrayer();
        }
    }
}
