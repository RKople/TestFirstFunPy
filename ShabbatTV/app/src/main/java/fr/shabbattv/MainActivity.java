package fr.shabbattv;

import android.app.Activity;
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
    private TextView plexState, movieState, scheduleState;

    @Override protected void onCreate(Bundle b){ super.onCreate(b); build(); }
    @Override protected void onResume(){ super.onResume(); refresh(); }

    private void build(){
        LinearLayout root = Ui.page(this);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = Ui.title(this, "Shabbat TV");
        top.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = Ui.pill(this, "v1.1", false);
        top.addView(version);
        root.addView(top);

        TextView sub = Ui.subtitle(this, "Prépare maintenant. La TV s’occupe du reste pendant Chabbat.");
        root.addView(sub, Ui.lp(-1, -2, this, 7));
        Ui.spacer(root, this, 26);

        LinearLayout statusCard = Ui.card(this);
        LinearLayout stateRow = new LinearLayout(this);
        stateRow.setOrientation(LinearLayout.HORIZONTAL);

        plexState = statusBlock("PLEX", statusCard);
        movieState = statusBlock("FILM", statusCard);
        scheduleState = statusBlock("PLANNING", statusCard);
        statusCard.addView(stateRow);
        // Rebuild with lightweight columns
        statusCard.removeAllViews();
        stateRow = new LinearLayout(this); stateRow.setOrientation(LinearLayout.HORIZONTAL);
        stateRow.addView(stateColumn("PLEX", plexState), new LinearLayout.LayoutParams(0,-2,1));
        stateRow.addView(stateColumn("FILM", movieState), new LinearLayout.LayoutParams(0,-2,1));
        stateRow.addView(stateColumn("PLANNING", scheduleState), new LinearLayout.LayoutParams(0,-2,1));
        statusCard.addView(stateRow);
        root.addView(statusCard, Ui.lp(-1, -2, this, 0));

        Ui.spacer(root, this, 26);
        TextView section = Ui.eyebrow(this, "Configuration"); root.addView(section);

        LinearLayout nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL);
        Button plex = Ui.button(this, "Plex", true); plex.setOnClickListener(v -> startActivity(new Intent(this, PlexSetupActivity.class)));
        Button movies = Ui.button(this, "Films", false); movies.setOnClickListener(v -> startActivity(new Intent(this, MoviePickerActivity.class)));
        Button plan = Ui.button(this, "Planning", false); plan.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        Button tests = Ui.button(this, "Tests", false); tests.setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));
        nav.addView(plex, weighted()); nav.addView(movies, weightedGap()); nav.addView(plan, weightedGap()); nav.addView(tests, weightedGap());
        root.addView(nav, Ui.lp(-1, Ui.dp(this, 66), this, 12));

        Ui.spacer(root, this, 20);
        LinearLayout note = Ui.card(this);
        TextView noteTitle = Ui.body(this, "Réveil automatique Philips activé"); noteTitle.setTypeface(null, android.graphics.Typeface.BOLD); note.addView(noteTitle);
        TextView noteText = Ui.muted(this, "Pré-réveil : "+AppState.preWakeMinutes(this)+" min avant chaque séance. Garde aussi l’autorisation “Activation de l’écran” activée pour Shabbat TV.");
        note.addView(noteText, Ui.lp(-1,-2,this,6));
        Button perm = Ui.button(this, "Vérifier l’autorisation des alarmes", false);
        perm.setOnClickListener(v -> requestExact()); note.addView(perm, Ui.lp(-1, Ui.dp(this,56), this,14));
        root.addView(note, Ui.lp(-1,-2,this,0));

        setContentView(root);
        refresh();
    }

    private TextView statusBlock(String name, LinearLayout ignored){
        TextView t = Ui.body(this, "—"); t.setTextSize(16); return t;
    }
    private LinearLayout stateColumn(String label, TextView value){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        TextView l=Ui.eyebrow(this,label); c.addView(l); c.addView(value,Ui.lp(-1,-2,this,5)); return c;
    }
    private LinearLayout.LayoutParams weighted(){ return new LinearLayout.LayoutParams(0,-1,1); }
    private LinearLayout.LayoutParams weightedGap(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1); p.setMargins(Ui.dp(this,10),0,0,0); return p; }

    private void refresh(){
        if(plexState==null)return;
        boolean plex=AppState.plexConnected(this);
        JSONObject m=AppState.selectedMovie(this);
        int n=AppState.schedules(this).length();
        plexState.setText(plex ? "Connecté\n"+AppState.prefs(this).getString("plex_server_name","Plex") : "À connecter");
        plexState.setTextColor(plex ? Ui.GOOD : Ui.TEXT);
        movieState.setText(m==null ? "Aucun film" : m.optString("title","Film"));
        scheduleState.setText(n==0 ? "Aucune séance" : n+" séance"+(n>1?"s":""));
    }

    private void requestExact(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){
            try{ Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM); i.setData(Uri.parse("package:"+getPackageName())); startActivity(i); }
            catch(Exception e){ startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }
    }
}
