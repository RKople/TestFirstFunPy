package fr.shabbattv;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LogsActivity extends Activity {
    private TextView logs;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.page(this);
        Ui.header(root, this, "Diagnostic", "Journal", "Démarrage, vidéo noire, horaires réels, lecture, erreurs et interruptions. Les 180 derniers événements restent sur la TV.");
        LinearLayout actions = new LinearLayout(this);
        Button refresh = Ui.button(this, "Rafraîchir", true);
        refresh.setOnClickListener(v -> refresh());
        Button clear = Ui.button(this, "Effacer le journal", false);
        clear.setOnClickListener(v -> { LogStore.clear(this); refresh(); });
        actions.addView(refresh, new LinearLayout.LayoutParams(0, Ui.dp(this, Ui.controlHeight(this)), 1));
        actions.addView(clear, new LinearLayout.LayoutParams(0, Ui.dp(this, Ui.controlHeight(this)), 1));
        root.addView(actions, Ui.lp(-1, -2, this, 12));
        logs = Ui.muted(this, ""); logs.setFocusable(true);
        root.addView(logs, Ui.lp(-1, -2, this, 12));
        Ui.setScrollable(this, root);
    }
    @Override protected void onResume() { super.onResume(); refresh(); }
    private void refresh() { if (logs != null) logs.setText(LogStore.text(this)); }
}
