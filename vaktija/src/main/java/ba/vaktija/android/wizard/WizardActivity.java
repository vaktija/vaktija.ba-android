package ba.vaktija.android.wizard;

import ba.vaktija.android.R;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class WizardActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/RobotoCondensed-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
		
		setContentView(R.layout.activity_wizard);

		if(savedInstanceState == null){
			getSupportFragmentManager()
			.beginTransaction()
			.add(R.id.activity_wizard_content, LocationFragment.newInstance(false))
			.commit();
		}
	}

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
