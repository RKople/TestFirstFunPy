package fr.shabbattv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public final class AppState {
    public static final String PREFS = "shabbat_tv_v1";

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

    /** Returns only future sessions. Past sessions disappear automatically. */
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
        if (changed) setSchedules(c, keep);
        return keep;
    }

    public static void setSchedules(Context c, JSONArray arr) {
        prefs(c).edit().putString("schedules", arr == null ? "[]" : arr.toString()).apply();
    }

    public static JSONObject scheduleById(Context c, String id) {
        if (id == null) return null;
        JSONArray a = schedules(c);
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

    public static int requestCodeForId(String id) {
        if (id == null) return 1;
        return id.hashCode() & 0x7fffffff;
    }

    public static int preWakeMinutes(Context c) {
        return prefs(c).getInt("prewake_minutes", 10);
    }

    public static int defaultVolume(Context c) {
        return prefs(c).getInt("default_volume", 20);
    }
}
