package ba.vaktija.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Locale;

import ba.vaktija.android.MainActivity;
import ba.vaktija.android.R;
import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.Utils;

/**
 * Created by e on 1/29/15.
 */
public class VaktijaWidgetProvider extends AppWidgetProvider {
    public static final String TAG = VaktijaWidgetProvider.class.getSimpleName();

    private static final int MAIN_ACTIVITY_ID = 12345;

    SharedPreferences mPrefs;

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        final int n = appWidgetIds.length;

        if (n == 0)
            return;

        PrayersSchedule schedule = PrayersSchedule.getInstance(context);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        Prayer fajr = schedule.getPrayer(Prayer.FAJR);
        Prayer sunrise = schedule.getPrayer(Prayer.SUNRISE);
        Prayer dhuhr = schedule.getPrayer(Prayer.DHUHR);
        Prayer asr = schedule.getPrayer(Prayer.ASR);
        Prayer maghrib = schedule.getPrayer(Prayer.MAGHRIB);
        Prayer ishaa = schedule.getPrayer(Prayer.ISHA);

        if (schedule.isJumaDay()) {
            dhuhr = schedule.getPrayer(Prayer.JUMA);
        }

        String city = mPrefs.getString(Prefs.LOCATION_NAME, Defaults.LOCATION_NAME);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                MAIN_ACTIVITY_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        int textColor = context.getResources().getColor(R.color.text_color);

        int themeColor = context.getResources().getColor(R.color.theme_gray);

        boolean isKeyguard = false;


        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i = 0; i < n; i++) {
            int appWidgetId = appWidgetIds[i];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

                Bundle myOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);

                int category = myOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1);
                isKeyguard = category == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;

                if (isKeyguard) {
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout_keyguard);

                    views.setTextViewText(R.id.widget_layout_keyguard_info, Utils.getTimeTillNext(
                            PrayersSchedule.getInstance(context).getCurrentPrayer(),
                            PrayersSchedule.getInstance(context).getTimeTillNextPrayer(),
                            true));

                    views.setTextViewText(R.id.widget_layout_keyguard_prevNext,
                            PrayersSchedule.getInstance(context).getCurrentAndNextTime());

                    appWidgetManager.updateAppWidget(appWidgetId, views);

                    continue;
                }
            }

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);


            // Get the layout for the App Widget and attach an on-click listener
            // to the button

            views.setInt(R.id.widget_layout_root, "setBackgroundResource", R.drawable.widget_bg_light);

            views.setImageViewBitmap(R.id.widget_layout_city, Utils.getFontBitmap(
                    context,
                    city.toUpperCase(Locale.getDefault()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_app, Utils.getFontBitmap(
                    context,
                    "vaktija.ba",
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

//            views.setTextViewText(R.id.widget_layout_fajr_time, FormattingUtils.getTimeStringDots(fajr.getPrayerTime()));

            views.setViewVisibility(R.id.widget_layout_fajr_alarm, fajr.isAlarmOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_sunrise_alarm, sunrise.isAlarmOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_dhuhr_alarm, dhuhr.isAlarmOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_asr_alarm, asr.isAlarmOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_maghrib_alarm, maghrib.isAlarmOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_ishaa_alarm, ishaa.isAlarmOn() ? View.VISIBLE : View.GONE);

            views.setViewVisibility(R.id.widget_layout_fajr_notif, fajr.isNotifOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_sunrise_notif, sunrise.isNotifOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_dhuhr_notif, dhuhr.isNotifOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_asr_notif, asr.isNotifOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_maghrib_notif, maghrib.isNotifOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_ishaa_notif, ishaa.isNotifOn() ? View.VISIBLE : View.GONE);

            views.setViewVisibility(R.id.widget_layout_fajr_silent, fajr.isSilentOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_sunrise_silent, sunrise.isSilentOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_dhuhr_silent, dhuhr.isSilentOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_asr_silent, asr.isSilentOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_maghrib_silent, maghrib.isSilentOn() ? View.VISIBLE : View.GONE);
            views.setViewVisibility(R.id.widget_layout_ishaa_silent, ishaa.isSilentOn() ? View.VISIBLE : View.GONE);

            views.setImageViewBitmap(R.id.widget_layout_fajr_time, Utils.getFontBitmap(
                    context,
                    FormattingUtils.getTimeStringDots(fajr.getPrayerTime()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_sunrise_time, Utils.getFontBitmap(
                    context,
                    FormattingUtils.getTimeStringDots(sunrise.getPrayerTime()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_dhuhr_time, Utils.getFontBitmap(
                    context,
                    FormattingUtils.getTimeStringDots(dhuhr.getPrayerTime()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_asr_time, Utils.getFontBitmap(
                    context,
                    FormattingUtils.getTimeStringDots(asr.getPrayerTime()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_maghrib_time, Utils.getFontBitmap(
                    context,
                    FormattingUtils.getTimeStringDots(maghrib.getPrayerTime()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_ishaa_time, Utils.getFontBitmap(
                    context,
                    FormattingUtils.getTimeStringDots(ishaa.getPrayerTime()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_fajr_title, Utils.getFontBitmap(
                    context,
                    fajr.getTitle().toUpperCase(Locale.getDefault()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_sunrise_title, Utils.getFontBitmap(
                    context,
                    "I. SUNCA",//sunrise.getTitle(false),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_dhuhr_title, Utils.getFontBitmap(
                    context,
                    dhuhr.getTitle().toUpperCase(Locale.getDefault()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_asr_title, Utils.getFontBitmap(
                    context,
                    asr.getTitle().toUpperCase(Locale.getDefault()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_maghrib_title, Utils.getFontBitmap(
                    context,
                    maghrib.getTitle().toUpperCase(Locale.getDefault()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setImageViewBitmap(R.id.widget_layout_ishaa_title, Utils.getFontBitmap(
                    context,
                    ishaa.getTitle().toUpperCase(Locale.getDefault()),
                    textColor,
                    context.getResources().getDimension(R.dimen.widget_font_size)));

            views.setInt(R.id.widget_layout_fajr_current, "setBackgroundColor", Color.TRANSPARENT);
            views.setInt(R.id.widget_layout_sunrise_current, "setBackgroundColor", Color.TRANSPARENT);
            views.setInt(R.id.widget_layout_dhuhr_current, "setBackgroundColor", Color.TRANSPARENT);
            views.setInt(R.id.widget_layout_asr_current, "setBackgroundColor", Color.TRANSPARENT);
            views.setInt(R.id.widget_layout_maghrib_current, "setBackgroundColor", Color.TRANSPARENT);
            views.setInt(R.id.widget_layout_ishaa_current, "setBackgroundColor", Color.TRANSPARENT);

            switch (PrayersSchedule.getInstance(context).getCurrentPrayer().getId()) {
                case Prayer.FAJR:
                    views.setInt(R.id.widget_layout_fajr_current, "setBackgroundColor", themeColor);
                    break;
                case Prayer.SUNRISE:
                    views.setInt(R.id.widget_layout_sunrise_current, "setBackgroundColor", themeColor);
                    break;
                case Prayer.DHUHR:
                    views.setInt(R.id.widget_layout_dhuhr_current, "setBackgroundColor", themeColor);
                    break;
                case Prayer.ASR:
                    views.setInt(R.id.widget_layout_asr_current, "setBackgroundColor", themeColor);
                    break;
                case Prayer.MAGHRIB:
                    views.setInt(R.id.widget_layout_maghrib_current, "setBackgroundColor", themeColor);
                    break;
                case Prayer.ISHA:
                    views.setInt(R.id.widget_layout_ishaa_current, "setBackgroundColor", themeColor);
                    break;
            }

            views.setOnClickPendingIntent(R.id.widget_layout_root, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
