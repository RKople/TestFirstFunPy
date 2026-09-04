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
        Ui.header(root, this, "Planning", "Détail de la séance", "Toutes les étapes prévues pour cette séance, du réveil de la Philips jusqu’à la fin du film.");

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
        long when=schedule.optLong("when",0L);
        long wakeAt=schedule.optLong("wakeAt",when-AppState.preWakeMinutes(this)*60_000L);
        long retryAt=schedule.optLong("retryAt",0L);
        long visibleEstimate=schedule.optLong("visibleEstimateAt",wakeAt+3*60_000L);
        long duration=schedule.optLong("durationMs",0L),endAt=schedule.optLong("endAt",duration>0?when+duration:0L);
        int volume=schedule.optInt("volume",AppState.defaultVolume(this));
        String server=schedule.optString("server",AppState.prefs(this).getString("plex_server_name","Plex"));
        String audio=schedule.optString("audioLabel","Automatique"),subs=schedule.optString("subtitleLabel","Aucun");
        DateFormat d=DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.SHORT),t=DateFormat.getTimeInstance(DateFormat.SHORT);
        StringBuilder s=new StringBuilder();
        s.append("Date  ·  ").append(d.format(new Date(when))).append("\n\n");
        s.append("Commande de réveil  ·  ").append(t.format(new Date(wakeAt))).append("\n");
        s.append("  Même mécanisme que le test Wake-up validé.\n");
        s.append("Allumage estimé  ·  vers ").append(t.format(new Date(visibleEstimate))).append("\n");
        s.append("  L’heure réelle peut varier de quelques minutes selon la veille Philips.\n");
        if(retryAt>0L)s.append("Réveil de secours  ·  ").append(t.format(new Date(retryAt))).append("\n");
        s.append("\nÉcran d’attente  ·  dès que la TV s’allume\n");
        s.append("  Affiche le film et un compte à rebours jusqu’au démarrage.\n\n");
        s.append("Début du film  ·  ").append(t.format(new Date(when))).append("\n");
        s.append("  Lancement Plex avec la langue et les sous-titres choisis.\n\n");
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
        try{
            AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);

            // v1.7: proven WakeReceiver primary/retry/target + direct playback target.
            cancelBroadcast(am, WakeReceiver.class, scheduleId+":wake");
            cancelBroadcast(am, WakeReceiver.class, scheduleId+":retry");
            cancelBroadcast(am, WakeReceiver.class, scheduleId+":target-wake");
            cancelBroadcast(am, ScheduleReceiver.class, scheduleId+":direct");

            // v1.6 compatibility.
            cancelBroadcast(am, RobustWakeReceiver.class, scheduleId+":pre");
            cancelBroadcast(am, RobustWakeReceiver.class, scheduleId+":target");

            // v1.4/v1.5 compatibility.
            int oldReq=AppState.requestCodeForId(scheduleId);
            Intent oldPlay=new Intent(this,ScheduleReceiver.class);
            PendingIntent oldPplay=PendingIntent.getBroadcast(this,oldReq,oldPlay,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmTools.cancel(am,oldPplay);
            Intent oldPre=new Intent(this,PreWakeReceiver.class);
            PendingIntent oldPpre=PendingIntent.getBroadcast(this,oldReq+1,oldPre,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmTools.cancel(am,oldPpre);
        }catch(Throwable ignored){}
        String title=schedule.optString("title","Film");AppState.removeSchedule(this,scheduleId);LogStore.add(this,"Planning","Séance supprimée : "+title);finish();
    }

    private void cancelBroadcast(AlarmManager am,Class<?> cls,String requestKey){
        Intent i=new Intent(this,cls);
        PendingIntent pi=PendingIntent.getBroadcast(this,AppState.requestCodeForId(requestKey),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmTools.cancel(am,pi);
    }
}
