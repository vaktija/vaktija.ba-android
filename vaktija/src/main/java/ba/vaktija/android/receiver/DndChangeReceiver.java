package ba.vaktija.android.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;

import ba.vaktija.android.App;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.SilentModeManager;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.service.VaktijaServiceHelper;
import ba.vaktija.android.util.FileLog;

public class DndChangeReceiver extends BroadcastReceiver {
	public static final String TAG = DndChangeReceiver.class.getSimpleName();

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public void onReceive(Context context, Intent intent) {
		FileLog.d(TAG, "onReceive (DnD settings changed)");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		boolean userClosed = prefs.getBoolean(Prefs.USER_CLOSED, false);

		if(userClosed){
            FileLog.d(TAG, "Vaktija is closed by user, not starting service");
			return;
		}

		boolean commingFromSilent = prefs.getBoolean(Prefs.COMMING_FROM_SILENT, false);
		boolean goingSilent = prefs.getBoolean(Prefs.GOING_SILENT, false);

        FileLog.i(TAG, "commingFromSilent: " + commingFromSilent);
        FileLog.i(TAG, "goingSilent: "+goingSilent);

		Intent i = VaktijaService.getStartIntent(context, TAG);
		i.setAction(VaktijaService.ACTION_UPDATE);

		boolean dndActive = App.app.notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_NONE;

		FileLog.i(TAG, "dndActive: "+dndActive);

		if(!dndActive && !commingFromSilent){

			prefs.edit()
					.putBoolean(Prefs.SILENT_BY_APP, false)
					.putBoolean(Prefs.COMMING_FROM_SILENT, false)
					.putBoolean(Prefs.GOING_SILENT, false)
					.apply();

		} else {

			prefs.edit()
					.putBoolean(Prefs.SILENT_BY_APP, goingSilent)
					.putBoolean(Prefs.COMMING_FROM_SILENT, false)
					.apply();
		}

		if(!dndActive && SilentModeManager.getInstance(context).silentShoudBeActive()){

			prefs.edit()
					.putBoolean(Prefs.SILENT_DISABLED_BY_USER, true)
					.apply();

			i.setAction(VaktijaService.ACTION_VOLUME_CHANGED);
		}

		if(!dndActive ){
			prefs.edit().putBoolean(Prefs.SILENT_DISABLED_BY_USER, false).apply();
		}

		VaktijaServiceHelper.startService(context, i);
	}
}
