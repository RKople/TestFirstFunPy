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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

public class MainActivity extends Activity {
    private TextView plexState, movieState, scheduleState, automationNote;
    private Button armButton;

    @Override protected void onCreate(Bundle b){ super.onCreate(b); build(); }
    @Override protected void onResume(){ super.onResume(); refresh(); }

    private void build(){
        LinearLayout root = Ui.page(this);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading = new LinearLayout(this); heading.setOrientation(LinearLayout.VERTICAL); heading.addView(Ui.eyebrow(this, "Accueil")); heading.addView(Ui.title(this, "Shabbat TV"), Ui.lp(-1,-2,this,3));
        top.addView(heading, new LinearLayout.LayoutParams(0,-2,1)); top.addView(Ui.pill(this, "v1.9", false)); root.addView(top);
        root.addView(Ui.subtitle(this, "Prépare les films et les horaires, puis arme le Mode Shabbat longue durée."), Ui.lp(-1,-2,this,5));

        LinearLayout statusCard = Ui.card(this); LinearLayout stateRow = new LinearLayout(this); stateRow.setOrientation(LinearLayout.HORIZONTAL);
        plexState = Ui.body(this, "—"); movieState = Ui.body(this, "—"); scheduleState = Ui.body(this, "—");
        stateRow.addView(stateColumn("PLEX", plexState), new LinearLayout.LayoutParams(0,-2,1)); stateRow.addView(stateColumn("FILM", movieState), gapWeight()); stateRow.addView(stateColumn("PLANNING", scheduleState), gapWeight()); statusCard.addView(stateRow);
        root.addView(statusCard, Ui.lp(-1,-2,this,Ui.compact(this)?16:24));

        LinearLayout armCard = Ui.card(this);
        armCard.addView(Ui.eyebrow(this, "Mode Shabbat longue durée"));
        TextView armInfo = Ui.body(this, "Garde Android actif sur un écran OLED totalement noir. C’est le mode recommandé pour les séances programmées plusieurs heures à l’avance.");
        armInfo.setMaxLines(4); armCard.addView(armInfo, Ui.lp(-1,-2,this,6));
        armButton = Ui.button(this, "Armer Shabbat", true);
        armButton.setOnClickListener(v -> armShabbat());
        armCard.addView(armButton, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 12));
        Button longTest = Ui.button(this, "Test mode Shabbat · film dans 20 min", false);
        longTest.setOnClickListener(v -> startLongModeTest());
        armCard.addView(longTest, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 7));
        TextView warning = Ui.muted(this, "Important : après avoir armé, ne mets pas la TV en veille avec la télécommande. L’écran devient noir automatiquement.");
        warning.setMaxLines(3); armCard.addView(warning, Ui.lp(-1,-2,this,7));
        root.addView(armCard, Ui.lp(-1,-2,this,Ui.compact(this)?14:20));

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

        LinearLayout note = Ui.card(this); TextView noteTitle = Ui.body(this, "Sécurité / secours"); noteTitle.setTypeface(null, android.graphics.Typeface.BOLD); note.addView(noteTitle);
        automationNote=Ui.muted(this,""); note.addView(automationNote, Ui.lp(-1,-2,this,5));
        Button perm = Ui.button(this, "Vérifier l’autorisation des alarmes", false); perm.setOnClickListener(v -> requestExact()); note.addView(perm, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 11));
        root.addView(note, Ui.lp(-1,-2,this,Ui.compact(this)?14:20));
        Ui.setScrollable(this, root); refresh();
    }

    private void armShabbat() {
        if (!AppState.plexConnected(this)) {
            automationNote.setText("Connecte d’abord Plex avant d’armer le Mode Shabbat.");
            return;
        }
        if (AppState.schedules(this).length() == 0) {
            automationNote.setText("Programme au moins une séance avant d’armer le Mode Shabbat.");
            return;
        }
        AppState.setShabbatArmed(this, true);
        LogStore.add(this, "Mode Shabbat", "Armement demandé depuis l’accueil");
        openArmedMode();
    }

    private void startLongModeTest() {
        JSONObject movie = AppState.selectedMovie(this);
        if (!AppState.plexConnected(this) || movie == null) {
            automationNote.setText("Connecte Plex et sélectionne un film avant de lancer le test 20 minutes.");
            return;
        }
        try {
            long when = System.currentTimeMillis() + 20 * 60_000L;
            String id = "armedtest-" + UUID.randomUUID();
            JSONObject s = new JSONObject();
            s.put("id", id);
            s.put("when", when);
            s.put("wakeAt", when - 10 * 60_000L);
            s.put("retryAt", 0L);
            s.put("visibleEstimateAt", when - 10 * 60_000L);
            s.put("title", movie.optString("title", "Film"));
            s.put("movie", movie.toString());
            s.put("durationMs", movie.optLong("durationMs", 0L));
            s.put("endAt", movie.optLong("durationMs", 0L) > 0 ? when + movie.optLong("durationMs", 0L) : 0L);
            s.put("volume", AppState.FILM_VOLUME_PERCENT);
            s.put("server", AppState.prefs(this).getString("plex_server_name", "Plex"));
            s.put("audioLabel", movie.optString("audioLabel", "Automatique"));
            s.put("subtitleLabel", movie.optString("subtitleLabel", movie.optBoolean("subtitlesOff", true) ? "Aucun" : "Automatiques"));
            s.put("alarmMode", "armed-engine-test");
            s.put("createdAt", System.currentTimeMillis());
            JSONArray a = AppState.schedules(this);
            a.put(s);
            AppState.setSchedules(this, a);
            AppState.setShabbatArmed(this, true);
            LogStore.add(this, "Test", "Mode Shabbat 20 min armé · écran noir 10 min · countdown 10 min · volume 37 %");
            openArmedMode();
        } catch (Exception e) {
            automationNote.setText("Impossible de préparer le test : " + e.getMessage());
        }
    }

    private void openArmedMode() {
        Intent i = new Intent(this, ShabbatModeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    private LinearLayout stateColumn(String label, TextView value){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.addView(Ui.eyebrow(this,label));value.setMaxLines(2);value.setEllipsize(android.text.TextUtils.TruncateAt.END);c.addView(value,Ui.lp(-1,-2,this,4));return c;}
    private LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-1,1);}
    private LinearLayout.LayoutParams gapWeight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(Ui.dp(this,Ui.compact(this)?6:10),0,0,0);return p;}

    private void refresh(){
        if(plexState==null)return;
        boolean plex=AppState.plexConnected(this); JSONObject m=AppState.selectedMovie(this); int n=AppState.schedules(this).length(); String server=AppState.prefs(this).getString("plex_server_name","Plex");
        plexState.setText(plex?server:"À connecter");plexState.setTextColor(plex?Ui.GOOD:Ui.TEXT);movieState.setText(m==null?"Aucun film":m.optString("title","Film"));scheduleState.setText(n==0?"Aucune séance":n+" séance"+(n>1?"s":""));
        boolean armed = AppState.isShabbatArmed(this);
        if (armButton != null) armButton.setText(armed ? "Mode Shabbat armé ✓" : "Armer Shabbat");
        if(automationNote!=null)automationNote.setText("Mode longue durée : Android reste actif et l’écran OLED est noir jusqu’au countdown des 10 dernières minutes. Les AlarmClock/WakeReceiver restent uniquement en secours. Volume films fixé à "+AppState.FILM_VOLUME_PERCENT+" %. Extinction Philips : "+(PhilipsTvClient.isPaired(this)?"prête ✓":"à associer dans Tests")+".");
    }

    private void requestExact(){if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S){try{Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);i.setData(Uri.parse("package:"+getPackageName()));startActivity(i);}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}}}
}
