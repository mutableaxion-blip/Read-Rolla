package com.lilolax.readrolla;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.RecognitionCandidate;
import com.google.mlkit.vision.digitalink.RecognitionContext;
import com.google.mlkit.vision.digitalink.WritingArea;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 1.77 · Handwriting (Lil Ole Ax: "handwritten notes that turn into text instantly").
 * Leeriana's pad sends the strokes it saw; this hands them to Google's on-device digital-ink recognizer (ML Kit) and
 * returns the text, with the next-best readings. The language model downloads once (about 20 MB, needs the internet
 * that one time); after that everything happens on the phone. Nothing leaves the phone.
 *
 * JS → native:  LeerianaInkBridge.recognize(id, strokesJson, lang, preContext, areaW, areaH)
 *               LeerianaInkBridge.ready(lang)  → asks for the model, downloading it if it is not here
 * native → JS:  LeerianaHand.result(id, text, [alternatives]) · LeerianaHand.status(lang, 'ready'|'downloading'|'error', detail)
 */
final class Ink {
    private final WebView web;
    private final Map<String, DigitalInkRecognizer> recognizers = new HashMap<>();
    private final Map<String, Boolean> downloaded = new HashMap<>();

    Ink(WebView web) { this.web = web; }

    private static String js(String s) { return JSONObject.quote(s == null ? "" : s); }

    private void run(final String code) {
        web.post(new Runnable() { @Override public void run() { try { web.evaluateJavascript(code, null); } catch (Exception ignored) {} } });
    }

    private void status(String lang, String state, String detail) {
        run("window.LeerianaHand&&LeerianaHand.status(" + js(lang) + "," + js(state) + "," + js(detail) + ")");
    }

    private DigitalInkRecognitionModel model(String lang) throws Exception {
        DigitalInkRecognitionModelIdentifier id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(lang);
        if (id == null) throw new Exception("No handwriting model for " + lang);
        return DigitalInkRecognitionModel.builder(id).build();
    }

    /** Makes sure the language's model is on the phone, downloading it when it is not, and says how that went. */
    @JavascriptInterface
    public void ready(final String lang) {
        try {
            final DigitalInkRecognitionModel m = model(lang);
            final RemoteModelManager rm = RemoteModelManager.getInstance();
            rm.isModelDownloaded(m).addOnSuccessListener(ok -> {
                if (ok) { downloaded.put(lang, true); status(lang, "ready", ""); return; }
                status(lang, "downloading", "");
                rm.download(m, new DownloadConditions.Builder().build())
                  .addOnSuccessListener(v -> { downloaded.put(lang, true); status(lang, "ready", ""); })
                  .addOnFailureListener(e -> status(lang, "error", String.valueOf(e.getMessage())));
            }).addOnFailureListener(e -> status(lang, "error", String.valueOf(e.getMessage())));
        } catch (Exception e) {
            status(lang, "error", String.valueOf(e.getMessage()));
        }
    }

    private DigitalInkRecognizer recognizer(String lang) throws Exception {
        DigitalInkRecognizer r = recognizers.get(lang);
        if (r == null) {
            r = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model(lang)).build());
            recognizers.put(lang, r);
        }
        return r;
    }

    /** strokesJson: [[[x,y,t],[x,y,t],…], …] in the pad's pixels and milliseconds. */
    @JavascriptInterface
    public void recognize(final String id, final String strokesJson, final String lang, final String preContext, final float areaW, final float areaH) {
        try {
            if (!Boolean.TRUE.equals(downloaded.get(lang))) { ready(lang); run("window.LeerianaHand&&LeerianaHand.result(" + js(id) + ",null,[],'not-ready')"); return; }
            JSONArray strokes = new JSONArray(strokesJson);
            com.google.mlkit.vision.digitalink.Ink.Builder ib = com.google.mlkit.vision.digitalink.Ink.builder();
            for (int s = 0; s < strokes.length(); s++) {
                JSONArray st = strokes.getJSONArray(s);
                com.google.mlkit.vision.digitalink.Ink.Stroke.Builder sb = com.google.mlkit.vision.digitalink.Ink.Stroke.builder();
                for (int p = 0; p < st.length(); p++) {
                    JSONArray pt = st.getJSONArray(p);
                    sb.addPoint(com.google.mlkit.vision.digitalink.Ink.Point.create((float) pt.getDouble(0), (float) pt.getDouble(1), pt.getLong(2)));
                }
                ib.addStroke(sb.build());
            }
            RecognitionContext.Builder cb = RecognitionContext.builder();
            if (preContext != null && !preContext.isEmpty()) cb.setPreContext(preContext.length() > 20 ? preContext.substring(preContext.length() - 20) : preContext);
            if (areaW > 0 && areaH > 0) cb.setWritingArea(new WritingArea(areaW, areaH));
            recognizer(lang).recognize(ib.build(), cb.build())
                .addOnSuccessListener(result -> {
                    JSONArray alts = new JSONArray();
                    String best = null;
                    for (RecognitionCandidate c : result.getCandidates()) { if (best == null) best = c.getText(); else alts.put(c.getText()); }
                    run("window.LeerianaHand&&LeerianaHand.result(" + js(id) + "," + (best == null ? "null" : js(best)) + "," + alts + ",'ok')");
                })
                .addOnFailureListener(e -> run("window.LeerianaHand&&LeerianaHand.result(" + js(id) + ",null,[]," + js("error: " + e.getMessage()) + ")"));
        } catch (Exception e) {
            run("window.LeerianaHand&&LeerianaHand.result(" + js(id) + ",null,[]," + js("error: " + e.getMessage()) + ")");
        }
    }
}
