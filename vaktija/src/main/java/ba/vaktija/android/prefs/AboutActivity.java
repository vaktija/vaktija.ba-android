package ba.vaktija.android.prefs;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;

import java.util.Calendar;
import java.util.Locale;

import ba.vaktija.android.App;
import ba.vaktija.android.BaseActivity;
import ba.vaktija.android.R;

public class AboutActivity extends BaseActivity {
    public static final String TAG = AboutActivity.class.getSimpleName();

    private RelativeLayout root;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(TAG);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        root = (RelativeLayout) findViewById(R.id.activity_about_root);

        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View customAb = LayoutInflater.from(this).inflate(R.layout.action_bar_title, null);
        TextView title = (TextView) customAb.findViewById(R.id.action_bar_title);

        title.setText(getString(R.string.about_app).toUpperCase(Locale.getDefault()));
        title.setTypeface(App.robotoCondensedRegular);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(customAb);

        TextView copyleftSign = (TextView) findViewById(R.id.activity_about_copyleftSign);
        ImageView vaktija = (ImageView) findViewById(R.id.logo);
        TextView copyleft = (TextView) findViewById(R.id.activity_about_copyleft);

        App app = (App) getApplication();

        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);

        copyleft.setText(" 2008 - " + Calendar.getInstance().get(Calendar.YEAR));

        vaktija.setImageResource(R.drawable.logo);

        copyleftSign.startAnimation(rotate);

        vaktija.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.vaktija.ba"));
                    startActivity(myIntent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        app.sendScreenView("About");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
