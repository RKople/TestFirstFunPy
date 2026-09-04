package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
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
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(60,40,60,40); root.setBackgroundColor(Color.rgb(8,12,20));
        TextView title=new TextView(this); title.setText("Tests"); title.setTextColor(Color.WHITE); title.setTextSize(34); title.setGravity(Gravity.CENTER); root.addView(title);
        status=new TextView(this); status.setTextColor(Color.LTGRAY); status.setTextSize(18); status.setGravity(Gravity.CENTER); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.setMargins(0,20,0,25); root.addView(status,slp);
        Button wake=button("TEST WAKE-UP DANS 2 MINUTES",()->wakeTest()); root.addView(wake,new LinearLayout.LayoutParams(820,82));
        Button play=button("LANCER LE FILM SÉLECTIONNÉ MAINTENANT",()->playNow()); LinearLayout.LayoutParams lp2=new LinearLayout.LayoutParams(820,82); lp2.setMargins(0,14,0,0); root.addView(play,lp2);
        Button sleepNow=button("TEST MISE EN VEILLE MAINTENANT",()->{String d=SleepHelper.sleepNow(this); status.setText("Commande veille envoyée : "+d);}); LinearLayout.LayoutParams lp3=new LinearLayout.LayoutParams(820,82); lp3.setMargins(0,14,0,0); root.addView(sleepNow,lp3);
        Button sleepLater=button("TEST MISE EN VEILLE DANS 1 MINUTE",()->sleepInMinute()); LinearLayout.LayoutParams lp4=new LinearLayout.LayoutParams(820,82); lp4.setMargins(0,14,0,0); root.addView(sleepLater,lp4);
        setContentView(root); refresh();
    }
    private Button button(String text,Runnable r){Button b=new Button(this);b.setText(text);b.setFocusable(true);b.setOnClickListener(v->r.run());return b;}
    private void refresh(){
        String wake= getSharedPreferences("wake_diag",MODE_PRIVATE).getString("diag_log","");
        String playErr=AppState.prefs(this).getString("last_play_error","");
        String sleep=AppState.prefs(this).getString("last_sleep_diag","");
        status.setText("Diagnostic wake précédent : "+(wake.isEmpty()?"aucun":wake)+"\nErreur lecture : "+(playErr.isEmpty()?"aucune":playErr)+"\nDiagnostic veille : "+(sleep.isEmpty()?"aucun":sleep));
    }
    private void playNow(){JSONObject m=AppState.selectedMovie(this); if(m==null){status.setText("Sélectionne d’abord un film dans Films Plex.");return;} Intent i=new Intent(this,PlayerActivity.class); i.putExtra("movie",m.toString()); i.putExtra("volume",AppState.defaultVolume(this)); i.putExtra("sleep_when_done",false); startActivity(i);}
    private void wakeTest(){
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE); if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){status.setText("Autorise les alarmes exactes depuis l’accueil.");return;}
        long when=System.currentTimeMillis()+120_000L; Intent i=new Intent(this,WakeReceiver.class); PendingIntent p=PendingIntent.getBroadcast(this,707,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else am.setExact(AlarmManager.RTC_WAKEUP,when,p); status.setText("Wake test armé pour "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+". Éteins la TV maintenant.");
    }
    private void sleepInMinute(){
        AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE); long when=System.currentTimeMillis()+60_000L; Intent i=new Intent(this,SleepTestActivity.class); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); PendingIntent p=PendingIntent.getActivity(this,708,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else am.setExact(AlarmManager.RTC_WAKEUP,when,p); status.setText("Mise en veille test prévue à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".");
    }
}
