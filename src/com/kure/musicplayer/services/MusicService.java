package com.kure.musicplayer.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.kure.musicplayer.NotificationMusic;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.model.Song;

/**
 * Service that makes the music play and notifies every action.
 *
 * Tasks:
 *
 * - Abstracts controlling the native Android MediaPlayer;
 * - Keep showing a system Notification with info on
 *   currently playing song;
 * - Starts the other service, `MusicScrobblerService`
 *   (if set on Settings) that scrobbles songs to Last.fm;
 * - LocalBroadcasts every action it takes;
 *
 * Broadcasts:
 *
 * This service makes sure to broadcast every action it
 * takes.
 *
 * It sends a LocalBroadcast of name `BROADCAST_EVENT_NAME`,
 * of which you can get it's action with the following
 * extras:
 *
 * - String BROADCAST_EXTRA_ACTION: Current action it's taking.
 *
 * - Long   BROADCAST_EXTRA_SONG_ID: ID of the Song it's taking
 *                                   action into.
 *
 * For example, see the following scenarios:
 *
 * - Starts playing Song with ID 1.
 *   + Send a LocalBroadcast with `BROADCAST_EXTRA_ACTION`
 *     of `BROADCAST_EXTRA_PLAYING` and
 *     `BROADCAST_EXTRA_SONG_ID` of 1.
 *
 * - User skips to a Song with ID 2:
 *   + Send a LocalBroadcast with `BROADCAST_EXTRA_ACTION`
 *     of `BROADCAST_EXTRA_SKIP_NEXT` and
 *     `BROADCAST_EXTRA_SONG_ID` of 1.
 *   + Send a LocalBriadcast with `BROADCAST_EXTRA_ACTION`
 *     of `BROADCAST_EXTRA_PLAYING` and
 *     `BROADCAST_EXTRA_SONG_ID` of 2.
 *
 * @note It keeps the music playing even when the
 *       device is locked.
 *       For that, we must add a special permission
 *       on the AndroidManifest.
 */
public class MusicService extends Service
	implements MediaPlayer.OnPreparedListener,
	           MediaPlayer.OnErrorListener,
	           MediaPlayer.OnCompletionListener {

	/**
	 * String that identifies all broadcasts this Service makes.
	 *
	 * Since this Service will send LocalBroadcasts to explain
	 * what it does (like "playing song" or "paused song"),
	 * other classes that might be interested on it must
	 * register a BroadcastReceiver to this String.
	 */
	public static final String BROADCAST_ACTION = "com.kure.musicplayer.MUSIC_SERVICE";

	/** String used to get the current state Extra on the Broadcast Intent */
	public static final String BROADCAST_EXTRA_STATE = "x_japan";

	/** String used to get the song ID Extra on the Broadcast Intent */
	public static final String BROADCAST_EXTRA_SONG_ID = "tenacious_d";

	// All possible messages this Service will broadcast
	// Ignore the actual values

	/** Broadcast for when some music started playing */
	public static final String BROADCAST_EXTRA_PLAYING = "beatles";

	/** Broadcast for when some music just got paused */
	public static final String BROADCAST_EXTRA_PAUSED = "santana";

	/** Broadcast for when a paused music got unpaused*/
	public static final String BROADCAST_EXTRA_UNPAUSED = "iron_maiden";

	/** Broadcast for when current music got played until the end */
	public static final String BROADCAST_EXTRA_COMPLETED = "los_hermanos";

	/** Broadcast for when the user skipped to the next song */
	public static final String BROADCAST_EXTRA_SKIP_NEXT = "paul_gilbert";

	/** Broadcast for when the user skipped to the previous song */
	public static final String BROADCAST_EXTRA_SKIP_PREVIOUS = "john_petrucci";

	/**
	 * Android Media Player - we control it in here.
	 */
	private MediaPlayer player;

	/**
	 * List of songs we're  currently playing.
	 */
	private ArrayList<Song> songs;

	/**
	 * Index of the current song we're playing on the `songs` list.
	 */
	public int currentSongPosition;

	/**
	 * Copy of the current song being played (or paused).
	 *
	 * Use it to get info from the current song.
	 */
	public Song currentSong = null;

	/**
	 * Flag that indicates whether we're at Shuffle mode.
	 */
	private boolean shuffleMode = false;

	/**
	 * Random number generator for the Shuffle Mode.
	 */
	private Random randomNumberGenerator;

	private boolean repeatMode = false;

	private boolean paused = false;

	/**
	 * Spawns an on-going notification with our current
	 * playing song.
	 */
	private NotificationMusic notification = new NotificationMusic();

	/**
	 * Whenever we're created, reset the MusicPlayer and
	 * start the MusicScrobblerService.
	 */
	public void onCreate() {
		super.onCreate();

		currentSongPosition = 0;
		player = new MediaPlayer();

		initMusicPlayer();

		randomNumberGenerator = new Random();

		// Starting the scrobbler.
		Context context = getApplicationContext();

		Intent scrobblerIntent = new Intent(context, MusicScrobblerService.class);
		context.startService(scrobblerIntent);
	}

	/**
	 * Initializes the internal Music Player.
	 */
	public void initMusicPlayer() {
		if (player == null)
			return;

		// This Wake Lock allows playback to continue
		// even when the device becomes idle.
		player.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);

		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		// These are the events that will "wake us up"
		player.setOnPreparedListener(this); // player initialized
		player.setOnCompletionListener(this); // song completed
		player.setOnErrorListener(this);
	}

	public void setList(ArrayList<Song> theSongs) {
		songs = theSongs;
	}

	/**
	 * Appends a `song` to the currently playing queue.
	 */
	public void add(Song song) {
		songs.add(song);
	}

	/**
	 * Actually plays the song set by `currentSongPosition`.
	 */
	public void playSong() {
		player.reset();

		// Get the song ID from the list, extract the ID and
		// get an URL based on it
		Song songToPlay = songs.get(currentSongPosition);

		currentSong = songToPlay;

		// Append the external URI with our songs'
		Uri songToPlayURI = ContentUris.withAppendedId
				(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				 songToPlay.getId());

		try {
			player.setDataSource(getApplicationContext(), songToPlayURI);
		}
		catch(Exception e) {
			Log.e("MUSIC SERVICE", "Error when changing the song", e);
		}

		// Prepare the MusicPlayer asynchronously.
		// When finished, will call `onPrepare`
		player.prepareAsync();

		broadcastMessage(MusicService.BROADCAST_EXTRA_PLAYING);
	}

	/**
	 * Called when the music is ready for playback.
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {

		// Start playback
		player.start();

		// If the user clicks on the notification, let's spawn the
		// Now Playing screen.
		if (kMP.settings.get("show_notification", true))
			notification.notifySong(this, this, currentSong);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// Service is not restarted.
		// Used for services which are periodically triggered anyway.
		// The service is only restarted if the runtime has pending
		// `startService()` calls since the service termination.
		//
		// Source:
		// http://www.vogella.com/tutorials/AndroidServices/article.html#service_starting
		return Service.START_NOT_STICKY;
	}

	/**
	 * Sets a specific song, already within internal Now Playing List.
	 *
	 * @param songIndex Index of the song inside the Now Playing List.
	 */
	public void setSong(int songIndex) {

		if (songIndex < 0 || songIndex >= songs.size())
			currentSongPosition = 0;

		currentSongPosition = songIndex;
	}

	/**
	 * Will be called when the music completes - either when the
	 * user presses 'next' or when the music ends or when the user
	 * selects another track.
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {

		// TODO: Why do I need this?
		if (player.getCurrentPosition() <= 0)
			return;

		broadcastMessage(MusicService.BROADCAST_EXTRA_COMPLETED);

		// Repeating current song if desired
		if (repeatMode) {
			playSong();
			return;
		}

		// Remember that by calling next(), if played
		// the last song on the list, will reset to the
		// first one.
		next(false);

		// Reached the end, should we restart playing
		// from the first song or simply stop?
		if (kMP.musicService.currentSongPosition == 0) {
			if (kMP.settings.get("repeat_list", false))
				playSong();
			else
			{
				stopSelf();
				currentSong = null;
			}

			return;
		}

		// Common case - skipped a track or anything
		playSong();
	}

	/**
	 * If something wrong happens with the MusicPlayer.
	 */
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		return false;
	}

	@Override
	public void onDestroy() {
		notification.cancel();
		currentSong = null;

		// Stopping the scrobbler service.
		Context context = getApplicationContext();

		Intent scrobblerIntent = new Intent(context, MusicScrobblerService.class);
		context.stopService(scrobblerIntent);

		super.onDestroy();
	}

	// These methods are to be called by the Activity
	// to work on the music-playing.

	/**
	 * Jumps to the previous song on the list.
	 *
	 * @note Remember to call `playSong()` to make the MusicPlayer
	 *       actually play the music.
	 */
	public void previous(boolean userSkippedSong) {

		if (userSkippedSong)
			broadcastMessage(MusicService.BROADCAST_EXTRA_SKIP_PREVIOUS);

		currentSongPosition--;
		if (currentSongPosition < 0)
			currentSongPosition = songs.size() - 1;
	}

	/**
	 * Jumps to the next song on the list.
	 *
	 * @note Remember to call `playSong()` to make the MusicPlayer
	 *       actually play the music.
	 */
	public void next(boolean userSkippedSong) {

		// TODO implement a queue of songs to prevent last songs
		//      to be played
		// TODO or maybe a playlist, whatever

		if (userSkippedSong)
			broadcastMessage(MusicService.BROADCAST_EXTRA_SKIP_NEXT);

		if (shuffleMode) {
			int newSongPosition = currentSongPosition;

			while (newSongPosition == currentSongPosition)
				newSongPosition = randomNumberGenerator.nextInt(songs.size());

			currentSongPosition = newSongPosition;
			return;
		}

		currentSongPosition++;

		if (currentSongPosition >= songs.size())
			currentSongPosition = 0;
	}

	public int getPosition() {
		return player.getCurrentPosition();
	}

	public int getDuration() {
		return player.getDuration();
	}

	public boolean isPlaying() {
		return player.isPlaying();
	}
	public boolean isPaused() {
		return paused;
	}

	public void pausePlayer() {
		player.pause();
		paused = !paused;

		notification.notifyPaused(true);

		broadcastMessage(MusicService.BROADCAST_EXTRA_PAUSED);
	}

	public void unpausePlayer() {
		player.start();
		paused = !paused;

		notification.notifyPaused(false);

		broadcastMessage(MusicService.BROADCAST_EXTRA_UNPAUSED);
	}

	public void togglePausePlayer() {
		if (paused)
			unpausePlayer();
		else
			pausePlayer();
	}

	public void seekTo(int position) {
		player.seekTo(position);
	}

	public void toggleShuffleMode() {
		shuffleMode = !shuffleMode;
	}

	public void toggleRepeatMode() {
		repeatMode = ! repeatMode;
	}


	public long getCurrentSongId() {
		Song currentSong = songs.get(currentSongPosition);

		return currentSong.getId();
	}

	// THESE ARE METHODS RELATED TO CONNECTING THE SERVICE
	// TO THE ANDROID PLATFORM
	// NOTHING TO DO WITH MUSIC-PLAYING

	/**
	 * Tells if this service is bound to an Activity.
	 */
	public boolean musicBound = false;

	/**
	 * Defines the interaction between an Activity and this Service.
	 */
	public class MusicBinder extends Binder {
		public MusicService getService() {
			return MusicService.this;
		}
	}

	/**
	 * Token for the interaction between an Activity and this Service.
	 */
	private final IBinder musicBind = new MusicBinder();

	/**
	 * Called when the Service is finally bound to the app.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return musicBind;
	}

	/**
	 * Called when the Service is unbound - user quitting
	 * the app or something.
	 */
	@Override
	public boolean onUnbind(Intent intent) {
		player.stop();
		player.release();
		return false;
	}

	public boolean isShuffle() {
		return shuffleMode;
	}

	public boolean isRepeat() {
		return repeatMode;
	}

	/**
	 * Sorts the internal Now Playing List according to
	 * a `rule`.
	 *
	 * Supported ways to sort are:
	 * - "title":  Sorts alphabetically by song title
	 * - "artist": Sorts alphabetically by artist name
	 * - "album":  Sorts alphabetically by album name
	 * - "track":  Sorts by track number
	 * - "random": Sorts randomly (shuffles song's orders)
	 */
	public void sortBy(String rule) {

		// We track the currently playing song to
		// a position on the song list.
		//
		// When we sort, it'll be on a different
		// position.
		//
		// So we keep a reference to the currently
		// playing song's ID and then look it up
		// after sorting.
		long nowPlayingSongID = ((currentSong == null) ?
		                         0 :
		                         currentSong.getId());

		if (rule.equals("title"))
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getTitle().compareTo(b.getTitle());
				}
			});

		else if (rule.equals("artist"))
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getArtist().compareTo(b.getArtist());
				}
			});

		else if (rule.equals("album"))
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getAlbum().compareTo(b.getAlbum());
				}
			});

		else if (rule.equals("track"))
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					int left  = a.getTrackNumber();
					int right = b.getTrackNumber();

					if (left == right)
						return 0;

					return ((left < right) ?
					         -1 :
					         1);
				}
			});

		else if (rule.equals("random")) {
			Collections.shuffle(songs, randomNumberGenerator);
		}


		// Now that we sorted, get again the current song
		// position.
		int position = 0;
		for (Song song : songs) {
			if (song.getId() == nowPlayingSongID) {
				currentSongPosition = position;
				break;
			}
			position++;
		}
	}

	/**
	 * Returns the song on the Now Playing List at `position`.
	 */
	public Song getSong(int position) {
		return songs.get(position);
	}

	public void cancelNotification() {
		notification.cancel();
	}

	private void broadcastMessage(String message) {

		Intent broadcastIntent = new Intent(MusicService.BROADCAST_ACTION);

		broadcastIntent.putExtra(MusicService.BROADCAST_EXTRA_STATE,   message);
		broadcastIntent.putExtra(MusicService.BROADCAST_EXTRA_SONG_ID, currentSong.getId());

		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.sendBroadcast(broadcastIntent);

		Log.w("MusicService", "sentBroadcast");
	}
}
