package fr.shabbattv;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PowerManager.WakeLock wakeLock;
    private String title = "Film";
    private boolean playLogged = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ui.prepareWindow(this);
        if (android.os.Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) { wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "ShabbatTV:Player"); wakeLock.acquire(120_000L); }

        try {
            String raw = getIntent().getStringExtra("movie");
            JSONObject movie = raw == null ? AppState.selectedMovie(this) : new JSONObject(raw);
            if (movie == null) throw new Exception("Aucun film sélectionné");
            title = movie.optString("title","Film");
            int vol = getIntent().getIntExtra("volume", AppState.defaultVolume(this));
            AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            if (am != null) { int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC); am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(0, Math.min(max, Math.round(max * vol / 100f))), 0); }

            PlayerView view = new PlayerView(this); view.setBackgroundColor(Color.BLACK); view.setUseController(true); view.setControllerAutoShow(false); view.setControllerShowTimeoutMs(3500); setContentView(view, new ViewGroup.LayoutParams(-1,-1));

            DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
            DefaultTrackSelector.Parameters.Builder params = trackSelector.buildUponParameters();
            String audioLanguage = movie.optString("audioLanguage","");
            if(!audioLanguage.isEmpty()) params.setPreferredAudioLanguage(audioLanguage);
            boolean subtitlesOff = movie.optBoolean("subtitlesOff", true);
            String subtitleLanguage = movie.optString("subtitleLanguage","");
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitlesOff);
            if(!subtitlesOff && !subtitleLanguage.isEmpty()) params.setPreferredTextLanguage(subtitleLanguage);
            trackSelector.setParameters(params);

            player = new ExoPlayer.Builder(this).setTrackSelector(trackSelector).build(); view.setPlayer(player);
            String url = PlexClient.streamUrl(this, movie);
            MediaItem.Builder item = new MediaItem.Builder().setUri(url);

            String subtitleKey = movie.optString("subtitleKey","");
            if(!subtitlesOff && !subtitleKey.isEmpty()) {
                String codec = movie.optString("subtitleCodec","");
                String mime = subtitleMime(codec);
                if(mime != null) {
                    MediaItem.SubtitleConfiguration sub = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(PlexClient.resourceUrl(this,subtitleKey)))
                            .setMimeType(mime)
                            .setLanguage(subtitleLanguage.isEmpty()?null:subtitleLanguage)
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build();
                    List<MediaItem.SubtitleConfiguration> list = new ArrayList<>(); list.add(sub); item.setSubtitleConfigurations(list);
                }
            }
            player.setMediaItem(item.build());
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY && !playLogged) { playLogged = true; LogStore.add(PlayerActivity.this,"Lecture","Lecture démarrée : "+title); }
                    if (state == Player.STATE_ENDED) {
                        LogStore.add(PlayerActivity.this,"Lecture","Film terminé : "+title);
                        if (getIntent().getBooleanExtra("sleep_when_done", true)) SleepHelper.sleepNow(PlayerActivity.this);
                    }
                }
                @Override public void onPlayerError(PlaybackException error) {
                    String msg = error.getMessage()==null ? error.toString() : error.getMessage(); AppState.prefs(PlayerActivity.this).edit().putString("last_play_error", msg).apply(); LogStore.add(PlayerActivity.this,"Erreur","Lecture de "+title+" : "+msg);
                }
            });
            player.prepare(); player.play();
            AppState.prefs(this).edit().putLong("last_play_started", System.currentTimeMillis()).putString("last_play_title", title).apply();
            LogStore.add(this,"Lecture","Préparation : "+title+" · audio "+movie.optString("audioLabel","auto")+" · sous-titres "+movie.optString("subtitleLabel","Aucun")+" · volume "+vol+" %");
        } catch (Exception e) {
            AppState.prefs(this).edit().putString("last_play_error", e.toString()).apply(); LogStore.add(this,"Erreur","Impossible de préparer le film : "+e); finish();
        }
    }

    private String subtitleMime(String codec) {
        if(codec==null)return null; codec=codec.toLowerCase();
        if(codec.contains("srt")||codec.contains("subrip"))return MimeTypes.APPLICATION_SUBRIP;
        if(codec.contains("vtt"))return MimeTypes.TEXT_VTT;
        if(codec.contains("ass")||codec.contains("ssa"))return MimeTypes.TEXT_SSA;
        if(codec.contains("ttml"))return MimeTypes.APPLICATION_TTML;
        return null;
    }

    @Override protected void onDestroy() { if (player != null) player.release(); if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); super.onDestroy(); }
}
