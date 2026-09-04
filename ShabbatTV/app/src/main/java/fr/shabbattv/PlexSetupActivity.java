package fr.shabbattv;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PlexSetupActivity extends Activity {
    private TextView status;
    private long pinId;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER); root.setPadding(70,50,70,50); root.setBackgroundColor(Color.rgb(8,12,20));
        TextView title = new TextView(this); title.setText("Connexion Plex"); title.setTextColor(Color.WHITE); title.setTextSize(34); title.setGravity(Gravity.CENTER); root.addView(title);
        status = new TextView(this); status.setTextColor(Color.LTGRAY); status.setTextSize(20); status.setGravity(Gravity.CENTER); LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2); slp.setMargins(0,25,0,30); root.addView(status,slp);
        Button connect = new Button(this); connect.setText("GÉNÉRER UN CODE PLEX"); connect.setOnClickListener(v -> createPin()); root.addView(connect,new LinearLayout.LayoutParams(700,85));
        Button verify = new Button(this); verify.setText("J’AI VALIDÉ LE CODE — VÉRIFIER"); LinearLayout.LayoutParams vlp=new LinearLayout.LayoutParams(700,85); vlp.setMargins(0,18,0,0); root.addView(verify,vlp); verify.setOnClickListener(v -> checkPin());
        setContentView(root);
        refresh();
    }

    private void refresh() {
        if (AppState.plexConnected(this)) status.setText("✅ Plex connecté\nServeur : " + AppState.prefs(this).getString("plex_server_name","Plex"));
        else status.setText("1. Génère un code.\n2. Sur ton téléphone, ouvre https://plex.tv/link et saisis le code.\n3. Reviens ici et vérifie.");
    }
    private void createPin() { status.setText("Création du code…"); new Thread(() -> { try { PlexClient.Pin p=PlexClient.createPin(this); pinId=p.id; runOnUiThread(() -> status.setText("CODE PLEX :  " + p.code + "\n\nVa sur plex.tv/link depuis ton téléphone, connecte-toi et saisis ce code.")); } catch(Exception e){ runOnUiThread(() -> status.setText("Erreur Plex : "+e.getMessage())); }}).start(); }
    private void checkPin() { if(pinId==0){status.setText("Génère d’abord un code Plex.");return;} status.setText("Vérification…"); new Thread(() -> { try { PlexClient.Pin p=PlexClient.checkPin(this,pinId); if(p.token==null||p.token.isEmpty()){runOnUiThread(() -> status.setText("Code pas encore validé. Valide-le sur plex.tv/link puis réessaie."));return;} PlexClient.discoverServer(this,p.token); runOnUiThread(this::refresh); } catch(Exception e){runOnUiThread(() -> status.setText("Erreur : "+e.getMessage()));}}).start(); }
}
