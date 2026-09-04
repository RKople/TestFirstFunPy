package fr.shabbattv;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
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
        int startId=View.generateViewId(); start.setId(startId);
        start.setOnClickListener(v->beginPair());
        root.addView(start,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,12));

        pin = Ui.input(this,"PIN affiché par la TV");
        int pinId=View.generateViewId(); pin.setId(pinId);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER);
        pin.setFilters(new InputFilter[]{new InputFilter.LengthFilter(8)});
        pin.setFocusable(true);
        pin.setFocusableInTouchMode(true);
        pin.setCursorVisible(true);
        pin.setSelectAllOnFocus(true);
        pin.setSingleLine(true);
        pin.setOnFocusChangeListener((v,hasFocus)-> pin.setBackground(Ui.round(this,Ui.SURFACE,hasFocus?Ui.ACCENT:Ui.STROKE,hasFocus?3:1,14)));
        pin.setOnClickListener(v->showKeyboard());
        pin.setOnKeyListener((v,keyCode,event)->{
            if(event.getAction()==KeyEvent.ACTION_UP && (keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_ENTER || keyCode==KeyEvent.KEYCODE_NUMPAD_ENTER)){
                showKeyboard();
                return true;
            }
            return false;
        });
        root.addView(pin,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));

        Button grant = Ui.button(this,"2 · Valider le PIN",false);
        int grantId=View.generateViewId(); grant.setId(grantId);
        grant.setOnClickListener(v->grantPair());
        root.addView(grant,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));

        Button test = Ui.button(this,"Tester la mise en veille réelle",false);
        int testId=View.generateViewId(); test.setId(testId);
        test.setOnClickListener(v->SleepHelper.sleepNow(this,(ok,msg)->status.setText((ok?"✓ ":"✕ ")+msg)));
        root.addView(test,Ui.lp(-1,Ui.dp(this,Ui.controlHeight(this)),this,8));

        // Explicit D-pad navigation for Philips/Google TV. This fixes the PIN field being
        // skipped by the remote focus cursor.
        start.setNextFocusDownId(pinId);
        pin.setNextFocusUpId(startId);
        pin.setNextFocusDownId(grantId);
        grant.setNextFocusUpId(pinId);
        grant.setNextFocusDownId(testId);
        test.setNextFocusUpId(grantId);

        Ui.setScrollable(this,root);
        refresh();
    }

    @Override protected void onResume(){ super.onResume(); if(status!=null) refresh(); }

    private void refresh(){
        if(PhilipsTvClient.isPaired(this)) {
            status.setText("Associé ✓  ·  API locale " + PhilipsTvClient.pairedHost(this) + "\nLa fin d’un film pourra maintenant mettre réellement la Philips en veille.");
            status.setTextColor(Ui.GOOD);
        } else {
            status.setText("Non associé. Démarre l’association : la Philips doit afficher un PIN. Le curseur peut ensuite descendre directement dans le champ PIN.");
            status.setTextColor(Ui.MUTED);
        }
    }

    private void beginPair(){
        status.setText("Demande d’association en cours…");
        new Thread(()->{
            try{
                PhilipsTvClient.PairRequest r=PhilipsTvClient.beginPair(this);
                LogStore.add(this,"Philips","Demande d’association envoyée");
                runOnUiThread(()->{
                    status.setText("Demande envoyée ✓\nSaisis le PIN affiché par la TV puis valide.\nHôte : "+r.host);
                    pin.requestFocus();
                    pin.postDelayed(this::showKeyboard,250L);
                });
            }catch(Exception e){
                LogStore.add(this,"Erreur","Association Philips : "+e.getMessage());
                runOnUiThread(()->status.setText("Erreur : "+e.getMessage()));
            }
        }).start();
    }

    private void showKeyboard(){
        pin.requestFocus();
        pin.setCursorVisible(true);
        try{
            InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm!=null) pin.postDelayed(()->imm.showSoftInput(pin,InputMethodManager.SHOW_IMPLICIT),120L);
        }catch(Throwable ignored){}
    }

    private void grantPair(){
        String code=pin.getText().toString().trim();
        if(code.isEmpty()){
            status.setText("Saisis d’abord le PIN affiché par la TV.");
            showKeyboard();
            return;
        }
        status.setText("Validation du PIN…");
        new Thread(()->{
            try{
                PhilipsTvClient.grantPair(this,code);
                LogStore.add(this,"Philips","Association validée ✓");
                runOnUiThread(this::refresh);
            }catch(Exception e){
                LogStore.add(this,"Erreur","PIN Philips : "+e.getMessage());
                runOnUiThread(()->{status.setText("PIN refusé ou erreur : "+e.getMessage());pin.requestFocus();});
            }
        }).start();
    }
}
