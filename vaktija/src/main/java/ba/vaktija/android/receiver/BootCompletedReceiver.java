package ba.vaktija.android.receiver;

import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootCompletedReceiver extends BroadcastReceiver {
	public static final String TAG = BootCompletedReceiver.class.getSimpleName();
	@Override
	public void onReceive(Context context, Intent intent) {
		FileLog.d(TAG, "onReceive (boot completed)");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        Utils.updateWidget(context);
		
		boolean userClosed = prefs.getBoolean(Prefs.USER_CLOSED, false);

		if(!userClosed){
			Intent service = VaktijaService.getStartIntent(context, TAG);
			service.setAction(VaktijaService.ACTION_BOOT_COMPLETED);
			context.startService(service);
		} else {
            FileLog.w(TAG, "Vaktija closed by user, not starting");
		}
	}
}
