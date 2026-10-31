package com.lilolax.readrolla;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Leeriana, served to the WebView from a private https address so its storage (localStorage, IndexedDB) behaves like a website's. */
final class Web {
    static final String HOST = "leeriana.local";
    static final String BASE = "https://" + HOST + "/";
    /** Opens Rhythm Reader’s chooser once Leeriana has loaded (the cover is opened first, as a tap on it would). */
    static final String WHEELS_JS =
        "(function(){var n=0,t=setInterval(function(){n++;" +
        "if(window.LeerianaRimReader&&window.StarSeekerCover){clearInterval(t);" +
        "Promise.resolve(StarSeekerCover.open()).then(function(){try{LeerianaRimReader.pick()}catch(e){}});}" +
        "else if(n>240){clearInterval(t);}},150);})();";

    private Web() {}

    /** Lets Leeriana's replay (v0.178) turn the screen sideways and back. Only the screen's turn is exposed; nothing else. */
    static final class Turn {
        private final Activity a;
        Turn(Activity a) { this.a = a; }
        @JavascriptInterface
        public void landscape(final boolean on) {
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    a.setRequestedOrientation(on ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    View d = a.getWindow().getDecorView();
                    d.setSystemUiVisibility(on ? (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) : View.SYSTEM_UI_FLAG_VISIBLE);
                }
            });
        }

        /** 1.78: a file Leeriana makes (3D Writer's picture, 3D model or scene) goes to Downloads/Leeriana on the phone.
         *  A web download goes nowhere inside the app, so the page hands the file over here. Returns where it went, or "" if it could not. */
        @JavascriptInterface
        public String save(String name, String mime, String b64) {
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                String safe = name == null ? "" : name.replaceAll("[^A-Za-z0-9._ -]", "_");
                if (safe.isEmpty()) safe = "Leeriana-file";
                String type = mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                    v.put(MediaStore.Downloads.MIME_TYPE, type);
                    v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Leeriana");
                    Uri u = a.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (u == null) return "";
                    try (OutputStream o = a.getContentResolver().openOutputStream(u)) { if (o == null) return ""; o.write(data); }
                    return "Downloads/Leeriana/" + safe;
                }
                File dir = new File(a.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Leeriana");
                if (!dir.exists() && !dir.mkdirs()) return "";
                File f = new File(dir, safe);
                try (FileOutputStream o = new FileOutputStream(f)) { o.write(data); }
                return f.getAbsolutePath();
            } catch (Exception e) {
                return "";
            }
        }
    }

    static File userFile(Context c) { return new File(c.getFilesDir(), "leeriana.html"); }

    static boolean usingOwnFile(Context c) { File f = userFile(c); return f.exists() && f.length() > 0; }

    static InputStream open(Context c) throws IOException {
        File f = userFile(c);
        if (f.exists() && f.length() > 0) return new FileInputStream(f);
        return c.getAssets().open("leeriana.html");
    }

    @SuppressLint("SetJavaScriptEnabled")
    static WebView make(final Context c, final boolean wheelsOnly) {
        WebView w = new WebView(c);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(100);
        w.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (!HOST.equals(u.getHost())) return null;
                String p = u.getPath();
                if (p == null || p.isEmpty() || "/".equals(p) || "/index.html".equals(p)) {
                    try {
                        return new WebResourceResponse("text/html", "utf-8", open(c));
                    } catch (IOException e) {
                        return null;
                    }
                }
                return new WebResourceResponse("text/plain", "utf-8", 404, "Not found", null, new ByteArrayInputStream(new byte[0]));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if (HOST.equals(u.getHost())) return false;
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, u);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    c.startActivity(i);
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (wheelsOnly) view.evaluateJavascript(WHEELS_JS, null);
            }
        });
        w.setWebChromeClient(new WebChromeClient());
        if (c instanceof Activity) w.addJavascriptInterface(new Turn((Activity) c), "LeerianaAndroid"); /* v1.4: the replay turns the screen sideways */
        w.addJavascriptInterface(new Ink(w), "LeerianaInkBridge"); /* 1.77: handwriting → text, on the phone */
        w.loadUrl(BASE + (wheelsOnly ? "#readrecords" : ""));
        return w;
    }
}
