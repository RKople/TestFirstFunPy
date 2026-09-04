package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
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
    private TextView status;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(60,45,60,45); root.setBackgroundColor(Color.rgb(8,12,20));
        TextView title=new TextView(this); title.setText("Planning Chabbat"); title.setTextColor(Color.WHITE); title.setTextSize(34); title.setGravity(Gravity.CENTER); root.addView(title);
        status=new TextView(this); status.setTextColor(Color.LTGRAY); status.setTextSize(19); status.setGravity(Gravity.CENTER); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.setMargins(0,25,0,30); root.addView(status,slp);
        Button add=new Button(this); add.setText("AJOUTER UNE SÉANCE AVEC LE FILM SÉLECTIONNÉ"); add.setOnClickListener(v->pickDateTime()); root.addView(add,new LinearLayout.LayoutParams(850,90));
        Button quick=new Button(this); quick.setText("TEST : LANCER LE FILM DANS 2 MINUTES"); LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(850,85); qlp.setMargins(0,18,0,0); root.addView(quick,qlp); quick.setOnClickListener(v->scheduleAt(System.currentTimeMillis()+120_000L,true));
        setContentView(root); refresh();
    }
    private void refresh(){
        JSONObject m=AppState.selectedMovie(this);
        if(m==null){status.setText("Aucun film sélectionné. Va d’abord dans Films Plex.");return;}
        JSONArray a=AppState.schedules(this); StringBuilder sb=new StringBuilder("Film sélectionné : ").append(m.optString("title")).append("\n\n");
        if(a.length()==0) sb.append("Aucune séance programmée.");
        else for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i); if(o!=null) sb.append("• ").append(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(o.optLong("when")))).append(" — ").append(o.optString("title")).append("\n");}
        status.setText(sb.toString());
    }
    private void pickDateTime(){
        if(AppState.selectedMovie(this)==null){status.setText("Sélectionne un film d’abord.");return;}
        Calendar now=Calendar.getInstance();
        new DatePickerDialog(this,(v,y,m,d)->{
            Calendar c=Calendar.getInstance(); c.set(Calendar.YEAR,y); c.set(Calendar.MONTH,m); c.set(Calendar.DAY_OF_MONTH,d);
            new TimePickerDialog(this,(tv,h,min)->{ c.set(Calendar.HOUR_OF_DAY,h); c.set(Calendar.MINUTE,min); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); scheduleAt(c.getTimeInMillis(),false); },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();
        },now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void scheduleAt(long when, boolean test){
        JSONObject movie=AppState.selectedMovie(this); if(movie==null){status.setText("Sélectionne un film d’abord.");return;}
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S && !am.canScheduleExactAlarms()){status.setText("Autorise d’abord les alarmes exactes depuis l’écran principal.");return;}
        String id=UUID.randomUUID().toString(); int req=Math.abs(id.hashCode());
        Intent play=new Intent(this,ScheduleReceiver.class); play.putExtra("movie",movie.toString()); play.putExtra("volume",AppState.defaultVolume(this));
        PendingIntent pplay=PendingIntent.getBroadcast(this,req,play,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pplay); else am.setExact(AlarmManager.RTC_WAKEUP,when,pplay);
        long pre=when-AppState.preWakeMinutes(this)*60_000L;
        if(pre>System.currentTimeMillis()){
            Intent pi=new Intent(this,PreWakeReceiver.class); PendingIntent ppre=PendingIntent.getBroadcast(this,req+1,pi,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,pre,ppre); else am.setExact(AlarmManager.RTC_WAKEUP,pre,ppre);
        }
        if(!test){ try{JSONArray a=AppState.schedules(this); JSONObject o=new JSONObject(); o.put("id",id); o.put("when",when); o.put("title",movie.optString("title")); a.put(o); AppState.setSchedules(this,a);}catch(Exception ignored){} }
        status.setText((test?"TEST ARMÉ ✅\n":"SÉANCE AJOUTÉE ✅\n")+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(when))+"\nPré-réveil : "+AppState.preWakeMinutes(this)+" min avant.");
    }
}
