package com.lilolax.readrolla;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** The home-screen wheel: place it like any widget (hold, then drag it anywhere); tap it and Rhythm Reader floats over whatever you're doing. */
public class WheelWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        Intent show = new Intent(ctx, FloatService.class).setAction(FloatService.ACT_SHOW);
        PendingIntent pi = PendingIntent.getForegroundService(ctx, 0, show, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_wheel);
            rv.setOnClickPendingIntent(R.id.widget_root, pi);
            mgr.updateAppWidget(id, rv);
        }
    }
}
