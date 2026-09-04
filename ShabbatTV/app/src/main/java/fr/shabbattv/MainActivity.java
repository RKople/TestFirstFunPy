package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(72, 48, 72, 48);
        root.setBackgroundColor(Color.rgb(8, 12, 20));

        TextView title = new TextView(this);
        title.setText("SHABBAT TV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView subtitle = new TextView(this);
        subtitle.setText("Prototype Philips OLED810 — test de réveil automatique");
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
        status.setText("1. Autorise les alarmes exactes si Android le demande.\n2. Programme le test.\n3. Éteins la TV avec la télécommande.\n4. Dans 2 minutes, l'app tentera de réveiller l'écran.");
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
            status.setText("Autorisation requise : ouvre 'Alarmes et rappels', autorise Shabbat TV, puis reviens ici.");
            requestExactAlarmPermission();
            return;
        }

        long when = System.currentTimeMillis() + minutes * 60_000L;
        Intent intent = new Intent(this, PlaybackActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            alarm.setExact(AlarmManager.RTC_WAKEUP, when, pi);
        }

        String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(when));
        status.setText("TEST ARMÉ ✅\nDéclenchement prévu à " + time + ".\nÉteins maintenant la TV avec la télécommande et n'y touche plus.");
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
        } else {
            status.setText("Cette version d'Android ne nécessite pas cette autorisation.");
        }
    }
}
