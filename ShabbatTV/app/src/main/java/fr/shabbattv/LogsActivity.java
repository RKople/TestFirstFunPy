package fr.shabbattv;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LogsActivity extends Activity {
    private TextView logs;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        Ui.header(root,this,"Historique","Logs","Les événements les plus récents apparaissent en premier. Jusqu’à 180 événements sont conservés localement sur la TV.");

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = Ui.button(this,"Rafraîchir",true);
        refresh.setOnClickListener(v->refresh());
        Button clear = Ui.button(this,"Effacer les logs",false);
        clear.setOnClickListener(v->{ LogStore.clear(this); refresh(); });
        actions.addView(refresh,new LinearLayout.LayoutParams(0,Ui.dp(this,Ui.controlHeight(this)),1));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,Ui.dp(this,Ui.controlHeight(this)),1);
        cp.setMargins(Ui.dp(this,8),0,0,0);
        actions.addView(clear,cp);
        root.addView(actions,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,Ui.compact(this)?12:18));

        LinearLayout card = Ui.card(this);
        card.addView(Ui.eyebrow(this,"Journal"));
        logs = Ui.muted(this,"");
        logs.setTextSize(Ui.compact(this)?12:14);
        logs.setFocusable(true);
        logs.setFocusableInTouchMode(true);
        logs.setVerticalScrollBarEnabled(true);
        logs.setMovementMethod(new ScrollingMovementMethod());
        logs.setPadding(0,Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,10));
        card.addView(logs,Ui.lp(-1,-2,this,7));
        root.addView(card,Ui.lp(-1,-2,this,10));

        Ui.setScrollable(this,root);
        refresh();
    }

    @Override protected void onResume(){ super.onResume(); if(logs!=null) refresh(); }

    private void refresh(){
        StringBuilder out = new StringBuilder(LogStore.text(this));
        String wake = getSharedPreferences("wake_diag",MODE_PRIVATE).getString("diag_log","");
        String play = AppState.prefs(this).getString("last_play_error","");
        String sleep = AppState.prefs(this).getString("last_sleep_diag","");
        String schedule = AppState.prefs(this).getString("last_schedule_error","");
        if(!wake.isEmpty() || !play.isEmpty() || !sleep.isEmpty() || !schedule.isEmpty()) {
            out.append("\n\n\n—— DIAGNOSTICS TECHNIQUES ——");
            if(!wake.isEmpty()) out.append("\n\nRéveil\n").append(wake);
            if(!play.isEmpty()) out.append("\n\nDernière erreur lecture\n").append(play);
            if(!schedule.isEmpty()) out.append("\n\nDernière erreur planning\n").append(schedule);
            if(!sleep.isEmpty()) out.append("\n\nVeille\n").append(sleep);
        }
        logs.setText(out.toString());
    }
}
