package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Proven Philips wake path. This is intentionally the SAME sequence used by the
 * standalone "Wake-up · 2 min" test, because that path is validated on the OLED810.
 * Scheduled sessions only change what is opened AFTER the TV has been woken.
 */
public class WakeReceiver extends BroadcastReceiver {
    private static final String TV_IP = "192.168.1.49";
    private static final String TV_MAC = "b8:d8:2d:4b:5d:d8";

    @Override public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final SharedPreferences diag = context.getSharedPreferences("wake_diag", Context.MODE_PRIVATE);
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final boolean interactive = pm != null && pm.isInteractive();
        final long receivedAt = System.currentTimeMillis();

        final String mode = intent.getStringExtra("mode"); // null/test | waiting | play
        final String scheduleId = intent.getStringExtra("schedule_id");
        final String movie = intent.getStringExtra("movie");
        final int volume = intent.getIntExtra("volume", AppState.defaultVolume(context));
        final long targetAt = intent.getLongExtra("target_at", receivedAt);
        final long expectedAt = intent.getLongExtra("expected_at", 0L);
        final boolean sleepWhenDone = intent.getBooleanExtra("sleep_when_done", true);

        long delay = expectedAt > 0L ? Math.max(0L, receivedAt - expectedAt) : 0L;
        if (expectedAt > 0L) {
            AppState.prefs(context).edit().putLong("last_wake_delay_ms", delay).apply();
        }

        diag.edit()
                .putLong("receiver_at", receivedAt)
                .putBoolean("receiver_interactive", interactive)
                .putString("diag_log", "Alarme reçue. Tentatives de réveil en cours…\n")
                .commit();

        String kind = "waiting".equals(mode) ? "Réveil séance" : "play".equals(mode) ? "Réveil secours film" : "Wake-up test";
        LogStore.add(context, "Réveil", kind + " reçu" + (expectedAt > 0L ? " · retard " + (delay/1000L) + " s" : ""));

        final PowerManager.WakeLock wl;
        if (pm != null) {
            PowerManager.WakeLock tmp = null;
            try {
                tmp = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE,
                        "ShabbatTV:PhilipsWake"
                );
                long hold = "waiting".equals(mode)
                        ? Math.max(90_000L, Math.min(15 * 60_000L, targetAt - receivedAt + 180_000L))
                        : 90_000L;
                tmp.acquire(hold);
            } catch (Throwable t) {
                LogStore.add(context, "Erreur", "WakeLock Philips : " + t.getClass().getSimpleName());
            }
            wl = tmp;
        } else {
            wl = null;
        }

        new Thread(() -> {
            StringBuilder log = new StringBuilder();
            try {
                // Keep this sequence aligned with the standalone wake test.
                log.append("1) WakeLock écran : demandé ✅\n");
                log.append("2) PowerManager.wakeUp(reflection) : ").append(tryReflectionWake(pm)).append("\n");
                log.append("3) input KEYCODE_WAKEUP : ").append(runShell("input keyevent 224")).append("\n");
                log.append("4) cmd power wakeup : ").append(runShell("cmd power wakeup")).append("\n");
                log.append("5) WoWLAN vers ").append(TV_MAC).append(" : ").append(sendWol()).append("\n");
                log.append("6) Philips HTTP localhost 1925 /6 input PowerOn : ")
                        .append(post("http://127.0.0.1:1925/6/input/key", "{\"key\":\"PowerOn\"}")).append("\n");
                log.append("7) Philips HTTPS localhost 1926 /6 input PowerOn : ")
                        .append(post("https://127.0.0.1:1926/6/input/key", "{\"key\":\"PowerOn\"}")).append("\n");
                log.append("8) Philips HTTPS IP /6 input PowerOn : ")
                        .append(post("https://" + TV_IP + ":1926/6/input/key", "{\"key\":\"PowerOn\"}")).append("\n");
                log.append("9) Philips /6 powerstate On : ")
                        .append(post("https://" + TV_IP + ":1926/6/powerstate", "{\"powerstate\":\"On\"}")).append("\n");
                log.append("10) Philips /6 screenstate On : ")
                        .append(post("https://" + TV_IP + ":1926/6/screenstate", "{\"screenstate\":\"On\"}")).append("\n");
            } catch (Throwable t) {
                log.append("Erreur générale : ").append(t.getClass().getSimpleName()).append(": ").append(safe(t.getMessage())).append("\n");
            }

            diag.edit().putString("diag_log", log.toString()).commit();

            try {
                long now = System.currentTimeMillis();
                if ("waiting".equals(mode)) {
                    if (targetAt <= now) {
                        LogStore.add(context, "Planning", "Réveil arrivé après l’heure cible · lancement immédiat");
                        PlaybackLauncher.launch(context, movie, volume, scheduleId, sleepWhenDone);
                    } else {
                        Intent show = new Intent(context, WaitingActivity.class);
                        show.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        show.putExtra("schedule_id", scheduleId);
                        show.putExtra("movie", movie);
                        show.putExtra("volume", volume);
                        show.putExtra("target_at", targetAt);
                        show.putExtra("sleep_when_done", sleepWhenDone);
                        context.startActivity(show);
                    }
                } else if ("play".equals(mode)) {
                    PlaybackLauncher.launch(context, movie, volume, scheduleId, sleepWhenDone);
                } else {
                    Intent show = new Intent(context, PlaybackActivity.class);
                    show.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(show);
                }
            } catch (Throwable t) {
                diag.edit().putString("diag_log", log + "Lancement après réveil : " + t.getClass().getSimpleName() + "\n").commit();
                LogStore.add(context, "Erreur", "Après réveil : " + t.getClass().getSimpleName());
            }

            try { Thread.sleep(2500L); } catch (InterruptedException ignored) {}
            try { if (wl != null && wl.isHeld()) wl.release(); } catch (Throwable ignored) {}
            pending.finish();
        }, "ShabbatWakeWorker").start();
    }

    private String tryReflectionWake(PowerManager pm) {
        if (pm == null) return "PowerManager absent";
        try {
            Method m = PowerManager.class.getDeclaredMethod("wakeUp", long.class);
            m.setAccessible(true);
            m.invoke(pm, android.os.SystemClock.uptimeMillis());
            return "appel accepté";
        } catch (Throwable first) {
            try {
                Method m = PowerManager.class.getDeclaredMethod("wakeUp", long.class, int.class, String.class);
                m.setAccessible(true);
                m.invoke(pm, android.os.SystemClock.uptimeMillis(), 0, "ShabbatTV");
                return "appel 3 paramètres accepté";
            } catch (Throwable second) {
                return "refusé (" + second.getClass().getSimpleName() + ")";
            }
        }
    }

    private String runShell(String cmd) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd + " 2>&1"});
            String out = readStream(p.getInputStream());
            int exit = p.waitFor();
            if (out.length() > 80) out = out.substring(0, 80);
            return "exit=" + exit + (out.isEmpty() ? "" : " " + out.replace('\n', ' '));
        } catch (Throwable t) {
            return "erreur " + t.getClass().getSimpleName();
        } finally {
            if (p != null) p.destroy();
        }
    }

    private String sendWol() {
        try {
            String[] hex = TV_MAC.split(":");
            byte[] mac = new byte[6];
            for (int i = 0; i < 6; i++) mac[i] = (byte) Integer.parseInt(hex[i], 16);
            byte[] data = new byte[6 + 16 * 6];
            for (int i = 0; i < 6; i++) data[i] = (byte) 0xFF;
            for (int i = 6; i < data.length; i += 6) System.arraycopy(mac, 0, data, i, 6);
            InetAddress broadcast = InetAddress.getByName("192.168.1.255");
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                for (int i = 0; i < 5; i++) {
                    socket.send(new DatagramPacket(data, data.length, broadcast, 9));
                    Thread.sleep(120L);
                }
            }
            return "5 paquets envoyés";
        } catch (Throwable t) {
            return "erreur " + t.getClass().getSimpleName();
        }
    }

    private String post(String target, String body) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(target);
            c = (HttpURLConnection) url.openConnection();
            if (c instanceof HttpsURLConnection) trustAll((HttpsURLConnection) c);
            c.setConnectTimeout(1400);
            c.setReadTimeout(1400);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = c.getOutputStream()) { os.write(bytes); }
            int code = c.getResponseCode();
            String text = "";
            InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
            if (is != null) text = readStream(is).replace('\n', ' ');
            if (text.length() > 55) text = text.substring(0, 55);
            return "HTTP " + code + (text.isEmpty() ? "" : " " + text);
        } catch (Throwable t) {
            return "erreur " + t.getClass().getSimpleName();
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private void trustAll(HttpsURLConnection c) throws Exception {
        TrustManager[] trust = new TrustManager[]{new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        }};
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trust, new java.security.SecureRandom());
        c.setSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = (hostname, session) -> true;
        c.setHostnameVerifier(hv);
    }

    private String readStream(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        return sb.toString().trim();
    }

    private String safe(String s) { return s == null ? "" : s; }
}
