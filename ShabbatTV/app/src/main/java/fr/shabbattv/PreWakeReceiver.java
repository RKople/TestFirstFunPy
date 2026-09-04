package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Compatibility receiver for alarms created by v1.4/v1.5.
 * New schedules use RobustWakeReceiver directly, but old pending alarms are upgraded
 * on delivery instead of being lost after an app update.
 */
public class PreWakeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        LogStore.add(context,"Réveil","Ancien pré-réveil reçu · transfert vers le moteur robuste");
        Intent robust = new Intent(context, RobustWakeReceiver.class);
        robust.putExtra("phase","pre");
        robust.putExtra("schedule_id",intent.getStringExtra("schedule_id"));
        robust.putExtra("movie",intent.getStringExtra("movie"));
        robust.putExtra("volume",intent.getIntExtra("volume",AppState.defaultVolume(context)));
        robust.putExtra("target_at",intent.getLongExtra("target_at",System.currentTimeMillis()));
        robust.putExtra("expected_at",System.currentTimeMillis());
        robust.putExtra("sleep_when_done",intent.getBooleanExtra("sleep_when_done",true));
        context.sendBroadcast(robust);
    }
}
