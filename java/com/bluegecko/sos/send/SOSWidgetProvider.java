package com.bluegecko.sos.send;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.widget.RemoteViews;

import com.bluegecko.sos.R;

public class SOSWidgetProvider extends AppWidgetProvider {

	public static String ACTION_WIDGET_CLICK = "ClickWidget";
	RemoteViews view;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int appWidgetId : appWidgetIds) {
			// Get the layout for the App Widget and attach an on-click listener to the button
			view = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

			// Create an Intent to launch Activity
			Intent intent;
			PendingIntent pendingIntent;

			intent = new Intent(context, SOSWidgetProvider.class);
			intent.setAction(SOSWidgetProvider.ACTION_WIDGET_CLICK);
			pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
			view.setOnClickPendingIntent(R.id.widget, pendingIntent);

			// Tell the AppWidgetManager to perform an update on the current App Widget
			appWidgetManager.updateAppWidget(appWidgetId, view);
		}
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
		if (intent.getAction().equals(SOSWidgetProvider.ACTION_WIDGET_CLICK)) {
			Rect pos = intent.getSourceBounds();
			Intent myIntent = new Intent(context, SOSActivity.class);

			myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
			myIntent.putExtra("BOTTOM", pos.bottom);
			myIntent.putExtra("TOP", pos.top);
			context.startActivity(myIntent);
		}
		super.onReceive(context, intent);
	}
}