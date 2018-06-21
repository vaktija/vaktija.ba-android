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

public class LockChangeReceiver extends BroadcastReceiver {
	public static final String TAG = LockChangeReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		FileLog.d(TAG, "onReceive (phone unlocked)");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		boolean userClosed = prefs.getBoolean(Prefs.USER_CLOSED, false);
        FileLog.i(TAG, "userClosed: "+userClosed);

        Utils.updateWidget(context);

		if(!userClosed){
			Intent service = VaktijaService.getStartIntent(context, TAG);
			service.setAction(VaktijaService.ACTION_LOCK_CHANGED);
			context.startService(service);
		} 
	}
}
