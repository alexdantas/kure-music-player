package com.kure.musicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import com.kure.musicplayer.activities.ActivityNowPlaying;
import com.kure.musicplayer.model.Song;

/**
 * Specific way to stick an on-going message on the system
 * with the current song I'm playing.
 *
 * It must be attached to a Service, since it'll run on
 * the background.
 */
public class NotificationMusic extends NotificationSimple {

	/**
	 * Reference to the context that notified.
	 */
	Context context = null;

	/**
	 * Reference to the service we're attached to.
	 */
	Service service = null;

	/**
	 * Sends a system notification with a song's information.
	 *
	 * If the user clicks the notification, will be redirected
	 * to the "Now Playing" Activity.
	 *
	 * @param context Activity that calls this function.
	 * @param service Service that calls this function.
	 *                Required so the Notification can
	 *                run on the background.
	 * @param song    Song which we'll display information.
	 *
	 * @note By calling this function multiple times, it'll
	 *       update the old notification.
	 */
	public void notifySong(Context context, Service service, Song song) {

		if (this.context == null)
			this.context = context;
		if (this.service == null)
			this.service = service;

		// Intent to launch the "Now Playing" Activity, without
		// needing to create it again from scratch.
		Intent notifyIntent = new Intent(context, ActivityNowPlaying.class);
		notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity
				(context,
				 0,
				 notifyIntent,
				 PendingIntent.FLAG_UPDATE_CURRENT);

		// Actually creating the Notification
		Notification.Builder notificationBuilder = new Notification.Builder(context);

		notificationBuilder.setContentIntent(pendingIntent)
		                   .setSmallIcon(R.drawable.play)
		                   .setTicker("kMP: Playing '" + song.getTitle() + "' from '" + song.getArtist() + "'")
		                   .setOngoing(true)
		                   .setContentTitle(song.getTitle())
		                   .setContentText(song.getArtist());

		Notification notification = notificationBuilder.build();

		// Sets the notification to run on the foreground.
		service.startForeground(NOTIFICATION_ID, notification);
	}

	/**
	 * Cancels this notification.
	 */
	public void cancel() {
		service.stopForeground(true);

		NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(NOTIFICATION_ID);
	}

	/**
	 * Cancels all sent notifications.
	 */
	public static void cancelAll(Context c) {
		NotificationManager manager = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancelAll();
	}
}
