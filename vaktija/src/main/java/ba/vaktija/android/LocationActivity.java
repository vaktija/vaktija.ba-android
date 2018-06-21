package ba.vaktija.android;

import ba.vaktija.android.wizard.LocationFragment;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class LocationActivity extends BaseActivity {
	public static final String TAG = LocationActivity.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState){
		setTheme(TAG);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_location);

        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View customAb = LayoutInflater.from(this).inflate(R.layout.action_bar_title, null);
        TextView title = (TextView) customAb.findViewById(R.id.action_bar_title);

        title.setText(R.string.location);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(customAb);

		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		if(savedInstanceState == null){
			getSupportFragmentManager()
			.beginTransaction()
			.add(R.id.activity_location_content, LocationFragment.newInstance(true))
			.commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
