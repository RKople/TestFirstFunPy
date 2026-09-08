package fr.shabbattv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * v1.9 recovery after a real Android reboot. Backup alarms are rebuilt and, if the
 * long-duration mode was armed, ShabbatModeActivity is relaunched so Android returns
 * to the black hold/countdown state instead of depending on a future deep-standby wake.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        JSONArray schedules = AppState.recoverableSchedules(context);
        long now = System.currentTimeMillis();
        int rearmed = 0;
        boolean shabbatArmed = AppState.isShabbatArmed(context);

        for (int i = 0; i < schedules.length(); i++) {
            JSONObject s = schedules.optJSONObject(i);
            if (s == null) continue;

            String id = s.optString("id", "");
            String movie = s.optString("movie", "");
            long when = s.optLong("when", 0L);
            if (id.isEmpty() || movie.isEmpty() || when <= now - AppState.RECOVERY_GRACE_MS) continue;

            int volume = AppState.FILM_VOLUME_PERCENT;
            boolean sleepWhenDone = !shabbatArmed;
            long storedWake = s.optLong("wakeAt", when - AppState.preWakeMinutes(context) * 60_000L);
            long wakeAt = storedWake > now + 5_000L ? storedWake : Math.min(when, now + 5_000L);
            long retryAt = s.optLong("retryAt", 0L);

            if (when > now && wakeAt < when) {
                PendingIntent wake = wakePending(context, id + ":wake", id, movie, volume, when, wakeAt, sleepWhenDone, "waiting");
                AlarmTools.setCritical(context, am, wakeAt, wake, AppState.requestCodeForId(id + ":show-wake"));
            }

            if (when > now && retryAt > now + 5_000L && retryAt < when) {
                PendingIntent retry = wakePending(context, id + ":retry", id, movie, volume, when, retryAt, sleepWhenDone, "waiting");
                AlarmTools.setCritical(context, am, retryAt, retry, AppState.requestCodeForId(id + ":show-retry"));
            }

            if (when > now) {
                Intent direct = new Intent(context, ScheduleReceiver.class);
                direct.putExtra("movie", movie);
                direct.putExtra("volume", volume);
                direct.putExtra("schedule_id", id);
                direct.putExtra("sleep_when_done", sleepWhenDone);
                PendingIntent directPi = PendingIntent.getBroadcast(
                        context,
                        AppState.requestCodeForId(id + ":direct"),
                        direct,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                AlarmTools.setCritical(context, am, when, directPi, AppState.requestCodeForId(id + ":show-direct"));

                PendingIntent targetWake = wakePending(context, id + ":target-wake", id, movie, volume, when, when, sleepWhenDone, "play");
                AlarmTools.setCritical(context, am, when, targetWake, AppState.requestCodeForId(id + ":show-target"));
            }
            rearmed++;
        }

        LogStore.add(context, "Planning", "Démarrage Android · " + rearmed + " séance" + (rearmed > 1 ? "s" : "") + " récupérée" + (rearmed > 1 ? "s" : "") + (shabbatArmed ? " · Mode Shabbat toujours armé" : ""));

        if (shabbatArmed) {
            Intent open = new Intent(context, ShabbatModeActivity.class);
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                context.startActivity(open);
                LogStore.add(context, "Mode Shabbat", "Redémarrage Android · écran noir armé relancé immédiatement");
            } catch (Throwable t) {
                LogStore.add(context, "Mode Shabbat", "Relance directe après boot refusée · fallback PendingIntent");
            }

            try {
                PendingIntent pi = PendingIntent.getActivity(
                        context,
                        AppState.requestCodeForId("shabbat-mode-after-boot"),
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                long at = System.currentTimeMillis() + 8_000L;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                else am.setExact(AlarmManager.RTC_WAKEUP, at, pi);
            } catch (Throwable t) {
                LogStore.add(context, "Erreur", "Fallback relance Mode Shabbat après boot : " + t.getClass().getSimpleName());
            }
        }
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
