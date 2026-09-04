package fr.shabbattv;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class MovieOptionsActivity extends Activity {
    private JSONObject movie;
    private PlexClient.PlaybackOptions options;
    private LinearLayout audioList, subtitleList;
    private TextView status;
    private int audioIndex = -1;
    private int subtitleIndex = -1; // -1 = off

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            String raw=getIntent().getStringExtra("movie");
            movie=raw==null||raw.isEmpty()?AppState.selectedMovie(this):new JSONObject(raw);
        } catch(Exception e){ movie=AppState.selectedMovie(this); }

        LinearLayout root=Ui.page(this);
        Ui.header(root,this,"Lecture Plex","Langue & sous-titres","Choisis ce qui sera utilisé automatiquement pour ce film et pour les séances programmées.");

        LinearLayout film=Ui.card(this);
        film.addView(Ui.eyebrow(this,"Film"));
        TextView title=Ui.body(this,movie==null?"Aucun film":movie.optString("title","Film"));
        title.setTypeface(null,android.graphics.Typeface.BOLD);
        film.addView(title,Ui.lp(-1,-2,this,5));
        root.addView(film,Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        LinearLayout audioCard=Ui.card(this);
        audioCard.addView(Ui.eyebrow(this,"Langue audio"));
        audioList=new LinearLayout(this);audioList.setOrientation(LinearLayout.VERTICAL);
        audioCard.addView(audioList,Ui.lp(-1,-2,this,5));
        root.addView(audioCard,Ui.lp(-1,-2,this,10));

        LinearLayout subCard=Ui.card(this);
        subCard.addView(Ui.eyebrow(this,"Sous-titres"));
        subtitleList=new LinearLayout(this);subtitleList.setOrientation(LinearLayout.VERTICAL);
        subCard.addView(subtitleList,Ui.lp(-1,-2,this,5));
        root.addView(subCard,Ui.lp(-1,-2,this,10));

        status=Ui.muted(this,"Chargement des pistes Plex…");
        root.addView(status,Ui.lp(-1,-2,this,8));

        Button save=Ui.button(this,"Enregistrer ce choix",true);
        save.setOnClickListener(v->save());
        root.addView(save,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,10));

        Ui.setScrollable(this,root);
        load();
    }

    private void load(){
        if(movie==null){status.setText("Aucun film sélectionné.");return;}
        new Thread(()->{
            try{
                PlexClient.PlaybackOptions o=PlexClient.playbackOptions(this,movie);
                runOnUiThread(()->{options=o;chooseDefaults();render();status.setText(o.audio.size()+" piste(s) audio · "+o.subtitles.size()+" piste(s) de sous-titres");});
            }catch(Exception e){runOnUiThread(()->status.setText("Impossible de charger les pistes Plex : "+e.getMessage()));}
        }).start();
    }

    private void chooseDefaults(){
        String savedAudio=movie.optString("audioLanguage","");
        String savedSub=movie.optString("subtitleLanguage","");
        boolean hasSubChoice=movie.has("subtitlesOff");
        boolean subOff=movie.optBoolean("subtitlesOff",true);
        audioIndex=-1;
        for(int i=0;i<options.audio.size();i++){
            PlexClient.StreamOption s=options.audio.get(i);
            if(!savedAudio.isEmpty()&&savedAudio.equalsIgnoreCase(s.languageCode)){audioIndex=i;break;}
            if(savedAudio.isEmpty()&&s.selected)audioIndex=i;
        }
        if(audioIndex<0&&!options.audio.isEmpty())audioIndex=0;
        subtitleIndex=-1;
        if(!subOff||!hasSubChoice){
            for(int i=0;i<options.subtitles.size();i++){
                PlexClient.StreamOption s=options.subtitles.get(i);
                if(!savedSub.isEmpty()&&savedSub.equalsIgnoreCase(s.languageCode)){subtitleIndex=i;break;}
                if(savedSub.isEmpty()&&s.selected)subtitleIndex=i;
            }
        }
        if(hasSubChoice&&subOff)subtitleIndex=-1;
    }

    private void render(){
        audioList.removeAllViews();subtitleList.removeAllViews();
        for(int i=0;i<options.audio.size();i++){
            final int idx=i;PlexClient.StreamOption s=options.audio.get(i);
            Button b=Ui.button(this,(i==audioIndex?"✓  ":"")+s.label,false);
            b.setTypeface(null,android.graphics.Typeface.NORMAL);
            b.setOnClickListener(v->{audioIndex=idx;render();});
            audioList.addView(b,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,5));
        }
        Button off=Ui.button(this,(subtitleIndex<0?"✓  ":"")+"Aucun sous-titre",false);
        off.setTypeface(null,android.graphics.Typeface.NORMAL);off.setOnClickListener(v->{subtitleIndex=-1;render();});
        subtitleList.addView(off,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,5));
        for(int i=0;i<options.subtitles.size();i++){
            final int idx=i;PlexClient.StreamOption s=options.subtitles.get(i);
            Button b=Ui.button(this,(i==subtitleIndex?"✓  ":"")+s.label,false);
            b.setTypeface(null,android.graphics.Typeface.NORMAL);b.setOnClickListener(v->{subtitleIndex=idx;render();});
            subtitleList.addView(b,Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,5));
        }
    }

    private void save(){
        if(movie==null){finish();return;}
        try{
            if(options!=null&&audioIndex>=0&&audioIndex<options.audio.size()){
                PlexClient.StreamOption a=options.audio.get(audioIndex);
                movie.put("audioLanguage",a.languageCode);movie.put("audioLabel",a.label);
            }
            if(options==null||subtitleIndex<0){
                movie.put("subtitlesOff",true);movie.put("subtitleLanguage","");movie.put("subtitleLabel","Aucun");movie.put("subtitleKey","");movie.put("subtitleCodec","");
            }else{
                PlexClient.StreamOption s=options.subtitles.get(subtitleIndex);
                movie.put("subtitlesOff",false);movie.put("subtitleLanguage",s.languageCode);movie.put("subtitleLabel",s.label);movie.put("subtitleKey",s.key);movie.put("subtitleCodec",s.codec);
            }
            AppState.setSelectedMovie(this,movie);
            LogStore.add(this,"Film","Options : "+movie.optString("audioLabel","auto")+" · sous-titres "+movie.optString("subtitleLabel","Aucun"));
        }catch(Exception ignored){}
        finish();
    }
}
