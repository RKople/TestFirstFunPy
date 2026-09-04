package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private TextView status;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("wake_diag", MODE_PRIVATE);
        buildUi();
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) refreshStatus();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 42, 64, 42);
        root.setBackgroundColor(Color.rgb(7, 11, 18));

        TextView title = new TextView(this);
        title.setText("SHABBAT TV v0.4");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Philips Wake Test — OLED810");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(19f);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, 12, 0, 30);
        root.addView(subtitle, subLp);

        Button test = new Button(this);
        test.setText("PROGRAMMER LE TEST DANS 2 MINUTES");
        test.setTextSize(18f);
        test.setFocusable(true);
        test.setOnClickListener(v -> scheduleWakeTest());
        root.addView(test, new LinearLayout.LayoutParams(820, 92));

        Button permission = new Button(this);
        permission.setText("AUTORISER LES ALARMES EXACTES");
        permission.setTextSize(16f);
        permission.setFocusable(true);
        permission.setOnClickListener(v -> requestExactAlarmPermission());
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(820, 82);
        pLp.setMargins(0, 18, 0, 0);
        root.addView(permission, pLp);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(17f);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, -2);
        sLp.setMargins(0, 30, 0, 0);
        root.addView(status, sLp);

        setContentView(root);
    }

    private void scheduleWakeTest() {
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            requestExactAlarmPermission();
            return;
        }

        long when = System.currentTimeMillis() + 120_000L;
        prefs.edit()
                .putLong("scheduled_at", when)
                .putString("diag_log", "")
                .remove("receiver_at")
                .remove("receiver_interactive")
                .remove("result_activity_at")
                .apply();

        Intent i = new Intent(this, WakeReceiver.class);
        i.setAction("fr.shabbattv.WAKE_TEST");
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                404,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        }

        String time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when));
        status.setText("TEST ARMÉ ✅\nDéclenchement prévu à " + time + ".\nÉteins la TV maintenant.\nSi elle ne se rallume pas, attends 5 minutes puis rallume-la manuellement et rouvre Shabbat TV.");
    }

    private void refreshStatus() {
        long scheduled = prefs.getLong("scheduled_at", 0L);
        long receiver = prefs.getLong("receiver_at", 0L);
        long result = prefs.getLong("result_activity_at", 0L);
        boolean interactive = prefs.getBoolean("receiver_interactive", false);
        String log = prefs.getString("diag_log", "");

        if (scheduled == 0L) {
            status.setText("1. Autorise les alarmes exactes.\n2. Programme le test.\n3. Éteins la TV avec la télécommande.\n\nLa v0.4 essaiera plusieurs méthodes de réveil Philips et gardera un journal persistant.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Dernier test prévu : ")
                .append(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(scheduled)))
                .append("\n");

        if (receiver > 0L) {
            sb.append("ALARME REÇUE ✅ : ")
                    .append(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(receiver)))
                    .append("\nÉcran Android interactif au départ : ")
                    .append(interactive ? "OUI" : "NON")
                    .append("\n");
        } else {
            sb.append("ALARME NON REÇUE ❌\n");
        }

        if (result > 0L) {
            sb.append("Activité résultat lancée ✅ : ")
                    .append(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(result)))
                    .append("\n");
        }

        if (log != null && !log.isEmpty()) {
            sb.append("\nDIAGNOSTIC :\n").append(log);
        }
        status.setText(sb.toString());
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        }
    }
}
