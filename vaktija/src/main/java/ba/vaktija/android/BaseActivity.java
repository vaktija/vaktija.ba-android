package ba.vaktija.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;

import ba.vaktija.android.util.FileLog;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class BaseActivity extends AppCompatActivity {

    private SharedPreferences prefs;

	protected void setTheme(String tag){
		FileLog.d("BaseActivity", "[setTheme] tag=" + tag);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		int themeResId = R.style.AppThemeLightNoOverlay;

		if(tag.equals(MainActivity.TAG)){
			themeResId = R.style.AppThemeLight;
			//FileLog.d("BaseActivity", "using ...Main as theme");
		}
		
		setTheme(themeResId);
	}

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
