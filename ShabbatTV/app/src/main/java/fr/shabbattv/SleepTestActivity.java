package fr.shabbattv;

import android.app.Activity;
import android.os.Bundle;

public class SleepTestActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b); SleepHelper.sleepNow(this);}
}
