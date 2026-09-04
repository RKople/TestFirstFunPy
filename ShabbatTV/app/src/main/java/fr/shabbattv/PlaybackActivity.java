package fr.shabbattv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlaybackActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        big.setText("TEST SHABBAT TV RÉUSSI ✅");
        big.setTextColor(Color.WHITE);
        big.setTextSize(42f);
        big.setGravity(Gravity.CENTER);
        root.addView(big, new LinearLayout.LayoutParams(-1, -2));

        TextView small = new TextView(this);
        small.setText("La TV a réveillé l'application automatiquement.\nProchaine étape : lecture programmée des films.");
        small.setTextColor(Color.LTGRAY);
        small.setTextSize(22f);
        small.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 28, 0, 0);
        root.addView(small, lp);

        setContentView(root);
    }
}
