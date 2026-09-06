package fr.shabbattv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public final class PlaybackLauncher {
    private PlaybackLauncher() {}

    public static synchronized boolean launch(Context context, String movie, int volume, String scheduleId, boolean sleepWhenDone) {
        long now = System.currentTimeMillis();
        SharedPreferences p = AppState.prefs(context);
        String key = scheduleId == null ? "" : scheduleId;
        String lastId = p.getString("last_launch_schedule_id", "");
        long lastAt = p.getLong("last_launch_at", 0L);
        if (!key.isEmpty() && key.equals(lastId) && now - lastAt < 5 * 60_000L) {
            LogStore.add(context,"Planning","Déclenchement doublon ignoré");
            return false;
        }
        p.edit().putString("last_launch_schedule_id", key).putLong("last_launch_at", now).apply();
        if (!key.isEmpty() && !key.startsWith("test-")) AppState.removeSchedule(context,key);

        Intent play = new Intent(context,PlayerActivity.class);
        play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        play.putExtra("movie",movie);
        play.putExtra("volume",AppState.FILM_VOLUME_PERCENT);
        play.putExtra("schedule_id",key);
        play.putExtra("sleep_when_done",sleepWhenDone);
        context.startActivity(play);
        return true;
    }
}
