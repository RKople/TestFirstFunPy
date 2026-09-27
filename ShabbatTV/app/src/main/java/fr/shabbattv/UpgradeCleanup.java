package fr.shabbattv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;

/** One-time cancellation only. This class never creates or rearms an alarm. */
public final class UpgradeCleanup {
    private UpgradeCleanup() {}
    public static void run(Context c) {
        SharedPreferences p = AppState.prefs(c);
        if (p.getBoolean("active_video_cleanup_v111", false)) return;
        try {
            AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            if (am != null && Build.VERSION.SDK_INT >= 34) am.cancelAll();
            else if (am != null) {
                JSONArray sessions = AppState.pendingSchedules(c);
                for (int i = 0; i < sessions.length(); i++) {
                    String id = sessions.getJSONObject(i).optString("id", "");
                    for (String suffix : new String[]{":wake", ":retry", ":target-wake"})
                        cancel(c, am, "WakeReceiver", hash(id + suffix));
                    cancel(c, am, "ScheduleReceiver", hash(id + ":direct"));
                    for (String suffix : new String[]{":pre", ":target"})
                        cancel(c, am, "RobustWakeReceiver", hash(id + suffix));
                    cancel(c, am, "ScheduleReceiver", hash(id));
                    cancel(c, am, "PreWakeReceiver", hash(id) + 1);
                }
            }
            JSONArray clean = AppState.pendingSchedules(c);
            for (int i = 0; i < clean.length(); i++) {
                JSONObject s = clean.getJSONObject(i);
                for (String key : new String[]{"wakeAt", "retryAt", "visibleEstimateAt", "alarmMode", "endAt"}) s.remove(key);
            }
            AppState.setSchedules(c, clean);
            SharedPreferences.Editor edit = p.edit();
            for (String key : p.getAll().keySet()) {
                if (key.startsWith("philips_") || key.startsWith("wake_") || key.startsWith("prewake_")
                        || key.startsWith("last_sleep_") || key.equals("shabbat_armed_at")) edit.remove(key);
            }
            edit.putBoolean("shabbat_armed", false).putBoolean("active_video_cleanup_v111", true).apply();
            c.getSharedPreferences("wake_diag", Context.MODE_PRIVATE).edit().clear().apply();
            LogStore.add(c, "Mise à jour", "v1.11 · anciens réveils annulés · Plex et planning conservés · réactiver le mode avant utilisation");
        } catch (Exception e) {
            LogStore.add(c, "Erreur", "Annulation anciens réveils : " + e.getClass().getSimpleName());
        }
    }
    private static int hash(String key) { int h = key.hashCode(); return h == Integer.MIN_VALUE ? 0 : Math.abs(h); }
    private static void cancel(Context c, AlarmManager am, String name, int request) {
        Intent intent = new Intent().setClassName(c.getPackageName(), "fr.shabbattv." + name);
        PendingIntent pi = PendingIntent.getBroadcast(c, request, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) { am.cancel(pi); pi.cancel(); }
    }
}
