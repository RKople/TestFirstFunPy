package fr.shabbattv;

import android.app.Activity;
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

        LinearLayout root = Ui.page(this);
        root.setGravity(Gravity.CENTER);

        LinearLayout card = Ui.card(this);
        TextView eyebrow = Ui.eyebrow(this,"Test de réveil");
        eyebrow.setGravity(Gravity.CENTER);
        card.addView(eyebrow);
        TextView title = Ui.title(this,"TV réveillée ✓");
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Ui.GOOD);
        card.addView(title,Ui.lp(-1,-2,this,5));
        TextView text = Ui.muted(this,
                "Le test s’est déclenché. Si cet écran est apparu sans télécommande, le réveil automatique a fonctionné.\n\n" +
                "Pour le détail technique, rouvre ensuite Shabbat TV → Tests.");
        text.setGravity(Gravity.CENTER);
        card.addView(text,Ui.lp(-1,-2,this,7));

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2);
        int maxWidthDp = Ui.compact(this) ? Math.max(520, Ui.widthDp(this)-120) : 820;
        cp.width = Ui.dp(this, Math.min(maxWidthDp, Ui.widthDp(this)-Math.max(56,Math.round(Ui.widthDp(this)*.10f))));
        root.addView(card,cp);
        setContentView(root);
    }
}
