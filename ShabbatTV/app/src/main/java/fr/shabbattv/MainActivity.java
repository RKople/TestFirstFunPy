package fr.shabbattv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private TextView status, feedback;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        UpgradeCleanup.run(this);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "v1.11 bêta", "Shabbat TV", "Tes films Plex, aux horaires choisis. Une vidéo noire reste en lecture entre les séances.");
        LinearLayout summary = Ui.card(this);
        status = Ui.body(this, ""); status.setMaxLines(4);
        summary.addView(status); root.addView(summary, Ui.lp(-1, -2, this, 14));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.addView(navButton("Plex", PlexSetupActivity.class), weight(false));
        nav.addView(navButton("Films", MoviePickerActivity.class), weight(true));
        nav.addView(navButton("Planning", ScheduleActivity.class), weight(true));
        root.addView(nav, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 12));

        LinearLayout mode = Ui.card(this);
        mode.addView(Ui.eyebrow(this, "Lecture continue"));
        mode.addView(Ui.body(this, "Vidéo noire → compte à rebours → film → vidéo noire"), Ui.lp(-1, -2, this, 6));
        Button start = Ui.button(this, "Démarrer le mode Shabbat", true);
        start.setOnClickListener(v -> confirmStart(false));
        mode.addView(start, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 12));
        TextView warning = Ui.muted(this, "La TV reste allumée, même quand elle paraît noire. Ne la mets pas en veille. Retour permet d’arrêter le mode.");
        warning.setMaxLines(4); mode.addView(warning, Ui.lp(-1, -2, this, 8));
        root.addView(mode, Ui.lp(-1, -2, this, 14));

        LinearLayout checks = new LinearLayout(this);
        checks.setOrientation(LinearLayout.HORIZONTAL);
        Button test = Ui.button(this, "Essai complet · 20 min", false);
        test.setOnClickListener(v -> confirmStart(true));
        checks.addView(test, weight(false));
        Button preview = Ui.button(this, "Tester le film", false);
        preview.setOnClickListener(v -> preview());
        checks.addView(preview, weight(true));
        checks.addView(navButton("Journal", LogsActivity.class), weight(true));
        root.addView(checks, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 12));
        feedback = Ui.muted(this, ""); feedback.setMaxLines(5);
        root.addView(feedback, Ui.lp(-1, -2, this, 9));
        Ui.setScrollable(this, root);
    }
    @Override protected void onResume() { super.onResume(); refresh(); }

    private Button navButton(String label, Class<? extends Activity> target) {
        Button b = Ui.button(this, label, false);
        b.setOnClickListener(v -> startActivity(new Intent(this, target)));
        return b;
    }
    private LinearLayout.LayoutParams weight(boolean gap) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        if (gap) p.setMargins(Ui.dp(this, 8), 0, 0, 0);
        return p;
    }
    private void refresh() {
        if (status == null) return;
        JSONObject movie = AppState.selectedMovie(this);
        int count = AppState.schedules(this).length();
        status.setText("Plex : " + (AppState.plexConnected(this) ? AppState.prefs(this).getString("plex_server_name", "Connecté") : "À connecter")
            + "\nFilm sélectionné : " + (movie == null ? "Aucun" : movie.optString("title", "Film"))
            + "\nPlanning : " + count + " séance(s) à venir · volume films " + AppState.FILM_VOLUME_PERCENT + " %");
        String error = AppState.prefs(this).getString("mode_error", "");
        if (!error.isEmpty()) feedback.setText(error + "\nConsulte le journal avant de relancer le mode.");
    }
    private void confirmStart(boolean test) {
        if (!AppState.plexConnected(this)) { feedback.setText("Connecte d’abord Plex."); return; }
        if (test && AppState.selectedMovie(this) == null) { feedback.setText("Choisis d’abord le film à tester."); return; }
        if (!test && AppState.schedules(this).length() == 0) { feedback.setText("Ajoute au moins une séance dans Planning."); return; }
        String currentServer = AppState.prefs(this).getString("plex_server_machine_id", "");
        JSONArray sessions = AppState.schedules(this);
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject s = sessions.optJSONObject(i);
            String expected = s == null ? "" : s.optString("serverId", "");
            if (!expected.isEmpty() && !expected.equals(currentServer)) {
                feedback.setText("Une séance utilise un autre serveur Plex. Sélectionne le serveur correspondant ou recrée cette séance."); return;
            }
        }
        new AlertDialog.Builder(this).setTitle(test ? "Essai du mode complet" : "Avant de démarrer")
            .setMessage("Vérifie l’heure de la TV et désactive son minuteur d’arrêt ainsi que son arrêt automatique d’inactivité. Coupe Ambilight pour garder la pièce sombre.\n\nLaisse Shabbat TV au premier plan, sans appuyer sur Marche/Arrêt ni Accueil. La vidéo noire ne remplace pas ces réglages.\n\n" + (test ? "Le film sélectionné commencera dans 20 minutes, via le même planning que les autres séances." : "Le compte à rebours apparaît 10 minutes avant chaque film. Après le dernier film, la vidéo noire continue jusqu’à ton arrêt manuel."))
            .setNegativeButton("Pas maintenant", null)
            .setPositiveButton("Démarrer", (d, which) -> {
                try {
                    if (test) AppState.addSchedule(this, AppState.selectedMovie(this), System.currentTimeMillis() + 20 * 60_000L);
                    AppState.prefs(this).edit().remove("mode_error").apply();
                    AppState.setShabbatArmed(this, true);
                    LogStore.add(this, "Mode Shabbat", "Démarrage manuel" + (test ? " · essai 20 minutes" : ""));
                    startActivity(new Intent(this, ShabbatModeActivity.class));
                } catch (Exception e) { feedback.setText(e.getMessage()); }
            }).show();
    }
    private void preview() {
        JSONObject movie = AppState.selectedMovie(this);
        if (!AppState.plexConnected(this) || movie == null) { feedback.setText("Connecte Plex et sélectionne un film."); return; }
        PlaybackLauncher.launch(this, movie.toString(), "");
    }
}
