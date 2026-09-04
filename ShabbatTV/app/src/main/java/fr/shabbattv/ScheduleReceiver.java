package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Compatibility receiver for target alarms created by v1.4/v1.5. */
public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String movie=intent.getStringExtra("movie");
        String scheduleId=intent.getStringExtra("schedule_id");
        int volume=intent.getIntExtra("volume",AppState.defaultVolume(context));
        boolean sleepWhenDone=intent.getBooleanExtra("sleep_when_done",true);
        AppState.prefs(context).edit().putLong("last_schedule_fire_at",System.currentTimeMillis()).apply();
        LogStore.add(context,"Planning","Ancienne alarme cible reçue · transfert vers réveil robuste");

        Intent robust = new Intent(context, RobustWakeReceiver.class);
        robust.putExtra("phase","target");
        robust.putExtra("schedule_id",scheduleId);
        robust.putExtra("movie",movie);
        robust.putExtra("volume",volume);
        robust.putExtra("target_at",System.currentTimeMillis());
        robust.putExtra("expected_at",System.currentTimeMillis());
        robust.putExtra("sleep_when_done",sleepWhenDone);
        context.sendBroadcast(robust);
    }
}
