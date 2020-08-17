package ba.vaktija.android.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;

public class VaktijaServiceHelper {
    private static final String TAG = "VaktijaServiceHelper";

    public static void startService(Context context, Intent intent){

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        boolean wizardCompleted = preferences.getBoolean(Prefs.WIZARD_COMPLETED, false);
        boolean userQuit = preferences.getBoolean(Prefs.USER_CLOSED, false);

        if(!wizardCompleted || userQuit){
            FileLog.w(TAG, "Not starting VaktijaService, wizardCompleted="+wizardCompleted+" userQuit="+userQuit);
            return;
        }

        ContextCompat.startForegroundService(context, intent);
    }
}
