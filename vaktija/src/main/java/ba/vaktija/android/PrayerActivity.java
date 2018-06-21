package ba.vaktija.android;

import com.astuetz.PagerSlidingTabStrip;

import ba.vaktija.android.models.Prayer;
import ba.vaktija.android.prefs.Prefs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PrayerActivity extends BaseActivity implements OnNavigationListener {
    public static final String TAG = PrayerActivity.class.getSimpleName();

    private static final String EXTRA_PRAYER = "EXTRA_PRAYER";

    private Prayer prayer;
    private ViewPager pager;

    public static Intent getLaunchIntent(Context context, Prayer prayer) {

        Intent i = new Intent(context, PrayerActivity.class);
        i.putExtra(EXTRA_PRAYER, prayer);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(TAG);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "[onCreate]");

        setContentView(R.layout.activity_prayer);
        pager = (ViewPager) findViewById(R.id.activity_prayer_view_pager);
        pager.setVisibility(View.GONE);

        prayer = getIntent().getParcelableExtra(EXTRA_PRAYER);

        boolean listNav = prayer.getId() == Prayer.DHUHR;
        listNav |= prayer.getId() == Prayer.JUMA;

        listNav &= App.prefs.getBoolean(Prefs.SEPARATE_JUMA_SETTINGS, true);

        if(listNav){
            setupActionBarLisNav();
        } else {

            View customAb = LayoutInflater.from(this).inflate(R.layout.action_bar_title, null);
            TextView title = (TextView) customAb.findViewById(R.id.action_bar_title);

            title.setText(prayer.getTitle().toUpperCase(Locale.getDefault()));
            title.setTypeface(App.robotoCondensedRegular);

            ActionBar ab = getSupportActionBar();
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayShowTitleEnabled(false);
            ab.setDisplayShowCustomEnabled(true);
            ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.very_light_gray)));
            ab.setCustomView(customAb);

            Fragment f = getSupportFragmentManager().findFragmentById(R.id.content);

            if(f == null)
                f = PrayerActivityFragment.newInstance(prayer.getId(), false);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content, f)
                    .commit();
        }
    }

    private void setupActionBarLisNav(){

        View customAb = LayoutInflater.from(this).inflate(R.layout.action_bar_tabs, null);
        pager.setVisibility(View.VISIBLE);

        List<PrayerActivityFragment> prayerFragments = new ArrayList<>();
        prayerFragments.add(PrayerActivityFragment.newInstance(prayer.getId(), false));
        prayerFragments.add(PrayerActivityFragment.newInstance(prayer.getId(), true));

        List<String> pageTitles = new ArrayList<>();
        pageTitles.add("Podne");
        pageTitles.add("Džuma");

        PrayersPagerAdapter pp = new PrayersPagerAdapter(getSupportFragmentManager(), prayerFragments, pageTitles);

        pager.setAdapter(pp);

        // Bind the tabs to the ViewPager
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) customAb.findViewById(R.id.action_bar_tabs);
        tabs.setViewPager(pager);
        tabs.setUnderlineColor(Color.TRANSPARENT);
        tabs.setTextSize(getResources().getDimensionPixelSize(R.dimen.custom_ab_title_size));
        tabs.setIndicatorHeight(getResources().getDimensionPixelSize(R.dimen.action_bar_tabs_indicator_h));
        tabs.setTypeface(App.robotoCondensedRegular, Typeface.NORMAL);

        /*
        mActionBarTitle = (TextView) customAb.findViewById(R.id.custom_action_bar_title);
        mActionBarSubtitle = (TextView) customAb.findViewById(R.id.custom_action_bar_subtitle);

        mActionBarTitle.setTypeface(App.robotoCondensedRegular);
        mActionBarSubtitle.setTypeface(App.robotoCondensedRegular);
        int color = Color.parseColor("#9d9d00");
        mActionBarTitle.setTextColor(getResources().getColor(android.R.color.darker_gray));
        mActionBarSubtitle.setTextColor(getResources().getColor(android.R.color.darker_gray));
        */

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.very_light_gray)));
        ab.setCustomView(customAb);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        boolean isFriday = (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);

        if(isFriday) {
            pager.setCurrentItem(1);
        }

        /*
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        ArrayAdapter<String> optionsAdapter = new ArrayAdapter<>(
                this,
                R.layout.ab_dropdown_nav_dark,
                new String[]{"Podne", "Džuma"});

        getSupportActionBar().setListNavigationCallbacks(optionsAdapter, this);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        boolean isFriday = (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY);

        if(isFriday){
            getSupportActionBar().setSelectedNavigationItem(1);
        }
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        Log.d(TAG, "onNavigationItemSelected itemPosition="+itemPosition);

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.content);

        if(f != null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(f)
                    .commit();
        }

        if(itemPosition == 0){
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content, PrayerActivityFragment.newInstance(prayer.getId(), false))
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content, PrayerActivityFragment.newInstance(prayer.getId(), true))
                    .commit();
        }
        return true;
    }
}
