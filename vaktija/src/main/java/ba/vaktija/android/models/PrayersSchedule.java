package ba.vaktija.android.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import ba.vaktija.android.App;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.SilentModeManager;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by e on 2/6/15.
 */
public class PrayersSchedule {

    public static final String TAG = PrayersSchedule.class.getSimpleName();

    private static PrayersSchedule instance;

    private List<Prayer> mPrayers = new ArrayList<>();
    private Prayer mYesterdaysIsha;
    private Prayer mTomorrowsFajr;
    private Context mContext;
    private Prayer mJuma;
    private SharedPreferences mPrefs;
    private App app;
    private int[] times;
    private int locationId;
    private int currentMonth;
    private int currentDay;
    private int currentYear;

    public static PrayersSchedule getInstance(Context context){
        if(instance == null)
            instance = new PrayersSchedule(context);

        return instance;
    }

    private PrayersSchedule(Context context){
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        app = (App) mContext.getApplicationContext();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        currentMonth = calendar.get(Calendar.MONTH) + 1;
        currentDay = calendar.get(Calendar.DAY_OF_MONTH);
        currentYear = calendar.get(Calendar.YEAR);

        locationId = mPrefs.getInt(Prefs.SELECTED_LOCATION_ID, Defaults.LOCATION_ID);

        times = app.db.getPrayerTimesSec(currentMonth, currentDay, locationId);

        for (int i = Prayer.FAJR; i <= Prayer.ISHA; i++) {
            mPrayers.add(getTodaysPrayer(i));
        }

        mYesterdaysIsha = getYesterdaysIsha();
        mTomorrowsFajr = getTomorrowsFajr();
    }

    private Prayer getTodaysPrayer(int whichPrayer){
        Log.d(TAG, "[getTodaysPrayer whichPrayer=" + whichPrayer + "]");

        if(whichPrayer == Prayer.DHUHR){
            mJuma = new Prayer(
                    getDstRespectingPrayerTime(times[whichPrayer], currentYear, currentMonth, currentDay),
                    Prayer.JUMA);
        }

        return new Prayer(
                getDstRespectingPrayerTime(times[whichPrayer], currentYear, currentMonth, currentDay),
                whichPrayer);
    }

    private Prayer getYesterdaysIsha(){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());

        calendar.add(Calendar.DATE, -1);

        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);

        int[] time = app.db.getPrayerTimesSec(month, day, locationId);

        return new Prayer(
                getDstRespectingPrayerTime(time[Prayer.ISHA], year, month, day),
                Prayer.ISHA);

    }

    private Prayer getTomorrowsFajr(){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());

        calendar.add(Calendar.DATE, 1);

        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int year = calendar.get(Calendar.YEAR);

        int[] time = app.db.getPrayerTimesSec(month, day, locationId);

        return new Prayer(
                getDstRespectingPrayerTime(time[Prayer.FAJR], year, month, day),
                Prayer.FAJR);

    }

    public List<Prayer> getAllPrayers(){
        return mPrayers;
    }

    public Prayer getPrayerForDate(int whichPrayer, int year, int month, int day){
        FileLog.d(TAG, "[getPrayerForDate year=" + year + " month=" + month + " day=" + day + "]");

        int[] time = app.db.getPrayerTimesSec(month, day, locationId);

        if(isJumaDay(year, month, day) && whichPrayer == Prayer.DHUHR){
            return new Prayer(
                    getDstRespectingPrayerTime(time[whichPrayer], year, month, day),
                    Prayer.JUMA);
        }

        return new Prayer(
                getDstRespectingPrayerTime(time[whichPrayer], year, month, day),
                whichPrayer);
    }

    private static int getDstRespectingPrayerTime(int defaultPrayerTime, int year, int month, int day){
        FileLog.d(TAG, "[getDstRespectingPrayerTime defaultPrayerTime="+defaultPrayerTime+" year="+year+" month="+month+" day="+day+"]");

        // beacuse months have index 0 in java Calendar
        --month;

        boolean summerTimeOn = isSummerTimeOn(year, month, day);

        FileLog.i(TAG, "summer time on: "+summerTimeOn);

        if(summerTimeOn){
            return defaultPrayerTime + 3600;
        }

        return defaultPrayerTime;
    }

    public synchronized Prayer getCurrentPrayer(){
        //FileLog.d(TAG, "getCurrentPrayer");

        int currentSeconds = Utils.getCurrentTimeSec();

        for(int i = 0; i < mPrayers.size(); i++){

            if(currentSeconds >= mPrayers.get(Prayer.ISHA).getPrayerTime()){
                return mPrayers.get(Prayer.ISHA);
            }

            if(currentSeconds < mPrayers.get(i).getPrayerTime()){
                if(i == 0) {
                    return mYesterdaysIsha;
                } else {

                    if(i == Prayer.ASR && isJumaDay()){
                        return mJuma;
                    }

                    return mPrayers.get(i - 1);
                }
            }
        }

        return null;
    }

    public boolean isJumaDay(){
        boolean respectJuma = App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        boolean isFriday = (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
        return respectJuma && isFriday;
    }

    private boolean isJumaDay(int year, int month, int day){
        boolean respectJuma = App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        boolean isFriday = (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);
        return (respectJuma && isFriday);
    }

    public int getTimeTillNextPrayer(){

        Prayer currentPrayer = getCurrentPrayer();

        Prayer nextPrayer = getNextPrayer(currentPrayer.getId());

        int nextPrayerTime = nextPrayer.getPrayerTime();

        if(isDayEnding()){
            nextPrayerTime = ((24 * 3600) + nextPrayerTime) - Utils.getCurrentTimeSec();
        } else {
            nextPrayerTime -= Utils.getCurrentTimeSec();
        }

        return nextPrayerTime;
    }

    public boolean isNextPrayerApproaching(){
        //FileLog.d(TAG, "isNextPrayerApproaching");
        Prayer currentPrayer = getCurrentPrayer();
        Prayer nextPrayer = getNextPrayer(currentPrayer.getId());

        FileLog.i(TAG, "current prayer: "+currentPrayer.getTitle());
        FileLog.i(TAG, "next prayer: "+nextPrayer.getTitle());

        int currentTime = Utils.getCurrentTimeSec();

        if(!nextPrayer.isNotifOn() || nextPrayer.skipNextNotif()){
            FileLog.i(TAG, "next prayer approaching: NO");
            return false;
        }

        int notifActivationTime = nextPrayer.getPrayerTime() - nextPrayer.getNotifMins() * 60;

        FileLog.i(TAG, "notification mins for next prayer: "+nextPrayer.getNotifMins());

        FileLog.i(TAG, "notif activation time: "+FormattingUtils.getTimeStringDots(notifActivationTime));
        FileLog.i(TAG, "current time: "+FormattingUtils.getTimeStringDots(currentTime));

        // FileLog.i(TAG, "notif - current time: "+(notifActivationTime - currentTime));

        if(currentTime >= notifActivationTime && currentTime < nextPrayer.getPrayerTime()){
            FileLog.i(TAG, "next prayer approaching: YES");
            return true;
        }

        FileLog.i(TAG, "next prayer approaching: NO");
        return false;
    }

    public Prayer getNextPrayer(int prayerId) {

        boolean respectJuma = App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);

        if(prayerId == Prayer.ISHA) {
            if(isDayEnding()){
                return mTomorrowsFajr;
            } else {
                return mPrayers.get(Prayer.FAJR);
            }
        }

        if(prayerId == Prayer.SUNRISE && isJumaDay()){
            return mJuma;
        }

        if(prayerId == Prayer.JUMA){
            return mPrayers.get(Prayer.ASR);
        }

        return mPrayers.get(prayerId + 1);
    }

    public Prayer getNextPrayer() {

        boolean respectJuma = App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);
        int prayerId = getCurrentPrayer().getId();

        if(prayerId == Prayer.ISHA) {
            if(isDayEnding()){
                return mTomorrowsFajr;
            } else {
                return mPrayers.get(Prayer.FAJR);
            }
        }

        if(prayerId == Prayer.SUNRISE && respectJuma){
            return mJuma;
        }

        return mPrayers.get(prayerId + 1);
    }

    public CharSequence getAllPrayersTimes(){
        Prayer currentPrayer = getCurrentPrayer();
        Prayer nextVakat = getNextPrayer(currentPrayer.getId());

        StringBuilder time = new StringBuilder();

        time
                .append(getPrayer(Prayer.FAJR).getPrayerTimeString())
                .append(" - ")
                .append(getPrayer(Prayer.FAJR).getTitle())
                .append("\n")
                .append(getPrayer(Prayer.SUNRISE).getPrayerTimeString())
                .append(" - ")
                .append(getPrayer(Prayer.SUNRISE).getTitle())
                .append("\n");

        if(isJumaDay()) {
            time.append(mJuma.getPrayerTimeString())
                    .append(" - ")
                    .append(mJuma.getTitle());
        } else {
            time.append(getPrayer(Prayer.DHUHR).getPrayerTimeString())
                    .append(" - ")
                    .append(getPrayer(Prayer.DHUHR).getTitle());
        }

        time.append("\n")
                .append(getPrayer(Prayer.ASR).getPrayerTimeString())
                .append(" - ")
                .append(getPrayer(Prayer.ASR).getTitle())
                .append("\n")
                .append(getPrayer(Prayer.MAGHRIB).getPrayerTimeString())
                .append(" - ")
                .append(getPrayer(Prayer.MAGHRIB).getTitle())
                .append("\n")
                .append(getPrayer(Prayer.ISHA).getPrayerTimeString())
                .append(" - ")
                .append(getPrayer(Prayer.ISHA).getTitle());

        return Utils.boldNumbers(time.toString());
    }

    public CharSequence getCurrentAndNextTime(){
        Prayer currentPrayer = getCurrentPrayer();
        Prayer nextVakat = getNextPrayer(currentPrayer.getId());

        String nextDay = "";
        String prevDay = "";

        if(isDayEnding()){
            //nextDay = " (sutra)";
            nextDay = "";
        } else if (currentPrayer.getId() == Prayer.ISHA) {
            //prevDay = " (jučer)";
            prevDay = "";
        }

        String time = currentPrayer.getShortTitle()
                +prevDay
                +" "+ currentPrayer.getHrsString()
                +":"+ currentPrayer.getMinsString()+
                " | "+nextVakat.getShortTitle()
                +nextDay
                +" "+nextVakat.getHrsString()
                +":"+nextVakat.getMinsString();

        return Utils.boldNumbers(time);
    }

    public Prayer getPreviousPrayer(int prayerId) {
        if(prayerId == Prayer.FAJR)
            return mYesterdaysIsha;

        return mPrayers.get(prayerId - 1);
    }

    public Prayer getPreviousPrayerIgnoringDate(int prayerId){
        if(prayerId == Prayer.FAJR) {
            return mPrayers.get(Prayer.ISHA);
        }

        if(isJumaDay() && prayerId == Prayer.JUMA){
            return mPrayers.get(Prayer.SUNRISE);
        }

        if(isJumaDay() && prayerId == Prayer.ASR){
            return mJuma;
        }

        return mPrayers.get(prayerId - 1);
    }

    private static boolean isSummerTimeOn(int year, int month, int day){
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        return TimeZone.getDefault().inDaylightTime(new Date(calendar.getTimeInMillis()));
    }

    public void reset(){
        Log.d(TAG, "resetting schedule");
        instance = new PrayersSchedule(mContext);
    }

    public boolean isNextPrayer(int prayerId) {
        if(prayerId == Prayer.FAJR && getCurrentPrayer().getId() == Prayer.ISHA) {
            return true;
        }

        if(getCurrentPrayer().getId() == Prayer.SUNRISE && prayerId == Prayer.JUMA){
            return true;
        }

        if(getCurrentPrayer().getId() == Prayer.JUMA && prayerId == Prayer.ASR){
            return true;
        }

        if(prayerId != Prayer.JUMA){
            return getCurrentPrayer().getId() + 1 == prayerId;
        } else {
            return false;
        }
    }

    private boolean isDayEnding(){
        Prayer current = getCurrentPrayer();
        boolean stepOne = current.getId() == Prayer.ISHA;

        boolean stepTwo = current.getPrayerTime() <= Utils.getCurrentTimeSec();

        boolean stepThree = Utils.getCurrentTimeSec() <= 24 * 3600;

        return stepOne && stepTwo && stepThree;

    }

    public Prayer getPrayer(int id){

        if(id == Prayer.JUMA){
            return mJuma;
        }

        return mPrayers.get(id);
    }

    private int getSilentModeDuration(){
        int silentTimeout = (getCurrentPrayer().getPrayerTime() + getCurrentPrayer().getSoundOnMins() * 60);

        if(SilentModeManager.getInstance(mContext).isSunriseSilentModeOn()){
            Prayer sunrise = PrayersSchedule.getInstance(mContext).getPrayer(Prayer.SUNRISE);
            silentTimeout = sunrise.getPrayerTime();// + sunrise.getSoundOnMins(false) * 60;
        }

        return silentTimeout;
    }

    public String getSilentModeDurationString(){
        return FormattingUtils.getTimeStringDots(getSilentModeDuration());
    }
}
