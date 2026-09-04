package fr.shabbattv;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG = Color.rgb(10, 12, 15);
    public static final int SURFACE = Color.rgb(22, 25, 30);
    public static final int SURFACE_2 = Color.rgb(29, 33, 39);
    public static final int TEXT = Color.rgb(246, 244, 239);
    public static final int MUTED = Color.rgb(159, 165, 174);
    public static final int ACCENT = Color.rgb(229, 160, 13);
    public static final int GOOD = Color.rgb(83, 200, 126);
    public static final int BAD = Color.rgb(255, 105, 105);
    public static final int STROKE = Color.rgb(52, 57, 65);

    private Ui() {}

    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout page(Activity a) {
        a.getWindow().setStatusBarColor(BG);
        a.getWindow().setNavigationBarColor(BG);
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a, 56), dp(a, 36), dp(a, 56), dp(a, 32));
        root.setBackgroundColor(BG);
        return root;
    }

    public static TextView eyebrow(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text.toUpperCase());
        v.setTextColor(ACCENT);
        v.setTextSize(12);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setLetterSpacing(.12f);
        return v;
    }

    public static TextView title(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(34);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    public static TextView subtitle(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(MUTED);
        v.setTextSize(16);
        v.setLineSpacing(0, 1.12f);
        return v;
    }

    public static TextView body(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(TEXT);
        v.setTextSize(17);
        return v;
    }

    public static TextView muted(Context c, String text) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextColor(MUTED);
        v.setTextSize(15);
        return v;
    }

    public static Button button(Context c, String text, boolean primary) {
        Button b = new Button(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? Color.rgb(20, 16, 7) : TEXT);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setPadding(dp(c, 22), 0, dp(c, 22), 0);
        b.setFocusable(true);
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
        e.setTextSize(17);
        e.setPadding(dp(c, 20), 0, dp(c, 20), 0);
        e.setBackground(round(c, SURFACE, STROKE, 1, 14));
        return e;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 22), dp(c, 18), dp(c, 22), dp(c, 18));
        l.setBackground(round(c, SURFACE, STROKE, 1, 16));
        return l;
    }

    public static TextView pill(Context c, String text, boolean good) {
        TextView v = new TextView(c);
        v.setText(text);
        v.setTextSize(13);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setTextColor(good ? GOOD : MUTED);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 14), dp(c, 7), dp(c, 14), dp(c, 7));
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
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, h)));
    }
}
