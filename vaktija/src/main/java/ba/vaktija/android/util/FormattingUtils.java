package ba.vaktija.android.util;

import android.annotation.SuppressLint;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.util.Locale;

import ba.vaktija.android.models.Prayer;

public class FormattingUtils {

    @SuppressLint("DefaultLocale")
    public static String getFormattedTime(long milliseconds, boolean withSeconds) {

        int seconds = (int) (milliseconds / 1000) % 60;
        int minutes = (int) ((milliseconds / (1000 * 60)) % 60);
        int hours = (int) ((milliseconds / (1000 * 60 * 60)) % 24);

        String result = String.format("%02d:%02d", hours, minutes);

        if (withSeconds)
            result = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        return result;

		/*
		SimpleDateFormat formatter = new SimpleDateFormat(withSeconds ? "HH:mm:ss" : "HH:mm", Locale.getDefault());
		formatter.setTimeZone(TimeZone.getDefault());

		return formatter.format(new Date(milliseconds));
		*/

    }

    public static CharSequence colorText(String string, int color) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(string);
        ssb.setSpan(new ForegroundColorSpan(color), 0, string.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return ssb;
    }

    public static String getTimeString(int totalSecs) {
        // MyLog.d("FormattingUtils", "getTimeString totalSecs="+totalSecs+" ceil="+ceil);
        //		int seconds = (int) (milliseconds / 1000) % 60 ;

        int seconds = totalSecs % 60;
        int minutes = (totalSecs / 60) % 60;
        int hours = (totalSecs / 3600) % 24;

        if (seconds > 0 && minutes > 0) {
            minutes += 1;
        }

        String result = String.format(Locale.getDefault(), "%dh %dm", hours, minutes);

        if (minutes == 0) {
            result = String.format(Locale.getDefault(), "%dh", hours);
        }

        if (hours == 0) {
            result = String.format(Locale.getDefault(), "%dm", minutes);
        }

        if (hours == 0 && minutes == 0) {
            result = String.format(Locale.getDefault(), "0m %ds", seconds);
        }

        return result;
    }

    public static class Case {
        public static final int AKUZATIV = 4;
    }

    public static String getTimeStringDots(int totalSecs) {
        // MyLog.d("FormattingUtils", "getTimeString totalSecs="+totalSecs+" ceil="+ceil);
        //		int seconds = (int) (milliseconds / 1000) % 60 ;

        int seconds = totalSecs % 60;
        int minutes = (totalSecs / 60) % 60;
        int hours = (totalSecs / 3600) % 24;

        String result = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);

        return result;
    }

    public static String getVakatAnnouncement(String title) {
        String gender = "Nastupio";
        String announcement = " je " + title;
        if (title.charAt(title.length() - 1) == 'a')
            gender = "Nastupila";
        if (title.charAt(title.length() - 1) == 'e')
            gender = "Nastupilo";

        return gender + announcement;
    }

    static String getSuffixForNumber(int number, boolean maleGender) {
        String numString = String.valueOf(number);
        String suffix = "i";
        String suffixFemale = "i";

        if (number > 10 && number < 15)
            return maleGender ? "i" : "a";

        switch (numString.charAt(numString.length() - 1)) {
            case '0':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                suffix = "i";
                suffixFemale = "a";
                break;
            case '1':
                suffix = "";
                suffixFemale = "a";
                break;
            case '2':
            case '3':
            case '4':
                suffix = "a";
                suffixFemale = "e";
                break;
            default:
                break;
        }

        return maleGender ? suffix : suffixFemale;
    }

    public static String getCaseTitle(int vakatId, int caseId) {
        switch (vakatId) {
            case Prayer.FAJR:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "zoru";
                }

            case Prayer.SUNRISE:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "izlazak sunca";
                }

            case Prayer.DHUHR:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "podne";
                }

            case Prayer.ASR:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "ikindiju";
                }

            case Prayer.MAGHRIB:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "akšam";
                }

            case Prayer.ISHA:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "jaciju";
                }

            case Prayer.JUMA:
                switch (caseId) {
                    case Case.AKUZATIV:
                        return "džumu";
                }
        }

        return "";
    }
}
