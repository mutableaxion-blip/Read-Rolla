package com.lilolax.readrolla;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** The front of the app: float Rhythm Reader, open the whole of Leeriana, or choose which Leeriana file it runs. */
public class MainActivity extends Activity {
    private static final int PICK = 41;
    private boolean wantFloat;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float d = getResources().getDisplayMetrics().density;
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF0F1C18);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * d);
        col.setPadding(pad, Math.round(36 * d), pad, pad);
        sv.addView(col);

        TextView h = new TextView(this);
        h.setText("Rhythm Reader");
        h.setTextSize(34);
        h.setTextColor(0xFFE3D3A3);
        h.setTypeface(android.graphics.Typeface.SERIF);
        col.addView(h);

        TextView p = new TextView(this);
        p.setText("Float Rhythm Reader over any app. Drag the wheel anywhere; tap it to open, drag the window by its title, resize it from the corner, and drag the wheel onto the cross to put it away.\n\nLeeriana has its own icon too: tap “Put Leeriana on my home screen”, or find Leeriana in your app list and drag it to your home screen. For the wheel on your home screen, tap “Put the Rhythm Reader wheel on my home screen”.");
        p.setTextSize(16);
        p.setTextColor(0xFFCFD6CB);
        p.setLineSpacing(0, 1.2f);
        p.setPadding(0, Math.round(10 * d), 0, Math.round(18 * d));
        col.addView(p);

        col.addView(button("◉  Float Rhythm Reader", true, v -> startFloat()));
        col.addView(button("📖  Open all of Leeriana", false, v -> startActivity(new Intent(this, LeerianaActivity.class))));
        col.addView(button("⌂  Put Leeriana on my home screen", false, v -> pinLeeriana()));
        col.addView(button("◉  Put the Rhythm Reader wheel on my home screen", false, v -> pinWheel()));
        col.addView(button("📄  Use my own Leeriana file", false, v -> pick()));
        col.addView(button("↺  Back to the built-in Leeriana", false, v -> {
            File f = Web.userFile(this);
            if (f.exists() && f.delete()) toast("Using the built-in Leeriana again.");
            showStatus();
        }));

        status = new TextView(this);
        status.setTextSize(14);
        status.setTextColor(0xFFA7B2A4);
        status.setPadding(0, Math.round(14 * d), 0, 0);
        col.addView(status);
        showStatus();
        setContentView(sv);

        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, 7);
    }

    private Button button(String t, boolean main, android.view.View.OnClickListener fn) {
        float d = getResources().getDisplayMetrics().density;
        Button b = new Button(this);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(main ? 0xFF1A1408 : 0xFFECE5D2);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(28 * d);
        bg.setColor(main ? 0xFFC9A44C : 0x00000000);
        bg.setStroke(Math.round(1.5f * d), 0xFFC9A44C);
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(56 * d));
        lp.topMargin = Math.round(10 * d);
        b.setLayoutParams(lp);
        b.setOnClickListener(fn);
        return b;
    }

    private void showStatus() {
        status.setText(Web.usingOwnFile(this) ? "Running your own Leeriana file." : "Running the built-in Leeriana (v0.228). Your books and writing come from your account when you sign in.");
    }

    private void startFloat() {
        if (!Settings.canDrawOverlays(this)) {
            wantFloat = true;
            toast("Turn on “Allow display over other apps” for Rhythm Reader, then come back.");
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            return;
        }
        startForegroundService(new Intent(this, FloatService.class).setAction(FloatService.ACT_SHOW));
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wantFloat && Settings.canDrawOverlays(this)) { wantFloat = false; startFloat(); }
    }

    private void pick() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "text/html", "application/xhtml+xml", "text/plain", "application/octet-stream" });
        startActivityForResult(i, PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != PICK || res != RESULT_OK || data == null || data.getData() == null) return;
        File tmp = new File(getFilesDir(), "leeriana.html.part");
        try (InputStream in = getContentResolver().openInputStream(data.getData()); OutputStream out = new FileOutputStream(tmp)) {
            if (in == null) throw new java.io.IOException("unreadable");
            byte[] buf = new byte[65536];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); total += n; }
            if (total < 1000) throw new java.io.IOException("too small");
        } catch (Exception e) {
            tmp.delete();
            toast("That file could not be read.");
            return;
        }
        File f = Web.userFile(this);
        if (f.exists()) f.delete();
        if (tmp.renameTo(f)) toast("Rhythm Reader now runs your Leeriana file."); else toast("That file could not be saved.");
        showStatus();
    }

    /** Asks the home screen to add a Leeriana icon that opens all of Leeriana straight away. */
    private void pinLeeriana() {
        android.content.pm.ShortcutManager sm = getSystemService(android.content.pm.ShortcutManager.class);
        if (sm == null || !sm.isRequestPinShortcutSupported()) { toast("Your home screen can’t add it from here: find Leeriana in your app list, hold it, and drag it to your home screen."); return; }
        Intent open = new Intent(this, LeerianaActivity.class).setAction(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        android.content.pm.ShortcutInfo info = new android.content.pm.ShortcutInfo.Builder(this, "leeriana-home")
            .setShortLabel("Leeriana").setLongLabel("Leeriana")
            .setIcon(android.graphics.drawable.Icon.createWithResource(this, R.mipmap.ic_leeriana))
            .setIntent(open).build();
        sm.requestPinShortcut(info, null);
    }

    /** Asks the home screen to add the Rhythm Reader wheel widget. */
    private void pinWheel() {
        android.appwidget.AppWidgetManager wm = android.appwidget.AppWidgetManager.getInstance(this);
        if (wm == null || !wm.isRequestPinAppWidgetSupported()) { toast("Hold an empty spot on your home screen → Widgets → Rhythm Reader, then drag the wheel where you like."); return; }
        wm.requestPinAppWidget(new android.content.ComponentName(this, WheelWidget.class), null, null);
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
