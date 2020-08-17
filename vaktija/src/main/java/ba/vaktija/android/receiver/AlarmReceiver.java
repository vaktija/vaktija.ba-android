package ba.vaktija.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import ba.vaktija.android.AlarmActivity;
import ba.vaktija.android.App;
import ba.vaktija.android.service.OngoingAlarmService;
import ba.vaktija.android.service.VaktijaServiceHelper;
import ba.vaktija.android.util.FileLog;

public class AlarmReceiver extends BroadcastReceiver {
	private static final String TAG = "AlarmReceiver";

	public static final String ACTION_SHOW_ALARM = "AlarmReceiver.ACTION_SHOW_ALARM";
	public static final String ACTION_DISMISS_ALARM = "AlarmReceiver.ACTION_DISMISS_ALARM";

	public static final String EXTRA_PRAYER_ID = "EXTRA_PRAYER_ID";

	@Override
	public void onReceive(Context context, Intent intent) {
		FileLog.d(TAG, "onReceive");

		String action = intent.getAction();

		if(TextUtils.isEmpty(action)) {
			FileLog.w(TAG, "AlarmReceiver requires action to be set");
			return;
		}

		FileLog.d(TAG, "action="+action);

		if(action.equals(ACTION_DISMISS_ALARM)) {
			Intent alarmServiceIntent = OngoingAlarmService.getStopAlarmIntent(context);
			VaktijaServiceHelper.startService(context, alarmServiceIntent);
			return;
		}

		if(action.startsWith(ACTION_SHOW_ALARM)) {
			showAlarm(context, intent);
		}
	}

	private void showAlarm(Context context, Intent intent) {
		int prayerId = intent.getIntExtra(EXTRA_PRAYER_ID, -1);

		if (prayerId == -1) {
			return;
		}

		if (Build.VERSION.SDK_INT >= 29) {
			FileLog.d(TAG, "Running on Android > 29, starting foreground service...");
			Intent alarmServiceIntent = OngoingAlarmService.getStartAlarmIntent(context, prayerId);
			context.startForegroundService(alarmServiceIntent);
		} else {
			Intent alarmActivity = new Intent(context, AlarmActivity.class);
			alarmActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			alarmActivity.setAction(AlarmActivity.LAUNCH_ALARM + "_" + prayerId);
			alarmActivity.putExtra(AlarmActivity.EXTRA_PRAYER_ID, prayerId);
			App.app.startActivity(alarmActivity);
		}
	}
}
