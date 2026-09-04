package fr.shabbattv;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PlexClient {
    public static final class Pin { public long id; public String code; public String token; }
    public static final class Movie {
        public String title, year, ratingKey, thumb, partKey;
        public JSONObject json() {
            JSONObject o = new JSONObject();
            try { o.put("title", title); o.put("year", year); o.put("ratingKey", ratingKey); o.put("thumb", thumb); o.put("partKey", partKey); } catch (Exception ignored) {}
            return o;
        }
    }

    private PlexClient() {}

    private static void headers(Context c, HttpURLConnection h, String token) {
        h.setRequestProperty("Accept", "application/json");
        h.setRequestProperty("X-Plex-Product", "Shabbat TV");
        h.setRequestProperty("X-Plex-Version", "1.0");
        h.setRequestProperty("X-Plex-Client-Identifier", AppState.clientId(c));
        h.setRequestProperty("X-Plex-Platform", "Android TV");
        h.setRequestProperty("X-Plex-Device-Name", "Shabbat TV");
        if (token != null && !token.isEmpty()) h.setRequestProperty("X-Plex-Token", token);
    }

    public static Pin createPin(Context c) throws Exception {
        URL u = new URL("https://plex.tv/api/v2/pins?strong=true");
        HttpURLConnection h = (HttpURLConnection) u.openConnection();
        h.setRequestMethod("POST"); h.setDoOutput(true); headers(c, h, "");
        h.setFixedLengthStreamingMode(0); try (OutputStream os = h.getOutputStream()) { }
        JSONObject j = new JSONObject(read(h));
        Pin p = new Pin(); p.id = j.getLong("id"); p.code = j.getString("code"); p.token = j.optString("authToken", "");
        return p;
    }

    public static Pin checkPin(Context c, long id) throws Exception {
        URL u = new URL("https://plex.tv/api/v2/pins/" + id);
        HttpURLConnection h = (HttpURLConnection) u.openConnection(); headers(c, h, "");
        JSONObject j = new JSONObject(read(h));
        Pin p = new Pin(); p.id = id; p.code = j.optString("code", ""); p.token = j.optString("authToken", "");
        return p;
    }

    public static void discoverServer(Context c, String accountToken) throws Exception {
        URL u = new URL("https://plex.tv/api/v2/resources?includeHttps=1&includeRelay=1");
        HttpURLConnection h = (HttpURLConnection) u.openConnection(); headers(c, h, accountToken);
        JSONArray a = new JSONArray(read(h));
        JSONObject best = null;
        for (int i = 0; i < a.length(); i++) {
            JSONObject r = a.getJSONObject(i);
            if (!r.optString("provides", "").contains("server")) continue;
            if (best == null || r.optBoolean("owned", false)) best = r;
            if (r.optBoolean("owned", false)) break;
        }
        if (best == null) throw new Exception("Aucun serveur Plex trouvé");
        JSONArray conns = best.getJSONArray("connections");
        String uri = null;
        for (int i = 0; i < conns.length(); i++) {
            JSONObject x = conns.getJSONObject(i);
            if (x.optString("uri", "").startsWith("https://") && !x.optBoolean("local", false)) { uri = x.getString("uri"); break; }
        }
        if (uri == null && conns.length() > 0) uri = conns.getJSONObject(0).getString("uri");
        if (uri == null) throw new Exception("Aucune connexion au serveur Plex");
        AppState.prefs(c).edit()
                .putString("plex_account_token", accountToken)
                .putString("plex_server_token", best.optString("accessToken", accountToken))
                .putString("plex_server_url", uri)
                .putString("plex_server_name", best.optString("name", "Plex"))
                .apply();
    }

    public static List<Movie> searchMovies(Context c, String query) throws Exception {
        String base = AppState.prefs(c).getString("plex_server_url", "");
        String token = AppState.prefs(c).getString("plex_server_token", "");
        if (base.isEmpty() || token.isEmpty()) throw new Exception("Plex non connecté");
        List<String> sections = movieSections(c, base, token);
        List<Movie> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase();
        for (String s : sections) {
            URL u = new URL(base + "/library/sections/" + s + "/all?type=1&X-Plex-Token=" + URLEncoder.encode(token, "UTF-8"));
            HttpURLConnection h = (HttpURLConnection) u.openConnection();
            parseMovies(h.getInputStream(), q, out);
            if (out.size() >= 120) break;
        }
        Collections.sort(out, Comparator.comparing(m -> m.title == null ? "" : m.title.toLowerCase()));
        return out;
    }

    private static List<String> movieSections(Context c, String base, String token) throws Exception {
        URL u = new URL(base + "/library/sections?X-Plex-Token=" + URLEncoder.encode(token, "UTF-8"));
        HttpURLConnection h = (HttpURLConnection) u.openConnection();
        XmlPullParser p = XmlPullParserFactory.newInstance().newPullParser(); p.setInput(h.getInputStream(), "UTF-8");
        List<String> result = new ArrayList<>();
        int e;
        while ((e = p.next()) != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG && "Directory".equals(p.getName()) && "movie".equals(p.getAttributeValue(null, "type"))) {
                String key = p.getAttributeValue(null, "key"); if (key != null) result.add(key);
            }
        }
        return result;
    }

    private static void parseMovies(InputStream in, String q, List<Movie> out) throws Exception {
        XmlPullParser p = XmlPullParserFactory.newInstance().newPullParser(); p.setInput(in, "UTF-8");
        Movie current = null;
        int e;
        while ((e = p.next()) != XmlPullParser.END_DOCUMENT) {
            if (e == XmlPullParser.START_TAG && "Video".equals(p.getName())) {
                String title = p.getAttributeValue(null, "title");
                if (title == null || (!q.isEmpty() && !title.toLowerCase().contains(q))) { current = null; continue; }
                current = new Movie(); current.title = title; current.year = p.getAttributeValue(null, "year"); current.ratingKey = p.getAttributeValue(null, "ratingKey"); current.thumb = p.getAttributeValue(null, "thumb");
            } else if (e == XmlPullParser.START_TAG && "Part".equals(p.getName()) && current != null && current.partKey == null) {
                current.partKey = p.getAttributeValue(null, "key");
            } else if (e == XmlPullParser.END_TAG && "Video".equals(p.getName()) && current != null) {
                if (current.partKey != null) out.add(current); current = null;
            }
        }
    }

    public static String streamUrl(Context c, JSONObject movie) throws Exception {
        String base = AppState.prefs(c).getString("plex_server_url", "");
        String token = AppState.prefs(c).getString("plex_server_token", "");
        String part = movie.optString("partKey", "");
        if (part.isEmpty()) throw new Exception("Film Plex sans fichier lisible");
        return base + part + (part.contains("?") ? "&" : "?") + "X-Plex-Token=" + URLEncoder.encode(token, "UTF-8");
    }

    private static String read(HttpURLConnection h) throws Exception {
        int code = h.getResponseCode();
        InputStream in = code >= 400 ? h.getErrorStream() : h.getInputStream();
        if (in == null) throw new Exception("HTTP " + code);
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder(); String line; while ((line = r.readLine()) != null) b.append(line);
        if (code >= 400) throw new Exception("HTTP " + code + " " + b);
        return b.toString();
    }
}
