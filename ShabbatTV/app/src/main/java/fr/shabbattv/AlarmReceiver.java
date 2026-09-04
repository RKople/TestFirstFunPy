package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("diag", Context.MODE_PRIVATE);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean interactive = pm != null && pm.isInteractive();
        long now = System.currentTimeMillis();
        prefs.edit()
                .putLong("receiver_at", now)
                .putBoolean("receiver_interactive", interactive)
                .apply();

        try {
            Intent launch = new Intent(context, PlaybackActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launch);
        } catch (Exception e) {
            prefs.edit().putString("start_error", e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage())).apply();
        }
    }
}
