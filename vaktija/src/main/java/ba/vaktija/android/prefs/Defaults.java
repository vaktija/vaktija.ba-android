package ba.vaktija.android.prefs;

import android.content.Context;

import ba.vaktija.android.R;
import ba.vaktija.android.models.Prayer;

public class Defaults {

    public static final int LOCATION_ID = 107;
    public static final String LOCATION_NAME = "Sarajevo";
    public static final boolean STATUSBAR_NOTIFICATION = true;
    public static final boolean ALL_PRAYERS_IN_NOTIF_DEFAULT = false;
    public static String NOTIF_TONE_TITLE = "Default (Beep)";
    public static String ALARM_TONE_TITLE = "Default (Beep Beep)";

    public static String getDefaultTone(Context context, boolean alarmTone){
        return "android.resource://"+context.getPackageName()+"/"+(alarmTone ? R.raw.long_beep : R.raw.short_beep);
    }

    //alarm notif vakat silent (max values)
    public static int getMaxValue(int prayerId, String field){

        switch (prayerId){

            case Prayer.FAJR: //70 70 zora 60
                if(field.equals(Prayer.FIELD_ALARM))
                    return 60;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 60;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 30;

            case Prayer.SUNRISE: //60 60 izlazak 20
                if(field.equals(Prayer.FIELD_ALARM))
                    return 90;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 90;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 60;

            case Prayer.DHUHR: //60 60 podne 60
                if(field.equals(Prayer.FIELD_ALARM))
                    return 60;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 60;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 60;

            case Prayer.JUMA:
                if(field.equals(Prayer.FIELD_ALARM))
                    return 60;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 60;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 60;

            case Prayer.ASR: //60 60 ikindija 60
                if(field.equals(Prayer.FIELD_ALARM))
                    return 60;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 60;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 60;

            case Prayer.MAGHRIB: ////60 60 aksam 30
                if(field.equals(Prayer.FIELD_ALARM))
                    return 60;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 60;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 30;

            case Prayer.ISHA: //50 50 jacija 60
                if(field.equals(Prayer.FIELD_ALARM))
                    return 50;
                if(field.equals(Prayer.FIELD_NOTIF))
                    return 50;
                if(field.equals(Prayer.FIELD_SILENT))
                    return 60;

            default:
                return 20;
        }
    }

    public static boolean getBooleanDefault(int prayerId, String field){

        switch (prayerId) {
            case Prayer.FAJR:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return false;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return false;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return false;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;

            case Prayer.SUNRISE:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return false;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;

            case Prayer.DHUHR:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return false;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;

            case Prayer.JUMA:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return false;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;

            case Prayer.ASR:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return false;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;

            case Prayer.MAGHRIB:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return false;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;

            case Prayer.ISHA:
                if(field.equals(Prayer.FIELD_SKIP_NEXT_ALARM))
                    return false;
                if(field.equals(Prayer.FIELD_ALARM_ON))
                    return false;
                if(field.equals(Prayer.FIELD_SILENT_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_SOUND_ON))
                    return true;
                if(field.equals(Prayer.FIELD_NOTIF_VIBRO_ON))
                    return true;
                if(field.equals(Prayer.FIELD_SILENT_VIBRO_OFF))
                    return false;
        }

        return false;
    }

    public static int getIntDefault(int prayerId, String field){
        switch (prayerId) {
            //alarm notif   vakat   silent (default values)
            //45    35      zora    30
            case Prayer.FAJR:
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 45;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 15;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return 30;

            case Prayer.SUNRISE: //30 15 izlazak 20
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 45;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 35;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return -30;

            case Prayer.DHUHR: //30 15 podne 25
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 30;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 15;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return 25;

            case Prayer.JUMA:
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 30;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 15;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return 50;

            case Prayer.ASR: //30 15 ikindija 20
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 30;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 15;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return 20;

            case Prayer.MAGHRIB: //30 15 aksam 15
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 30;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 15;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return 15;

            case Prayer.ISHA: //30 15 jacija 30
                if(field.equals(Prayer.FIELD_ALARM_ON_MINS))
                    return 30;
                if(field.equals(Prayer.FIELD_NOTIF_ON_MINS))
                    return 15;
                if(field.equals(Prayer.FIELD_SILENT_TIMEOUT))
                    return 30;
        }

        return 0;
    }
}
