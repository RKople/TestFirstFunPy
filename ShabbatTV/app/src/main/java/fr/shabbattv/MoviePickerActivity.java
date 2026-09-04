package fr.shabbattv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
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
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(50,35,50,35); root.setBackgroundColor(Color.rgb(8,12,20));
        TextView title=new TextView(this); title.setText("Films Plex"); title.setTextColor(Color.WHITE); title.setTextSize(32); root.addView(title);
        search=new EditText(this); search.setHint("Rechercher un film…"); search.setTextColor(Color.WHITE); search.setHintTextColor(Color.GRAY); root.addView(search,new LinearLayout.LayoutParams(-1,70));
        Button go=new Button(this); go.setText("RECHERCHER"); go.setOnClickListener(v -> load()); root.addView(go,new LinearLayout.LayoutParams(-1,70));
        status=new TextView(this); status.setTextColor(Color.LTGRAY); status.setTextSize(17); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.setMargins(0,10,0,10); root.addView(status,slp);
        ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        load();
    }

    private void load() {
        if(!AppState.plexConnected(this)){status.setText("Connecte Plex d’abord.");return;}
        status.setText("Chargement de la bibliothèque…"); list.removeAllViews();
        String q=search.getText().toString();
        new Thread(() -> { try { List<PlexClient.Movie> movies=PlexClient.searchMovies(this,q); runOnUiThread(() -> show(movies)); } catch(Exception e){ runOnUiThread(() -> status.setText("Erreur Plex : "+e.getMessage())); }}).start();
    }
    private void show(List<PlexClient.Movie> movies){
        status.setText(movies.size()+" film(s). Sélectionne-en un.");
        int max=Math.min(80,movies.size());
        for(int i=0;i<max;i++){
            PlexClient.Movie m=movies.get(i);
            Button b=new Button(this); b.setAllCaps(false); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL); b.setText(m.title+(m.year==null?"":"  ("+m.year+")"));
            b.setOnClickListener(v -> { AppState.setSelectedMovie(this,m.json()); status.setText("✅ Sélectionné : "+m.title); Intent in=new Intent(this,MainActivity.class); in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); startActivity(in); finish(); });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,72); lp.setMargins(0,4,0,4); list.addView(b,lp);
        }
    }
}
