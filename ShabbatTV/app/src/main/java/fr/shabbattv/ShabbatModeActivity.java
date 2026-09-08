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
import android.view.KeyEvent;
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
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * v1.10 long-duration mode.
 *
 * v1.9 proved that the active engine survives and launches the film on time, but Philips' own
 * "Ambilight TV" screensaver could still cover the app after ~7 minutes. v1.10 therefore keeps
 * a REAL silent Media3 video playback session running behind the black UI for the entire armed
 * wait/countdown period. The pixels remain black, but Philips/Android sees active video playback
 * rather than an idle static Activity.
 */
public class ShabbatModeActivity extends Activity {
    private static final long COUNTDOWN_WINDOW_MS = 10 * 60_000L;
    private static final long INTRO_MS = 7_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock cpuLock;
    private FrameLayout stage;
    private PlayerView blackVideoView;
    private ExoPlayer blackKeeper;
    private LinearLayout root;
    private TextView eyebrow, title, countdown, clock, note;
    private long openedAt;
    private String shownScheduleId = "";
    private boolean launching = false;
    private boolean introLogged = false;
    private boolean keeperReadyLogged = false;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            ensureKeeperPlayback();
            updateState();
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
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );
        getWindow().getDecorView().setKeepScreenOn(true);
        openedAt = System.currentTimeMillis();
        buildUi();
        acquireCpuLock();
        ensureKeeperPlayback();

        if (!AppState.isShabbatArmed(this)) AppState.setShabbatArmed(this, true);
        LogStore.add(this, "Mode Shabbat", "Mode v1.10 armé · Android actif · vidéo noire silencieuse anti-économiseur");
        handler.post(tick);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openedAt = System.currentTimeMillis();
        launching = false;
        introLogged = false;
        ensureKeeperPlayback();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.prepareWindow(this);
        getWindow().getDecorView().setKeepScreenOn(true);
        launching = false;
        ensureKeeperPlayback();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void buildUi() {
        stage = new FrameLayout(this);
        stage.setBackgroundColor(Color.BLACK);
        stage.setKeepScreenOn(true);

        blackVideoView = new PlayerView(this);
        blackVideoView.setBackgroundColor(Color.BLACK);
        blackVideoView.setUseController(false);
        blackVideoView.setKeepScreenOn(true);
        stage.addView(blackVideoView, new FrameLayout.LayoutParams(-1, -1));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(Ui.dp(this, 70), Ui.dp(this, 40), Ui.dp(this, 70), Ui.dp(this, 40));
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setKeepScreenOn(true);

        eyebrow = new TextView(this);
        eyebrow.setText("SHABBAT TV");
        eyebrow.setTextColor(Ui.ACCENT);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eyebrow.setTextSize(Ui.compact(this) ? 14 : 17);
        eyebrow.setGravity(Gravity.CENTER);
        root.addView(eyebrow);

        title = new TextView(this);
        title.setTextColor(Ui.TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(Ui.compact(this) ? 30 : 42);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        root.addView(title, Ui.lp(-1, -2, this, 14));

        countdown = new TextView(this);
        countdown.setTextColor(Ui.TEXT);
        countdown.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        countdown.setTextSize(Ui.compact(this) ? 58 : 82);
        countdown.setGravity(Gravity.CENTER);
        root.addView(countdown, Ui.lp(-1, -2, this, 18));

        clock = new TextView(this);
        clock.setTextColor(Ui.MUTED);
        clock.setTextSize(Ui.compact(this) ? 14 : 18);
        clock.setGravity(Gravity.CENTER);
        root.addView(clock, Ui.lp(-1, -2, this, 8));

        note = new TextView(this);
        note.setTextColor(Ui.MUTED);
        note.setTextSize(Ui.compact(this) ? 12 : 14);
        note.setGravity(Gravity.CENTER);
        root.addView(note, Ui.lp(-1, -2, this, 22));

        stage.addView(root, new FrameLayout.LayoutParams(-1, -1));
        setContentView(stage);
        showIntro();
    }

    private void showIntro() {
        setUiVisible(true);
        eyebrow.setText("MODE SHABBAT ARMÉ");
        title.setText("La TV reste prête");
        countdown.setText("");
        JSONObject next = nextSchedule();
        if (next != null) {
            long when = next.optLong("when", 0L);
            clock.setText("Prochaine séance · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(when)));
            note.setText("Ne mets pas la TV en veille avec la télécommande. Dans quelques secondes l’écran devient totalement noir.");
        } else {
            clock.setText("Aucune séance programmée");
            note.setText("Retour télécommande = désarmer le mode Shabbat.");
        }
    }

    private void updateState() {
        if (!AppState.isShabbatArmed(this)) {
            finish();
            return;
        }

        long now = System.currentTimeMillis();
        JSONObject next = nextSchedule();
        if (next == null) {
            showBlack();
            scheduleNextTick(15_000L);
            return;
        }

        long when = next.optLong("when", 0L);
        long left = when - now;
        String id = next.optString("id", "");

        if (left <= 1_000L && left > -AppState.RECOVERY_GRACE_MS) {
            launch(next, left < -1_000L);
            return;
        }

        if (now - openedAt < INTRO_MS && left > COUNTDOWN_WINDOW_MS) {
            if (!introLogged) {
                introLogged = true;
                LogStore.add(this, "Mode Shabbat", "Confirmation affichée puis passage au noir intégral avec lecture vidéo active");
            }
            showIntro();
            scheduleNextTick(1_000L);
            return;
        }

        if (left <= COUNTDOWN_WINDOW_MS) {
            showCountdown(next, left);
            if (!id.equals(shownScheduleId)) {
                shownScheduleId = id;
                LogStore.add(this, "Mode Shabbat", "Countdown affiché · " + next.optString("title", "Film") + " · film dans " + Math.max(0L, left / 1000L) + " s");
            }
            scheduleNextTick(500L);
        } else {
            showBlack();
            shownScheduleId = "";
            long untilCountdown = left - COUNTDOWN_WINDOW_MS;
            scheduleNextTick(Math.max(5_000L, Math.min(30_000L, untilCountdown)));
        }
    }

    private JSONObject nextSchedule() {
        org.json.JSONArray a = AppState.recoverableSchedules(this);
        long now = System.currentTimeMillis();
        JSONObject future = null;
        long futureWhen = Long.MAX_VALUE;
        JSONObject missed = null;
        long missedWhen = Long.MIN_VALUE;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            long when = o.optLong("when", 0L);
            if (when <= 0L) continue;
            if (when <= now && when > now - AppState.RECOVERY_GRACE_MS) {
                if (when > missedWhen) { missedWhen = when; missed = o; }
            } else if (when > now && when < futureWhen) {
                futureWhen = when; future = o;
            }
        }
        return missed != null ? missed : future;
    }

    private void showBlack() {
        stage.setBackgroundColor(Color.BLACK);
        root.setBackgroundColor(Color.TRANSPARENT);
        setUiVisible(false);
    }

    private void showCountdown(JSONObject s, long left) {
        root.setBackgroundColor(Color.rgb(4, 5, 7));
        setUiVisible(true);
        eyebrow.setText("LE FILM COMMENCE DANS");
        title.setText(s.optString("title", "Film"));
        long sec = Math.max(0L, (left + 999L) / 1000L);
        countdown.setText(String.format(Locale.FRANCE, "%02d:%02d", sec / 60L, sec % 60L));
        long when = s.optLong("when", 0L);
        clock.setText("Début prévu à " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(when)));
        note.setText("Audio : " + s.optString("audioLabel", "Automatique") + "  ·  Sous-titres : " + s.optString("subtitleLabel", "Aucun") + "  ·  Volume 37 %");

        int step = (int)((System.currentTimeMillis() / 30_000L) % 4L);
        root.setTranslationX(Ui.dp(this, step == 0 ? -3 : step == 2 ? 3 : 0));
        root.setTranslationY(Ui.dp(this, step == 1 ? -2 : step == 3 ? 2 : 0));
    }

    private void setUiVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.INVISIBLE;
        eyebrow.setVisibility(v);
        title.setVisibility(v);
        countdown.setVisibility(v);
        clock.setVisibility(v);
        note.setVisibility(v);
        if (!visible) {
            root.setTranslationX(0f);
            root.setTranslationY(0f);
        }
    }

    private void launch(JSONObject s, boolean recovery) {
        if (launching) return;
        launching = true;
        String id = s.optString("id", "");
        String movie = s.optString("movie", "");
        if (movie.isEmpty()) {
            JSONObject selected = AppState.selectedMovie(this);
            if (selected != null) movie = selected.toString();
        }
        if (movie.isEmpty()) {
            LogStore.add(this, "Erreur", "Mode Shabbat : film introuvable pour la séance " + id);
            AppState.removeSchedule(this, id);
            launching = false;
            handler.postDelayed(tick, 1_000L);
            return;
        }
        if (recovery) LogStore.add(this, "Mode Shabbat", "Récupération après retard/redémarrage · lancement immédiat");
        else LogStore.add(this, "Mode Shabbat", "Heure cible atteinte · arrêt vidéo noire puis lancement direct du film");

        releaseKeeperPlayback();
        PlaybackLauncher.launch(this, movie, AppState.FILM_VOLUME_PERCENT, id, false);
    }

    private void ensureKeeperPlayback() {
        if (launching || isFinishing()) return;
        try {
            if (blackKeeper == null) {
                blackKeeper = new ExoPlayer.Builder(this).build();
                blackKeeper.setWakeMode(C.WAKE_MODE_LOCAL);
                blackKeeper.setVolume(0f);
                blackKeeper.setRepeatMode(Player.REPEAT_MODE_ONE);
                blackKeeper.addListener(new Player.Listener() {
                    @Override public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_READY && !keeperReadyLogged) {
                            keeperReadyLogged = true;
                            LogStore.add(ShabbatModeActivity.this, "Mode Shabbat", "Vidéo noire silencieuse active · protection anti-économiseur prête");
                        }
                    }

                    @Override public void onPlayerError(PlaybackException error) {
                        LogStore.add(ShabbatModeActivity.this, "Erreur", "Vidéo noire anti-économiseur : " + error.getErrorCodeName());
                        handler.postDelayed(() -> {
                            releaseKeeperPlayback();
                            ensureKeeperPlayback();
                        }, 2_000L);
                    }
                });
                blackVideoView.setPlayer(blackKeeper);
                blackKeeper.setMediaItem(MediaItem.fromUri(BlackPlaybackAsset.uri(this)));
                blackKeeper.prepare();
            }
            if (!blackKeeper.isPlaying()) blackKeeper.play();
        } catch (Throwable t) {
            LogStore.add(this, "Erreur", "Impossible de maintenir la vidéo noire : " + t.getClass().getSimpleName());
        }
    }

    private void releaseKeeperPlayback() {
        try {
            if (blackVideoView != null) blackVideoView.setPlayer(null);
            if (blackKeeper != null) {
                blackKeeper.stop();
                blackKeeper.release();
                blackKeeper = null;
            }
        } catch (Throwable ignored) {}
        keeperReadyLogged = false;
    }

    private void scheduleNextTick(long delay) {
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, delay);
    }

    private void acquireCpuLock() {
        try {
            PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
            if (pm != null) {
                cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ShabbatTV:LongMode");
                cpuLock.setReferenceCounted(false);
                cpuLock.acquire();
            }
        } catch (Throwable t) {
            LogStore.add(this, "Erreur", "WakeLock mode Shabbat : " + t.getClass().getSimpleName());
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AppState.setShabbatArmed(this, false);
            LogStore.add(this, "Mode Shabbat", "Mode désarmé manuellement");
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        releaseKeeperPlayback();
        try { if (cpuLock != null && cpuLock.isHeld()) cpuLock.release(); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
