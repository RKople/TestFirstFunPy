package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class PreWakeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String scheduleId=intent.getStringExtra("schedule_id");
        String movie=intent.getStringExtra("movie");
        int volume=intent.getIntExtra("volume",AppState.defaultVolume(context));
        long target=intent.getLongExtra("target_at",System.currentTimeMillis());
        boolean sleepWhenDone=intent.getBooleanExtra("sleep_when_done",true);

        AppState.prefs(context).edit().putLong("last_prewake_at",System.currentTimeMillis()).apply();
        LogStore.add(context,"Réveil","Pré-réveil reçu"+(scheduleId!=null&&scheduleId.startsWith("test-")?" (test)":"")+" · cible dans "+Math.max(0,(target-System.currentTimeMillis())/1000)+" s");

        try {
            PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
            if(pm!=null){
                PowerManager.WakeLock shortLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ShabbatTV:PreWakeReceiver");
                shortLock.acquire(30_000L);
            }
            Intent armed=new Intent(context,ArmedActivity.class);
            armed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            armed.putExtra("schedule_id",scheduleId);
            armed.putExtra("movie",movie);
            armed.putExtra("volume",volume);
            armed.putExtra("target_at",target);
            armed.putExtra("sleep_when_done",sleepWhenDone);
            context.startActivity(armed);
        } catch(Throwable t) {
            AppState.prefs(context).edit().putString("last_prewake_error",t.toString()).apply();
            LogStore.add(context,"Erreur","Pré-réveil : "+t);
        }
    }
}
