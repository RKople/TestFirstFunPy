package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class ScheduleActivity extends Activity {
    private TextView movieStatus, feedback, emptyStatus;
    private LinearLayout scheduleList;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Automatisation", "Planning", "Le pré-réveil allume réellement la TV sur un écran noir, garde Android éveillé, puis lance le film à l’heure cible.");

        LinearLayout movieCard = Ui.card(this);
        movieCard.addView(Ui.eyebrow(this,"Film sélectionné"));
        movieStatus = Ui.body(this,"—"); movieStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        movieCard.addView(movieStatus, Ui.lp(-1,-2,this,5));
        Button tracks=Ui.button(this,"Langue & sous-titres",false);
        tracks.setOnClickListener(v->{if(AppState.selectedMovie(this)!=null)startActivity(new Intent(this,MovieOptionsActivity.class));else feedback.setText("Sélectionne d’abord un film.");});
        movieCard.addView(tracks,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,8));
        root.addView(movieCard, Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        LinearLayout actionsCard = Ui.card(this);
        actionsCard.addView(Ui.eyebrow(this,"Nouvelle séance"));
        Button add = Ui.button(this,"Choisir la date et l’heure",true); add.setOnClickListener(v->pickDateTime());
        actionsCard.addView(add,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));
        Button quick = Ui.button(this,"Test · réveiller puis lancer dans 2 minutes",false); quick.setOnClickListener(v->scheduleAt(System.currentTimeMillis()+120_000L,true));
        actionsCard.addView(quick,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,7));
        feedback = Ui.muted(this,""); feedback.setMaxLines(5); actionsCard.addView(feedback,Ui.lp(-1,-2,this,7));
        root.addView(actionsCard,Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        LinearLayout listCard = Ui.card(this);
        listCard.addView(Ui.eyebrow(this,"Séances programmées"));
        emptyStatus = Ui.muted(this,"Aucune séance programmée."); listCard.addView(emptyStatus,Ui.lp(-1,-2,this,6));
        scheduleList = new LinearLayout(this); scheduleList.setOrientation(LinearLayout.VERTICAL); listCard.addView(scheduleList,Ui.lp(-1,-2,this,4));
        root.addView(listCard,Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        Ui.setScrollable(this, root); refresh();
    }

    @Override protected void onResume() { super.onResume(); if (scheduleList != null) refresh(); }

    private void refresh(){
        JSONObject m = AppState.selectedMovie(this);
        if(m==null){movieStatus.setText("Aucun film");movieStatus.setTextColor(Ui.MUTED);} else {
            StringBuilder label=new StringBuilder(m.optString("title","Film")); long duration=m.optLong("durationMs",0L); if(duration>0)label.append("  ·  ").append(formatDuration(duration));
            label.append("\nAudio : ").append(m.optString("audioLabel","Automatique"));
            label.append("  ·  Sous-titres : ").append(m.optString("subtitleLabel",m.optBoolean("subtitlesOff",true)?"Aucun":"Automatiques"));
            movieStatus.setText(label.toString()); movieStatus.setTextColor(Ui.TEXT);
        }

        JSONArray a = AppState.schedules(this); scheduleList.removeAllViews(); emptyStatus.setVisibility(a.length()==0?android.view.View.VISIBLE:android.view.View.GONE);
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i); if(o==null)continue; long when=o.optLong("when"); String title=o.optString("title","Film");
            String date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when)); Button row=Ui.button(this,date+"  ·  "+title,false); row.setTypeface(null,android.graphics.Typeface.NORMAL);
            String id=o.optString("id",""); row.setOnClickListener(v->{Intent detail=new Intent(this,ScheduleDetailActivity.class);detail.putExtra("schedule_id",id);startActivity(detail);});
            scheduleList.addView(row,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,6));
        }
    }

    private void pickDateTime(){
        if(AppState.selectedMovie(this)==null){feedback.setText("Sélectionne un film d’abord.");return;}
        Calendar now=Calendar.getInstance();
        new DatePickerDialog(this,(v,y,m,d)->{Calendar c=Calendar.getInstance();c.set(Calendar.YEAR,y);c.set(Calendar.MONTH,m);c.set(Calendar.DAY_OF_MONTH,d);
            new TimePickerDialog(this,(tv,h,min)->{c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,min);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);scheduleAt(c.getTimeInMillis(),false);},now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();
        },now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleAt(long when, boolean test){
        JSONObject movie=AppState.selectedMovie(this); if(movie==null){feedback.setText("Sélectionne un film d’abord.");return;}
        long now=System.currentTimeMillis(); if(when<=now+20_000L){feedback.setText("Choisis une heure au moins 30 secondes dans le futur.");return;}
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!am.canScheduleExactAlarms()){feedback.setText("Autorise d’abord les alarmes exactes depuis l’accueil.");return;}

        String id=(test?"test-":"")+UUID.randomUUID(); int req=AppState.requestCodeForId(id); int volume=AppState.defaultVolume(this);
        long desiredPre=when-AppState.preWakeMinutes(this)*60_000L;
        long pre=test?now+15_000L:Math.max(desiredPre,now+15_000L);
        if(pre>=when)pre=Math.max(now+5_000L,when-30_000L);
        long duration=movie.optLong("durationMs",0L),endAt=duration>0?when+duration:0L;
        boolean sleepWhenDone=!test;

        Intent play=new Intent(this,ScheduleReceiver.class); play.putExtra("movie",movie.toString());play.putExtra("volume",volume);play.putExtra("schedule_id",id);play.putExtra("sleep_when_done",sleepWhenDone);
        PendingIntent pplay=PendingIntent.getBroadcast(this,req,play,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pplay);else am.setExact(AlarmManager.RTC_WAKEUP,when,pplay);

        Intent pi=new Intent(this,PreWakeReceiver.class);pi.putExtra("schedule_id",id);pi.putExtra("movie",movie.toString());pi.putExtra("volume",volume);pi.putExtra("target_at",when);pi.putExtra("sleep_when_done",sleepWhenDone);
        PendingIntent ppre=PendingIntent.getBroadcast(this,req+1,pi,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,pre,ppre);else am.setExact(AlarmManager.RTC_WAKEUP,pre,ppre);

        if(!test){
            try{JSONArray a=AppState.schedules(this);JSONObject o=new JSONObject();o.put("id",id);o.put("when",when);o.put("wakeAt",pre);o.put("title",movie.optString("title"));o.put("durationMs",duration);o.put("endAt",endAt);o.put("volume",volume);o.put("server",AppState.prefs(this).getString("plex_server_name","Plex"));o.put("audioLabel",movie.optString("audioLabel","Automatique"));o.put("subtitleLabel",movie.optString("subtitleLabel",movie.optBoolean("subtitlesOff",true)?"Aucun":"Automatiques"));a.put(o);AppState.setSchedules(this,a);LogStore.add(this,"Planning","Séance ajoutée : "+movie.optString("title")+" à "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when)));}catch(Exception ignored){}
        } else LogStore.add(this,"Test","Réveil dans 15 s puis lecture à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+" : "+movie.optString("title"));

        StringBuilder result=new StringBuilder(test?"Test armé ✓":"Séance ajoutée ✓");
        result.append("\nTV réveillée en écran noir : ").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(pre)));
        result.append("\nFilm : ").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(when)));
        if(endAt>0&&!test)result.append(" · veille vers ").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(endAt)));
        feedback.setText(result.toString()); refresh();
    }

    private String formatDuration(long ms){long mins=Math.max(1,ms/60000L),h=mins/60,m=mins%60;return h>0?h+" h "+(m<10?"0":"")+m:mins+" min";}
}
