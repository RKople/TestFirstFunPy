package fr.shabbattv;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Critical alarms used by Shabbat TV. AlarmClock is deliberately preferred on TV:
 * it is the strongest public AlarmManager primitive and is not deferred by Doze.
 */
public final class AlarmTools {
    private AlarmTools() {}

    public static void setCritical(Context c, AlarmManager am, long when, PendingIntent operation, int showRequestCode) {
        try {
            Intent showIntent = new Intent(c, MainActivity.class);
            showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent show = PendingIntent.getActivity(
                    c,
                    showRequestCode,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), operation);
            return;
        } catch (Throwable t) {
            LogStore.add(c, "Alarme", "setAlarmClock indisponible, fallback exact : " + t.getClass().getSimpleName());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, when, operation);
        }
    }

    public static void cancel(AlarmManager am, PendingIntent pi) {
        try { am.cancel(pi); } catch (Throwable ignored) {}
        try { pi.cancel(); } catch (Throwable ignored) {}
    }
}
