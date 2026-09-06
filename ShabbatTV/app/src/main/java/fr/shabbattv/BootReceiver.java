package fr.shabbattv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * AlarmManager entries disappear across a real Android reboot. Philips TVs can move
 * between different standby/deep-standby states, so v1.8 stores enough session data
 * to rebuild every future critical alarm if Android is restarted before the film.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        JSONArray schedules = AppState.schedules(context);
        long now = System.currentTimeMillis();
        int armed = 0;

        for (int i = 0; i < schedules.length(); i++) {
            JSONObject s = schedules.optJSONObject(i);
            if (s == null) continue;

            String id = s.optString("id", "");
            String movie = s.optString("movie", "");
            long when = s.optLong("when", 0L);
            if (id.isEmpty() || movie.isEmpty() || when <= now) continue;

            int volume = AppState.FILM_VOLUME_PERCENT;
            boolean sleepWhenDone = true;
            long storedWake = s.optLong("wakeAt", when - AppState.preWakeMinutes(context) * 60_000L);
            long wakeAt = storedWake > now + 5_000L ? storedWake : Math.min(when, now + 5_000L);
            long retryAt = s.optLong("retryAt", 0L);

            if (wakeAt < when) {
                PendingIntent wake = wakePending(context, id + ":wake", id, movie, volume, when, wakeAt, sleepWhenDone, "waiting");
                AlarmTools.setCritical(context, am, wakeAt, wake, AppState.requestCodeForId(id + ":show-wake"));
            }

            if (retryAt > now + 5_000L && retryAt < when) {
                PendingIntent retry = wakePending(context, id + ":retry", id, movie, volume, when, retryAt, sleepWhenDone, "waiting");
                AlarmTools.setCritical(context, am, retryAt, retry, AppState.requestCodeForId(id + ":show-retry"));
            }

            Intent direct = new Intent(context, ScheduleReceiver.class);
            direct.putExtra("movie", movie);
            direct.putExtra("volume", volume);
            direct.putExtra("schedule_id", id);
            direct.putExtra("sleep_when_done", true);
            PendingIntent directPi = PendingIntent.getBroadcast(
                    context,
                    AppState.requestCodeForId(id + ":direct"),
                    direct,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            AlarmTools.setCritical(context, am, when, directPi, AppState.requestCodeForId(id + ":show-direct"));

            PendingIntent targetWake = wakePending(context, id + ":target-wake", id, movie, volume, when, when, true, "play");
            AlarmTools.setCritical(context, am, when, targetWake, AppState.requestCodeForId(id + ":show-target"));
            armed++;
        }

        LogStore.add(context, "Planning", "Démarrage Android · " + armed + " séance" + (armed > 1 ? "s" : "") + " réarmée" + (armed > 1 ? "s" : "") + " en AlarmClock");
    }

    private PendingIntent wakePending(Context context, String requestKey, String id, String movie,
                                      int volume, long target, long expected,
                                      boolean sleepWhenDone, String mode) {
        Intent i = new Intent(context, WakeReceiver.class);
        i.putExtra("mode", mode);
        i.putExtra("schedule_id", id);
        i.putExtra("movie", movie);
        i.putExtra("volume", volume);
        i.putExtra("target_at", target);
        i.putExtra("expected_at", expected);
        i.putExtra("sleep_when_done", sleepWhenDone);
        return PendingIntent.getBroadcast(
                context,
                AppState.requestCodeForId(requestKey),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
