package fr.shabbattv;

import android.app.Activity;
import android.content.Intent;
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
    private TextView plexState, movieState, scheduleState, automationNote;

    @Override protected void onCreate(Bundle b){ super.onCreate(b); build(); }
    @Override protected void onResume(){ super.onResume(); refresh(); }

    private void build(){
        LinearLayout root = Ui.page(this);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading = new LinearLayout(this); heading.setOrientation(LinearLayout.VERTICAL); heading.addView(Ui.eyebrow(this, "Accueil")); heading.addView(Ui.title(this, "Shabbat TV"), Ui.lp(-1,-2,this,3));
        top.addView(heading, new LinearLayout.LayoutParams(0,-2,1)); top.addView(Ui.pill(this, "v1.8", false)); root.addView(top);
        root.addView(Ui.subtitle(this, "Prépare les films et les horaires. Ensuite la TV s’occupe du reste."), Ui.lp(-1,-2,this,5));

        LinearLayout statusCard = Ui.card(this); LinearLayout stateRow = new LinearLayout(this); stateRow.setOrientation(LinearLayout.HORIZONTAL);
        plexState = Ui.body(this, "—"); movieState = Ui.body(this, "—"); scheduleState = Ui.body(this, "—");
        stateRow.addView(stateColumn("PLEX", plexState), new LinearLayout.LayoutParams(0,-2,1)); stateRow.addView(stateColumn("FILM", movieState), gapWeight()); stateRow.addView(stateColumn("PLANNING", scheduleState), gapWeight()); statusCard.addView(stateRow);
        root.addView(statusCard, Ui.lp(-1,-2,this,Ui.compact(this)?16:24));

        root.addView(Ui.eyebrow(this, "Configuration"), Ui.lp(-1,-2,this,Ui.compact(this)?16:24));
        LinearLayout nav1 = new LinearLayout(this); nav1.setOrientation(LinearLayout.HORIZONTAL);
        Button plex = Ui.button(this, "Plex", true); plex.setOnClickListener(v -> startActivity(new Intent(this, PlexSetupActivity.class)));
        Button movies = Ui.button(this, "Films", false); movies.setOnClickListener(v -> startActivity(new Intent(this, MoviePickerActivity.class)));
        Button plan = Ui.button(this, "Planning", false); plan.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));
        nav1.addView(plex, weight()); nav1.addView(movies, gapWeight()); nav1.addView(plan, gapWeight()); root.addView(nav1, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 9));

        LinearLayout nav2 = new LinearLayout(this); nav2.setOrientation(LinearLayout.HORIZONTAL);
        Button tests = Ui.button(this, "Tests", false); tests.setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));
        Button logs = Ui.button(this, "Logs", false); logs.setOnClickListener(v -> startActivity(new Intent(this, LogsActivity.class)));
        nav2.addView(tests, weight()); nav2.addView(logs, gapWeight()); root.addView(nav2, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 7));

        LinearLayout note = Ui.card(this); TextView noteTitle = Ui.body(this, "Automatisation longue veille"); noteTitle.setTypeface(null, android.graphics.Typeface.BOLD); note.addView(noteTitle);
        automationNote=Ui.muted(this,""); note.addView(automationNote, Ui.lp(-1,-2,this,5));
        Button perm = Ui.button(this, "Vérifier l’autorisation des alarmes", false); perm.setOnClickListener(v -> requestExact()); note.addView(perm, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 11));
        root.addView(note, Ui.lp(-1,-2,this,Ui.compact(this)?14:20));
        Ui.setScrollable(this, root); refresh();
    }

    private LinearLayout stateColumn(String label, TextView value){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.addView(Ui.eyebrow(this,label));value.setMaxLines(2);value.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(value,Ui.lp(-1,-2,this,4));return c;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-1,1);}
    private LinearLayout.LayoutParams gapWeight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(Ui.dp(this,Ui.compact(this)?6:10),0,0,0);return p;}

    private void refresh(){
        if(plexState==null)return; boolean plex=AppState.plexConnected(this); JSONObject m=AppState.selectedMovie(this); int n=AppState.schedules(this).length(); String server=AppState.prefs(this).getString("plex_server_name","Plex");
        plexState.setText(plex?server:"À connecter");plexState.setTextColor(plex?Ui.GOOD:Ui.TEXT);movieState.setText(m==null?"Aucun film":m.optString("title","Film"));scheduleState.setText(n==0?"Aucune séance":n+" séance"+(n>1?"s":""));
        if(automationNote!=null)automationNote.setText("Vraies séances : AlarmClock Android critique + même WakeReceiver que le test 8 minutes validé. Les séances sont réarmées après un redémarrage Android. Volume films fixé à "+AppState.FILM_VOLUME_PERCENT+" %. Extinction Philips : "+(PhilipsTvClient.isPaired(this)?"prête ✓":"à associer dans Tests")+". Laisse activées “Alarmes exactes” et “Activation de l’écran”.");
    }

    private void requestExact(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){try{Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);i.setData(Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}}
}
