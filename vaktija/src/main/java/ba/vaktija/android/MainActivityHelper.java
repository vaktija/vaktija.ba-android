package ba.vaktija.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import ba.vaktija.android.util.FileLog;

public class MainActivityHelper extends Activity {

    public static final String TAG = MainActivityHelper.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileLog.d(TAG, "onCreate");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
//		overridePendingTransition(0, 0);
    }
}
