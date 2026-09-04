package fr.shabbattv;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private TextView summary;
    @Override protected void onCreate(Bundle b){super.onCreate(b); build();}
    @Override protected void onResume(){super.onResume(); if(summary!=null) refresh();}
    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(70,45,70,45); root.setBackgroundColor(Color.rgb(7,11,18));
        TextView title=new TextView(this); title.setText("SHABBAT TV"); title.setTextColor(Color.WHITE); title.setTextSize(40); title.setGravity(Gravity.CENTER); root.addView(title);
        TextView sub=new TextView(this); sub.setText("Plex • Planning automatique • Philips OLED810"); sub.setTextColor(Color.LTGRAY); sub.setTextSize(20); sub.setGravity(Gravity.CENTER); LinearLayout.LayoutParams sublp=new LinearLayout.LayoutParams(-1,-2); sublp.setMargins(0,8,0,28); root.addView(sub,sublp);
        summary=new TextView(this); summary.setTextColor(Color.WHITE); summary.setTextSize(19); summary.setGravity(Gravity.CENTER); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.setMargins(0,0,0,25); root.addView(summary,slp);
        root.addView(btn("1. CONNEXION PLEX",PlexSetupActivity.class),new LinearLayout.LayoutParams(820,86));
        LinearLayout.LayoutParams x=new LinearLayout.LayoutParams(820,86); x.setMargins(0,14,0,0); root.addView(btn("2. CHOISIR UN FILM",MoviePickerActivity.class),x);
        LinearLayout.LayoutParams y=new LinearLayout.LayoutParams(820,86); y.setMargins(0,14,0,0); root.addView(btn("3. PLANNING CHABBAT",ScheduleActivity.class),y);
        LinearLayout.LayoutParams z=new LinearLayout.LayoutParams(820,86); z.setMargins(0,14,0,0); root.addView(btn("4. TESTS",TestActivity.class),z);
        Button perm=new Button(this); perm.setText("AUTORISER LES ALARMES EXACTES"); perm.setOnClickListener(v->requestExact()); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(820,76); p.setMargins(0,20,0,0); root.addView(perm,p);
        setContentView(root); refresh();
    }
    private Button btn(String t,Class<?> c){Button b=new Button(this);b.setText(t);b.setTextSize(18);b.setFocusable(true);b.setOnClickListener(v->startActivity(new Intent(this,c)));return b;}
    private void refresh(){
        boolean plex=AppState.plexConnected(this); JSONObject m=AppState.selectedMovie(this);
        String server=AppState.prefs(this).getString("plex_server_name","—");
        String movie=m==null?"—":m.optString("title","—");
        int n=AppState.schedules(this).length();
        summary.setText("Plex : "+(plex?"✅ "+server:"❌ non connecté")+"\nFilm sélectionné : "+movie+"\nSéances programmées : "+n+"\nPré-réveil automatique : "+AppState.preWakeMinutes(this)+" min");
    }
    private void requestExact(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){try{Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);i.setData(Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}
    }
}
