package ba.vaktija.android.wizard;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ba.vaktija.android.App;
import ba.vaktija.android.LocationsAdapter;
import ba.vaktija.android.R;
import ba.vaktija.android.models.Location;
import ba.vaktija.android.models.PrayersSchedule;
import ba.vaktija.android.prefs.Defaults;
import ba.vaktija.android.prefs.Prefs;
import ba.vaktija.android.util.FileLog;
import ba.vaktija.android.util.Utils;

public class LocationFragment extends Fragment implements OnClickListener {
    public static final String TAG = LocationFragment.class.getSimpleName();

    private static final String EXTRA_NO_WIZARD = "EXTRA_NO_WIZARD";

    ListView mListView;
    Button next;
    Button prev;
    AppCompatActivity mActivity;
    App app;

    SharedPreferences prefs;
    boolean noWizard;
    private int locationId = Defaults.LOCATION_ID;
    private String locationName = Defaults.LOCATION_NAME;

    public static Fragment newInstance(boolean noWizard) {
        Bundle args = new Bundle();
        args.putBoolean(EXTRA_NO_WIZARD, noWizard);
        Fragment f = new LocationFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        FileLog.d(TAG, "onAttach");

        app = (App) activity.getApplicationContext();
        mActivity = (AppCompatActivity) activity;
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
        View view = inflater.inflate(R.layout.fragment_location, container, false);
        mListView = (ListView) view.findViewById(R.id.location_list);
        next = (Button) view.findViewById(R.id.wizard_next);
        prev = (Button) view.findViewById(R.id.wizard_prev);

        return view;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FileLog.d(TAG, "onActivityCreated");

        prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mActivity.getSupportActionBar().setTitle("Lokacija");

        prev.setVisibility(View.INVISIBLE);
        next.setOnClickListener(this);

        noWizard = getArguments().getBoolean(EXTRA_NO_WIZARD);

        if (noWizard) {
            getView().findViewById(R.id.wizard_bottom_nav).setVisibility(View.GONE);
        }

        List<Location> locations = app.db.getLocations();
        List<Location> locationsList = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            if (i == 0) {
                locationsList.add(new Location("Bosna i Hercegovina", -1));
            }

            if (i == 107) {
                locationsList.add(new Location("Sandžak", -1));
            }

            locationsList.add(locations.get(i));
        }

        HashMap<Integer, Location> sections = new HashMap<>();

        sections.put(0, new Location("Bosna i Hercegovina", -1));
        sections.put(108, new Location("Sandžak", -1));

        final LocationsAdapter adapter = new LocationsAdapter(
                mActivity,
                locationsList);

        adapter.setSections(sections);

        mListView.setAdapter(adapter);

        locationId = prefs.getInt(Prefs.SELECTED_LOCATION_ID, Defaults.LOCATION_ID); // 77 - Sarajevo
        String selectedName = locationId + "";

        FileLog.i(TAG, "locationId: " + locationId + " adapter.count: " + adapter.getCount());
        FileLog.i(TAG, "selectedName: " + selectedName);

        //		mListView.setSelection(adapter.getIdPosition(locationId));
        mListView.setItemChecked(adapter.getPositionForLocationId(locationId), true);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick position=" + position);

                Location loc = (Location) mListView.getItemAtPosition(position);

                locationId = loc.id;
                locationName = loc.name;

                saveSelectedLocation();
            }
        });

//		mListView.smoothScrollToPosition(locationId);
        mListView.setSelectionFromTop(adapter.getPositionForLocationId(locationId), getHalfScreenHeight());
    }

    private int getHalfScreenHeight() {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        return (display.getHeight() - 500) / 2;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.wizard_next) {
            saveSelectedLocation();
        }
    }

    private void saveSelectedLocation() {
        FileLog.i(TAG, "locationId=" + locationId);

        prefs.edit()
                .putInt(Prefs.SELECTED_LOCATION_ID, locationId)
                .putString(Prefs.LOCATION_NAME, locationName)
                .commit();

        app.sendEvent("Odabrana lokacija", locationName);

        PrayersSchedule.getInstance(app).reset();

        Utils.updateWidget(getActivity());

        if (noWizard) {
            mActivity.finish();
        } else {
            next.post(new Runnable() {
                @Override
                public void run() {
                    replaceFragments();
                }
            });
        }
    }

    private void replaceFragments() {

        FragmentTransaction ft = mActivity.getSupportFragmentManager()
                .beginTransaction();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            ft.setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
        }

        ft.replace(R.id.activity_wizard_content, new VakatTweaksFragment());
        ft.commit();
    }
}
