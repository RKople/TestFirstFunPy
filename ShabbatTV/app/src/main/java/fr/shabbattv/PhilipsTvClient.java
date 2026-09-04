package fr.shabbattv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public final class PhilipsTvClient {
    private static final String SECRET_KEY = "ZmVay1EQVFOaZhwQ4Kv81ypLAZNczV9sG4KkseXWn1NEk6cXmPKO/MCa9sryslvLCFMnNe4Z4CPXzToowvhHvA==";
    private static final String FALLBACK_IP = "192.168.1.49";

    public static final class PairRequest {
        public String host;
        public String deviceId;
        public String authKey;
        public String timestamp;
    }

    private PhilipsTvClient() {}

    public static boolean isPaired(Context c) {
        SharedPreferences p = AppState.prefs(c);
        return !p.getString("philips_device_id", "").isEmpty()
                && !p.getString("philips_auth_key", "").isEmpty()
                && !p.getString("philips_host", "").isEmpty();
    }

    public static String pairedHost(Context c) {
        return AppState.prefs(c).getString("philips_host", "");
    }

    public static PairRequest beginPair(Context c) throws Exception {
        Exception last = null;
        String saved = AppState.prefs(c).getString("philips_host", "");
        String[] hosts = saved.isEmpty()
                ? new String[]{"127.0.0.1", FALLBACK_IP}
                : new String[]{saved, "127.0.0.1", FALLBACK_IP};

        for (String host : hosts) {
            if (host == null || host.isEmpty()) continue;
            try {
                PairRequest r = beginPairOnHost(c, host);
                AppState.prefs(c).edit()
                        .putString("philips_pair_host", r.host)
                        .putString("philips_pair_device_id", r.deviceId)
                        .putString("philips_pair_auth_key", r.authKey)
                        .putString("philips_pair_timestamp", r.timestamp)
                        .apply();
                return r;
            } catch (Exception e) {
                last = e;
            }
        }
        throw last != null ? last : new Exception("API Philips introuvable");
    }

    private static PairRequest beginPairOnHost(Context c, String host) throws Exception {
        String deviceId = AppState.prefs(c).getString("philips_device_id", "");
        if (deviceId.isEmpty()) deviceId = randomId(16);

        JSONObject device = device(deviceId);
        JSONObject body = new JSONObject();
        JSONArray scope = new JSONArray();
        scope.put("read").put("write").put("control");
        body.put("scope", scope);
        body.put("device", device);

        Response res = request("POST", "https://" + host + ":1926/6/pair/request", body.toString(), null, null);
        if (res.code < 200 || res.code >= 300) throw new Exception("pair/request HTTP " + res.code + " " + res.body);
        JSONObject j = new JSONObject(res.body);

        PairRequest out = new PairRequest();
        out.host = host;
        out.deviceId = deviceId;
        out.authKey = j.getString("auth_key");
        out.timestamp = String.valueOf(j.get("timestamp"));
        return out;
    }

    public static void grantPair(Context c, String pin) throws Exception {
        pin = pin == null ? "" : pin.trim();
        if (pin.isEmpty()) throw new Exception("PIN vide");
        SharedPreferences p = AppState.prefs(c);
        String host = p.getString("philips_pair_host", "");
        String deviceId = p.getString("philips_pair_device_id", "");
        String authKey = p.getString("philips_pair_auth_key", "");
        String timestamp = p.getString("philips_pair_timestamp", "");
        if (host.isEmpty() || deviceId.isEmpty() || authKey.isEmpty() || timestamp.isEmpty()) {
            throw new Exception("Démarre d’abord l’association");
        }

        JSONObject auth = new JSONObject();
        auth.put("auth_AppId", "1");
        auth.put("pin", pin);
        auth.put("auth_timestamp", timestamp);
        auth.put("auth_signature", signature(timestamp, pin));

        JSONObject body = new JSONObject();
        body.put("auth", auth);
        body.put("device", device(deviceId));

        Response res = request("POST", "https://" + host + ":1926/6/pair/grant", body.toString(), deviceId, authKey);
        if (res.code < 200 || res.code >= 300) throw new Exception("pair/grant HTTP " + res.code + " " + res.body);

        p.edit()
                .putString("philips_host", host)
                .putString("philips_device_id", deviceId)
                .putString("philips_auth_key", authKey)
                .remove("philips_pair_host")
                .remove("philips_pair_device_id")
                .remove("philips_pair_auth_key")
                .remove("philips_pair_timestamp")
                .apply();
        LogStore.add(c, "Philips", "Contrôle TV associé · hôte " + host);
    }

    public static String standby(Context c) throws Exception {
        SharedPreferences p = AppState.prefs(c);
        String host = p.getString("philips_host", "");
        String user = p.getString("philips_device_id", "");
        String pass = p.getString("philips_auth_key", "");
        if (host.isEmpty() || user.isEmpty() || pass.isEmpty()) throw new Exception("Contrôle Philips non associé");

        Response res = request("POST", "https://" + host + ":1926/6/input/key", "{\"key\":\"Standby\"}", user, pass);
        if (res.code < 200 || res.code >= 300) throw new Exception("Standby HTTP " + res.code + " " + res.body);
        return "Commande Standby Philips acceptée (HTTP " + res.code + ")";
    }

    private static JSONObject device(String id) throws Exception {
        JSONObject d = new JSONObject();
        d.put("device_name", "Shabbat TV");
        d.put("device_os", "Android");
        d.put("app_name", "Shabbat TV");
        d.put("type", "native");
        d.put("app_id", "fr.shabbattv");
        d.put("id", id);
        return d;
    }

    private static String signature(String timestamp, String pin) throws Exception {
        byte[] key = Base64.decode(SECRET_KEY, Base64.DEFAULT);
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] raw = mac.doFinal((timestamp + pin).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : raw) hex.append(String.format(Locale.US, "%02x", b & 0xff));
        return Base64.encodeToString(hex.toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
    }

    private static Response request(String method, String target, String body, String user, String pass) throws Exception {
        Response first = one(method, target, body, null);
        if (user == null || pass == null || first.code != 401) return first;
        if (first.challenge == null || !first.challenge.toLowerCase(Locale.US).startsWith("digest")) return first;
        String authorization = digestAuthorization(method, target, first.challenge, user, pass);
        return one(method, target, body, authorization);
    }

    private static Response one(String method, String target, String body, String authorization) throws Exception {
        HttpsURLConnection h = null;
        try {
            h = (HttpsURLConnection) new URL(target).openConnection();
            trustAll(h);
            h.setConnectTimeout(3500);
            h.setReadTimeout(4500);
            h.setRequestMethod(method);
            h.setRequestProperty("Accept", "application/json");
            h.setRequestProperty("Content-Type", "application/json");
            if (authorization != null) h.setRequestProperty("Authorization", authorization);
            if (body != null) {
                h.setDoOutput(true);
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                h.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = h.getOutputStream()) { os.write(bytes); }
            }
            Response r = new Response();
            r.code = h.getResponseCode();
            r.challenge = h.getHeaderField("WWW-Authenticate");
            InputStream in = r.code >= 400 ? h.getErrorStream() : h.getInputStream();
            r.body = in == null ? "" : read(in);
            return r;
        } finally {
            if (h != null) h.disconnect();
        }
    }

    private static String digestAuthorization(String method, String target, String challenge, String user, String pass) throws Exception {
        Map<String,String> m = parseDigest(challenge);
        String realm = m.get("realm");
        String nonce = m.get("nonce");
        if (realm == null || nonce == null) throw new Exception("Challenge Digest incomplet");
        String qop = m.get("qop");
        if (qop != null && qop.contains(",")) qop = qop.split(",")[0].trim();
        if (qop != null) qop = qop.replace("\"", "").trim();
        String opaque = m.get("opaque");
        URL u = new URL(target);
        String uri = u.getPath();
        if (u.getQuery() != null) uri += "?" + u.getQuery();
        String nc = "00000001";
        String cnonce = randomHex(16);
        String ha1 = md5(user + ":" + realm + ":" + pass);
        String ha2 = md5(method + ":" + uri);
        String response = (qop == null || qop.isEmpty())
                ? md5(ha1 + ":" + nonce + ":" + ha2)
                : md5(ha1 + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);

        StringBuilder a = new StringBuilder("Digest ");
        a.append("username=\"").append(user).append("\"");
        a.append(", realm=\"").append(realm).append("\"");
        a.append(", nonce=\"").append(nonce).append("\"");
        a.append(", uri=\"").append(uri).append("\"");
        a.append(", response=\"").append(response).append("\"");
        a.append(", algorithm=MD5");
        if (opaque != null && !opaque.isEmpty()) a.append(", opaque=\"").append(opaque).append("\"");
        if (qop != null && !qop.isEmpty()) {
            a.append(", qop=").append(qop);
            a.append(", nc=").append(nc);
            a.append(", cnonce=\"").append(cnonce).append("\"");
        }
        return a.toString();
    }

    private static Map<String,String> parseDigest(String challenge) {
        String s = challenge.substring(challenge.indexOf(' ') + 1);
        Map<String,String> out = new HashMap<>();
        boolean quoted = false;
        StringBuilder cur = new StringBuilder();
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (int i=0;i<s.length();i++) {
            char ch=s.charAt(i);
            if (ch=='\"') quoted=!quoted;
            if (ch==',' && !quoted) { parts.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        if (cur.length()>0) parts.add(cur.toString());
        for (String part:parts) {
            int eq=part.indexOf('=');
            if(eq<0) continue;
            String k=part.substring(0,eq).trim().toLowerCase(Locale.US);
            String v=part.substring(eq+1).trim();
            if(v.startsWith("\"")&&v.endsWith("\"")&&v.length()>=2) v=v.substring(1,v.length()-1);
            out.put(k,v);
        }
        return out;
    }

    private static String md5(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = md.digest(s.getBytes(StandardCharsets.ISO_8859_1));
        StringBuilder out = new StringBuilder();
        for (byte x : b) out.append(String.format(Locale.US, "%02x", x & 0xff));
        return out.toString();
    }

    private static String randomId(int n) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom r = new SecureRandom();
        StringBuilder b = new StringBuilder();
        for(int i=0;i<n;i++) b.append(chars.charAt(r.nextInt(chars.length())));
        return b.toString();
    }

    private static String randomHex(int n) {
        final String chars = "0123456789abcdef";
        SecureRandom r = new SecureRandom();
        StringBuilder b = new StringBuilder();
        for(int i=0;i<n;i++) b.append(chars.charAt(r.nextInt(chars.length())));
        return b.toString();
    }

    private static String read(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        String line;
        while((line=br.readLine())!=null) b.append(line);
        return b.toString();
    }

    private static void trustAll(HttpsURLConnection h) throws Exception {
        TrustManager[] trust = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers(){return new java.security.cert.X509Certificate[0];}
            public void checkClientTrusted(java.security.cert.X509Certificate[] c,String a){}
            public void checkServerTrusted(java.security.cert.X509Certificate[] c,String a){}
        }};
        SSLContext sc=SSLContext.getInstance("TLS");
        sc.init(null,trust,new SecureRandom());
        h.setSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv=(hostname,session)->true;
        h.setHostnameVerifier(hv);
    }

    private static final class Response {
        int code;
        String body;
        String challenge;
    }
}
