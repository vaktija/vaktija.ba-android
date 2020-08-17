package ba.vaktija.android.service;

import android.app.Notification;

import ba.vaktija.android.models.Prayer;

public interface NotifManager {

    String DEFAULT_CHANNEL = "DEFAULT_CHANNEL";
    String ALARMS_CHANNEL = "ALARMS_CHANNEL";
    String APPROACHING_CHANNEL = "APPROACHING_CHANNEL";

    int ONGOING_NOTIF = 77;
    int ALARM_NOTIF = 78;

    int NOTIF_UPDATE_INTERVAL = 10 * 1000; //10s

    void buildCountDownNotif(boolean showTicker);

    Notification getOngoingNotif(boolean showTicker, String channel);

    void cancelNotification();

    void onSilentNotifDeleted();

    void onApproachingNotifDeleted();

    void showApproachingNotification();

    void setNotificationsEnabled(boolean enabled);

    void updateNotification();

    Notification getAlarmNotif(Prayer prayer);

    void cancelAlarmNotif();

    void cancelApproachingNotif();
}
