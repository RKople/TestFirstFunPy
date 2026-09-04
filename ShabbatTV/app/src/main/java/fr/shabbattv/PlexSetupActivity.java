package fr.shabbattv;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class PlexSetupActivity extends Activity {
    private TextView code, status, serverStatus;
    private Button generate, verify, refreshServers;
    private LinearLayout serverList;
    private long pinId;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);

        root.addView(Ui.eyebrow(this, "Connexion"));
        root.addView(Ui.title(this, "Plex"), Ui.lp(-1,-2,this,5));
        root.addView(Ui.subtitle(this, "Relie ton compte, puis choisis le serveur Plex à utiliser. Les serveurs partagés sont pris en charge."), Ui.lp(-1,-2,this,7));

        LinearLayout linkCard = Ui.card(this);
        linkCard.addView(Ui.eyebrow(this, "Compte Plex"));
        code = Ui.title(this, "— — — —");
        code.setTextSize(48);
        code.setLetterSpacing(.16f);
        code.setGravity(Gravity.START);
        linkCard.addView(code, Ui.lp(-1,-2,this,10));

        status = Ui.muted(this, "Génère un code, puis ouvre plex.tv/link sur ton téléphone.");
        status.setLineSpacing(0,1.12f);
        linkCard.addView(status, Ui.lp(-1,-2,this,8));

        generate = Ui.button(this, "Générer un nouveau code", true);
        generate.setOnClickListener(v -> createPin());
        linkCard.addView(generate, Ui.lp(-1,Ui.dp(this,58),this,18));

        verify = Ui.button(this, "J’ai validé le code", false);
        verify.setOnClickListener(v -> checkPin());
        linkCard.addView(verify, Ui.lp(-1,Ui.dp(this,58),this,10));
        root.addView(linkCard, Ui.lp(-1,-2,this,28));

        LinearLayout serversCard = Ui.card(this);
        serversCard.addView(Ui.eyebrow(this, "Serveur utilisé"));
        serverStatus = Ui.muted(this, "Connecte d’abord ton compte Plex.");
        serversCard.addView(serverStatus, Ui.lp(-1,-2,this,9));
        serverList = new LinearLayout(this);
        serverList.setOrientation(LinearLayout.VERTICAL);
        serversCard.addView(serverList, Ui.lp(-1,-2,this,8));
        refreshServers = Ui.button(this, "Rafraîchir les serveurs", false);
        refreshServers.setOnClickListener(v -> loadServers());
        serversCard.addView(refreshServers, Ui.lp(-1,Ui.dp(this,54),this,12));
        root.addView(serversCard, Ui.lp(-1,-2,this,14));

        LinearLayout help = Ui.card(this);
        TextView h = Ui.body(this, "Connexion rapide");
        h.setTypeface(null, Typeface.BOLD);
        help.addView(h);
        help.addView(Ui.muted(this,
                "1  Génère un code\n" +
                "2  Ouvre plex.tv/link sur ton téléphone\n" +
                "3  Saisis les 4 caractères\n" +
                "4  Valide sur la TV\n" +
                "5  Choisis ensuite le bon serveur, même s’il appartient à un ami"), Ui.lp(-1,-2,this,8));
        root.addView(help, Ui.lp(-1,-2,this,14));

        setContentView(root);
        refreshAccountUi();
        if (hasAccountToken()) loadServers();
    }

    private boolean hasAccountToken() {
        return !AppState.prefs(this).getString("plex_account_token", "").isEmpty();
    }

    private void refreshAccountUi() {
        if (hasAccountToken()) {
            code.setText("✓");
            code.setLetterSpacing(0);
            code.setTextColor(Ui.GOOD);
            status.setText("Compte Plex lié. Choisis ci-dessous le serveur qui contient les films à utiliser.");
            verify.setText("Compte lié");
        }
    }

    private void createPin() {
        code.setText("…");
        code.setLetterSpacing(0);
        code.setTextColor(Ui.TEXT);
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
                        status.setText("Plex a renvoyé un code inattendu (" + display.length() + " caractères). Génère un nouveau code.");
                    } else {
                        status.setText("Code prêt. Ouvre plex.tv/link sur ton téléphone et saisis ces 4 caractères.");
                    }
                    generate.setEnabled(true);
                    verify.requestFocus();
                });
            } catch(Exception e) {
                runOnUiThread(() -> {
                    status.setText("Impossible de créer le code : " + e.getMessage());
                    generate.setEnabled(true);
                });
            }
        }).start();
    }

    private void checkPin() {
        if (pinId == 0) pinId = AppState.prefs(this).getLong("plex_pin_id", 0);
        if (pinId == 0) {
            status.setText("Génère d’abord un code Plex.");
            return;
        }
        status.setText("Vérification de la liaison…");
        verify.setEnabled(false);
        final long id = pinId;
        new Thread(() -> {
            try {
                PlexClient.Pin p = PlexClient.checkPin(this, id);
                if (p.token == null || p.token.isEmpty()) {
                    runOnUiThread(() -> {
                        status.setText("Pas encore validé côté Plex. Vérifie le code sur plex.tv/link puis réessaie.");
                        verify.setEnabled(true);
                    });
                    return;
                }
                AppState.prefs(this).edit().putString("plex_account_token", p.token).apply();
                runOnUiThread(() -> {
                    verify.setEnabled(true);
                    refreshAccountUi();
                    loadServers();
                });
            } catch(Exception e) {
                runOnUiThread(() -> {
                    status.setText("Erreur Plex : " + e.getMessage());
                    verify.setEnabled(true);
                });
            }
        }).start();
    }

    private void loadServers() {
        String token = AppState.prefs(this).getString("plex_account_token", "");
        if (token.isEmpty()) {
            serverStatus.setText("Connecte d’abord ton compte Plex.");
            return;
        }

        serverStatus.setText("Recherche des serveurs accessibles…");
        serverList.removeAllViews();
        refreshServers.setEnabled(false);

        new Thread(() -> {
            try {
                List<PlexClient.Server> servers = PlexClient.listServers(this, token);
                runOnUiThread(() -> renderServers(token, servers));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    serverStatus.setText("Impossible de charger les serveurs : " + e.getMessage());
                    refreshServers.setEnabled(true);
                });
            }
        }).start();
    }

    private void renderServers(String accountToken, List<PlexClient.Server> servers) {
        serverList.removeAllViews();
        refreshServers.setEnabled(true);
        if (servers == null || servers.isEmpty()) {
            serverStatus.setText("Aucun serveur Plex accessible avec ce compte.");
            return;
        }

        String selectedMachine = AppState.prefs(this).getString("plex_server_machine_id", "");
        String selectedName = AppState.prefs(this).getString("plex_server_name", "");
        serverStatus.setText(servers.size() + (servers.size() > 1 ? " serveurs disponibles. Choisis celui qui contient tes films." : " serveur disponible."));

        for (PlexClient.Server s : servers) {
            boolean selected = (!selectedMachine.isEmpty() && selectedMachine.equals(s.machineId))
                    || (selectedMachine.isEmpty() && !selectedName.isEmpty() && selectedName.equals(s.name));
            String type = s.owned ? "Mon serveur" : "Partagé avec moi";
            String label = (selected ? "✓  " : "") + s.name + "   ·   " + type;
            Button b = Ui.button(this, label, selected);
            b.setOnClickListener(v -> chooseServer(accountToken, s, b));
            serverList.addView(b, Ui.lp(-1, Ui.dp(this, 56), this, 8));
        }
    }

    private void chooseServer(String accountToken, PlexClient.Server server, Button source) {
        serverStatus.setText("Test de connexion à “" + server.name + "”…");
        setServerButtonsEnabled(false);
        new Thread(() -> {
            try {
                PlexClient.selectServer(this, accountToken, server);
                AppState.setSelectedMovie(this, null);
                runOnUiThread(() -> {
                    serverStatus.setText("✓ “" + server.name + "” est sélectionné et joignable. Tu peux aller dans Films.");
                    loadServers();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    serverStatus.setText("“" + server.name + "” est actuellement inaccessible : " + e.getMessage());
                    setServerButtonsEnabled(true);
                });
            }
        }).start();
    }

    private void setServerButtonsEnabled(boolean enabled) {
        for (int i = 0; i < serverList.getChildCount(); i++) {
            View v = serverList.getChildAt(i);
            v.setEnabled(enabled);
        }
        refreshServers.setEnabled(enabled);
    }
}
