package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

/**
 * Wake path used for real sessions.
 * Important: the screen wake lock is acquired inside the BroadcastReceiver BEFORE
 * attempting to start an Activity. This mirrors the standalone wake test that works
 * reliably on the Philips OLED810.
 */
public class RobustWakeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final String phase = intent.getStringExtra("phase"); // pre | target
        final String scheduleId = intent.getStringExtra("schedule_id");
        final String movie = intent.getStringExtra("movie");
        final int volume = intent.getIntExtra("volume", AppState.defaultVolume(context));
        final long target = intent.getLongExtra("target_at", System.currentTimeMillis());
        final long expected = intent.getLongExtra("expected_at", System.currentTimeMillis());
        final boolean sleepWhenDone = intent.getBooleanExtra("sleep_when_done", true);
        final long received = System.currentTimeMillis();

        SharedPreferences p = AppState.prefs(context);
        p.edit()
                .putLong("last_robust_wake_at", received)
                .putString("last_robust_wake_phase", phase == null ? "" : phase)
                .apply();

        long lateMs = Math.max(0L, received - expected);
        String test = scheduleId != null && scheduleId.startsWith("test-") ? " (test)" : "";
        LogStore.add(context, "Réveil",
                ("target".equals(phase) ? "Alarme cible robuste reçue" : "Pré-réveil robuste reçu") + test +
                        " · retard " + (lateMs / 1000L) + " s" +
                        " · film dans " + Math.max(0L, (target - received) / 1000L) + " s");

        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock;
        if (pm != null) {
            PowerManager.WakeLock tmp = null;
            try {
                tmp = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "ShabbatTV:RobustWake"
                );
                long hold = Math.max(120_000L, Math.min(15 * 60_000L, target - received + 180_000L));
                tmp.acquire(hold);
            } catch (Throwable t) {
                LogStore.add(context, "Erreur", "WakeLock robuste : " + t);
            }
            wakeLock = tmp;
        } else {
            wakeLock = null;
        }

        new Thread(() -> {
            try {
                // Give Philips a short moment to physically activate the panel before
                // starting a foreground activity. This is the key difference versus v1.5.
                Thread.sleep(900L);

                long now = System.currentTimeMillis();
                if ("target".equals(phase) || target <= now + 1500L) {
                    LogStore.add(context, "Planning", "Réveil confirmé · lancement immédiat du film");
                    PlaybackLauncher.launch(context, movie, volume, scheduleId, sleepWhenDone);
                } else {
                    Intent armed = new Intent(context, ArmedActivity.class);
                    armed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    armed.putExtra("schedule_id", scheduleId);
                    armed.putExtra("movie", movie);
                    armed.putExtra("volume", volume);
                    armed.putExtra("target_at", target);
                    armed.putExtra("sleep_when_done", sleepWhenDone);
                    try {
                        context.startActivity(armed);
                    } catch (Throwable first) {
                        LogStore.add(context, "Réveil", "1er lancement écran noir refusé, nouvelle tentative");
                        Thread.sleep(1500L);
                        context.startActivity(armed);
                    }
                }

                // Keep the receiver wake lock long enough for ArmedActivity / PlayerActivity
                // to acquire their own lock, avoiding a short sleep race between activities.
                Thread.sleep(3000L);
            } catch (Throwable t) {
                p.edit().putString("last_robust_wake_error", t.toString()).apply();
                LogStore.add(context, "Erreur", "Réveil robuste : " + t);
            } finally {
                try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Throwable ignored) {}
                pending.finish();
            }
        }, "ShabbatRobustWake").start();
    }
}
