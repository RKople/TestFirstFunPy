package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, android.content.Intent intent) {
        String movie=intent.getStringExtra("movie");
        String scheduleId=intent.getStringExtra("schedule_id");
        int volume=intent.getIntExtra("volume",AppState.defaultVolume(context));
        boolean sleepWhenDone=intent.getBooleanExtra("sleep_when_done",true);
        AppState.prefs(context).edit().putLong("last_schedule_fire_at",System.currentTimeMillis()).apply();
        LogStore.add(context,"Planning","Alarme cible reçue"+(scheduleId!=null&&scheduleId.startsWith("test-")?" (test)":""));
        try {
            PlaybackLauncher.launch(context,movie,volume,scheduleId,sleepWhenDone);
        } catch(Throwable t) {
            AppState.prefs(context).edit().putString("last_schedule_error",t.toString()).apply();
            LogStore.add(context,"Erreur","Impossible de lancer la séance : "+t);
        }
    }
}
