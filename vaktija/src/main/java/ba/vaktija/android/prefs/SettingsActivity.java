/*******************************************************************************
 * Copyright 2013 Gabriele Mariotti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package ba.vaktija.android.prefs;

import ba.vaktija.android.App;
import ba.vaktija.android.BaseActivity;
import ba.vaktija.android.R;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {
	
	public static final String TAG = SettingsActivity.class.getSimpleName();
    public static final String EXTRA_FIRST_VISIBLE_ITEM = "EXTRA_FIRST_VISIBLE_ITEM";
    public static final String EXTRA_ITEM_TOP = "EXTRA_ITEM_TOP";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(TAG);

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);

		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        View customAb = LayoutInflater.from(this).inflate(R.layout.action_bar_title, null);
        TextView title = (TextView) customAb.findViewById(R.id.action_bar_title);

        title.setText("POSTAVKE");
        title.setTypeface(App.robotoCondensedRegular);

        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(false);
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayShowCustomEnabled(true);
        ab.setCustomView(customAb);

        SettingsFragment sf = new SettingsFragment();

        if(getIntent().hasExtra(EXTRA_FIRST_VISIBLE_ITEM)){
            int scrollPosition = getIntent().getIntExtra(EXTRA_FIRST_VISIBLE_ITEM, 0);
            int itemTop = getIntent().getIntExtra(EXTRA_ITEM_TOP, 0);
            Log.i(TAG, "scrollPosition=" + scrollPosition);
            Log.i(TAG, "itemTop=" + itemTop);

            sf = SettingsFragment.newInstance(scrollPosition, itemTop);
        }

		getSupportFragmentManager()
		.beginTransaction()
		.replace(R.id.activity_settings_content, sf).commit();
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
