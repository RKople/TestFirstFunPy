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

public class TestActivity extends Activity {
    private TextView status;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=Ui.page(this);
        Ui.header(root,this,"Diagnostic","Tests","Teste séparément le réveil, la lecture Plex et la mise en veille avant un vrai Chabbat.");

        LinearLayout displayCard=Ui.card(this);
        displayCard.addView(Ui.eyebrow(this,"Affichage détecté"));
        displayCard.addView(Ui.body(this,"Philips OLED810 · dalle 3840 × 2160"),Ui.lp(-1,-2,this,5));
        displayCard.addView(Ui.muted(this,"Fenêtre Android : "+Ui.metrics(this)+"\nInterface protégée par une zone sûre de 5 % sur chaque bord."),Ui.lp(-1,-2,this,4));
        root.addView(displayCard,Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        LinearLayout row1=new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        Button wake=Ui.button(this,"Wake-up · 2 min",true);
        wake.setOnClickListener(v->wakeTest());
        Button play=Ui.button(this,"Lire le film · maintenant",false);
        play.setOnClickListener(v->playNow());
        row1.addView(wake,w());
        row1.addView(play,wg());
        root.addView(row1,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,Ui.compact(this)?10:16));

        LinearLayout row2=new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        Button sleep=Ui.button(this,"Veille · maintenant",false);
        sleep.setOnClickListener(v->{
            String d=SleepHelper.sleepNow(this);
            status.setText("Veille : "+d);
            LogStore.add(this,"Test","Test veille immédiate");
        });
        Button sleepLater=Ui.button(this,"Veille · dans 1 min",false);
        sleepLater.setOnClickListener(v->sleepInMinute());
        row2.addView(sleep,w());
        row2.addView(sleepLater,wg());
        root.addView(row2,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,6));

        Button allLogs=Ui.button(this,"Ouvrir les logs complets",false);
        allLogs.setOnClickListener(v->startActivity(new Intent(this,LogsActivity.class)));
        root.addView(allLogs,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,8));

        LinearLayout card=Ui.card(this);
        card.addView(Ui.eyebrow(this,"Dernier diagnostic"));
        status=Ui.muted(this,"Aucun test lancé.");
        status.setTextSize(Ui.compact(this)?12:14);
        status.setTextIsSelectable(true);
        status.setFocusable(true);
        status.setFocusableInTouchMode(true);
        status.setVerticalScrollBarEnabled(true);
        status.setMovementMethod(new ScrollingMovementMethod());
        status.setMaxHeight(Ui.dp(this,Ui.compact(this)?170:260));
        status.setPadding(0,0,Ui.dp(this,8),Ui.dp(this,6));
        card.addView(status,Ui.lp(-1,-2,this,7));
        root.addView(card,Ui.lp(-1,-2,this,Ui.compact(this)?10:16));

        Ui.setScrollable(this,root);
        refresh();
    }

    @Override protected void onResume(){ super.onResume(); if(status!=null) refresh(); }

    private LinearLayout.LayoutParams w(){
        return new LinearLayout.LayoutParams(0,-1,1);
    }

    private LinearLayout.LayoutParams wg(){
        LinearLayout.LayoutParams p=w();
        p.setMargins(Ui.dp(this,Ui.compact(this)?6:10),0,0,0);
        return p;
    }

    private void refresh(){
        String wake=getSharedPreferences("wake_diag",MODE_PRIVATE).getString("diag_log","");
        String playErr=AppState.prefs(this).getString("last_play_error","");
        String sleep=AppState.prefs(this).getString("last_sleep_diag","");
        status.setText("Réveil\n"+(wake.isEmpty()?"Aucun test":wake)+"\n\nLecture Plex\n"+(playErr.isEmpty()?"Aucune erreur enregistrée":playErr)+"\n\nVeille\n"+(sleep.isEmpty()?"Aucun test":sleep));
    }

    private void playNow(){
        JSONObject m=AppState.selectedMovie(this);
        if(m==null){ status.setText("Sélectionne d’abord un film dans Films."); return; }
        LogStore.add(this,"Test","Lecture immédiate : "+m.optString("title","Film"));
        Intent i=new Intent(this,PlayerActivity.class);
        i.putExtra("movie",m.toString());
        i.putExtra("volume",AppState.defaultVolume(this));
        i.putExtra("sleep_when_done",false);
        startActivity(i);
    }

    private void wakeTest(){
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){
            status.setText("Autorise les alarmes exactes depuis l’accueil.");
            return;
        }
        long when=System.currentTimeMillis()+120_000L;
        Intent i=new Intent(this,WakeReceiver.class);
        PendingIntent p=PendingIntent.getBroadcast(this,707,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
        else am.setExact(AlarmManager.RTC_WAKEUP,when,p);
        LogStore.add(this,"Test","Wake-up programmé à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when)));
        status.setText("Wake-up armé pour "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".\nÉteins maintenant la TV avec la télécommande.");
    }

    private void sleepInMinute(){
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        long when=System.currentTimeMillis()+60_000L;
        Intent i=new Intent(this,SleepTestActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent p=PendingIntent.getActivity(this,708,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
        else am.setExact(AlarmManager.RTC_WAKEUP,when,p);
        LogStore.add(this,"Test","Veille programmée à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when)));
        status.setText("Mise en veille prévue à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".");
    }
}
