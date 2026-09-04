package fr.shabbattv;

import android.app.Activity;

public final class SleepHelper {
    public interface Callback { void done(boolean ok, String message); }
    private SleepHelper() {}

    public static void sleepNow(Activity a) { sleepNow(a,null); }

    public static void sleepNow(Activity a, Callback callback) {
        LogStore.add(a,"Veille","Demande de mise en veille réelle");
        new Thread(() -> {
            boolean ok=false;
            String message;
            try {
                if(!PhilipsTvClient.isPaired(a)) throw new Exception("Contrôle Philips non associé. Ouvre Tests → Associer la TV.");
                message=PhilipsTvClient.standby(a);
                ok=true;
                AppState.prefs(a).edit().putString("last_sleep_diag",message).putLong("last_sleep_at",System.currentTimeMillis()).apply();
                LogStore.add(a,"Veille",message);
            } catch(Exception e) {
                message="Échec de la veille réelle : "+e.getMessage();
                AppState.prefs(a).edit().putString("last_sleep_diag",message).putLong("last_sleep_at",System.currentTimeMillis()).apply();
                LogStore.add(a,"Erreur",message);
            }
            final boolean result=ok; final String msg=message;
            if(callback!=null) a.runOnUiThread(() -> callback.done(result,msg));
        },"PhilipsStandby").start();
    }
}
