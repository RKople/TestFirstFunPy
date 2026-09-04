package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

public class TestActivity extends Activity {
    private TextView status, philipsState;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=Ui.page(this);
        Ui.header(root,this,"Diagnostic","Tests","Teste séparément le réveil, la lecture Plex et la vraie mise en veille Philips.");

        LinearLayout philips=Ui.card(this);
        philips.addView(Ui.eyebrow(this,"Extinction Philips"));
        philipsState=Ui.muted(this,""); philips.addView(philipsState,Ui.lp(-1,-2,this,5));
        Button pair=Ui.button(this,"Associer / vérifier le contrôle TV",false); pair.setOnClickListener(v->startActivity(new Intent(this,PhilipsPairActivity.class)));
        philips.addView(pair,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,7));
        root.addView(philips,Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        LinearLayout row1=new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL);
        Button wake=Ui.button(this,"Wake-up · 2 min",true); wake.setOnClickListener(v->wakeTest());
        Button play=Ui.button(this,"Lire le film · maintenant",false); play.setOnClickListener(v->playNow());
        row1.addView(wake,w()); row1.addView(play,wg()); root.addView(row1,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,10));

        LinearLayout row2=new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        Button scheduled=Ui.button(this,"Réveil + film · dans 2 min",false); scheduled.setOnClickListener(v->scheduledPlayTest());
        Button sleep=Ui.button(this,"Veille réelle · maintenant",false); sleep.setOnClickListener(v->SleepHelper.sleepNow(this,(ok,msg)->status.setText((ok?"✓ ":"✕ ")+msg)));
        row2.addView(scheduled,w()); row2.addView(sleep,wg()); root.addView(row2,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,6));

        LinearLayout row3=new LinearLayout(this); row3.setOrientation(LinearLayout.HORIZONTAL);
        Button sleepLater=Ui.button(this,"Veille réelle · dans 1 min",false); sleepLater.setOnClickListener(v->sleepInMinute());
        Button allLogs=Ui.button(this,"Ouvrir les logs complets",false); allLogs.setOnClickListener(v->startActivity(new Intent(this,LogsActivity.class)));
        row3.addView(sleepLater,w()); row3.addView(allLogs,wg()); root.addView(row3,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,6));

        LinearLayout card=Ui.card(this); card.addView(Ui.eyebrow(this,"Dernier diagnostic"));
        status=Ui.muted(this,"Aucun test lancé."); status.setTextSize(Ui.compact(this)?12:14); status.setTextIsSelectable(true); status.setFocusable(true); status.setFocusableInTouchMode(true); status.setVerticalScrollBarEnabled(true); status.setMovementMethod(new ScrollingMovementMethod()); status.setMaxHeight(Ui.dp(this,Ui.compact(this)?190:280)); status.setPadding(0,0,Ui.dp(this,8),Ui.dp(this,6));
        card.addView(status,Ui.lp(-1,-2,this,7)); root.addView(card,Ui.lp(-1,-2,this,Ui.compact(this)?10:16));

        Ui.setScrollable(this,root); refresh();
    }

    @Override protected void onResume(){ super.onResume(); if(status!=null)refresh(); }
    private LinearLayout.LayoutParams w(){return new LinearLayout.LayoutParams(0,-1,1);}
    private LinearLayout.LayoutParams wg(){LinearLayout.LayoutParams p=w();p.setMargins(Ui.dp(this,Ui.compact(this)?6:10),0,0,0);return p;}

    private void refresh(){
        philipsState.setText(PhilipsTvClient.isPaired(this)?"Associé ✓ · la commande Standby Philips sera utilisée":"Non associé · nécessaire pour une extinction réelle");
        philipsState.setTextColor(PhilipsTvClient.isPaired(this)?Ui.GOOD:Ui.MUTED);
        String wake=getSharedPreferences("wake_diag",MODE_PRIVATE).getString("diag_log",""); String playErr=AppState.prefs(this).getString("last_play_error",""); String sleep=AppState.prefs(this).getString("last_sleep_diag","");
        status.setText("Réveil\n"+(wake.isEmpty()?"Aucun test":wake)+"\n\nLecture Plex\n"+(playErr.isEmpty()?"Aucune erreur enregistrée":playErr)+"\n\nVeille\n"+(sleep.isEmpty()?"Aucun test":sleep));
    }

    private void playNow(){JSONObject m=AppState.selectedMovie(this);if(m==null){status.setText("Sélectionne d’abord un film dans Films.");return;}LogStore.add(this,"Test","Lecture immédiate : "+m.optString("title","Film"));Intent i=new Intent(this,PlayerActivity.class);i.putExtra("movie",m.toString());i.putExtra("volume",AppState.defaultVolume(this));i.putExtra("sleep_when_done",false);startActivity(i);}

    private void scheduledPlayTest(){
        JSONObject movie=AppState.selectedMovie(this);if(movie==null){status.setText("Sélectionne d’abord un film dans Films.");return;}
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){status.setText("Autorise les alarmes exactes depuis l’accueil.");return;}
        long now=System.currentTimeMillis(),target=now+120_000L,pre=now+15_000L;String id="test-"+UUID.randomUUID();int req=AppState.requestCodeForId(id),volume=AppState.defaultVolume(this);
        Intent preIntent=new Intent(this,PreWakeReceiver.class);preIntent.putExtra("schedule_id",id);preIntent.putExtra("movie",movie.toString());preIntent.putExtra("volume",volume);preIntent.putExtra("target_at",target);preIntent.putExtra("sleep_when_done",false);
        PendingIntent ppre=PendingIntent.getBroadcast(this,req+1,preIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent play=new Intent(this,ScheduleReceiver.class);play.putExtra("schedule_id",id);play.putExtra("movie",movie.toString());play.putExtra("volume",volume);play.putExtra("sleep_when_done",false);
        PendingIntent pplay=PendingIntent.getBroadcast(this,req,play,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23){am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,pre,ppre);am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,target,pplay);}else{am.setExact(AlarmManager.RTC_WAKEUP,pre,ppre);am.setExact(AlarmManager.RTC_WAKEUP,target,pplay);}
        LogStore.add(this,"Test","Réveil prévu dans 15 s, film à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(target)));
        status.setText("Test armé ✓\nÉteins la TV maintenant.\nRéveil écran noir dans ~15 s, film à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(target))+".");
    }

    private void wakeTest(){AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){status.setText("Autorise les alarmes exactes depuis l’accueil.");return;}long when=System.currentTimeMillis()+120_000L;Intent i=new Intent(this,WakeReceiver.class);PendingIntent p=PendingIntent.getBroadcast(this,707,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else am.setExact(AlarmManager.RTC_WAKEUP,when,p);LogStore.add(this,"Test","Wake-up programmé à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when)));status.setText("Wake-up armé pour "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".\nÉteins maintenant la TV.");}

    private void sleepInMinute(){AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);long when=System.currentTimeMillis()+60_000L;Intent i=new Intent(this,SleepTestActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent p=PendingIntent.getActivity(this,708,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else am.setExact(AlarmManager.RTC_WAKEUP,when,p);LogStore.add(this,"Test","Veille réelle programmée à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when)));status.setText("Mise en veille réelle prévue à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".");}
}
