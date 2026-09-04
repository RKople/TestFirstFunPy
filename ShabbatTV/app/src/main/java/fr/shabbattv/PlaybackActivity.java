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
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        boolean interactiveBefore = pm != null && pm.isInteractive();

        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE,
                    "ShabbatTV:WakeScreen"
            );
            wakeLock.acquire(60_000L);
        }

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

        LinearLayout root = new LinearLayout(this);
        root.setGravity(Gravity.CENTER);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        TextView big = new TextView(this);
        big.setText("TEST SHABBAT TV v0.2");
        big.setTextColor(Color.WHITE);
        big.setTextSize(42f);
        big.setGravity(Gravity.CENTER);
        root.addView(big, new LinearLayout.LayoutParams(-1, -2));

        TextView small = new TextView(this);
        small.setText("L'alarme s'est déclenchée.\nÉcran interactif avant tentative : " + (interactiveBefore ? "OUI" : "NON") +
                "\nWakeLock écran forcé pendant 60 secondes.");
        small.setTextColor(Color.LTGRAY);
        small.setTextSize(22f);
        small.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 28, 0, 0);
        root.addView(small, lp);

        setContentView(root);
    }

    @Override
    protected void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
}
