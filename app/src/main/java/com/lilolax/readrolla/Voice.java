package com.lilolax.readrolla;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.media.AudioAttributes;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * 2.02 · Leeriana Keys, decked out (Lil Ole Ax: "each press of a key to have heavy haptic … add a dictation button").
 * JS → native:  LeerianaVoice.buzz(ms, amplitude 1–255)  → the phone's motor at the strength asked (a web page can only ask for a length)
 *               LeerianaVoice.listen(lang) · LeerianaVoice.stopListening()  → Android's own speech recognizer, words as they come
 * native → JS:  LeerianaEar.partial(text) · LeerianaEar.result(text) · LeerianaEar.state('listening'|'ended'|'asking') · LeerianaEar.error(msg)
 * 2.23 · read aloud (Lil Ole Ax: "I still can't hear the text to voice in Lens Reader"): a web page inside the app has no voice of its own,
 *        so Android's text-to-speech speaks for it, on the media volume.
 * JS → native:  LeerianaVoice.say(text, rate, pitch, volume 0–1, id, flush) · LeerianaVoice.hush() · LeerianaVoice.ttsState() → "ready"|"starting"|"none"
 * native → JS:  LeerianaMouth.start(id) · LeerianaMouth.done(id) · LeerianaMouth.error(id)
 */
final class Voice {
    private final Context c;
    private final WebView web;
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer sr;
    private boolean on;

    private TextToSpeech tts;
    private int ttsOk = 0; /* 0 starting, 1 ready, -1 none */

    Voice(Context c, WebView web) { this.c = c; this.web = web; }

    private void ttsInit() {
        if (tts != null) return;
        try {
            tts = new TextToSpeech(c.getApplicationContext(), status -> {
                if (status != TextToSpeech.SUCCESS) { ttsOk = -1; return; }
                try { int r = tts.setLanguage(Locale.getDefault()); if (r < 0) tts.setLanguage(Locale.US); } catch (Exception ignored) {}
                try { tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()); } catch (Exception ignored) {}
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { call("window.LeerianaMouth&&LeerianaMouth.start(" + js(id) + ")"); }
                    @Override public void onDone(String id) { call("window.LeerianaMouth&&LeerianaMouth.done(" + js(id) + ")"); }
                    @Override public void onError(String id) { call("window.LeerianaMouth&&LeerianaMouth.error(" + js(id) + ")"); }
                    @Override public void onStop(String id, boolean interrupted) { call("window.LeerianaMouth&&LeerianaMouth.done(" + js(id) + ")"); }
                });
                ttsOk = 1;
            });
        } catch (Exception e) { ttsOk = -1; }
    }

    @JavascriptInterface
    public String ttsState() { main.post(this::ttsInit); return ttsOk == 1 ? "ready" : ttsOk == -1 ? "none" : "starting"; }

    @JavascriptInterface
    public void say(final String text, final float rate, final float pitch, final float volume, final String id, final boolean flush) {
        main.post(() -> {
            ttsInit();
            if (ttsOk != 1) { /* still waking: try again shortly */ final long until = System.currentTimeMillis() + 4000;
                main.postDelayed(new Runnable() { @Override public void run() { if (ttsOk == 1) speakNow(text, rate, pitch, volume, id, flush); else if (ttsOk == 0 && System.currentTimeMillis() < until) main.postDelayed(this, 150); else call("window.LeerianaMouth&&LeerianaMouth.error(" + js(id) + ")"); } }, 150);
                return; }
            speakNow(text, rate, pitch, volume, id, flush);
        });
    }

    private void speakNow(String text, float rate, float pitch, float volume, String id, boolean flush) {
        try {
            tts.setSpeechRate(Math.max(0.1f, Math.min(4f, rate)));
            tts.setPitch(Math.max(0.1f, Math.min(2f, pitch)));
            Bundle b = new Bundle(); b.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, Math.max(0f, Math.min(1f, volume)));
            tts.speak(text == null ? "" : text, flush ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, b, id == null ? "u" : id);
        } catch (Exception e) { call("window.LeerianaMouth&&LeerianaMouth.error(" + js(id) + ")"); }
    }

    @JavascriptInterface
    public void hush() { main.post(() -> { try { if (tts != null) tts.stop(); } catch (Exception ignored) {} }); }

    private static String js(String s) { return JSONObject.quote(s == null ? "" : s); }
    private void call(final String code) { web.post(() -> { try { web.evaluateJavascript(code, null); } catch (Exception ignored) {} }); }

    @JavascriptInterface
    public void buzz(int ms, int amp) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || ms <= 0) return;
            int a = Math.max(1, Math.min(255, amp));
            if (Build.VERSION.SDK_INT >= 26 && v.hasAmplitudeControl()) v.vibrate(VibrationEffect.createOneShot(ms, a));
            else if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        } catch (Exception ignored) {}
    }

    private boolean allowed() {
        return Build.VERSION.SDK_INT < 23 || c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @JavascriptInterface
    public void listen(final String lang) {
        main.post(() -> {
            if (!allowed()) {
                if (c instanceof Activity && Build.VERSION.SDK_INT >= 23) {
                    ((Activity) c).requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 288);
                    /* start by itself once the person says yes (checked for half a minute) */
                    final long until = System.currentTimeMillis() + 30000;
                    main.postDelayed(new Runnable() { @Override public void run() {
                        if (allowed()) { start(lang); return; }
                        if (System.currentTimeMillis() < until) main.postDelayed(this, 400); else call("window.LeerianaEar&&LeerianaEar.error('The microphone was not allowed')");
                    } }, 400);
                }
                call("window.LeerianaEar&&LeerianaEar.state('asking')");
                return;
            }
            start(lang);
        });
    }

    private void start(String lang) {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(c)) { call("window.LeerianaEar&&LeerianaEar.error('This phone has no speech recognizer')"); return; }
            if (sr == null) {
                sr = SpeechRecognizer.createSpeechRecognizer(c);
                sr.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle b) { call("window.LeerianaEar&&LeerianaEar.state('listening')"); }
                    @Override public void onBeginningOfSpeech() {}
                    @Override public void onRmsChanged(float v) {}
                    @Override public void onBufferReceived(byte[] b) {}
                    @Override public void onEndOfSpeech() {}
                    @Override public void onError(int e) { if (on) call("window.LeerianaEar&&LeerianaEar.error(" + js(String.valueOf(e)) + ")"); }
                    @Override public void onResults(Bundle b) {
                        ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (r != null && !r.isEmpty()) call("window.LeerianaEar&&LeerianaEar.result(" + js(r.get(0)) + ")");
                        if (on) call("window.LeerianaEar&&LeerianaEar.state('ended')");
                    }
                    @Override public void onPartialResults(Bundle b) {
                        ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (r != null && !r.isEmpty()) call("window.LeerianaEar&&LeerianaEar.partial(" + js(r.get(0)) + ")");
                    }
                    @Override public void onEvent(int t, Bundle b) {}
                });
            }
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            if (lang != null && !lang.isEmpty()) i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
            on = true;
            sr.startListening(i);
        } catch (Exception e) {
            call("window.LeerianaEar&&LeerianaEar.error(" + js(String.valueOf(e.getMessage())) + ")");
        }
    }

    @JavascriptInterface
    public void stopListening() {
        main.post(() -> { on = false; try { if (sr != null) { sr.stopListening(); sr.cancel(); } } catch (Exception ignored) {} });
    }
}
