package fr.shabbattv;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;

public class ScheduleDetailActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String id = getIntent().getStringExtra("schedule_id");
        JSONObject s = AppState.scheduleById(this, id);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Planning", s == null ? "Séance introuvable" : s.optString("title", "Film"), "Une seule lecture programmée, sans commande d’allumage ni d’extinction.");
        LinearLayout card = Ui.card(this);
        if (s == null) card.addView(Ui.body(this, "Cette séance a déjà été lancée ou supprimée."));
        else {
            long when = s.optLong("when"), duration = s.optLong("durationMs");
            String text = "Début : " + DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(new Date(when))
                + "\n\nAvant : vidéo noire en boucle, puis compte à rebours dans les 10 dernières minutes."
                + "\nAprès : retour à la vidéo noire, y compris après le dernier film."
                + "\n\nAudio : " + s.optString("audioLabel", "Automatique")
                + "\nSous-titres : " + s.optString("subtitleLabel", "Aucun")
                + "\nVolume : " + AppState.FILM_VOLUME_PERCENT + " %"
                + "\nServeur Plex : " + s.optString("server", "Plex")
                + (duration > 0L ? "\nDurée indicative : " + Math.round(duration / 60_000.0) + " min" : "\nDurée inconnue : vérifie l’espacement des séances.")
                + "\n\nLa lecture d’un film n’est pas coupée pour en démarrer un autre. Une séance trop tardive est signalée dans le journal, pas relancée plusieurs heures après.";
            card.addView(Ui.body(this, text));
        }
        root.addView(card, Ui.lp(-1, -2, this, 14));
        if (s != null) {
            Button delete = Ui.button(this, "Supprimer cette séance", false);
            delete.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Supprimer la séance ?")
                .setMessage(s.optString("title", "Film")).setNegativeButton("Garder", null)
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    AppState.removeSchedule(this, id);
                    LogStore.add(this, "Planning", "Séance supprimée : " + s.optString("title", "Film"));
                    finish();
                }).show());
            root.addView(delete, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 12));
        }
        Button back = Ui.button(this, "Retour au planning", true);
        back.setOnClickListener(v -> finish());
        root.addView(back, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 8));
        Ui.setScrollable(this, root);
    }
}
