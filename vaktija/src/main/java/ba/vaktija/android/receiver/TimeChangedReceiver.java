package ba.vaktija.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.service.VaktijaService;
import ba.vaktija.android.util.FileLog;

public class TimeChangedReceiver extends BroadcastReceiver {
	private static final String TAG = TimeChangedReceiver.class.getSimpleName();
	@Override
	public void onReceive(Context context, Intent intent) {
		FileLog.d(TAG, "onReceive (time has been adjusted)");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		
		boolean userClosed = prefs.getBoolean(Prefs.USER_CLOSED, false);
        FileLog.i(TAG, "userClosed: "+userClosed);
		
		if(!userClosed){
			Intent service = VaktijaService.getStartIntent(context, TAG);
			service.setAction(VaktijaService.ACTION_TIME_CHANGED);
			context.startService(service);
		} 
	}
}
