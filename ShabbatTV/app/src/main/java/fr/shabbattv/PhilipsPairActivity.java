package fr.shabbattv;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PhilipsPairActivity extends Activity {
    private TextView status;
    private EditText pin;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        Ui.header(root,this,"Contrôle TV","Extinction réelle","Association locale avec l’API Philips. À faire une seule fois avant Chabbat.");

        LinearLayout state = Ui.card(this);
        state.addView(Ui.eyebrow(this,"État"));
        status = Ui.muted(this,"");
        state.addView(status,Ui.lp(-1,-2,this,6));
        root.addView(state,Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        Button start = Ui.button(this,"1 · Démarrer l’association",true);
        start.setOnClickListener(v->beginPair());
        root.addView(start,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,12));

        pin = Ui.input(this,"PIN affiché par la TV");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER);
        root.addView(pin,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));

        Button grant = Ui.button(this,"2 · Valider le PIN",false);
        grant.setOnClickListener(v->grantPair());
        root.addView(grant,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));

        Button test = Ui.button(this,"Tester la mise en veille réelle",false);
        test.setOnClickListener(v->SleepHelper.sleepNow(this,(ok,msg)->status.setText((ok?"✓ ":"✕ ")+msg)));
        root.addView(test,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));

        Ui.setScrollable(this,root);
        refresh();
    }

    @Override protected void onResume(){ super.onResume(); if(status!=null) refresh(); }

    private void refresh(){
        if(PhilipsTvClient.isPaired(this)) {
            status.setText("Associé ✓  ·  API locale " + PhilipsTvClient.pairedHost(this) + "\nLa fin d’un film pourra maintenant mettre réellement la Philips en veille.");
            status.setTextColor(Ui.GOOD);
        } else {
            status.setText("Non associé. Démarre l’association : la Philips doit afficher un PIN, puis saisis-le ici.");
            status.setTextColor(Ui.MUTED);
        }
    }

    private void beginPair(){
        status.setText("Demande d’association en cours…");
        new Thread(()->{
            try{
                PhilipsTvClient.PairRequest r=PhilipsTvClient.beginPair(this);
                LogStore.add(this,"Philips","Demande d’association envoyée");
                runOnUiThread(()->status.setText("Demande envoyée ✓\nUn PIN doit être affiché par la TV. Saisis-le puis valide.\nHôte : "+r.host));
            }catch(Exception e){
                LogStore.add(this,"Erreur","Association Philips : "+e.getMessage());
                runOnUiThread(()->status.setText("Erreur : "+e.getMessage()));
            }
        }).start();
    }

    private void grantPair(){
        String code=pin.getText().toString().trim();
        status.setText("Validation du PIN…");
        new Thread(()->{
            try{
                PhilipsTvClient.grantPair(this,code);
                runOnUiThread(this::refresh);
            }catch(Exception e){
                LogStore.add(this,"Erreur","PIN Philips : "+e.getMessage());
                runOnUiThread(()->status.setText("PIN refusé ou erreur : "+e.getMessage()));
            }
        }).start();
    }
}
