package fr.shabbattv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlaybackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSharedPreferences("wake_diag", MODE_PRIVATE)
                .edit()
                .putLong("result_activity_at", System.currentTimeMillis())
                .commit();

        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            try {
                PowerManager.WakeLock wl = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                        "ShabbatTV:ResultWake"
                );
                wl.acquire(60_000L);
            } catch (Throwable ignored) {}
        }

        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        TextView big = new TextView(this);
        big.setText("SHABBAT TV v0.4 — WAKE TEST");
        big.setTextColor(Color.WHITE);
        big.setTextSize(38f);
        big.setGravity(Gravity.CENTER);
        root.addView(big, new LinearLayout.LayoutParams(-1, -2));

        TextView small = new TextView(this);
        small.setText("Le test s'est déclenché.\nSi tu vois cet écran sans avoir utilisé la télécommande, une méthode de réveil a fonctionné.\nSinon, rallume manuellement puis rouvre Shabbat TV pour lire le diagnostic détaillé.");
        small.setTextColor(Color.LTGRAY);
        small.setTextSize(21f);
        small.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 28, 0, 0);
        root.addView(small, lp);

        setContentView(root);
    }
}
