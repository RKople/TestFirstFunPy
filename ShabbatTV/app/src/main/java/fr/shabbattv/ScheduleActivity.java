package fr.shabbattv;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class ScheduleActivity extends Activity {
    private TextView movieStatus, feedback, emptyStatus;
    private LinearLayout scheduleList;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Programmation", "Planning", "Choisis un film et son horaire. Après avoir préparé les séances, démarre le mode Shabbat depuis l’accueil.");
        LinearLayout movieCard = Ui.card(this);
        movieCard.addView(Ui.eyebrow(this, "Film sélectionné"));
        movieStatus = Ui.body(this, ""); movieStatus.setMaxLines(5);
        movieCard.addView(movieStatus, Ui.lp(-1, -2, this, 6));
        Button choose = Ui.button(this, "Choisir un film", false);
        choose.setOnClickListener(v -> startActivity(new Intent(this, MoviePickerActivity.class)));
        movieCard.addView(choose, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 9));
        Button tracks = Ui.button(this, "Langue & sous-titres", false);
        tracks.setOnClickListener(v -> {
            if (AppState.selectedMovie(this) == null) feedback.setText("Sélectionne d’abord un film.");
            else startActivity(new Intent(this, MovieOptionsActivity.class));
        });
        movieCard.addView(tracks, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 7));
        root.addView(movieCard, Ui.lp(-1, -2, this, 14));
        Button add = Ui.button(this, "Ajouter une séance · date et heure", true);
        add.setOnClickListener(v -> pickDateTime());
        root.addView(add, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, 12));
        feedback = Ui.muted(this, "Horaires locaux de la TV. Les séances restent modifiables en les supprimant puis en les recréant.");
        feedback.setMaxLines(5); root.addView(feedback, Ui.lp(-1, -2, this, 8));
        LinearLayout card = Ui.card(this); card.addView(Ui.eyebrow(this, "À venir"));
        emptyStatus = Ui.muted(this, "Aucune séance programmée.");
        card.addView(emptyStatus, Ui.lp(-1, -2, this, 6));
        scheduleList = new LinearLayout(this); scheduleList.setOrientation(LinearLayout.VERTICAL);
        card.addView(scheduleList, Ui.lp(-1, -2, this, 4));
        root.addView(card, Ui.lp(-1, -2, this, 12));
        Ui.setScrollable(this, root);
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
    private void refresh() {
        JSONObject movie = AppState.selectedMovie(this);
        movieStatus.setText(movie == null ? "Aucun film" : movie.optString("title", "Film")
            + "\nAudio : " + movie.optString("audioLabel", "Automatique")
            + " · Sous-titres : " + movie.optString("subtitleLabel", "Aucun")
            + "\nVolume : " + AppState.FILM_VOLUME_PERCENT + " %"
            + (movie.optLong("durationMs", 0L) <= 0L ? "\nDurée inconnue : laisse assez de temps avant la séance suivante." : ""));
        JSONArray a = AppState.schedules(this);
        scheduleList.removeAllViews();
        emptyStatus.setVisibility(a.length() == 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        for (int i = 0; i < a.length(); i++) {
            JSONObject s = a.optJSONObject(i); if (s == null) continue;
            String id = s.optString("id", "");
            String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(s.optLong("when")));
            Button row = Ui.button(this, date + " · " + s.optString("title", "Film"), false);
            row.setOnClickListener(v -> startActivity(new Intent(this, ScheduleDetailActivity.class).putExtra("schedule_id", id)));
            scheduleList.addView(row, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 6));
        }
    }
    private void pickDateTime() {
        JSONObject selected = AppState.selectedMovie(this);
        if (selected == null) { feedback.setText("Sélectionne un film d’abord."); return; }
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar date = Calendar.getInstance();
            date.set(Calendar.YEAR, year); date.set(Calendar.MONTH, month); date.set(Calendar.DAY_OF_MONTH, day);
            new TimePickerDialog(this, (time, hour, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hour); date.set(Calendar.MINUTE, minute);
                date.set(Calendar.SECOND, 0); date.set(Calendar.MILLISECOND, 0);
                try {
                    AppState.addSchedule(this, selected, date.getTimeInMillis());
                    feedback.setText("Séance ajoutée. Pense à démarrer le mode Shabbat depuis l’accueil. À la fin du film, retour à la vidéo noire.");
                    refresh();
                } catch (Exception e) { feedback.setText(e.getMessage()); }
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }
}
