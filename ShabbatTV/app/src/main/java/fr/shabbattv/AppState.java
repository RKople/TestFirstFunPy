package fr.shabbattv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Keeps the existing preference namespace: updating must not erase Plex or the planning. */
public final class AppState {
    public static final String PREFS = "shabbat_tv_v1";
    public static final int FILM_VOLUME_PERCENT = 37;
    private AppState() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
    public static String clientId(Context c) {
        String id = prefs(c).getString("client_id", "");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs(c).edit().putString("client_id", id).apply();
        }
        return id;
    }
    public static boolean plexConnected(Context c) {
        return !prefs(c).getString("plex_account_token", "").isEmpty()
            && !prefs(c).getString("plex_server_token", "").isEmpty()
            && !prefs(c).getString("plex_server_url", "").isEmpty();
    }
    public static JSONObject selectedMovie(Context c) {
        try { return new JSONObject(prefs(c).getString("selected_movie", "")); }
        catch (Exception e) { return null; }
    }
    public static void setSelectedMovie(Context c, JSONObject movie) {
        prefs(c).edit().putString("selected_movie", movie == null ? "" : movie.toString()).apply();
    }
    /** Reading never deletes sessions. Only an explicit action or the active engine consumes them. */
    public static JSONArray pendingSchedules(Context c) {
        List<JSONObject> sorted = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("schedules", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) sorted.add(o);
            }
        } catch (Exception ignored) {}
        Collections.sort(sorted, (a, b) -> Long.compare(a.optLong("when"), b.optLong("when")));
        JSONArray out = new JSONArray();
        for (JSONObject o : sorted) out.put(o);
        return out;
    }
    public static JSONArray schedules(Context c) {
        JSONArray src = pendingSchedules(c), out = new JSONArray();
        long now = System.currentTimeMillis();
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o != null && o.optLong("when", 0L) >= now) out.put(o);
        }
        return out;
    }
    public static void setSchedules(Context c, JSONArray a) {
        prefs(c).edit().putString("schedules", a == null ? "[]" : a.toString()).apply();
    }
    public static JSONObject scheduleById(Context c, String id) {
        if (id == null || id.isEmpty()) return null;
        JSONArray a = pendingSchedules(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return o;
        }
        return null;
    }
    public static synchronized boolean removeSchedule(Context c, String id) {
        if (id == null || id.isEmpty()) return false;
        JSONArray src = pendingSchedules(c), out = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < src.length(); i++) {
            JSONObject o = src.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) removed = true;
            else if (o != null) out.put(o);
        }
        if (removed) setSchedules(c, out);
        return removed;
    }
    public static synchronized JSONObject addSchedule(Context c, JSONObject movie, long when) throws Exception {
        if (!plexConnected(c)) throw new Exception("Connecte d’abord Plex.");
        if (movie == null || movie.optString("partKey", "").isEmpty()) throw new Exception("Sélectionne un film lisible.");
        long now = System.currentTimeMillis();
        if (when <= now + 5_000L) throw new Exception("Choisis un horaire dans le futur.");
        long duration = movie.optLong("durationMs", 0L);
        JSONArray a = schedules(c);
        for (int i = 0; i < a.length(); i++) {
            JSONObject other = a.getJSONObject(i);
            if (SchedulePolicy.overlaps(when, duration, other.optLong("when"), other.optLong("durationMs")))
                throw new Exception("Cette séance chevauche « " + other.optString("title", "Film") + " ».");
        }
        JSONObject s = new JSONObject();
        s.put("id", UUID.randomUUID().toString());
        s.put("when", when); s.put("title", movie.optString("title", "Film"));
        s.put("movie", movie.toString()); s.put("durationMs", duration);
        s.put("volume", FILM_VOLUME_PERCENT);
        s.put("server", prefs(c).getString("plex_server_name", "Plex"));
        s.put("serverId", prefs(c).getString("plex_server_machine_id", ""));
        s.put("audioLabel", movie.optString("audioLabel", "Automatique"));
        s.put("subtitleLabel", movie.optString("subtitleLabel", "Aucun"));
        s.put("createdAt", now);
        a.put(s); setSchedules(c, a);
        LogStore.add(c, "Planning", "Séance ajoutée : " + s.optString("title") + " · " + when);
        return s;
    }
    public static boolean isShabbatArmed(Context c) { return prefs(c).getBoolean("shabbat_armed", false); }
    public static void setShabbatArmed(Context c, boolean armed) {
        prefs(c).edit().putBoolean("shabbat_armed", armed).apply();
    }
}
