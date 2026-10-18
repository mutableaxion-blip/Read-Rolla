package com.lilolax.readrolla;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Read Rolla, floating over any app (Lil ol Ax: "turn word wheels into a widget I can drag anywhere on my phone").
 * A wheel bubble you drag anywhere; tap it and it opens into a window running Read Rolla; drag the window by its
 * title bar, resize it from the corner, shrink it back to the wheel. Drag the wheel onto the cross to put it away.
 * Sizes and places are remembered; every number lives in the settings below (SharedPreferences "float").
 */
public class FloatService extends Service {
    static final String ACT_SHOW = "com.lilolax.readrolla.SHOW";
    static final String ACT_STOP = "com.lilolax.readrolla.STOP";
    private static final String CH = "wheels";

    private WindowManager wm;
    private SharedPreferences sp;
    private float density;
    private WheelView bubble;
    private WindowManager.LayoutParams bp, pp, tp;
    private FrameLayout panel;
    private WebView web;
    private TextView trash, keysBtn;
    private boolean expanded, bubbleShown, panelShown, trashShown, typing;

    private int dp(float v) { return Math.round(v * density); }
    private int setting(String k, int def) { return sp.getInt(k, def); }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        sp = getSharedPreferences("float", MODE_PRIVATE);
        density = getResources().getDisplayMetrics().density;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String act = intent == null ? null : intent.getAction();
        startForeground(1, note());
        if (ACT_STOP.equals(act)) { stopSelf(); return START_NOT_STICKY; }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Open the Read Rolla app and allow “Display over other apps” first.", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (bubble == null) build();
        if (ACT_SHOW.equals(act)) expand(); else if (!expanded) showBubble();
        return START_STICKY;
    }

    /* ───────── the notification Android asks a floating app to keep ───────── */
    private Notification note() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CH) == null) {
            NotificationChannel ch = new NotificationChannel(CH, "Floating Read Rolla", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        PendingIntent open = PendingIntent.getService(this, 1, new Intent(this, FloatService.class).setAction(ACT_SHOW), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, FloatService.class).setAction(ACT_STOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_stat_wheel)
            .setContentTitle("Read Rolla is floating")
            .setContentText("Tap to open it. Drag the wheel onto the cross to put it away.")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat_wheel), "Put it away", stop).build())
            .build();
    }

    private WindowManager.LayoutParams params(int w, int h, boolean focusable) {
        int f = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (!focusable) f |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, f, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void build() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = dp(setting("bubbleDp", 60));
        bubble = new WheelView(this);
        bp = params(size, size, false);
        bp.x = sp.getInt("bx", dm.widthPixels - size - dp(12));
        bp.y = sp.getInt("by", dm.heightPixels / 3);
        bubble.setOnTouchListener(new BubbleDrag());

        trash = new TextView(this);
        trash.setText("✕");
        trash.setTextSize(26);
        trash.setTextColor(0xFFECE5D2);
        trash.setGravity(Gravity.CENTER);
        GradientDrawable tb = new GradientDrawable();
        tb.setShape(GradientDrawable.OVAL);
        tb.setColor(0xCC132A24);
        tb.setStroke(dp(2), 0xFFC9A44C);
        trash.setBackground(tb);
        int ts = dp(64);
        tp = params(ts, ts, false);
        tp.x = (dm.widthPixels - ts) / 2;
        tp.y = dm.heightPixels - ts - dp(72);

        buildPanel();
    }

    private void buildPanel() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        panel = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0F1C18);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(1), 0xFFC9A44C);
        panel.setBackground(bg);
        panel.setClipToOutline(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), 0, dp(4), 0);
        bar.setBackgroundColor(0xFF172A24);

        TextView title = new TextView(this);
        title.setText("◉  Read Rolla");
        title.setTextColor(0xFFE3D3A3);
        title.setTextSize(15);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1f));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setOnTouchListener(new PanelDrag());

        keysBtn = barButton("⌨", "Let the window take typing (tap again to give the keyboard back to the app behind)");
        keysBtn.setOnClickListener(v -> setTyping(!typing));
        TextView shrink = barButton("–", "Shrink to the wheel");
        shrink.setOnClickListener(v -> collapse());
        TextView close = barButton("✕", "Put Read Rolla away");
        close.setOnClickListener(v -> stopSelf());
        bar.addView(keysBtn);
        bar.addView(shrink);
        bar.addView(close);
        col.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)));

        web = Web.make(this, true);
        col.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        panel.addView(col, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView grip = new TextView(this);
        grip.setText("◢");
        grip.setTextColor(0xFFC9A44C);
        grip.setTextSize(18);
        grip.setGravity(Gravity.BOTTOM | Gravity.END);
        grip.setPadding(0, 0, dp(4), dp(2));
        grip.setContentDescription("Resize");
        grip.setOnTouchListener(new PanelResize());
        panel.addView(grip, new FrameLayout.LayoutParams(dp(40), dp(40), Gravity.BOTTOM | Gravity.END));

        int w = Math.min(dp(setting("panelWdp", 340)), dm.widthPixels);
        int h = Math.min(dp(setting("panelHdp", 460)), dm.heightPixels - dp(40));
        pp = params(sp.getInt("pw", w), sp.getInt("ph", h), false);
        pp.x = sp.getInt("px", (dm.widthPixels - pp.width) / 2);
        pp.y = sp.getInt("py", dp(80));
    }

    private TextView barButton(String t, String desc) {
        TextView b = new TextView(this);
        b.setText(t);
        b.setTextColor(0xFFECE5D2);
        b.setTextSize(18);
        b.setGravity(Gravity.CENTER);
        b.setContentDescription(desc);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        return b;
    }

    private void setTyping(boolean on) {
        typing = on;
        if (on) pp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE; else pp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        keysBtn.setTextColor(on ? 0xFFC9A44C : 0xFFECE5D2);
        if (panelShown) wm.updateViewLayout(panel, pp);
    }

    /* ───────── showing and hiding ───────── */
    private void showBubble() { if (!bubbleShown) { clamp(bp); wm.addView(bubble, bp); bubbleShown = true; } }
    private void hideBubble() { if (bubbleShown) { wm.removeView(bubble); bubbleShown = false; } }
    private void showPanel() { if (!panelShown) { clamp(pp); wm.addView(panel, pp); panelShown = true; } }
    private void hidePanel() { if (panelShown) { if (typing) setTyping(false); wm.removeView(panel); panelShown = false; } }
    private void showTrash() { if (!trashShown) { wm.addView(trash, tp); trashShown = true; } }
    private void hideTrash() { if (trashShown) { wm.removeView(trash); trashShown = false; } }

    private void expand() { expanded = true; hideBubble(); showPanel(); }
    private void collapse() {
        expanded = false;
        hidePanel();
        bp.x = pp.x + pp.width - bp.width;
        bp.y = pp.y;
        showBubble();
        save();
    }

    private void clamp(WindowManager.LayoutParams p) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        p.x = Math.max(0, Math.min(p.x, dm.widthPixels - Math.min(p.width, dm.widthPixels)));
        p.y = Math.max(0, Math.min(p.y, dm.heightPixels - Math.min(p.height, dm.heightPixels)));
    }

    private void save() {
        sp.edit().putInt("bx", bp.x).putInt("by", bp.y).putInt("px", pp.x).putInt("py", pp.y).putInt("pw", pp.width).putInt("ph", pp.height).apply();
    }

    private boolean overTrash() {
        int bcx = bp.x + bp.width / 2, bcy = bp.y + bp.height / 2, tcx = tp.x + tp.width / 2, tcy = tp.y + tp.height / 2;
        int dx = bcx - tcx, dy = bcy - tcy, r = tp.width;
        return dx * dx + dy * dy < r * r;
    }

    /* ───────── gestures ───────── */
    private final class BubbleDrag implements View.OnTouchListener {
        private float sx, sy; private int ox, oy; private boolean moved;
        private final int slop = ViewConfiguration.get(FloatService.this).getScaledTouchSlop();
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    sx = e.getRawX(); sy = e.getRawY(); ox = bp.x; oy = bp.y; moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                    if (!moved && Math.abs(dx) + Math.abs(dy) > slop) { moved = true; showTrash(); }
                    if (moved) {
                        bp.x = ox + Math.round(dx); bp.y = oy + Math.round(dy);
                        wm.updateViewLayout(bubble, bp);
                        bubble.turnBy((Math.abs(dx) + Math.abs(dy)) * 0.02f);
                        bubble.setHot(overTrash());
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved) { v.performClick(); expand(); return true; }
                    boolean away = overTrash();
                    hideTrash(); bubble.setHot(false);
                    if (away) { stopSelf(); return true; }
                    clamp(bp); wm.updateViewLayout(bubble, bp); save();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    hideTrash(); bubble.setHot(false);
                    return true;
                default:
                    return false;
            }
        }
    }

    private final class PanelDrag implements View.OnTouchListener {
        private float sx, sy; private int ox, oy;
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: sx = e.getRawX(); sy = e.getRawY(); ox = pp.x; oy = pp.y; return true;
                case MotionEvent.ACTION_MOVE:
                    pp.x = ox + Math.round(e.getRawX() - sx); pp.y = oy + Math.round(e.getRawY() - sy);
                    wm.updateViewLayout(panel, pp); return true;
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                    clamp(pp); wm.updateViewLayout(panel, pp); save(); return true;
                default: return false;
            }
        }
    }

    private final class PanelResize implements View.OnTouchListener {
        private float sx, sy; private int ow, oh;
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: sx = e.getRawX(); sy = e.getRawY(); ow = pp.width; oh = pp.height; return true;
                case MotionEvent.ACTION_MOVE:
                    pp.width = Math.max(dp(setting("minWdp", 220)), Math.min(dm.widthPixels, ow + Math.round(e.getRawX() - sx)));
                    pp.height = Math.max(dp(setting("minHdp", 240)), Math.min(dm.heightPixels, oh + Math.round(e.getRawY() - sy)));
                    wm.updateViewLayout(panel, pp); return true;
                case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
                    clamp(pp); wm.updateViewLayout(panel, pp); save(); return true;
                default: return false;
            }
        }
    }

    @Override
    public void onDestroy() {
        try { hideTrash(); hidePanel(); hideBubble(); } catch (Exception ignored) { }
        if (web != null) { web.destroy(); web = null; }
        if (bp != null) save();
        super.onDestroy();
    }
}
