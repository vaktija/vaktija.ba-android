package ba.vaktija.android.service;

import android.app.NotificationChannel;
import android.content.Context;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import ba.vaktija.android.R;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;

public class NotifManagerV26 extends LegacyNotifManager {
    public static final String TAG = "NotifManagerV26";

    private static NotifManagerV26 instance;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static NotifManagerV26 getInstance(Context context){
        if(instance == null){
            instance = new NotifManagerV26(context);
        } else {
            instance.mPrayer = PrayersSchedule.getInstance(context).getCurrentPrayer();
            instance.approaching = PrayersSchedule.getInstance(context).isNextPrayerApproaching();
        }

        return instance;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotifManagerV26(Context context){
        super(context);
        Log.d(TAG, "<init>");
        createNotifChannels();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotifChannels(){
        Log.d(TAG, "createNotifChannels");

        String desc = context.getString(R.string.notif_default_channel_desc);
        NotificationChannel defaultChannel = new NotificationChannel(DEFAULT_CHANNEL, desc, IMPORTANCE_LOW);
        defaultChannel.enableVibration(true);
        defaultChannel.setShowBadge(false);

        notificationManager.createNotificationChannel(defaultChannel);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();

        Uri soundUri = Uri.parse(Defaults.getDefaultTone(context, false));

        String approachingDesc = context.getString(R.string.notif_approaching_channel_desc);
        NotificationChannel approachingChannel = new NotificationChannel(APPROACHING_CHANNEL, approachingDesc, IMPORTANCE_HIGH);
        approachingChannel.enableVibration(true);
        approachingChannel.setShowBadge(false);
        approachingChannel.setSound(soundUri, audioAttributes);
        notificationManager.createNotificationChannel(approachingChannel);

        String alarmsChannelDesc = context.getString(R.string.notif_alarms_channel_desc);

        NotificationChannel alarmChannel = new NotificationChannel(ALARMS_CHANNEL, alarmsChannelDesc, IMPORTANCE_HIGH);
        alarmChannel.enableVibration(true);
        alarmChannel.setShowBadge(false);
        notificationManager.createNotificationChannel(alarmChannel);
    }
}
