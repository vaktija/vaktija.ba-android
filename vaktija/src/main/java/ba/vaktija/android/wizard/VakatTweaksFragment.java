package ba.vaktija.android.wizard;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import ba.vaktija.android.App;
import ba.vaktija.android.MainActivity;
import ba.vaktija.android.R;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;

public class VakatTweaksFragment extends Fragment implements OnClickListener, OnCheckedChangeListener {
    public static final String TAG = VakatTweaksFragment.class.getSimpleName();

    Button next;
    Button prev;
    AppCompatActivity mActivity;
    RadioGroup radioGroup;
    App app;
    CheckBox mSeparateSettingsJuma;
    TextView mSeparateSettingsJumaExpl;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        FileLog.d(TAG, "onAttach");

        mActivity = (AppCompatActivity) activity;
        app = (App) activity.getApplicationContext();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileLog.d(TAG, "onCreate");
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        FileLog.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.fragment_prayer_tweaks, container, false);
        next = (Button) view.findViewById(R.id.wizard_next);
        prev = (Button) view.findViewById(R.id.wizard_prev);
        radioGroup = (RadioGroup) view.findViewById(R.id.fragment_vakat_tweaks_radioGroup);
        mSeparateSettingsJuma = (CheckBox) view.findViewById(R.id.fragment_vakat_tweaks_jumaChbox);
        mSeparateSettingsJumaExpl = (TextView) view.findViewById(R.id.fragment_vakat_tweaks_juma_explTxtView);

        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FileLog.d(TAG, "onActivityCreated");

        mActivity.getSupportActionBar().setTitle("Podne namaz");

        App.prefs.edit().putString(Prefs.DHUHR_TIME_COUNTING, "1").commit();

        next.setText(R.string.done);

        next.setOnClickListener(this);
        prev.setOnClickListener(this);
        radioGroup.setOnCheckedChangeListener(this);
        mSeparateSettingsJuma.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                App.prefs.edit().putBoolean(Prefs.SEPARATE_JUMA_SETTINGS, isChecked).commit();

                mSeparateSettingsJumaExpl.setText(
                        isChecked
                                ? R.string.prefs_separateJumaSettings_enabled
                                : R.string.prefs_separateJumaSettings_disabled);
            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.wizard_next) {

            App.prefs.edit()
                    .putBoolean(Prefs.WIZARD_COMPLETED, true)
                    .commit();

            startActivity(new Intent(mActivity, MainActivity.class));
            getActivity().finish();
        }

        if (v.getId() == R.id.wizard_prev) {
            FragmentTransaction ft = mActivity.getSupportFragmentManager()
                    .beginTransaction();

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                ft.setCustomAnimations(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
            }

            ft.replace(R.id.activity_wizard_content, LocationFragment.newInstance(false));
            ft.commit();
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        FileLog.d(TAG, "onCheckedChanged");

        if (checkedId == R.id.radioNormalized) {
            FileLog.i(TAG, "radioNormalized");
            App.prefs.edit().putString(Prefs.DHUHR_TIME_COUNTING, "1").commit();
        } else {
            FileLog.i(TAG, "radioReal");
            App.prefs.edit().putString(Prefs.DHUHR_TIME_COUNTING, "0").commit();
        }
    }
}
