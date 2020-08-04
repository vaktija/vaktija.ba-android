package ba.vaktija.android.util;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.TypedValue;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.widget.VaktijaWidgetProvider;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();

    public static int getCurrentTimeSec() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

        int currentHours = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMins = calendar.get(Calendar.MINUTE);
        int currentSec = calendar.get(Calendar.SECOND);

        return currentHours * 3600 + currentMins * 60 + currentSec;
    }

    public static String[] concatArrays(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;
        String[] c = new String[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static void updateWidget(Context context) {
        Intent intent = new Intent(context, VaktijaWidgetProvider.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, VaktijaWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

    public static Bitmap getFontBitmap(Context context, String text, int color, float fontSizeSP) {
        int fontSizePX = convertDiptoPix(context, fontSizeSP);
        int pad = (fontSizePX / 9);
        Paint paint = new Paint();
        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/RobotoCondensed-Light.ttf");
        paint.setAntiAlias(true);
        paint.setTypeface(typeface);
        paint.setColor(color);
        paint.setTextSize(fontSizePX);
        paint.setFakeBoldText(true);

        int textWidth = (int) (paint.measureText(text) + pad * 2);
        int height = (int) (fontSizePX / 0.75);
        Bitmap bitmap = Bitmap.createBitmap(textWidth, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        float xOriginal = pad;
        canvas.drawText(text, xOriginal, fontSizePX, paint);
        return bitmap;
    }

    public static int convertDiptoPix(Context context, float dip) {
        int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
        return value;
    }

    public static CharSequence getTimeTillNext(Prayer currentPrayer, int seconds, boolean ceil) {

        String time = Prayer.getNextVakatTitle(currentPrayer.getId()) + " je za " + FormattingUtils.getTimeString(seconds, ceil);

        return boldNumbers(time);
//        return time;
    }

    public static CharSequence boldNumbers(String text) {
        Pattern pattern = Pattern.compile("\\d");

        SpannableStringBuilder sbOut = new SpannableStringBuilder(text);

        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            sbOut.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return sbOut;
    }


}
