package com.kure.musicplayer.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.kure.musicplayer.kMP;
import com.kure.musicplayer.model.Song;

/**
 * Asynchronous service that will communicate with the
 * MusicService and scrobble songs to Last.fm through
 * installed applications.
 *
 * - It listens to Broadcasts from MusicService, like when
 *   the music started, paused, has changed...
 * - Then sends that information to other applications that
 *   directly communicates with Last.fm.
 *   There's a handful of them, check out on Settings.
 *
 * Thanks:
 * - Vogella, for the awesome tutorial on Services.
 *   http://www.vogella.com/tutorials/AndroidServices/article.html
 *
 */
public class ServiceScrobbleMusic extends Service {

	/**
	 * Service just got created.
	 */
	@Override
	public void onCreate() {

		// Registering the BroadcastReceiver to listen
		// to the MusicService.
		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.registerReceiver(musicServiceBroadcastReceiver, new IntentFilter(ServicePlayMusic.BROADCAST_ACTION));
	}

	/**
	 * Service is triggered to (re)start.
	 *
	 * @note Might be called several times.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (intent == null) {
			// We just got restarted after Android killed us
		}
		else {
			// This service is being explicitly started
		}

		// This makes sure this service will be restarted
		// when Android kills it.
		// When it does, the `intent` will be `null`.
		return Service.START_STICKY;
	}

	@Override
	public void onDestroy() {

		// Unregistering the BroadcastReceiver
		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.unregisterReceiver(musicServiceBroadcastReceiver);

		super.onDestroy();
	}

	/**
	 * The thing that will keep an eye on LocalBroadcasts
	 * for the MusicService.
	 */
	BroadcastReceiver musicServiceBroadcastReceiver = new BroadcastReceiver() {

		/**
		 * What it'll do when receiving a message from the
		 * MusicService?
		 */
		@Override
		public void onReceive(Context context, Intent intent) {

			// Getting the information sent by the MusicService
			// (and ignoring it if invalid)
			String action  = intent.getStringExtra(ServicePlayMusic.BROADCAST_EXTRA_STATE);
			Long   song_id = intent.getLongExtra(ServicePlayMusic.BROADCAST_EXTRA_SONG_ID, -1);

			if (song_id != -1)
				scrobbleSong(kMP.songs.getSongById(song_id), action);
		}
	};

	/**
	 * Sends a song information to Last.fm.
	 *
	 * It supports several Last.fm Android scrobblers, as specified
	 * on `res/xml/preferences.xml`.
	 *
	 * @param song              Song it'll send to Last.fm
	 * @param musicPlayerAction What is happening to the song right
	 *                          now - as specified on `MusicService`.
	 */
	public void scrobbleSong(Song song, String musicPlayerAction) {

		// Double-checking - won't scrobble if the user
		// don't want us to.
		if (! kMP.settings.get("lastfm", false))
			return;

		String scrobbler = kMP.settings.get("lastfm_which", "sls");

		// See the ScrobbleDroid API here:
		// http://code.google.com/p/scrobbledroid/wiki/DeveloperAPI
		if (scrobbler.equals("scrobbledroid")) {

			Intent scrobble = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");

			boolean isPlaying = true;

			// Assuming the music is playing, unless...
			if (musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_PAUSED) ||
				musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_COMPLETED) ||
				musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_SKIP_NEXT) ||
				musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_SKIP_PREVIOUS))
				isPlaying = false;

			scrobble.putExtra("playing", isPlaying);
			scrobble.putExtra("id", song.getId());

			sendBroadcast(scrobble);
			return;
		}

		// See the SimpleLastfmScrobbler API here:
		// https://github.com/tgwizard/sls/wiki/Developer%27s-API
		if (scrobbler.equals("sls")) {

			Intent scrobble = new Intent("com.adam.aslfms.notify.playstatechanged");

			// One of the specific states allowed from the
			// SimpleLastfmScrobbler API
			int state;

			if (musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_PLAYING))
				state = 0;
			else if (musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_UNPAUSED))
				state = 1;
			else if (musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_PAUSED))
				state = 2;
			else if (musicPlayerAction.equals(ServicePlayMusic.BROADCAST_EXTRA_COMPLETED))
				state = 3;

			// Ignoring any other states - won't be necessary
			else
				return;

			scrobble.putExtra("state", state);

			// Now, to the song's details.
			scrobble.putExtra("app-name",    kMP.applicationName);
			scrobble.putExtra("app-package", kMP.packageName);

			scrobble.putExtra("track",    song.getTitle());
			scrobble.putExtra("artist",   song.getArtist());
			scrobble.putExtra("album",    song.getAlbum());
			scrobble.putExtra("duration", song.getDurationSeconds());

			sendBroadcast(scrobble);
			return;
		}
	}

	/**
	 * Used for Services that want to bind to a specific
	 * Activity or such. Since this service is not bound
	 * to anything, let's just ignore this function.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
