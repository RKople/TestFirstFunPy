package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class PreWakeReceiver extends BroadcastReceiver {
    private static PowerManager.WakeLock lock;
    @Override public void onReceive(Context context, Intent intent) {
        String scheduleId=intent.getStringExtra("schedule_id");
        PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
        if(pm!=null){
            try{
                if(lock!=null&&lock.isHeld()) lock.release();
                lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ShabbatTV:PreWake");
                lock.acquire(12*60_000L);
                AppState.prefs(context).edit().putLong("last_prewake_at",System.currentTimeMillis()).apply();
                LogStore.add(context,"Réveil","Pré-réveil reçu"+(scheduleId==null||scheduleId.isEmpty()?" (test)":""));
            }catch(Throwable t){
                AppState.prefs(context).edit().putString("last_prewake_error",t.toString()).apply();
                LogStore.add(context,"Erreur","Pré-réveil : "+t);
            }
        }
    }
}
