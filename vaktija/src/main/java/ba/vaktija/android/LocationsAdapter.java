package ba.vaktija.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import ba.vaktija.android.models.Location;

public class LocationsAdapter extends BaseAdapter {

    private static int TYPE_SECTION_HEADER = 0;
    private static int TYPE_NORMAL = 1;

    Context mContext;
    List<Location> locations;

    HashMap<Integer, Location> sections = new HashMap<>();

    public LocationsAdapter(Context context, List<Location> locations) {
        mContext = context;
        this.locations = locations;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return sections.containsKey(position) ? TYPE_SECTION_HEADER : TYPE_NORMAL;
    }

    @Override
    public boolean isEnabled(int position) {
        return !sections.containsKey(position);
    }

    public void setSections(HashMap<Integer, Location> sections) {
        this.sections = sections;
    }

    @Override
    public int getCount() {
        return locations.size();
    }

    @Override
    public Location getItem(int position) {
        return locations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (getItemViewType(position) == TYPE_NORMAL) {

            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_location, null);
            }

            ((TextView) convertView).setText(locations.get(position).name);
        } else {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_location_section_header, null);
            }

            ((TextView) convertView).setText(sections.get(position).name);
        }

        return convertView;
    }

    public int getPositionForLocationId(int locationId) {
        for (int i = 0; i < locations.size(); i++) {
            if (locations.get(i).id == locationId)
                return i;
        }

        return 0;
    }
}
