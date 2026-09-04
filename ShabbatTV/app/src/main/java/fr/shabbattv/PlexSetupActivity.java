package fr.shabbattv;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlexSetupActivity extends Activity {
    private TextView code, status;
    private Button generate, verify;
    private long pinId;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);

        root.addView(Ui.eyebrow(this, "Connexion"));
        root.addView(Ui.title(this, "Plex"), Ui.lp(-1,-2,this,5));
        root.addView(Ui.subtitle(this, "Relie ton compte Plex une seule fois. Aucun mot de passe n’est saisi sur la TV."), Ui.lp(-1,-2,this,7));

        LinearLayout card = Ui.card(this);
        TextView step = Ui.eyebrow(this, "Code de liaison"); card.addView(step);
        code = Ui.title(this, "— — — —");
        code.setTextSize(48); code.setLetterSpacing(.16f); code.setGravity(Gravity.START); card.addView(code, Ui.lp(-1,-2,this,10));
        status = Ui.muted(this, "Génère un code, puis ouvre plex.tv/link sur ton téléphone.");
        status.setLineSpacing(0,1.12f); card.addView(status, Ui.lp(-1,-2,this,8));

        generate = Ui.button(this, "Générer un nouveau code", true);
        generate.setOnClickListener(v -> createPin()); card.addView(generate, Ui.lp(-1,Ui.dp(this,58),this,18));
        verify = Ui.button(this, "J’ai validé le code", false);
        verify.setOnClickListener(v -> checkPin()); card.addView(verify, Ui.lp(-1,Ui.dp(this,58),this,10));
        root.addView(card, Ui.lp(-1,-2,this,28));

        LinearLayout help = Ui.card(this);
        TextView h = Ui.body(this, "Depuis ton téléphone"); h.setTypeface(null, Typeface.BOLD); help.addView(h);
        help.addView(Ui.muted(this, "1  Ouvre plex.tv/link\n2  Connecte-toi à ton compte Plex\n3  Saisis les 4 caractères affichés ci-dessus\n4  Reviens sur la TV et choisis “J’ai validé le code”"), Ui.lp(-1,-2,this,8));
        root.addView(help, Ui.lp(-1,-2,this,14));

        setContentView(root);
        refresh();
    }

    private void refresh() {
        if (AppState.plexConnected(this)) {
            code.setText("✓"); code.setLetterSpacing(0);
            code.setTextColor(Ui.GOOD);
            status.setText("Connecté à " + AppState.prefs(this).getString("plex_server_name","Plex") + ". Tu peux maintenant choisir un film.");
            verify.setText("Connexion vérifiée");
        }
    }

    private void createPin() {
        code.setText("…"); code.setLetterSpacing(0); code.setTextColor(Ui.TEXT);
        status.setText("Création du code Plex…");
        generate.setEnabled(false);
        new Thread(() -> {
            try {
                PlexClient.Pin p = PlexClient.createPin(this);
                pinId = p.id;
                AppState.prefs(this).edit().putLong("plex_pin_id", pinId).apply();
                runOnUiThread(() -> {
                    String display = p.code == null ? "" : p.code.trim().toUpperCase();
                    code.setLetterSpacing(.16f);
                    code.setText(display);
                    if (display.length() != 4) {
                        status.setText("Plex a renvoyé un code inattendu ("+display.length()+" caractères). Génère un nouveau code.");
                    } else {
                        status.setText("Code prêt. Ouvre plex.tv/link sur ton téléphone et saisis ces 4 caractères.");
                    }
                    generate.setEnabled(true);
                    verify.requestFocus();
                });
            } catch(Exception e) {
                runOnUiThread(() -> { status.setText("Impossible de créer le code : "+e.getMessage()); generate.setEnabled(true); });
            }
        }).start();
    }

    private void checkPin() {
        if(pinId==0) pinId=AppState.prefs(this).getLong("plex_pin_id",0);
        if(pinId==0){ status.setText("Génère d’abord un code Plex."); return; }
        status.setText("Vérification de la liaison…");
        verify.setEnabled(false);
        final long id = pinId;
        new Thread(() -> {
            try {
                PlexClient.Pin p=PlexClient.checkPin(this,id);
                if(p.token==null||p.token.isEmpty()){
                    runOnUiThread(() -> { status.setText("Pas encore validé côté Plex. Vérifie le code sur plex.tv/link puis réessaie."); verify.setEnabled(true); });
                    return;
                }
                PlexClient.discoverServer(this,p.token);
                runOnUiThread(() -> { verify.setEnabled(true); refresh(); });
            } catch(Exception e){ runOnUiThread(() -> { status.setText("Erreur Plex : "+e.getMessage()); verify.setEnabled(true); }); }
        }).start();
    }
}
