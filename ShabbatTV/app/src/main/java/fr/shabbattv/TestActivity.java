package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
        root.addView(Ui.eyebrow(this,"Diagnostic"));
        root.addView(Ui.title(this,"Tests"),Ui.lp(-1,-2,this,5));
        root.addView(Ui.subtitle(this,"Teste chaque brique séparément avant de programmer un vrai Chabbat."),Ui.lp(-1,-2,this,7));

        LinearLayout grid=new LinearLayout(this); grid.setOrientation(LinearLayout.HORIZONTAL);
        Button wake=Ui.button(this,"Wake-up · 2 min",true); wake.setOnClickListener(v->wakeTest());
        Button play=Ui.button(this,"Lire le film · maintenant",false); play.setOnClickListener(v->playNow());
        Button sleep=Ui.button(this,"Veille · maintenant",false); sleep.setOnClickListener(v->{String d=SleepHelper.sleepNow(this);status.setText("Veille : "+d);});
        Button sleepLater=Ui.button(this,"Veille · 1 min",false); sleepLater.setOnClickListener(v->sleepInMinute());
        grid.addView(wake,w()); grid.addView(play,wg()); grid.addView(sleep,wg()); grid.addView(sleepLater,wg());
        root.addView(grid,Ui.lp(-1,Ui.dp(this,66),this,24));

        LinearLayout card=Ui.card(this);
        card.addView(Ui.eyebrow(this,"Dernier diagnostic"));
        status=Ui.muted(this,"Aucun test lancé."); status.setTextSize(14); card.addView(status,Ui.lp(-1,-2,this,10));
        root.addView(card,Ui.lp(-1,-2,this,20));
        setContentView(root); refresh();
    }
    private LinearLayout.LayoutParams w(){return new LinearLayout.LayoutParams(0,-1,1);} private LinearLayout.LayoutParams wg(){LinearLayout.LayoutParams p=w();p.setMargins(Ui.dp(this,10),0,0,0);return p;}
    private void refresh(){
        String wake=getSharedPreferences("wake_diag",MODE_PRIVATE).getString("diag_log","");
        String playErr=AppState.prefs(this).getString("last_play_error","");
        String sleep=AppState.prefs(this).getString("last_sleep_diag","");
        status.setText("Réveil : "+(wake.isEmpty()?"aucun test":wake)+"\n\nLecture : "+(playErr.isEmpty()?"aucune erreur":playErr)+"\n\nVeille : "+(sleep.isEmpty()?"aucun test":sleep));
    }
    private void playNow(){JSONObject m=AppState.selectedMovie(this);if(m==null){status.setText("Sélectionne d’abord un film dans Films.");return;}Intent i=new Intent(this,PlayerActivity.class);i.putExtra("movie",m.toString());i.putExtra("volume",AppState.defaultVolume(this));i.putExtra("sleep_when_done",false);startActivity(i);}
    private void wakeTest(){AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms()){status.setText("Autorise les alarmes exactes depuis l’accueil.");return;}long when=System.currentTimeMillis()+120_000L;Intent i=new Intent(this,WakeReceiver.class);PendingIntent p=PendingIntent.getBroadcast(this,707,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else am.setExact(AlarmManager.RTC_WAKEUP,when,p);status.setText("Wake-up armé pour "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".\nÉteins maintenant la TV avec la télécommande.");}
    private void sleepInMinute(){AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);long when=System.currentTimeMillis()+60_000L;Intent i=new Intent(this,SleepTestActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);PendingIntent p=PendingIntent.getActivity(this,708,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);if(Build.VERSION.SDK_INT>=23)am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);else am.setExact(AlarmManager.RTC_WAKEUP,when,p);status.setText("Mise en veille prévue à "+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(when))+".");}
}
