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
        Ui.header(root, this, "Automatisation", "Planning", "Programme les séances avant Chabbat. Ouvre une séance pour voir le réveil, le début, l’extinction prévue ou la supprimer.");

        LinearLayout movieCard = Ui.card(this);
        movieCard.addView(Ui.eyebrow(this,"Film sélectionné"));
        movieStatus = Ui.body(this,"—");
        movieStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        movieCard.addView(movieStatus, Ui.lp(-1,-2,this,5));
        root.addView(movieCard, Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        LinearLayout actionsCard = Ui.card(this);
        actionsCard.addView(Ui.eyebrow(this,"Nouvelle séance"));
        Button add = Ui.button(this,"Choisir la date et l’heure",true);
        add.setOnClickListener(v->pickDateTime());
        actionsCard.addView(add,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));
        Button quick = Ui.button(this,"Test · lancer le film dans 2 minutes",false);
        quick.setOnClickListener(v->scheduleAt(System.currentTimeMillis()+120_000L,true));
        actionsCard.addView(quick,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,7));
        feedback = Ui.muted(this,"");
        feedback.setMaxLines(4);
        actionsCard.addView(feedback,Ui.lp(-1,-2,this,7));
        root.addView(actionsCard,Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        LinearLayout listCard = Ui.card(this);
        listCard.addView(Ui.eyebrow(this,"Séances programmées"));
        emptyStatus = Ui.muted(this,"Aucune séance programmée.");
        listCard.addView(emptyStatus,Ui.lp(-1,-2,this,6));
        scheduleList = new LinearLayout(this);
        scheduleList.setOrientation(LinearLayout.VERTICAL);
        listCard.addView(scheduleList,Ui.lp(-1,-2,this,4));
        root.addView(listCard,Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        Ui.setScrollable(this, root);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (scheduleList != null) refresh();
    }

    private void refresh(){
        JSONObject m = AppState.selectedMovie(this);
        if(m==null){
            movieStatus.setText("Aucun film");
            movieStatus.setTextColor(Ui.MUTED);
        } else {
            String label = m.optString("title","Film");
            long duration = m.optLong("durationMs",0L);
            if (duration > 0) label += "  ·  " + formatDuration(duration);
            movieStatus.setText(label);
            movieStatus.setTextColor(Ui.TEXT);
        }

        JSONArray a = AppState.schedules(this); // also cleans sessions whose start time has passed
        scheduleList.removeAllViews();
        emptyStatus.setVisibility(a.length()==0 ? android.view.View.VISIBLE : android.view.View.GONE);

        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i);
            if(o==null) continue;
            long when=o.optLong("when");
            String title=o.optString("title","Film");
            String date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when));
            Button row=Ui.button(this,date+"  ·  "+title,false);
            row.setTypeface(null,android.graphics.Typeface.NORMAL);
            String id=o.optString("id","");
            row.setOnClickListener(v->{
                Intent detail=new Intent(this,ScheduleDetailActivity.class);
                detail.putExtra("schedule_id",id);
                startActivity(detail);
            });
            scheduleList.addView(row,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,6));
        }
    }

    private void pickDateTime(){
        if(AppState.selectedMovie(this)==null){
            feedback.setText("Sélectionne un film d’abord.");
            return;
        }
        Calendar now=Calendar.getInstance();
        new DatePickerDialog(this,(v,y,m,d)->{
            Calendar c=Calendar.getInstance();
            c.set(Calendar.YEAR,y);
            c.set(Calendar.MONTH,m);
            c.set(Calendar.DAY_OF_MONTH,d);
            new TimePickerDialog(this,(tv,h,min)->{
                c.set(Calendar.HOUR_OF_DAY,h);
                c.set(Calendar.MINUTE,min);
                c.set(Calendar.SECOND,0);
                c.set(Calendar.MILLISECOND,0);
                scheduleAt(c.getTimeInMillis(),false);
            },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();
        },now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void scheduleAt(long when, boolean test){
        JSONObject movie=AppState.selectedMovie(this);
        if(movie==null){ feedback.setText("Sélectionne un film d’abord."); return; }
        if (when <= System.currentTimeMillis()) { feedback.setText("Choisis une heure dans le futur."); return; }

        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !am.canScheduleExactAlarms()){
            feedback.setText("Autorise d’abord les alarmes exactes depuis l’accueil.");
            return;
        }

        String id=UUID.randomUUID().toString();
        int req=AppState.requestCodeForId(id);
        int volume=AppState.defaultVolume(this);
        long pre=when-AppState.preWakeMinutes(this)*60_000L;
        long duration=movie.optLong("durationMs",0L);
        long endAt=duration>0 ? when+duration : 0L;

        Intent play=new Intent(this,ScheduleReceiver.class);
        play.putExtra("movie",movie.toString());
        play.putExtra("volume",volume);
        play.putExtra("schedule_id",test ? "" : id);
        PendingIntent pplay=PendingIntent.getBroadcast(this,req,play,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pplay);
        else am.setExact(AlarmManager.RTC_WAKEUP,when,pplay);

        if(pre>System.currentTimeMillis()){
            Intent pi=new Intent(this,PreWakeReceiver.class);
            pi.putExtra("schedule_id",test ? "" : id);
            PendingIntent ppre=PendingIntent.getBroadcast(this,req+1,pi,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,pre,ppre);
            else am.setExact(AlarmManager.RTC_WAKEUP,pre,ppre);
        }

        if(!test){
            try{
                JSONArray a=AppState.schedules(this);
                JSONObject o=new JSONObject();
                o.put("id",id);
                o.put("when",when);
                o.put("wakeAt",pre);
                o.put("title",movie.optString("title"));
                o.put("durationMs",duration);
                o.put("endAt",endAt);
                o.put("volume",volume);
                o.put("server",AppState.prefs(this).getString("plex_server_name","Plex"));
                a.put(o);
                AppState.setSchedules(this,a);
                LogStore.add(this,"Planning","Séance ajoutée : "+movie.optString("title")+" à "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when)));
            }catch(Exception ignored){}
        } else {
            LogStore.add(this,"Test","Lecture programmée dans 2 minutes : "+movie.optString("title"));
        }

        String date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when));
        StringBuilder result=new StringBuilder(test?"Test armé ✓  ":"Séance ajoutée ✓  ");
        result.append(date);
        if(pre>System.currentTimeMillis()) result.append("\nPré-réveil à ").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(pre)));
        if(endAt>0) result.append(" · extinction prévue vers ").append(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(endAt)));
        feedback.setText(result.toString());
        refresh();
    }

    private String formatDuration(long ms){
        long mins=Math.max(1,ms/60000L);
        long h=mins/60;
        long m=mins%60;
        return h>0 ? h+" h "+(m<10?"0":"")+m : mins+" min";
    }
}
