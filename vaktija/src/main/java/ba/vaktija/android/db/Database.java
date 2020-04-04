package ba.vaktija.android.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;
import ba.vaktija.android.models.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by e on 5/25/15.
 */
public class Database extends SQLiteAssetHelper {

    public static final String TAG = Database.class.getSimpleName();

    private static final String DATABASE_NAME = "vaktija.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_LOCATIONS = "locations";
    public static String TABLE_SCHEDULE = "schedule";
    public static final String TABLE_SCHEDULE_DE = "schedule_de";
    public static String TABLE_OFFSET = "offset";
    public static final String TABLE_OFFSET_DE = "offset_de";

    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_LOCATION_ID = "location_id";
    public static final String COLUMN_DATUM = "datum";
    public static final String COLUMN_MONTH = "month";

    public static final String COLUMN_FAJR = "fajr";
    public static final String COLUMN_SUNRISE = "sunrise";
    public static final String COLUMN_DHUHR = "dhuhr";
    public static final String COLUMN_ASR = "asr";
    public static final String COLUMN_MAGHRIB = "maghrib";
    public static final String COLUMN_ISHA = "isha";

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        setForcedUpgrade();
    }

    public List<Location> getLocations(){
        List<Location> locations = new ArrayList<>();

        Cursor c = getReadableDatabase().query(TABLE_LOCATIONS, null, null, null, null, null, COLUMN_WEIGHT);

        while(c.moveToNext()){
            locations.add(new Location(
                    c.getString(c.getColumnIndex(COLUMN_LOCATION)),
                    c.getInt(c.getColumnIndex(COLUMN_ID))));
        }

        c.close();
        return locations;
    }

    public int[] getPrayerTimesSec(int month, int day, int locationId) {
        Log.d(TAG, "getPrayerTimeMins month=" + month + " day=" + day + " locationId=" + locationId);

        String monthStr = month+"";
        String dayStr = day+"";

        if(month < 10){
            monthStr = "0"+month;
        }

        if(day < 10){
            dayStr = "0"+day;
        }

        String datum = monthStr+"-"+dayStr;

        Log.i(TAG, "datum="+datum);

        if (locationId >= 100000)
        {
            TABLE_SCHEDULE  = TABLE_SCHEDULE_DE;
            TABLE_OFFSET = TABLE_OFFSET_DE;
        }

        Cursor prayerTimes = getReadableDatabase().query(TABLE_SCHEDULE, null, COLUMN_DATUM+"=?", new String[]{datum}, null, null, null);

        //offset and offset_de table are different -> takvim for BIH and takvim for Germany
        Cursor offset;
        if (locationId <100000)
        {
            offset = getReadableDatabase().query(TABLE_OFFSET, null, COLUMN_MONTH + "=? AND " + COLUMN_LOCATION_ID + "=?", new String[]{month + "", locationId + ""}, null, null, null);
        }
        else
        {
            offset = getReadableDatabase().query(TABLE_OFFSET, null, COLUMN_LOCATION_ID + "=?", new String[]{locationId + ""}, null, null, null);
        }

        Log.i(TAG, "prayerTimes count="+prayerTimes.getCount());
        Log.i(TAG, "offset count="+offset.getCount());

        prayerTimes.moveToFirst();

        String fajr = prayerTimes.getString(prayerTimes.getColumnIndex(COLUMN_FAJR));
        String sunrise = prayerTimes.getString(prayerTimes.getColumnIndex(COLUMN_SUNRISE));
        String dhuhr = prayerTimes.getString(prayerTimes.getColumnIndex(COLUMN_DHUHR));
        String asr = prayerTimes.getString(prayerTimes.getColumnIndex(COLUMN_ASR));
        String maghrib = prayerTimes.getString(prayerTimes.getColumnIndex(COLUMN_MAGHRIB));
        String isha = prayerTimes.getString(prayerTimes.getColumnIndex(COLUMN_ISHA));

        Log.i(TAG, "fajr="+fajr+" sunrise="+sunrise+" dhuhr="+dhuhr+" asr="+asr+" maghrib="+maghrib+" isha="+isha);

        int fajrTime = Integer.parseInt(fajr.split(":")[0]) * 60
                + Integer.parseInt(fajr.split(":")[1]);

        int sunriseTime = Integer.parseInt(sunrise.split(":")[0]) * 60
                + Integer.parseInt(sunrise.split(":")[1]);

        int dhuhrTime = Integer.parseInt(dhuhr.split(":")[0]) * 60
                + Integer.parseInt(dhuhr.split(":")[1]);

        int asrTime = Integer.parseInt(asr.split(":")[0]) * 60
                + Integer.parseInt(asr.split(":")[1]);

        int maghribTime = Integer.parseInt(maghrib.split(":")[0]) * 60
                + Integer.parseInt(maghrib.split(":")[1]);

        int ishaTime = Integer.parseInt(isha.split(":")[0]) * 60
                + Integer.parseInt(isha.split(":")[1]);

        offset.moveToFirst();

        int offsetFajr = offset.getInt(offset.getColumnIndex(COLUMN_FAJR));
        int offsetSunrise = offsetFajr;
        int offsetDhuhr = offset.getInt(offset.getColumnIndex(COLUMN_DHUHR));
        int offsetAsr = offset.getInt(offset.getColumnIndex(COLUMN_ASR));
        int offsetMaghrib = offsetAsr;
        int offsetIsha = offsetAsr;
        if (locationId >= 100000) {
            offsetSunrise = offset.getInt(offset.getColumnIndex(COLUMN_SUNRISE));
            offsetMaghrib = offset.getInt(offset.getColumnIndex(COLUMN_MAGHRIB));
            offsetIsha = offset.getInt(offset.getColumnIndex(COLUMN_ISHA));
        }

        Log.i(TAG, "offsetFajr="+offsetFajr+" offsetDhuhr="+offsetDhuhr+" offsetAsr="+offsetAsr);

        fajrTime = (fajrTime + offsetFajr) * 60;
        sunriseTime = (sunriseTime + offsetSunrise) * 60;
        dhuhrTime = (dhuhrTime + offsetDhuhr) * 60;
        asrTime = (asrTime + offsetAsr) * 60;
        maghribTime = (maghribTime + offsetMaghrib) * 60;
        ishaTime = (ishaTime + offsetIsha) * 60;

        int[] times = new int[]{fajrTime, sunriseTime, dhuhrTime, asrTime, maghribTime, ishaTime};

        Log.i(TAG, "times="+ Arrays.toString(times));

        prayerTimes.close();
        offset.close();

        return times;
    }

    public String getLocationName(int locationId){
        Cursor c = getReadableDatabase().query(TABLE_LOCATIONS, null, COLUMN_ID + "=?", new String[]{locationId+""}, null, null, null);

        c.moveToFirst();

        return c.getString(c.getColumnIndex(COLUMN_LOCATION));
    }
}
