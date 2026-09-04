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
    private TextView movieStatus, scheduleStatus, feedback;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Automatisation", "Planning", "Programme les séances avant Chabbat. Le pré-réveil se déclenche automatiquement avant chaque film.");

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
        scheduleStatus = Ui.muted(this,"Aucune séance programmée.");
        listCard.addView(scheduleStatus,Ui.lp(-1,-2,this,6));
        root.addView(listCard,Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        Ui.setScrollable(this, root);
        refresh();
    }

    private void refresh(){
        JSONObject m = AppState.selectedMovie(this);
        if(m==null){
            movieStatus.setText("Aucun film");
            movieStatus.setTextColor(Ui.MUTED);
            scheduleStatus.setText("Sélectionne d’abord un film dans Films.");
            return;
        }
        movieStatus.setText(m.optString("title","Film"));
        movieStatus.setTextColor(Ui.TEXT);
        JSONArray a = AppState.schedules(this);
        if(a.length()==0){
            scheduleStatus.setText("Aucune séance programmée.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<a.length();i++){
            JSONObject o=a.optJSONObject(i);
            if(o!=null){
                if(sb.length()>0) sb.append("\n");
                sb.append("•  ")
                        .append(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(o.optLong("when"))))
                        .append("  ·  ")
                        .append(o.optString("title"));
            }
        }
        scheduleStatus.setText(sb.toString());
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
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !am.canScheduleExactAlarms()){
            feedback.setText("Autorise d’abord les alarmes exactes depuis l’accueil.");
            return;
        }
        String id=UUID.randomUUID().toString();
        int req=Math.abs(id.hashCode());
        Intent play=new Intent(this,ScheduleReceiver.class);
        play.putExtra("movie",movie.toString());
        play.putExtra("volume",AppState.defaultVolume(this));
        PendingIntent pplay=PendingIntent.getBroadcast(this,req,play,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pplay);
        else am.setExact(AlarmManager.RTC_WAKEUP,when,pplay);

        long pre=when-AppState.preWakeMinutes(this)*60_000L;
        if(pre>System.currentTimeMillis()){
            Intent pi=new Intent(this,PreWakeReceiver.class);
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
                o.put("title",movie.optString("title"));
                a.put(o);
                AppState.setSchedules(this,a);
            }catch(Exception ignored){}
        }

        String date=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when));
        feedback.setText((test?"Test armé ✓  ":"Séance ajoutée ✓  ")+date+"\nPré-réveil : "+AppState.preWakeMinutes(this)+" min avant.");
        refresh();
    }
}
