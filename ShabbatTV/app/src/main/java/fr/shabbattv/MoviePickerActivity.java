package fr.shabbattv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class MoviePickerActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private EditText search;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Bibliothèque", "Films", "Recherche sur le serveur Plex sélectionné, puis choisis le film à programmer.");

        LinearLayout searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        search = Ui.input(this, "Rechercher un film…");
        searchRow.addView(search, new LinearLayout.LayoutParams(0, Ui.dp(this, Ui.controlHeight(this)), 1));
        Button go = Ui.button(this, "Rechercher", true);
        int buttonWidthDp = Ui.compact(this) ? 145 : 180;
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(Ui.dp(this, buttonWidthDp), Ui.dp(this, Ui.controlHeight(this)));
        glp.setMargins(Ui.dp(this, Ui.compact(this)?6:10),0,0,0);
        searchRow.addView(go, glp);
        go.setOnClickListener(v -> load());
        root.addView(searchRow, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, Ui.compact(this)?14:22));

        status = Ui.muted(this, "Chargement…");
        status.setMaxLines(2);
        root.addView(status, Ui.lp(-1,-2,this,8));

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(false);
        sv.setSmoothScrollingEnabled(true);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list, new ScrollView.LayoutParams(-1,-2));
        LinearLayout.LayoutParams svp = new LinearLayout.LayoutParams(-1,0,1);
        svp.setMargins(0, Ui.dp(this,6), 0, 0);
        root.addView(sv, svp);

        setContentView(root);
        load();
    }

    private void load() {
        if(!AppState.plexConnected(this)){
            status.setText("Plex n’est pas encore connecté. Reviens à l’accueil puis ouvre Plex.");
            return;
        }
        String server = AppState.prefs(this).getString("plex_server_name","Plex");
        status.setText("Chargement depuis “" + server + "”…");
        list.removeAllViews();
        String q = search.getText().toString();
        new Thread(() -> {
            try {
                List<PlexClient.Movie> movies = PlexClient.searchMovies(this,q);
                runOnUiThread(() -> show(movies));
            } catch(Exception e){
                runOnUiThread(() -> status.setText("Erreur Plex : "+e.getMessage()));
            }
        }).start();
    }

    private void show(List<PlexClient.Movie> movies){
        String server = AppState.prefs(this).getString("plex_server_name","Plex");
        status.setText(movies.size()+" film"+(movies.size()>1?"s":"")+" trouvé"+(movies.size()>1?"s":"")+" · "+server);
        int max = Math.min(100,movies.size());
        for(int i=0;i<max;i++){
            PlexClient.Movie m = movies.get(i);
            String label = m.title + (m.year==null || m.year.isEmpty() ? "" : "  ·  "+m.year);
            Button button = Ui.button(this,label,false);
            button.setTypeface(null, android.graphics.Typeface.NORMAL);
            button.setOnClickListener(v -> {
                AppState.setSelectedMovie(this,m.json());
                Intent in = new Intent(this,MainActivity.class);
                in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(in);
                finish();
            });
            list.addView(button,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,6));
        }
        if (movies.isEmpty()) {
            list.addView(Ui.muted(this,"Aucun résultat. Essaie un autre titre ou vérifie le serveur Plex sélectionné."), Ui.lp(-1,-2,this,8));
        }
    }
}
