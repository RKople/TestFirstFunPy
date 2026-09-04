package fr.shabbattv;

import android.app.Activity;
import android.graphics.Color;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.reflect.Method;

public final class SleepHelper {
    private SleepHelper() {}

    public static String sleepNow(Activity a) {
        StringBuilder log = new StringBuilder();
        try {
            PowerManager pm = (PowerManager)a.getSystemService(Activity.POWER_SERVICE);
            if (pm != null) {
                try {
                    Method m = PowerManager.class.getDeclaredMethod("goToSleep", long.class);
                    m.setAccessible(true);
                    m.invoke(pm, android.os.SystemClock.uptimeMillis());
                    log.append("goToSleep accepté; ");
                } catch (Throwable t) { log.append("goToSleep refusé; "); }
            }
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"sh","-c","input keyevent 223"});
                log.append("keyevent sleep exit=").append(p.waitFor()).append("; ");
            } catch (Throwable t) { log.append("keyevent refusé; "); }
        } catch (Throwable ignored) {}

        try {
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            TextView v = new TextView(a);
            v.setBackgroundColor(Color.BLACK); v.setTextColor(Color.DKGRAY); v.setGravity(Gravity.CENTER); v.setText("Shabbat TV — fin de séance");
            a.setContentView(v);
        } catch (Throwable ignored) {}
        AppState.prefs(a).edit().putString("last_sleep_diag", log.toString()).putLong("last_sleep_at", System.currentTimeMillis()).apply();
        LogStore.add(a,"Veille","Demande de mise en veille : "+log);
        return log.toString();
    }
}
