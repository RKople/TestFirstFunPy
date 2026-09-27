package fr.shabbattv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.PlayerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** The only waiting mode: real, local, silent video playback. No power or wake commands. */
public class ShabbatModeActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PlayerView videoView;
    private ExoPlayer keeper;
    private MediaSession session;
    private LinearLayout overlay;
    private TextView heading, title, countdown, note;
    private boolean foreground, launching, restartPending, readyLogged;
    private long openedAt, lastProgressAt, lastPosition = -1L, heartbeatAt;
    private int failures;
    private String countdownId = "";
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!foreground || launching || isFinishing()) return;
            if (!AppState.isShabbatArmed(ShabbatModeActivity.this)) { finish(); return; }
            checkVideo();
            if (isFinishing()) return;
            updateSchedule();
            if (!launching && !isFinishing()) {
                handler.removeCallbacks(this);
                handler.postDelayed(this, 500L);
            }
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Ui.prepareWindow(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        openedAt = getIntent().getBooleanExtra("returning", false) ? 0L : SystemClock.elapsedRealtime();
        FrameLayout stage = new FrameLayout(this); stage.setBackgroundColor(Color.BLACK); stage.setKeepScreenOn(true);
        videoView = new PlayerView(this);
        videoView.setBackgroundColor(Color.BLACK); videoView.setShutterBackgroundColor(Color.BLACK);
        videoView.setUseController(false); videoView.setKeepScreenOn(true);
        stage.addView(videoView, new FrameLayout.LayoutParams(-1, -1));
        overlay = new LinearLayout(this); overlay.setOrientation(LinearLayout.VERTICAL);
        // Keep the actual video surface visible even underneath the countdown.
        overlay.setGravity(Gravity.CENTER); overlay.setBackgroundColor(Color.TRANSPARENT);
        overlay.setPadding(Ui.dp(this, 48), Ui.dp(this, 24), Ui.dp(this, 48), Ui.dp(this, 24));
        heading = text(16, false); title = text(Ui.compact(this) ? 30 : 40, true);
        countdown = text(Ui.compact(this) ? 58 : 78, true); note = text(15, false);
        overlay.addView(heading); overlay.addView(title, Ui.lp(-1, -2, this, 12));
        overlay.addView(countdown, Ui.lp(-1, -2, this, 14)); overlay.addView(note, Ui.lp(-1, -2, this, 12));
        stage.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        setContentView(stage);
    }
    private TextView text(int size, boolean bold) {
        TextView v = new TextView(this); v.setGravity(Gravity.CENTER); v.setTextSize(size);
        v.setTextColor(Color.WHITE); v.setMaxLines(3);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        launching = false; openedAt = intent.getBooleanExtra("returning", false) ? 0L : SystemClock.elapsedRealtime();
    }
    @Override protected void onResume() {
        super.onResume();
        if (!AppState.isShabbatArmed(this)) { finish(); return; }
        foreground = true; launching = false; failures = 0;
        Ui.prepareWindow(this); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // A system overlay may pause without stopping us. Do not retain a cancelled recovery.
        if (restartPending || (keeper != null && keeper.getPlayerError() != null)) releaseVideo();
        restartPending = false;
        startVideo(); handler.removeCallbacks(tick); handler.post(tick);
    }
    private void startVideo() {
        if (!foreground || launching || isFinishing() || keeper != null) return;
        restartPending = false; readyLogged = false; lastPosition = -1L;
        lastProgressAt = SystemClock.elapsedRealtime();
        try {
            keeper = new ExoPlayer.Builder(this).build();
            keeper.setWakeMode(C.WAKE_MODE_LOCAL);
            keeper.setVolume(0f); keeper.setRepeatMode(Player.REPEAT_MODE_ONE);
            videoView.setPlayer(keeper);
            session = new MediaSession.Builder(this, keeper).build();
            keeper.addListener(new Player.Listener() {
                @Override public void onRenderedFirstFrame() {
                    if (!readyLogged && foreground && !launching) {
                        readyLogged = true;
                        LogStore.add(ShabbatModeActivity.this, "Vidéo noire", "Première image rendue · lecture locale silencieuse en boucle");
                    }
                }
                @Override public void onPlayerError(PlaybackException error) { restartVideo(error.getErrorCodeName()); }
            });
            keeper.setMediaItem(MediaItem.fromUri(BlackPlaybackAsset.uri(this)));
            keeper.prepare(); keeper.play();
        } catch (Exception e) { restartVideo(e.getClass().getSimpleName()); }
    }
    private void checkVideo() {
        if (restartPending) return;
        if (keeper == null) { startVideo(); return; }
        long now = SystemClock.elapsedRealtime(), position = keeper.getCurrentPosition();
        if (keeper.isPlaying() && position != lastPosition) { lastPosition = position; lastProgressAt = now; }
        if (now - lastProgressAt > 30_000L) { restartVideo("Aucune progression vidéo pendant 30 secondes"); return; }
        if (!keeper.getPlayWhenReady() && keeper.getPlayerError() == null) keeper.play();
        if (now - heartbeatAt >= 30 * 60_000L) {
            heartbeatAt = now;
            LogStore.add(this, "Contrôle", "Mode au premier plan · vidéo noire en lecture : " + keeper.isPlaying());
        }
    }
    private void restartVideo(String reason) {
        if (!foreground || launching || isFinishing() || restartPending) return;
        restartPending = true;
        LogStore.add(this, "Erreur", "Vidéo noire : " + reason);
        if (++failures > 3) {
            stopWithError("La vidéo noire ne reste pas en lecture. Le mode a été arrêté ; aucun maintien par simple écran statique n’est utilisé.");
            return;
        }
        handler.postDelayed(() -> {
            if (!foreground || launching || isFinishing()) return;
            releaseVideo(); restartPending = false; startVideo();
        }, 2_000L);
    }
    private JSONObject nextSchedule() {
        JSONArray all = AppState.pendingSchedules(this);
        long now = System.currentTimeMillis();
        for (int i = 0; i < all.length(); i++) {
            JSONObject s = all.optJSONObject(i); if (s == null) continue;
            if (s.optString("id", "").isEmpty()) {
                stopWithError("Une séance enregistrée est invalide. Vérifie et recrée le planning avant de relancer le mode.");
                return null;
            }
            if (SchedulePolicy.decide(now, s.optLong("when")) == SchedulePolicy.Decision.MISSED) {
                AppState.removeSchedule(this, s.optString("id"));
                LogStore.add(this, "Planning", "Séance dépassée, non relancée : " + s.optString("title", "Film"));
            } else return s;
        }
        return null;
    }
    private void updateSchedule() {
        JSONObject next = nextSchedule();
        if (isFinishing()) return;
        long now = System.currentTimeMillis();
        if (next != null && SchedulePolicy.decide(now, next.optLong("when")) == SchedulePolicy.Decision.START) {
            launch(next); return;
        }
        long left = next == null ? Long.MAX_VALUE : next.optLong("when") - now;
        if (next != null && left <= SchedulePolicy.COUNTDOWN_MS) {
            overlay.setVisibility(View.VISIBLE);
            heading.setText("LE FILM COMMENCE DANS"); title.setText(next.optString("title", "Film"));
            long seconds = Math.max(0L, (left + 999L) / 1000L);
            countdown.setText(String.format(Locale.FRANCE, "%02d:%02d", seconds / 60L, seconds % 60L));
            note.setText("Début à " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(next.optLong("when")))
                + " · Volume " + AppState.FILM_VOLUME_PERCENT + " %");
            int step = (int)((now / 30_000L) % 4L);
            overlay.setTranslationX(Ui.dp(this, step == 0 ? -3 : step == 2 ? 3 : 0));
            overlay.setTranslationY(Ui.dp(this, step == 1 ? -2 : step == 3 ? 2 : 0));
            String id = next.optString("id");
            if (!id.equals(countdownId)) {
                countdownId = id; LogStore.add(this, "Planning", "Compte à rebours : " + next.optString("title"));
            }
        } else if (openedAt > 0L && SystemClock.elapsedRealtime() - openedAt < 7_000L) {
            overlay.setVisibility(View.VISIBLE); heading.setText("MODE SHABBAT");
            title.setText("La vidéo noire va prendre le relais"); countdown.setText("");
            note.setText("Laisse la TV allumée et l’application ouverte. Retour = arrêter le mode.");
        } else {
            overlay.setVisibility(View.INVISIBLE); overlay.setTranslationX(0f); overlay.setTranslationY(0f);
            countdownId = "";
        }
    }
    private void launch(JSONObject s) {
        if (launching) return;
        String id = s.optString("id", ""), movie = s.optString("movie", "");
        String expected = s.optString("serverId", "");
        String current = AppState.prefs(this).getString("plex_server_machine_id", "");
        if (movie.isEmpty() || (!expected.isEmpty() && !expected.equals(current))) {
            AppState.removeSchedule(this, id);
            LogStore.add(this, "Erreur", "Séance ignorée : film absent ou serveur Plex différent · " + s.optString("title"));
            return;
        }
        launching = true; handler.removeCallbacksAndMessages(null);
        overlay.setVisibility(View.INVISIBLE); releaseVideo();
        LogStore.add(this, "Planning", "Heure atteinte : " + s.optString("title") + " · retard de déclenchement " + Math.max(0L, System.currentTimeMillis() - s.optLong("when")) + " ms");
        if (!PlaybackLauncher.launch(this, movie, id)) {
            AppState.removeSchedule(this, id); launching = false;
            startVideo(); // The single tick loop below schedules the next check.
        }
    }
    @Override public void onBackPressed() {
        new AlertDialog.Builder(this).setTitle("Arrêter le mode Shabbat ?")
            .setMessage("La programmation restera enregistrée, mais aucune séance ne démarrera tant que le mode n’est pas relancé.")
            .setNegativeButton("Continuer", null).setPositiveButton("Arrêter", (dialog, which) -> {
                AppState.setShabbatArmed(this, false); LogStore.add(this, "Mode Shabbat", "Arrêt manuel"); finish();
            }).show();
    }
    private void stopWithError(String message) {
        AppState.prefs(this).edit().putString("mode_error", message).apply();
        AppState.setShabbatArmed(this, false); LogStore.add(this, "Mode Shabbat", message); finish();
    }
    @Override protected void onPause() {
        foreground = false; handler.removeCallbacksAndMessages(null); super.onPause();
    }
    @Override protected void onStop() {
        releaseVideo(); restartPending = false;
        if (!launching && !isChangingConfigurations() && !isFinishing() && AppState.isShabbatArmed(this))
            stopWithError("Le mode a été interrompu : Shabbat TV n’est plus au premier plan. Vérifie la veille, le minuteur et l’économiseur de la TV.");
        super.onStop();
    }
    private void releaseVideo() {
        if (session != null) { session.release(); session = null; }
        if (videoView != null) videoView.setPlayer(null);
        if (keeper != null) { keeper.release(); keeper = null; }
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); releaseVideo(); super.onDestroy(); }
}
