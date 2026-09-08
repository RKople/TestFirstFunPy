package fr.shabbattv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public final class AppState {
    public static final String PREFS = "shabbat_tv_v1";
    public static final int FILM_VOLUME_PERCENT = 37;
    public static final long RECOVERY_GRACE_MS = 6 * 60 * 60_000L;

    private AppState() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String clientId(Context c) {
        SharedPreferences p = prefs(c);
        String id = p.getString("client_id", null);
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
            p.edit().putString("client_id", id).apply();
        }
        return id;
    }

    public static boolean plexConnected(Context c) {
        return !prefs(c).getString("plex_account_token", "").isEmpty()
                && !prefs(c).getString("plex_server_token", "").isEmpty()
                && !prefs(c).getString("plex_server_url", "").isEmpty();
    }

    public static JSONObject selectedMovie(Context c) {
        String raw = prefs(c).getString("selected_movie", "");
        if (raw.isEmpty()) return null;
        try { return new JSONObject(raw); } catch (Exception e) { return null; }
    }

    public static void setSelectedMovie(Context c, JSONObject movie) {
        prefs(c).edit().putString("selected_movie", movie == null ? "" : movie.toString()).apply();
    }

    private static JSONArray rawSchedules(Context c) {
        String raw = prefs(c).getString("schedules", "[]");
        try { return new JSONArray(raw); } catch (Exception e) { return new JSONArray(); }
    }

    /** Returns only future sessions for the normal planning UI. */
    public static JSONArray schedules(Context c) {
        JSONArray src = rawSchedules(c);
        JSONArray keep = new JSONArray();
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o == null) { changed = true; continue; }
            long when = o.optLong("when", 0L);
            if (when > 0 && when < now) {
                changed = true;
                continue;
            }
            keep.put(o);
        }
        if (changed && !isShabbatArmed(c)) setSchedules(c, keep);
        return keep;
    }

    /**
     * While Shabbat mode is armed, keep recently-past sessions available for crash/reboot recovery.
     * PlaybackLauncher removes a session immediately once it is actually launched.
     */
    public static JSONArray recoverableSchedules(Context c) {
        JSONArray src = rawSchedules(c);
        JSONArray keep = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o == null) continue;
            long when = o.optLong("when", 0L);
            if (when <= 0L) continue;
            if (when >= now - RECOVERY_GRACE_MS) keep.put(o);
        }
        return keep;
    }

    public static void setSchedules(Context c, JSONArray arr) {
        prefs(c).edit().putString("schedules", arr == null ? "[]" : arr.toString()).apply();
    }

    public static JSONObject scheduleById(Context c, String id) {
        if (id == null) return null;
        JSONArray a = isShabbatArmed(c) ? recoverableSchedules(c) : schedules(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }

    public static boolean removeSchedule(Context c, String id) {
        if (id == null || id.isEmpty()) return false;
        JSONArray src = rawSchedules(c);
        JSONArray keep = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) {
                removed = true;
                continue;
            }
            if (o != null) keep.put(o);
        }
        if (removed) setSchedules(c, keep);
        return removed;
    }

    public static boolean isShabbatArmed(Context c) {
        return prefs(c).getBoolean("shabbat_armed", false);
    }

    public static long shabbatArmedAt(Context c) {
        return prefs(c).getLong("shabbat_armed_at", 0L);
    }

    public static void setShabbatArmed(Context c, boolean armed) {
        SharedPreferences.Editor e = prefs(c).edit().putBoolean("shabbat_armed", armed);
        if (armed) e.putLong("shabbat_armed_at", System.currentTimeMillis());
        else e.remove("shabbat_armed_at");
        e.apply();
    }

    public static JSONObject nextRecoverableSchedule(Context c) {
        JSONArray a = recoverableSchedules(c);
        JSONObject best = null;
        long bestWhen = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            long when = o.optLong("when", 0L);
            if (when <= 0L) continue;
            // Recently missed sessions have priority so recovery can launch immediately.
            long rank = when < now ? when - Long.MAX_VALUE / 4 : when;
            if (best == null || rank < bestWhen) {
                best = o;
                bestWhen = rank;
            }
        }
        return best;
    }

    public static boolean hasFutureSchedule(Context c) {
        JSONArray a = recoverableSchedules(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && o.optLong("when", 0L) > now) return true;
        }
        return false;
    }

    /** Keep the same request-code algorithm used by previous versions so old alarms can be cancelled too. */
    public static int requestCodeForId(String id) {
        if (id == null) return 1;
        int hash = id.hashCode();
        return hash == Integer.MIN_VALUE ? 0 : Math.abs(hash);
    }

    public static int preWakeMinutes(Context c) {
        return prefs(c).getInt("prewake_minutes", 10);
    }

    /** Film volume is intentionally fixed everywhere, including old stored schedules. */
    public static int defaultVolume(Context c) {
        return FILM_VOLUME_PERCENT;
    }
}
