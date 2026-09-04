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
        prefs = getSharedPreferences("diag", MODE_PRIVATE);
        buildUi();
        showPersistentDiagnostics();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != null) showPersistentDiagnostics();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(72, 48, 72, 48);
        root.setBackgroundColor(Color.rgb(8, 12, 20));

        TextView title = new TextView(this);
        title.setText("SHABBAT TV v0.3");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Diagnostic persistant de veille Philips OLED810");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(18f);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.setMargins(0, 14, 0, 36);
        root.addView(subtitle, subLp);

        Button test = new Button(this);
        test.setText("PROGRAMMER UN TEST DANS 2 MINUTES");
        test.setTextSize(18f);
        test.setFocusable(true);
        test.setOnClickListener(v -> scheduleTest(2));
        root.addView(test, new LinearLayout.LayoutParams(760, 90));

        Button permission = new Button(this);
        permission.setText("AUTORISER LES ALARMES EXACTES");
        permission.setTextSize(16f);
        permission.setFocusable(true);
        permission.setOnClickListener(v -> requestExactAlarmPermission());
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(760, 80);
        pLp.setMargins(0, 22, 0, 0);
        root.addView(permission, pLp);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(18f);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, -2);
        sLp.setMargins(0, 36, 0, 0);
        root.addView(status, sLp);

        setContentView(root);
    }

    private void scheduleTest(int minutes) {
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms()) {
            status.setText("Autorisation requise : autorise Shabbat TV dans 'Alarmes et rappels', puis reviens.");
            requestExactAlarmPermission();
            return;
        }

        long when = System.currentTimeMillis() + minutes * 60_000L;
        prefs.edit()
                .putLong("scheduled_at", when)
                .remove("receiver_at")
                .remove("receiver_interactive")
                .remove("playback_at")
                .remove("start_error")
                .apply();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("fr.shabbattv.TEST_ALARM");
        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                3003,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        }

        String time = DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when));
        status.setText("TEST ARMÉ ✅\nAlarme prévue à " + time + ".\nÉteins maintenant la TV. Attends 5 minutes puis rallume-la manuellement et rouvre Shabbat TV.");
    }

    private void showPersistentDiagnostics() {
        long scheduled = prefs.getLong("scheduled_at", 0L);
        long receiver = prefs.getLong("receiver_at", 0L);
        long playback = prefs.getLong("playback_at", 0L);
        boolean interactive = prefs.getBoolean("receiver_interactive", false);
        String error = prefs.getString("start_error", "");

        if (scheduled == 0L) {
            status.setText("Aucun test enregistré.\nProgramme un test, éteins la TV, attends 5 minutes, puis rallume-la.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Dernier test prévu : ")
                .append(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(scheduled)))
                .append("\n\n");

        if (receiver > 0L) {
            sb.append("ALARME REÇUE ✅ à ")
                    .append(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(receiver)))
                    .append("\nÉcran interactif à cet instant : ")
                    .append(interactive ? "OUI" : "NON")
                    .append("\n");
        } else {
            sb.append("ALARME NON REÇUE ❌ (aucune trace persistante)\n");
        }

        if (playback > 0L) {
            sb.append("Écran de test lancé ✅ à ")
                    .append(DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(playback)))
                    .append("\n");
        } else {
            sb.append("Écran de test non confirmé\n");
        }

        if (error != null && !error.isEmpty()) {
            sb.append("Erreur lancement : ").append(error).append("\n");
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
