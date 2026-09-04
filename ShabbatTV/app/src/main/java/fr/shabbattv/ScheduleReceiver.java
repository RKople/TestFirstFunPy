package fr.shabbattv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences p=AppState.prefs(context);
        String movie=intent.getStringExtra("movie");
        String scheduleId=intent.getStringExtra("schedule_id");
        int volume=intent.getIntExtra("volume",AppState.defaultVolume(context));
        p.edit().putLong("last_schedule_fire_at",System.currentTimeMillis()).apply();

        if(scheduleId!=null && !scheduleId.isEmpty()) {
            AppState.removeSchedule(context,scheduleId);
        }
        LogStore.add(context,"Planning","Déclenchement de la séance"+(scheduleId==null||scheduleId.isEmpty()?" de test":""));

        try{
            Intent play=new Intent(context,PlayerActivity.class);
            play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            play.putExtra("movie",movie);
            play.putExtra("volume",volume);
            play.putExtra("sleep_when_done",true);
            context.startActivity(play);
        }catch(Throwable t){
            p.edit().putString("last_schedule_error",t.toString()).apply();
            LogStore.add(context,"Erreur","Impossible de lancer la séance : "+t);
        }
    }
}
