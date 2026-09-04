package fr.shabbattv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
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
        root.addView(Ui.eyebrow(this,"Bibliothèque"));
        root.addView(Ui.title(this,"Choisir un film"),Ui.lp(-1,-2,this,5));
        root.addView(Ui.subtitle(this,"Recherche dans tes bibliothèques Plex puis sélectionne le film à programmer."),Ui.lp(-1,-2,this,7));

        LinearLayout searchRow=new LinearLayout(this); searchRow.setOrientation(LinearLayout.HORIZONTAL);
        search=Ui.input(this,"Rechercher un film…"); searchRow.addView(search,new LinearLayout.LayoutParams(0,Ui.dp(this,58),1));
        Button go=Ui.button(this,"Rechercher",true); LinearLayout.LayoutParams glp=new LinearLayout.LayoutParams(Ui.dp(this,180),Ui.dp(this,58)); glp.setMargins(Ui.dp(this,10),0,0,0); searchRow.addView(go,glp);
        go.setOnClickListener(v->load()); root.addView(searchRow,Ui.lp(-1,Ui.dp(this,58),this,24));

        status=Ui.muted(this,"Chargement…"); root.addView(status,Ui.lp(-1,-2,this,12));

        ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); load();
    }

    private void load() {
        if(!AppState.plexConnected(this)){status.setText("Plex n’est pas encore connecté.");return;}
        status.setText("Chargement de la bibliothèque…"); list.removeAllViews();
        String q=search.getText().toString();
        new Thread(() -> { try { List<PlexClient.Movie> movies=PlexClient.searchMovies(this,q); runOnUiThread(() -> show(movies)); } catch(Exception e){ runOnUiThread(() -> status.setText("Erreur Plex : "+e.getMessage())); }}).start();
    }

    private void show(List<PlexClient.Movie> movies){
        status.setText(movies.size()+" film"+(movies.size()>1?"s":"")+" trouvé"+(movies.size()>1?"s":""));
        int max=Math.min(80,movies.size());
        for(int i=0;i<max;i++){
            PlexClient.Movie m=movies.get(i);
            Button b=Ui.button(this,m.title+(m.year==null?"":"   ·   "+m.year),false);
            b.setTypeface(null, android.graphics.Typeface.NORMAL);
            b.setOnClickListener(v -> { AppState.setSelectedMovie(this,m.json()); Intent in=new Intent(this,MainActivity.class); in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); startActivity(in); finish(); });
            list.addView(b,Ui.lp(-1,Ui.dp(this,58),this,8));
        }
    }
}
