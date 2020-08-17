package ba.vaktija.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;

import ba.vaktija.android.models.Events;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.SilentModeManager;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.service.VaktijaServiceHelper;
import ba.vaktija.android.util.FileLog;
import de.greenrobot.event.EventBus;

public class RingerChangeReceiver extends BroadcastReceiver {
    public static final String TAG = RingerChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        FileLog.d(TAG, "onReceive (ringer settings changed)");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean userClosed = prefs.getBoolean(Prefs.USER_CLOSED, false);

        if (userClosed) {
            FileLog.d(TAG, "Vaktija is closed by user, not starting service");
            return;
        }

        EventBus.getDefault().post(new Events.RingerModeChanged());

        int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL);

        FileLog.i(TAG, "ringerMode: " + ringerMode);

        boolean commingFromSilent = prefs.getBoolean(Prefs.COMMING_FROM_SILENT, false);
        boolean goingSilent = prefs.getBoolean(Prefs.GOING_SILENT, false);

        FileLog.i(TAG, "commingFromSilent: " + commingFromSilent);
        FileLog.i(TAG, "goingSilent: " + goingSilent);

        Intent i = VaktijaService.getStartIntent(context, TAG);
        i.setAction(VaktijaService.ACTION_UPDATE);

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL && !commingFromSilent) {

            prefs.edit()
                    .putBoolean(Prefs.SILENT_BY_APP, false)
                    .putBoolean(Prefs.COMMING_FROM_SILENT, false)
                    .putBoolean(Prefs.GOING_SILENT, false)
                    .commit();

        } else {

            prefs.edit()
                    .putBoolean(Prefs.SILENT_BY_APP, goingSilent)
                    .putBoolean(Prefs.COMMING_FROM_SILENT, false)
                    .commit();
        }

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL
                && SilentModeManager.getInstance(context).silentShoudBeActive()) {

            prefs.edit()
                    .putBoolean(Prefs.SILENT_DISABLED_BY_USER, true)
                    .commit();

            i.setAction(VaktijaService.ACTION_VOLUME_CHANGED);
        }

        if (ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            prefs.edit().putBoolean(Prefs.SILENT_DISABLED_BY_USER, false).commit();
        }

        VaktijaServiceHelper.startService(context, i);
    }
}
