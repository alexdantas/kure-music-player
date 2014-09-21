package com.kure.musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Sticks a message outside of the application UI, both
 * on the "notification area" and the "notification drawer".
 *
 * Simple class that wraps the Android API.
 *
 * You should inherit it and add your custom needs.
 */
public class NotificationSimple {

	/**
	 * Unique identifier for the current Notification.
	 *
	 * When sending a new Notification, if it has the
	 * same ID number it'll only get updated, not
	 * created from scratch.
	 */
	protected int NOTIFICATION_ID;

	/**
	 * Counter to assure each created Notification gets
	 * an unique ID at runtime.
	 */
	protected static int LAST_NOTIFICATION_ID = 1;

	public NotificationSimple() {
		NOTIFICATION_ID = LAST_NOTIFICATION_ID;
		LAST_NOTIFICATION_ID++;
	}

	/**
	 * Sends a quick text notification.
	 *
	 * @note This notification can be dismissed by the user and
	 *       if clicked won't do nothing.
	 *
	 * @param title Title of the notification.
	 * @param text  Text of the notification.
	 */
	public void notify(Context c, String title, String text) {

		Notification.Builder builder = new Notification.Builder(c);

		builder.setSmallIcon(R.drawable.play)
		       .setContentTitle(title)
		       .setContentText(text);

		NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_ID, builder.build());
	}

	/**
	 * Sends a quick text redirectable notification.
	 *
	 * @note This notification can be dismissed by the user and
	 *       will be redirected to specified Activity if clicked.
	 *
	 * @param where Class of the Activity it'll redirect when
	 *              it is clicked.
	 * @param title Title of the notification.
	 * @param text  Text of the notification.
	 */
	public void notify(Context c, Class<?> toWhere, String title, String text) {

		Notification.Builder builder = new Notification.Builder(c);

		builder.setSmallIcon(R.drawable.play)
		       .setContentTitle(title)
		       .setContentText(text);

		Intent intent = new Intent(c, toWhere);
		PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent, 0);

		builder.setContentIntent(pendingIntent);

		NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(NOTIFICATION_ID, builder.build());
	}
}
