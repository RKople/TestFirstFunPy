package fr.shabbattv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public final class LogStore {
    private static final String PREFS = "shabbat_tv_logs";
    private static final String KEY = "entries";
    private static final int MAX = 180;

    private LogStore() {}

    public static synchronized void add(Context c, String category, String message) {
        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(p.getString(KEY, "[]"));
            JSONArray next = new JSONArray();
            int start = Math.max(0, old.length() - (MAX - 1));
            for (int i = start; i < old.length(); i++) next.put(old.opt(i));
            JSONObject o = new JSONObject();
            o.put("time", System.currentTimeMillis());
            o.put("category", category == null ? "Info" : category);
            o.put("message", message == null ? "" : message);
            next.put(o);
            p.edit().putString(KEY, next.toString()).commit();
        } catch (Exception ignored) {}
    }

    public static synchronized String text(Context c) {
        try {
            SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONArray a = new JSONArray(p.getString(KEY, "[]"));
            if (a.length() == 0) return "Aucun événement enregistré pour le moment.";
            StringBuilder b = new StringBuilder();
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
            for (int i = a.length() - 1; i >= 0; i--) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                if (b.length() > 0) b.append("\n\n");
                b.append(df.format(new Date(o.optLong("time"))))
                        .append("  ·  ")
                        .append(o.optString("category", "Info"))
                        .append("\n")
                        .append(o.optString("message", ""));
            }
            return b.toString();
        } catch (Exception e) {
            return "Impossible de lire les logs : " + e.getMessage();
        }
    }

    public static synchronized void clear(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).commit();
    }
}
