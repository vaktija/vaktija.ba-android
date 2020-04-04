package ba.vaktija.android;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by e on 4/1/15.
 */
public class PrayersPagerAdapter extends FragmentPagerAdapter {

    List<PrayerActivityFragment> fragmentList;
    List<String> pageTitles;

    public PrayersPagerAdapter(FragmentManager fm, List<PrayerActivityFragment> fragmentList, List<String> pageTitles) {
        super(fm);
        this.pageTitles = pageTitles;
        this.fragmentList = fragmentList;
    }

    @Override
    public PrayerActivityFragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }

    @Override
    public String getPageTitle(int position) {
        return pageTitles.get(position);
    }
}
