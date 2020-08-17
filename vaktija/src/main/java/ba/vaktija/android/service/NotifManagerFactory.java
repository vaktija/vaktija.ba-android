package ba.vaktija.android.service;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class NotifManagerFactory {

    public static NotifManager getNotifManager(Context context){
        Log.d("NotifManagerFactory", "getNotifManager");

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            Log.d("NotifManagerFactory", "NotifManagerV24");
            return LegacyNotifManager.getInstance(context);
        }

        Log.d("NotifManagerFactory", "NotifManagerV26");
        return NotifManagerV26.getInstance(context);
    }
}
