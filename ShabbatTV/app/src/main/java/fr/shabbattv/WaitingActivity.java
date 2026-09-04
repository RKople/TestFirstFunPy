package fr.shabbattv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

/**
 * Visible waiting screen shown after the proven Philips wake path succeeds.
 * It keeps the TV awake and launches the already-validated Plex player at target time.
 */
public class WaitingActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView countdown, clock, title;
    private PowerManager.WakeLock wakeLock;
    private long targetAt;
    private String movieJson, scheduleId;
    private int volume;
    private boolean sleepWhenDone;
    private boolean launched;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long left = targetAt - now;
            if (left <= 0L) {
                launchFilm();
                return;
            }
            long totalSeconds = (left + 999L) / 1000L;
            long mins = totalSeconds / 60L;
            long secs = totalSeconds % 60L;
            countdown.setText(String.format(java.util.Locale.FRANCE, "%02d:%02d", mins, secs));
            clock.setText("Début prévu à " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(targetAt)));

            // Very small position drift every 30 s: subtle on screen and OLED-friendly.
            int step = (int)((now / 30_000L) % 4L);
            float dx = Ui.dp(WaitingActivity.this, step == 0 ? -3 : step == 2 ? 3 : 0);
            float dy = Ui.dp(WaitingActivity.this, step == 1 ? -2 : step == 3 ? 2 : 0);
            countdown.setTranslationX(dx);
            countdown.setTranslationY(dy);
            handler.postDelayed(this, 1000L);
        }
    };

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
        readIntent(getIntent());
        buildUi();
        acquireLock();
        LogStore.add(this, "Attente", "Écran d’attente affiché · film dans " + Math.max(0L, (targetAt-System.currentTimeMillis())/1000L) + " s");
        handler.post(ticker);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readIntent(intent);
        launched = false;
        if (title != null) updateMovieTitle();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        LogStore.add(this, "Attente", "Réveil de secours reçu · compte à rebours resynchronisé");
    }

    private void readIntent(Intent i) {
        targetAt = i.getLongExtra("target_at", System.currentTimeMillis());
        movieJson = i.getStringExtra("movie");
        scheduleId = i.getStringExtra("schedule_id");
        volume = i.getIntExtra("volume", AppState.defaultVolume(this));
        sleepWhenDone = i.getBooleanExtra("sleep_when_done", true);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 70), Ui.dp(this, 40), Ui.dp(this, 70), Ui.dp(this, 40));
        root.setBackgroundColor(Color.rgb(5, 7, 10));

        TextView eyebrow = Ui.eyebrow(this, "Shabbat TV");
        eyebrow.setGravity(Gravity.CENTER);
        root.addView(eyebrow);

        title = Ui.title(this, "Film");
        title.setGravity(Gravity.CENTER);
        title.setTextSize(Ui.compact(this) ? 28 : 38);
        title.setMaxLines(2);
        updateMovieTitle();
        root.addView(title, Ui.lp(-1, -2, this, 14));

        TextView intro = Ui.muted(this, "Le film commence dans");
        intro.setGravity(Gravity.CENTER);
        intro.setTextSize(Ui.compact(this) ? 15 : 18);
        root.addView(intro, Ui.lp(-1, -2, this, 20));

        countdown = new TextView(this);
        countdown.setText("--:--");
        countdown.setTextColor(Ui.TEXT);
        countdown.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        countdown.setTextSize(Ui.compact(this) ? 54 : 74);
        countdown.setGravity(Gravity.CENTER);
        root.addView(countdown, Ui.lp(-1, -2, this, 3));

        clock = Ui.muted(this, "");
        clock.setGravity(Gravity.CENTER);
        root.addView(clock, Ui.lp(-1, -2, this, 7));

        TextView note = Ui.muted(this, "La TV est prête. Aucune action n’est nécessaire.");
        note.setGravity(Gravity.CENTER);
        note.setTextSize(Ui.compact(this) ? 12 : 14);
        root.addView(note, Ui.lp(-1, -2, this, 24));

        setContentView(root);
    }

    private void updateMovieTitle() {
        String value = "Film";
        try {
            JSONObject movie = movieJson == null ? null : new JSONObject(movieJson);
            if (movie != null) value = movie.optString("title", "Film");
        } catch (Exception ignored) {}
        title.setText(value);
    }

    private void acquireLock() {
        try {
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            if (pm != null) {
                long hold = Math.max(2 * 60_000L, Math.min(20 * 60_000L, targetAt - System.currentTimeMillis() + 3 * 60_000L));
                wakeLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "ShabbatTV:Waiting"
                );
                wakeLock.acquire(hold);
            }
        } catch (Throwable t) {
            LogStore.add(this, "Erreur", "WakeLock attente : " + t.getClass().getSimpleName());
        }
    }

    private void launchFilm() {
        if (launched) return;
        launched = true;
        handler.removeCallbacks(ticker);
        LogStore.add(this, "Planning", "Compte à rebours terminé · lancement du film");
        try {
            PlaybackLauncher.launch(this, movieJson, volume, scheduleId, sleepWhenDone);
        } catch (Throwable t) {
            LogStore.add(this, "Erreur", "Lancement après compte à rebours : " + t);
        }
        finish();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
