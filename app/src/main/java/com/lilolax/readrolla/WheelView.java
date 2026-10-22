package com.lilolax.readrolla;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** The floating bubble: a small gold-rimmed wheel that turns a little as you drag it. */
final class WheelView extends View {
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint disc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spoke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float turn = 0f;
    private boolean hot = false;

    WheelView(Context c) {
        super(c);
        rim.setColor(0xFFC9A44C);
        disc.setColor(0xFF3B2A6B);
        spoke.setColor(0xFFE3D3A3);
        spoke.setStrokeCap(Paint.Cap.ROUND);
        shadow.setColor(0x55000000);
        setContentDescription("Rhythm Reader. Tap to open, drag to move, drag to the cross to close.");
    }

    void turnBy(float deg) { turn = (turn + deg) % 360f; invalidate(); }

    void setHot(boolean h) { if (hot != h) { hot = h; invalidate(); } }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f, r = Math.min(w, h) / 2f - 3f;
        cv.drawCircle(cx, cy + 2f, r, shadow);
        rim.setColor(hot ? 0xFFE0573F : 0xFFC9A44C);
        cv.drawCircle(cx, cy, r, rim);
        cv.drawCircle(cx, cy, r * 0.82f, disc);
        spoke.setStrokeWidth(Math.max(2f, r * 0.07f));
        cv.save();
        cv.rotate(turn, cx, cy);
        for (int i = 0; i < 4; i++) {
            cv.drawLine(cx, cy - r * 0.76f, cx, cy + r * 0.76f, spoke);
            cv.rotate(45f, cx, cy);
        }
        cv.restore();
        cv.drawCircle(cx, cy, r * 0.22f, rim);
        cv.drawCircle(cx, cy, r * 0.09f, disc);
    }
}
