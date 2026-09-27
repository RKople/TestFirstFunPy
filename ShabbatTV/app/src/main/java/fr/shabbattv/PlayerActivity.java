package fr.shabbattv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.PlayerView;
import org.json.JSONObject;
import java.util.Collections;
import java.util.Locale;

/** Foreground Plex playback. End and errors return to the same black-video mode, never standby. */
public class PlayerActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private MediaSession session;
    private String title = "Film", scheduleId = "";
    private boolean automated, endHandled, started;
    private long plannedAt, bufferingSince, position;
    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            if (player == null || endHandled || isFinishing()) return;
            if (bufferingSince > 0L && SystemClock.elapsedRealtime() - bufferingSince > 90_000L) {
                fail("Chargement Plex bloqué pendant 90 secondes"); return;
            }
            handler.postDelayed(this, 5_000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); Ui.prepareWindow(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        scheduleId = getIntent().getStringExtra("schedule_id");
        if (scheduleId == null) scheduleId = "";
        automated = !scheduleId.isEmpty() && AppState.isShabbatArmed(this);
        JSONObject schedule = AppState.scheduleById(this, scheduleId);
        plannedAt = schedule == null ? 0L : schedule.optLong("when");
        try {
            String raw = getIntent().getStringExtra("movie");
            JSONObject movie = raw == null ? AppState.selectedMovie(this) : new JSONObject(raw);
            if (movie == null) throw new Exception("Aucun film sélectionné");
            title = movie.optString("title", "Film");
            AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(0, Math.min(max, Math.round(max * AppState.FILM_VOLUME_PERCENT / 100f))), 0);
            }
            PlayerView view = new PlayerView(this);
            view.setBackgroundColor(Color.BLACK); view.setShutterBackgroundColor(Color.BLACK);
            view.setKeepScreenOn(true); view.setUseController(true);
            view.setControllerAutoShow(false); view.setControllerShowTimeoutMs(3500);
            setContentView(view, new ViewGroup.LayoutParams(-1, -1));

            DefaultTrackSelector selector = new DefaultTrackSelector(this);
            DefaultTrackSelector.Parameters.Builder params = selector.buildUponParameters();
            String audio = movie.optString("audioLanguage", "");
            if (!audio.isEmpty()) params.setPreferredAudioLanguage(audio);
            boolean off = movie.optBoolean("subtitlesOff", true);
            String language = movie.optString("subtitleLanguage", "");
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, off);
            if (!off && !language.isEmpty()) params.setPreferredTextLanguage(language);
            selector.setParameters(params);
            player = new ExoPlayer.Builder(this).setTrackSelector(selector).build();
            player.setWakeMode(C.WAKE_MODE_NETWORK); view.setPlayer(player);
            session = new MediaSession.Builder(this, player).build();
            MediaItem.Builder item = new MediaItem.Builder().setUri(PlexClient.streamUrl(this, movie));
            String key = movie.optString("subtitleKey", "");
            String mime = subtitleMime(movie.optString("subtitleCodec", ""));
            if (!off && !key.isEmpty() && mime != null) {
                MediaItem.SubtitleConfiguration sub = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(PlexClient.resourceUrl(this, key)))
                    .setMimeType(mime).setLanguage(language.isEmpty() ? null : language)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build();
                item.setSubtitleConfigurations(Collections.singletonList(sub));
            }
            player.addListener(new Player.Listener() {
                @Override public void onIsPlayingChanged(boolean playing) {
                    if (playing && !started) {
                        started = true; AppState.removeSchedule(PlayerActivity.this, scheduleId);
                        LogStore.add(PlayerActivity.this, "Lecture", "Lecture démarrée : " + title
                            + (plannedAt > 0L ? " · délai après horaire prévu " + Math.max(0L, System.currentTimeMillis() - plannedAt) + " ms" : " · essai manuel"));
                    }
                }
                @Override public void onRenderedFirstFrame() { LogStore.add(PlayerActivity.this, "Lecture", "Première image du film rendue : " + title); }
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) {
                        if (bufferingSince == 0L) bufferingSince = SystemClock.elapsedRealtime();
                    } else bufferingSince = 0L;
                    if (state == Player.STATE_ENDED && !endHandled) {
                        LogStore.add(PlayerActivity.this, "Lecture", "Film terminé : " + title); finishPlayback();
                    }
                }
                @Override public void onPlayerError(PlaybackException error) { fail(error.getErrorCodeName()); }
            });
            player.setMediaItem(item.build());
            if (state != null) player.seekTo(state.getLong("position", 0L));
            bufferingSince = SystemClock.elapsedRealtime();
            player.prepare(); player.play(); handler.postDelayed(watchdog, 5_000L);
            LogStore.add(this, "Lecture", "Préparation : " + title + " · " + movie.optString("audioLabel", "Automatique")
                + " · sous-titres " + movie.optString("subtitleLabel", "Aucun") + " · volume " + AppState.FILM_VOLUME_PERCENT + " %");
        } catch (Exception e) { fail("Préparation impossible : " + e.getClass().getSimpleName()); }
    }
    private void fail(String reason) {
        if (endHandled) return;
        LogStore.add(this, "Erreur", "Lecture : " + title + " · " + reason);
        if (!automated) AppState.prefs(this).edit().putString("mode_error", "Échec du test de lecture : " + reason).apply();
        finishPlayback();
    }
    private void finishPlayback() {
        if (endHandled) return;
        endHandled = true; handler.removeCallbacksAndMessages(null);
        AppState.removeSchedule(this, scheduleId);
        releasePlayer();
        if (automated && AppState.isShabbatArmed(this)) {
            LogStore.add(this, "Mode Shabbat", "Retour à la vidéo noire en boucle");
            Intent next = new Intent(this, ShabbatModeActivity.class).putExtra("returning", true);
            next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(next);
        }
        finish();
    }
    @Override public void onBackPressed() {
        if (!automated) { finish(); return; }
        new AlertDialog.Builder(this).setTitle("Arrêter le mode Shabbat ?")
            .setMessage("Le film s’arrêtera. Les séances suivantes resteront enregistrées, mais ne démarreront plus automatiquement.")
            .setNegativeButton("Continuer", null).setPositiveButton("Arrêter", (dialog, which) -> {
                AppState.setShabbatArmed(this, false);
                LogStore.add(this, "Mode Shabbat", "Arrêt manuel pendant le film");
                finishPlayback();
            }).show();
    }
    private static String subtitleMime(String codec) {
        String value = codec == null ? "" : codec.toLowerCase(Locale.ROOT);
        if (value.contains("srt") || value.contains("subrip")) return MimeTypes.APPLICATION_SUBRIP;
        if (value.contains("vtt")) return MimeTypes.TEXT_VTT;
        if (value.contains("ass") || value.contains("ssa")) return MimeTypes.TEXT_SSA;
        if (value.contains("ttml")) return MimeTypes.APPLICATION_TTML;
        return null;
    }
    @Override protected void onPause() {
        if (player != null) position = player.getCurrentPosition();
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong("position", player == null ? position : player.getCurrentPosition());
        super.onSaveInstanceState(out);
    }
    @Override protected void onStop() {
        handler.removeCallbacksAndMessages(null); releasePlayer();
        if (!endHandled && !isChangingConfigurations()) {
            if (automated && AppState.isShabbatArmed(this)) {
                AppState.setShabbatArmed(this, false);
                AppState.prefs(this).edit().putString("mode_error", "Lecture interrompue : l’application n’est plus au premier plan.").apply();
                LogStore.add(this, "Mode Shabbat", "Interruption pendant le film · réactivation manuelle nécessaire");
            }
            finish();
        }
        super.onStop();
    }
    private void releasePlayer() {
        if (session != null) { session.release(); session = null; }
        if (player != null) { player.release(); player = null; }
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); releasePlayer(); super.onDestroy(); }
}
