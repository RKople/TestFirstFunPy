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
        Ui.header(root, this, "Automatisation", "Planning", "La TV est réveillée largement en avance avec le même mécanisme que le test Wake-up validé. Un écran d’attente affiche ensuite le compte à rebours jusqu’au film.");

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
        Button quick = Ui.button(this,"Test complet · film dans 8 minutes",false); quick.setOnClickListener(v->scheduleAt(System.currentTimeMillis()+8*60_000L,true));
        actionsCard.addView(quick,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,7));
        feedback = Ui.muted(this,""); feedback.setMaxLines(8); actionsCard.addView(feedback,Ui.lp(-1,-2,this,7));
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
        long now=System.currentTimeMillis();
        if(test && when < now + 7*60_000L){feedback.setText("Le test complet a besoin d’environ 8 minutes pour reproduire le vrai scénario.");return;}
        if(!test && when <= now+2*60_000L){feedback.setText("Choisis une heure au moins 2 minutes dans le futur. Pour une TV éteinte, plus tu programmes en avance, plus le réveil est sûr.");return;}

        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S&&!am.canScheduleExactAlarms()){feedback.setText("Autorise d’abord les alarmes exactes depuis l’accueil.");return;}

        String id=(test?"test-":"")+UUID.randomUUID();
        int volume=AppState.defaultVolume(this);
        boolean sleepWhenDone=!test;

        // Crucial rule from real OLED810 tests:
        // an alarm requested for +2 min physically wakes the panel around +5 min.
        // Therefore the wake phase must be separated by several minutes from playback.
        long wakeAt;
        long retryAt;
        if(test){
            wakeAt=now+2*60_000L;       // same timing as the validated standalone Wake-up test
            retryAt=now+5*60_000L;      // safety retry while there is still time before T+8
        } else {
            long desired=when-AppState.preWakeMinutes(this)*60_000L; // normally T-10 min
            wakeAt=Math.max(desired,now+2*60_000L);
            retryAt=Math.min(when-2*60_000L,wakeAt+4*60_000L);
            if(retryAt<=wakeAt+30_000L) retryAt=0L;
        }

        long duration=movie.optLong("durationMs",0L),endAt=duration>0?when+duration:0L;

        // 1) Primary wake: EXACTLY the same WakeReceiver and exact-alarm primitive as the
        // standalone Wake-up test that works on this Philips.
        PendingIntent wakePi = wakePending(id+":wake", id, movie, volume, when, wakeAt, sleepWhenDone, "waiting");
        setExact(am,wakeAt,wakePi);

        // 2) A second identical wake attempt, still before the film, protects against a
        // delayed/missed first delivery. WaitingActivity is singleTask and simply resyncs.
        if(retryAt>0L){
            PendingIntent retryPi = wakePending(id+":retry", id, movie, volume, when, retryAt, sleepWhenDone, "waiting");
            setExact(am,retryAt,retryPi);
        }

        // 3) At the target time, use the validated immediate playback path. When the wake
        // succeeded, Android is already awake so this fires promptly.
        Intent direct=new Intent(this,ScheduleReceiver.class);
        direct.putExtra("movie",movie.toString()); direct.putExtra("volume",volume); direct.putExtra("schedule_id",id); direct.putExtra("sleep_when_done",sleepWhenDone);
        PendingIntent directPi=PendingIntent.getBroadcast(this,AppState.requestCodeForId(id+":direct"),direct,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        setExact(am,when,directPi);

        // 4) Independent target fallback: same proven wake sequence, then playback. If the
        // TV is already awake it becomes a harmless duplicate (PlaybackLauncher de-dupes).
        PendingIntent targetWake = wakePending(id+":target-wake", id, movie, volume, when, when, sleepWhenDone, "play");
        setExact(am,when,targetWake);

        long estimateDelay=AppState.prefs(this).getLong("last_wake_delay_ms",3*60_000L);
        estimateDelay=Math.max(0L,Math.min(6*60_000L,estimateDelay));
        long visibleEstimate=wakeAt+estimateDelay;

        if(!test){
            try{
                JSONArray a=AppState.schedules(this);JSONObject o=new JSONObject();
                o.put("id",id);o.put("when",when);o.put("wakeAt",wakeAt);o.put("retryAt",retryAt);o.put("visibleEstimateAt",visibleEstimate);
                o.put("title",movie.optString("title"));o.put("durationMs",duration);o.put("endAt",endAt);o.put("volume",volume);
                o.put("server",AppState.prefs(this).getString("plex_server_name","Plex"));o.put("audioLabel",movie.optString("audioLabel","Automatique"));
                o.put("subtitleLabel",movie.optString("subtitleLabel",movie.optBoolean("subtitlesOff",true)?"Aucun":"Automatiques"));
                a.put(o);AppState.setSchedules(this,a);
                LogStore.add(this,"Planning","Séance v1.7 ajoutée : "+movie.optString("title")+" · réveil "+time(wakeAt)+" · film "+time(when));
            }catch(Exception ignored){}
        } else {
            LogStore.add(this,"Test","Test complet armé · wake validé à "+time(wakeAt)+" · retry "+time(retryAt)+" · film "+time(when));
        }

        StringBuilder result=new StringBuilder(test?"Test complet armé ✓":"Séance ajoutée ✓");
        result.append("\nCommande de réveil : ").append(time(wakeAt));
        result.append("\nAllumage attendu vers : ").append(time(visibleEstimate)).append(" (estimation)");
        if(retryAt>0)result.append("\nRéveil de secours : ").append(time(retryAt));
        result.append("\nFilm : ").append(time(when));
        result.append("\nQuand la TV s’allume, elle affiche le compte à rebours du film.");
        if(endAt>0&&!test)result.append("\nVeille estimée : ").append(time(endAt));
        feedback.setText(result.toString()); refresh();
    }

    private PendingIntent wakePending(String requestKey,String id,JSONObject movie,int volume,long target,long expected,boolean sleepWhenDone,String mode){
        Intent i=new Intent(this,WakeReceiver.class);
        i.putExtra("mode",mode);i.putExtra("schedule_id",id);i.putExtra("movie",movie.toString());i.putExtra("volume",volume);
        i.putExtra("target_at",target);i.putExtra("expected_at",expected);i.putExtra("sleep_when_done",sleepWhenDone);
        return PendingIntent.getBroadcast(this,AppState.requestCodeForId(requestKey),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private void setExact(AlarmManager am,long when,PendingIntent pi){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);else am.setExact(AlarmManager.RTC_WAKEUP,when,pi);
    }

    private String time(long ms){return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(ms));}
    private String formatDuration(long ms){long mins=Math.max(1,ms/60000L),h=mins/60,m=mins%60;return h>0?h+" h "+(m<10?"0":"")+m:mins+" min";}
}
