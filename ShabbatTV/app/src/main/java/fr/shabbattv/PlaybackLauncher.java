package fr.shabbattv;

import android.content.Context;
import android.content.Intent;

/** Foreground handoff only. The player consumes the session after playback actually starts. */
public final class PlaybackLauncher {
    private PlaybackLauncher() {}
    public static boolean launch(Context c, String movie, String scheduleId) {
        try {
            Intent play = new Intent(c, PlayerActivity.class);
            if (!(c instanceof android.app.Activity)) play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            play.putExtra("movie", movie);
            play.putExtra("schedule_id", scheduleId == null ? "" : scheduleId);
            c.startActivity(play);
            return true;
        } catch (Exception e) {
            LogStore.add(c, "Erreur", "Ouverture du lecteur impossible : " + e.getClass().getSimpleName());
            return false;
        }
    }
}
