package ba.vaktija.android;
import java.util.Calendar;
import java.util.List;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.FormattingUtils;
import ba.vaktija.android.util.HijriCalendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DateFragment extends DialogFragment {
	public static final String TAG = DateFragment.class.getSimpleName();

	public static final String EXTRA_VALUES = "EXTRA_VALUES";

	LayoutInflater mLayoutInflater;

	Handler uiHandler;
	LinearLayout rootLayout;

	App app;

	AlertDialog.Builder mDialogBuilder;

	int month = 0;
	int day = 0;
    int year = 0;
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Use the current date as the default date in the picker

		mLayoutInflater = LayoutInflater.from(getActivity());

		View view = mLayoutInflater.inflate(R.layout.fragment_date, null);
		rootLayout = (LinearLayout) view.findViewById(R.id.fragment_date_root);

		List<String> values = getArguments().getStringArrayList(EXTRA_VALUES);

		month = Integer.parseInt(values.get(1));
		day = Integer.parseInt(values.get(0));
		year = Integer.parseInt(values.get(2));

        FileLog.i(TAG, "month="+month+" day="+day+" year="+year);

		mDialogBuilder = new  AlertDialog.Builder(getActivity())

		.setNegativeButton(R.string.ok,
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});

		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1);
		calendar.set(Calendar.DAY_OF_MONTH, day);

		mDialogBuilder.setTitle(day+". "+month+". "+year+" / "+HijriCalendar.getSimpleDate(calendar));
		mDialogBuilder.setView(view);

		return mDialogBuilder.create();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FileLog.d(TAG, "onCreate");
		setHasOptionsMenu(true);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		FileLog.d(TAG, "onActivityCreated");

		uiHandler = new Handler();

		app = (App) getActivity().getApplicationContext();


		for(int i = Prayer.FAJR; i <= Prayer.ISHA; i++){
            Prayer p = PrayersSchedule.getInstance(app).getPrayerForDate(i, year, month, day);
			View view = mLayoutInflater.inflate(R.layout.fragment_date_row, null);
			TextView title = (TextView) view.findViewById(R.id.fragment_date_row_title);
			TextView time = (TextView) view.findViewById(R.id.fragment_date_row_time);

			title.setText(p.getTitle());
			time.setText(FormattingUtils.getFormattedTime(p.getRawPrayerTime() * 1000, false));

			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB){
				title.setTextColor(Color.WHITE);
				time.setTextColor(Color.WHITE);
			}

			rootLayout.addView(view);
		}
	}
}
