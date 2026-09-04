package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;

public class ScheduleDetailActivity extends Activity {
    private String scheduleId;
    private JSONObject schedule;
    private TextView info;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        scheduleId = getIntent().getStringExtra("schedule_id");
        schedule = AppState.scheduleById(this, scheduleId);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Planning", "Détail de la séance", "Réveil, lecture Plex et extinction prévus pour cette séance.");

        LinearLayout card = Ui.card(this); card.addView(Ui.eyebrow(this,"Séance programmée"));
        TextView title = Ui.title(this, schedule == null ? "Séance introuvable" : schedule.optString("title","Film")); title.setTextSize(Ui.compact(this) ? 23 : 28);
        card.addView(title, Ui.lp(-1,-2,this,6)); info = Ui.muted(this, ""); info.setTextSize(Ui.compact(this) ? 13 : 15); card.addView(info, Ui.lp(-1,-2,this,10));
        root.addView(card, Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        Button delete = Ui.button(this,"Supprimer cette séance",false); delete.setOnClickListener(v -> deleteSchedule()); root.addView(delete,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,12));
        Button back = Ui.button(this,"Retour au planning",true); back.setOnClickListener(v -> finish()); root.addView(back,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));
        Ui.setScrollable(this,root); render();
    }

    private void render() {
        if (schedule == null) { info.setText("Cette séance n’existe plus. Elle a peut-être déjà été jouée ou supprimée."); return; }
        long when=schedule.optLong("when",0L),wakeAt=schedule.optLong("wakeAt",when-AppState.preWakeMinutes(this)*60_000L),duration=schedule.optLong("durationMs",0L),endAt=schedule.optLong("endAt",duration>0?when+duration:0L);
        int volume=schedule.optInt("volume",AppState.defaultVolume(this));
        String server=schedule.optString("server",AppState.prefs(this).getString("plex_server_name","Plex"));
        String audio=schedule.optString("audioLabel","Automatique"),subs=schedule.optString("subtitleLabel","Aucun");
        DateFormat d=DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.SHORT),t=DateFormat.getTimeInstance(DateFormat.SHORT);
        StringBuilder s=new StringBuilder();
        s.append("Date  ·  ").append(d.format(new Date(when))).append("\n\n");
        s.append("TV réveillée  ·  ").append(t.format(new Date(wakeAt))).append("\n");
        s.append("  La dalle est allumée sur un écran noir afin de garder Android éveillé et fiabiliser l’heure de départ.\n\n");
        s.append("Début du film  ·  ").append(t.format(new Date(when))).append("\n");
        if(endAt>0){s.append("Extinction estimée  ·  vers ").append(t.format(new Date(endAt))).append("\n");s.append("  La commande de veille réelle part à la fin effective du film.\n");}
        else s.append("Extinction  ·  à la fin réelle du film\n");
        s.append("\nAudio  ·  ").append(audio).append("\nSous-titres  ·  ").append(subs);
        s.append("\nVolume  ·  ").append(volume).append(" %\nServeur Plex  ·  ").append(server);
        if(duration>0)s.append("\nDurée Plex  ·  ").append(formatDuration(duration));
        info.setText(s.toString());
    }

    private String formatDuration(long ms){long mins=Math.max(1,ms/60000L),h=mins/60,m=mins%60;return h>0?h+" h "+(m<10?"0":"")+m:mins+" min";}

    private void deleteSchedule() {
        if(schedule==null||scheduleId==null){finish();return;}
        try{AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);int req=AppState.requestCodeForId(scheduleId);
            Intent play=new Intent(this,ScheduleReceiver.class);PendingIntent pplay=PendingIntent.getBroadcast(this,req,play,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pplay);pplay.cancel();
            Intent pre=new Intent(this,PreWakeReceiver.class);PendingIntent ppre=PendingIntent.getBroadcast(this,req+1,pre,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(ppre);ppre.cancel();
        }catch(Throwable ignored){}
        String title=schedule.optString("title","Film");AppState.removeSchedule(this,scheduleId);LogStore.add(this,"Planning","Séance supprimée : "+title);finish();
    }
}
