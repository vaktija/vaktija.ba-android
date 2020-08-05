package ba.vaktija.android;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;

import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;

@SuppressLint("LongLogTag")
public class SystemSettingsHelperActivity extends BaseActivity implements View.OnClickListener {
    public static final String TAG = "SystemSettingsHelperActivity";

    private ImageView dozeModeCheckIcon;
    private ImageView dndModeCheckIcon;
    private ImageView overlayCheckIcon;

    private TextView dozeModeDescriptionLabel;
    private TextView dndModeDescriptionLabel;
    private TextView overlayDescriptionLabel;

    private Button dozeModeSettingsButton;
    private Button dndModeSettingsButton;
    private Button overlaySettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(TAG);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_system_settings_helper);

        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View customAb = LayoutInflater.from(this).inflate(R.layout.action_bar_title, null);
        TextView title = customAb.findViewById(R.id.action_bar_title);

        title.setText(getString(R.string.system_settings));
        title.setTypeface(App.robotoCondensedRegular);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.very_light_gray)));
        ab.setCustomView(customAb);

        dozeModeCheckIcon = findViewById(R.id.doze_mode_check_icon);
        dndModeCheckIcon = findViewById(R.id.dnd_mode_check_icon);
        overlayCheckIcon = findViewById(R.id.overlay_check_icon);

        dozeModeDescriptionLabel = findViewById(R.id.doze_mode_description_label);
        dndModeDescriptionLabel = findViewById(R.id.dnd_mode_description_label);
        overlayDescriptionLabel = findViewById(R.id.overlay_description_label);

        dozeModeSettingsButton = findViewById(R.id.doze_settings_button);
        dndModeSettingsButton = findViewById(R.id.dnd_settings_button);
        overlaySettingsButton = findViewById(R.id.overlay_settings_button);

        findViewById(R.id.close_button).setOnClickListener(this);

        dozeModeSettingsButton.setOnClickListener(this);
        dndModeSettingsButton.setOnClickListener(this);
        overlaySettingsButton.setOnClickListener(this);

        setResult(RESULT_CANCELED);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        checkDozeModeState();
        checkDndModeStatus();
        checkOverlayPermission();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.close_button:
                setResult(RESULT_OK);
                finish();
                break;

            case R.id.doze_settings_button:

                try {
                    Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }

                break;

            case R.id.dnd_settings_button:

                try {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }

                break;

            case R.id.overlay_settings_button:

                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }

                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkDozeModeState(){
        Log.d(TAG, "checkDozeModeState");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        String packageName = getPackageName();
        FileLog.i(TAG, "isIgnoringBatteryOptimizations: " + pm.isIgnoringBatteryOptimizations(packageName));

        boolean dozeEnabled = !pm.isIgnoringBatteryOptimizations(packageName);

        if(dozeEnabled) {
            dozeModeCheckIcon.setImageResource(R.drawable.ic_check_circle);
            dozeModeSettingsButton.setVisibility(View.VISIBLE);
        } else {
            dozeModeCheckIcon.setImageResource(R.drawable.ic_check_circle_green);
            dozeModeSettingsButton.setVisibility(View.GONE);
            dozeModeDescriptionLabel.setText(R.string.label_doze_mode_set);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkDndModeStatus() {

        if (App.app.notificationManager.isNotificationPolicyAccessGranted()) {
            App.prefs.edit().putBoolean(Prefs.DND_GRANTED, true).apply();
            dndModeCheckIcon.setImageResource(R.drawable.ic_check_circle_green);
            dndModeSettingsButton.setVisibility(View.GONE);
            dndModeDescriptionLabel.setText(R.string.label_dnd_mode_set);
        } else {
            dndModeCheckIcon.setImageResource(R.drawable.ic_check_circle);
            dndModeSettingsButton.setVisibility(View.VISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkOverlayPermission(){
        Log.d(TAG, "checkOverlayPermission");

        boolean canDrawOverlays = Settings.canDrawOverlays(this);

        if(!canDrawOverlays) {
            overlayCheckIcon.setImageResource(R.drawable.ic_check_circle);
            overlaySettingsButton.setVisibility(View.VISIBLE);
        } else {
            overlayCheckIcon.setImageResource(R.drawable.ic_check_circle_green);
            overlaySettingsButton.setVisibility(View.GONE);
            overlayDescriptionLabel.setText(R.string.label_overlay_set);
        }
    }
}
