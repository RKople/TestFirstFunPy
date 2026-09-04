package fr.shabbattv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;

public class ArmedActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private Runnable launchTask;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.prepareWindow(this);
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );
        View black = new View(this);
        black.setBackgroundColor(Color.BLACK);
        setContentView(black);

        long target = getIntent().getLongExtra("target_at", System.currentTimeMillis());
        String movie = getIntent().getStringExtra("movie");
        int volume = getIntent().getIntExtra("volume", AppState.defaultVolume(this));
        String scheduleId = getIntent().getStringExtra("schedule_id");
        boolean sleepWhenDone = getIntent().getBooleanExtra("sleep_when_done", true);

        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        if (pm != null) {
            try {
                long hold = Math.max(90_000L, Math.min(20 * 60_000L, target - System.currentTimeMillis() + 3 * 60_000L));
                wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                        "ShabbatTV:Armed"
                );
                wakeLock.acquire(hold);
            } catch (Throwable t) {
                LogStore.add(this,"Erreur","WakeLock pré-réveil : "+t);
            }
        }

        long delay = Math.max(0L, target - System.currentTimeMillis());
        LogStore.add(this,"Réveil","TV réveillée en écran noir · lancement dans "+Math.max(0,delay/1000)+" s");
        launchTask = () -> {
            try {
                LogStore.add(this,"Planning","Heure cible atteinte · lancement du film");
                PlaybackLauncher.launch(this,movie,volume,scheduleId,sleepWhenDone);
            } catch (Throwable t) {
                LogStore.add(this,"Erreur","Lancement depuis pré-réveil : "+t);
            }
            finish();
        };
        handler.postDelayed(launchTask, delay);
    }

    @Override protected void onDestroy() {
        if (launchTask != null) handler.removeCallbacks(launchTask);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
}
