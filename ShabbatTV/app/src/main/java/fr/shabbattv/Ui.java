package fr.shabbattv;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public final class Ui {
    public static final int BG = Color.rgb(9, 11, 14);
    public static final int SURFACE = Color.rgb(20, 23, 28);
    public static final int SURFACE_2 = Color.rgb(28, 32, 38);
    public static final int TEXT = Color.rgb(247, 246, 242);
    public static final int MUTED = Color.rgb(157, 164, 174);
    public static final int ACCENT = Color.rgb(229, 160, 13);
    public static final int GOOD = Color.rgb(86, 204, 132);
    public static final int BAD = Color.rgb(255, 105, 105);
    public static final int STROKE = Color.rgb(48, 54, 63);

    private Ui() {}

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int widthDp(Context c) {
        Configuration cfg = c.getResources().getConfiguration();
        return cfg.screenWidthDp > 0 ? cfg.screenWidthDp : Math.round(c.getResources().getDisplayMetrics().widthPixels / c.getResources().getDisplayMetrics().density);
    }

    public static int heightDp(Context c) {
        Configuration cfg = c.getResources().getConfiguration();
        return cfg.screenHeightDp > 0 ? cfg.screenHeightDp : Math.round(c.getResources().getDisplayMetrics().heightPixels / c.getResources().getDisplayMetrics().density);
    }

    public static boolean compact(Context c) {
        return heightDp(c) <= 600 || widthDp(c) <= 1000;
    }

    public static int controlHeight(Context c) {
        return compact(c) ? 50 : 58;
    }

    public static int smallControlHeight(Context c) {
        return compact(c) ? 46 : 54;
    }

    /**
     * Android TV can expose a logical viewport much smaller than the 4K panel.
     * Keep every interactive screen inside a 5% TV-safe area instead of using
     * panel pixels or fixed widths.
     */
    public static LinearLayout page(Activity a) {
        prepareWindow(a);
        int w = widthDp(a);
        int h = heightDp(a);
        int safeX = Math.max(28, Math.round(w * .05f));
        int safeY = Math.max(18, Math.round(h * .05f));
        safeX = Math.min(safeX, 76);
        safeY = Math.min(safeY, 42);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, safeX), dp(a, safeY), dp(a, safeX), dp(a, safeY));
        root.setBackgroundColor(BG);
        root.setClipToPadding(false);
        return root;
    }

    public static void setScrollable(Activity a, LinearLayout content) {
        ScrollView scroll = new ScrollView(a);
        scroll.setFillViewport(true);
        scroll.setSmoothScrollingEnabled(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        a.setContentView(scroll);
    }

    public static void prepareWindow(Activity a) {
        a.getWindow().setStatusBarColor(BG);
        a.getWindow().setNavigationBarColor(BG);
        a.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    public static void header(LinearLayout root, Context c, String eyebrow, String title, String subtitle) {
        root.addView(eyebrow(c, eyebrow));
        root.addView(title(c, title), lp(-1, -2, c, compact(c) ? 3 : 5));
        if (subtitle != null && !subtitle.isEmpty()) {
            root.addView(subtitle(c, subtitle), lp(-1, -2, c, compact(c) ? 4 : 7));
        }
    }

    public static TextView eyebrow(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text.toUpperCase(Locale.ROOT));
        v.setTextColor(ACCENT);
        v.setTextSize(compact(c) ? 10 : 12);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.12f);
        v.setMaxLines(1);
        return v;
    }

    public static TextView title(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(compact(c) ? 28 : 34);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setMaxLines(2);
        return v;
    }

    public static TextView subtitle(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(MUTED);
        v.setTextSize(compact(c) ? 14 : 16);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    public static TextView body(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(compact(c) ? 15 : 17);
        return v;
    }

    public static TextView muted(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(MUTED);
        v.setTextSize(compact(c) ? 13 : 15);
        v.setLineSpacing(0, 1.08f);
        return v;
    }

    public static Button button(Context c, String text, boolean primary) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(compact(c) ? 15 : 17);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(20, 16, 7) : TEXT);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(c, compact(c) ? 16 : 22), 0, dp(c, compact(c) ? 16 : 22), 0);
        b.setFocusable(true);
        b.setSingleLine(true);
        b.setEllipsize(android.text.TextUtils.TruncateAt.END);
        b.setStateListAnimator(null);
        b.setBackground(selector(c, primary));
        return b;
    }

    public static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(compact(c) ? 15 : 17);
        e.setPadding(dp(c, compact(c) ? 15 : 20), 0, dp(c, compact(c) ? 15 : 20), 0);
        e.setBackground(round(c, SURFACE, STROKE, 1, 14));
        return e;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        int x = compact(c) ? 16 : 22;
        int y = compact(c) ? 13 : 18;
        l.setPadding(dp(c, x), dp(c, y), dp(c, x), dp(c, y));
        l.setBackground(round(c, SURFACE, STROKE, 1, 16));
        return l;
    }

    public static TextView pill(Context c, String text, boolean good) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(compact(c) ? 11 : 13);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setTextColor(good ? GOOD : MUTED);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, compact(c) ? 10 : 14), dp(c, 5), dp(c, compact(c) ? 10 : 14), dp(c, 5));
        int stroke = good ? Color.rgb(47, 108, 72) : STROKE;
        v.setBackground(round(c, SURFACE, stroke, 1, 99));
        return v;
    }

    public static LinearLayout.LayoutParams lp(int w, int h, Context c, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(0, dp(c, top), 0, 0);
        return p;
    }

    public static GradientDrawable round(Context c, int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(c, radiusDp));
        if (strokeDp > 0) d.setStroke(dp(c, strokeDp), stroke);
        return d;
    }

    private static StateListDrawable selector(Context c, boolean primary) {
        StateListDrawable s = new StateListDrawable();
        int normalFill = primary ? ACCENT : SURFACE;
        int focusFill = primary ? Color.rgb(244, 181, 49) : SURFACE_2;
        s.addState(new int[]{android.R.attr.state_focused}, round(c, focusFill, ACCENT, 2, 14));
        s.addState(new int[]{android.R.attr.state_pressed}, round(c, focusFill, ACCENT, 2, 14));
        s.addState(new int[]{}, round(c, normalFill, primary ? ACCENT : STROKE, 1, 14));
        return s;
    }

    public static void spacer(LinearLayout parent, Context c, int h) {
        View v = new View(c);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, compact(c) ? Math.max(4, Math.round(h * .72f)) : h)));
    }

    public static String metrics(Context c) {
        DisplayMetrics d = c.getResources().getDisplayMetrics();
        return d.widthPixels + " × " + d.heightPixels + " px  ·  " + widthDp(c) + " × " + heightDp(c) + " dp  ·  densité " + String.format(Locale.US, "%.2f", d.density);
    }
}
