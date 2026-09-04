package fr.shabbattv;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PowerManager.WakeLock wakeLock;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "ShabbatTV:Player");
            wakeLock.acquire(120_000L);
        }

        try {
            String raw = getIntent().getStringExtra("movie");
            JSONObject movie = raw == null ? AppState.selectedMovie(this) : new JSONObject(raw);
            if (movie == null) throw new Exception("Aucun film sélectionné");
            int vol = getIntent().getIntExtra("volume", AppState.defaultVolume(this));
            AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(0, Math.min(max, Math.round(max * vol / 100f))), 0);
            }
            PlayerView view = new PlayerView(this);
            view.setUseController(true);
            setContentView(view, new ViewGroup.LayoutParams(-1,-1));
            player = new ExoPlayer.Builder(this).build();
            view.setPlayer(player);
            String url = PlexClient.streamUrl(this, movie);
            player.setMediaItem(MediaItem.fromUri(url));
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED && getIntent().getBooleanExtra("sleep_when_done", true)) SleepHelper.sleepNow(PlayerActivity.this);
                }
                @Override public void onPlayerError(PlaybackException error) {
                    AppState.prefs(PlayerActivity.this).edit().putString("last_play_error", error.getMessage()).apply();
                }
            });
            player.prepare(); player.play();
            AppState.prefs(this).edit().putLong("last_play_started", System.currentTimeMillis()).putString("last_play_title", movie.optString("title")).apply();
        } catch (Exception e) {
            AppState.prefs(this).edit().putString("last_play_error", e.toString()).apply();
            finish();
        }
    }

    @Override protected void onDestroy() {
        if (player != null) player.release();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
}
