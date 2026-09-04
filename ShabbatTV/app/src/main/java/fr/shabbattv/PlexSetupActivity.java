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
    private LinearLayout serverList, helpCard;
    private long pinId;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Configuration", "Plex", "Relie ton compte une fois, puis choisis le serveur qui contient les films à utiliser.");

        LinearLayout linkCard = Ui.card(this);
        linkCard.addView(Ui.eyebrow(this, "Compte Plex"));
        code = Ui.title(this, "— — — —");
        code.setTextSize(Ui.compact(this) ? 36 : 46);
        code.setLetterSpacing(.14f);
        code.setGravity(Gravity.START);
        code.setSingleLine(true);
        linkCard.addView(code, Ui.lp(-1,-2,this,Ui.compact(this)?6:9));

        status = Ui.muted(this, "Génère un code, puis ouvre plex.tv/link sur ton téléphone.");
        linkCard.addView(status, Ui.lp(-1,-2,this,5));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        generate = Ui.button(this, "Générer un code", true);
        generate.setOnClickListener(v -> createPin());
        verify = Ui.button(this, "J’ai validé le code", false);
        verify.setOnClickListener(v -> checkPin());
        actions.addView(generate, new LinearLayout.LayoutParams(0, Ui.dp(this, Ui.controlHeight(this)), 1));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, Ui.dp(this, Ui.controlHeight(this)), 1);
        vp.setMargins(Ui.dp(this, Ui.compact(this)?6:10),0,0,0);
        actions.addView(verify, vp);
        linkCard.addView(actions, Ui.lp(-1, Ui.dp(this, Ui.controlHeight(this)), this, Ui.compact(this)?10:16));
        root.addView(linkCard, Ui.lp(-1,-2,this,Ui.compact(this)?14:22));

        LinearLayout serversCard = Ui.card(this);
        serversCard.addView(Ui.eyebrow(this, "Serveur utilisé"));
        serverStatus = Ui.muted(this, "Connecte d’abord ton compte Plex.");
        serversCard.addView(serverStatus, Ui.lp(-1,-2,this,6));
        serverList = new LinearLayout(this);
        serverList.setOrientation(LinearLayout.VERTICAL);
        serversCard.addView(serverList, Ui.lp(-1,-2,this,5));
        refreshServers = Ui.button(this, "Rafraîchir les serveurs", false);
        refreshServers.setOnClickListener(v -> loadServers());
        serversCard.addView(refreshServers, Ui.lp(-1,Ui.dp(this,Ui.smallControlHeight(this)),this,8));
        root.addView(serversCard, Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        helpCard = Ui.card(this);
        TextView h = Ui.body(this, "Connexion rapide");
        h.setTypeface(null, Typeface.BOLD);
        helpCard.addView(h);
        helpCard.addView(Ui.muted(this,
                "1  Génère un code  ·  2  Ouvre plex.tv/link  ·  3  Saisis les 4 caractères\n" +
                "4  Valide sur la TV  ·  5  Choisis ensuite le bon serveur, y compris un serveur partagé"),
                Ui.lp(-1,-2,this,6));
        root.addView(helpCard, Ui.lp(-1,-2,this,Ui.compact(this)?10:14));

        Ui.setScrollable(this, root);
        refreshAccountUi();
        if (hasAccountToken()) loadServers();
    }

    private boolean hasAccountToken() {
        return !AppState.prefs(this).getString("plex_account_token", "").isEmpty();
    }

    private void refreshAccountUi() {
        if (hasAccountToken()) {
            code.setText("✓  Compte lié");
            code.setLetterSpacing(0);
            code.setTextSize(Ui.compact(this) ? 23 : 28);
            code.setTextColor(Ui.GOOD);
            status.setText("Connexion Plex active. Sélectionne ci-dessous le serveur à utiliser.");
            generate.setText("Relier un autre compte");
            verify.setVisibility(View.GONE);
            helpCard.setVisibility(View.GONE);
        } else {
            code.setTextColor(Ui.TEXT);
            verify.setVisibility(View.VISIBLE);
            helpCard.setVisibility(View.VISIBLE);
        }
    }

    private void createPin() {
        code.setText("…");
        code.setTextSize(Ui.compact(this) ? 36 : 46);
        code.setLetterSpacing(0);
        code.setTextColor(Ui.TEXT);
        verify.setVisibility(View.VISIBLE);
        helpCard.setVisibility(View.VISIBLE);
        status.setText("Création du code Plex…");
        generate.setEnabled(false);
        new Thread(() -> {
            try {
                PlexClient.Pin p = PlexClient.createPin(this);
                pinId = p.id;
                AppState.prefs(this).edit().putLong("plex_pin_id", pinId).apply();
                runOnUiThread(() -> {
                    String display = p.code == null ? "" : p.code.trim().toUpperCase();
                    code.setLetterSpacing(.14f);
                    code.setText(display);
                    if (display.length() != 4) {
                        status.setText("Code Plex inattendu. Génère un nouveau code.");
                    } else {
                        status.setText("Ouvre plex.tv/link sur ton téléphone et saisis ces 4 caractères.");
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
                        status.setText("Pas encore validé côté Plex. Vérifie le code puis réessaie.");
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
        serverStatus.setText(servers.size() + (servers.size() > 1 ? " serveurs disponibles" : " serveur disponible"));

        for (PlexClient.Server s : servers) {
            boolean selected = (!selectedMachine.isEmpty() && selectedMachine.equals(s.machineId))
                    || (selectedMachine.isEmpty() && !selectedName.isEmpty() && selectedName.equals(s.name));
            String type = s.owned ? "Mon serveur" : "Partagé avec moi";
            String label = (selected ? "✓  " : "") + s.name + "  ·  " + type;
            Button b = Ui.button(this, label, selected);
            b.setOnClickListener(v -> chooseServer(accountToken, s));
            serverList.addView(b, Ui.lp(-1, Ui.dp(this, Ui.smallControlHeight(this)), this, 6));
        }
    }

    private void chooseServer(String accountToken, PlexClient.Server server) {
        serverStatus.setText("Test de connexion à “" + server.name + "”…");
        setServerButtonsEnabled(false);
        new Thread(() -> {
            try {
                PlexClient.selectServer(this, accountToken, server);
                AppState.setSelectedMovie(this, null);
                runOnUiThread(() -> {
                    serverStatus.setText("✓ “" + server.name + "” est sélectionné. Tu peux ouvrir Films.");
                    loadServers();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    serverStatus.setText("“" + server.name + "” est inaccessible : " + e.getMessage());
                    setServerButtonsEnabled(true);
                });
            }
        }).start();
    }

    private void setServerButtonsEnabled(boolean enabled) {
        for (int i = 0; i < serverList.getChildCount(); i++) serverList.getChildAt(i).setEnabled(enabled);
        refreshServers.setEnabled(enabled);
    }
}
