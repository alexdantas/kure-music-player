package com.kure.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.kure.musicplayer.activities.ActivityNowPlaying;

/**
 * Service that makes the music play regardless if our
 * app is on focus.
 *
 * Tasks:
 *
 * - Abstracts controlling the native Android MediaPlayer
 * - Keep showing a system Notification with info on
 *   currently playing song.
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
	 * WHAT THE FUCK
	 */
	private static final int NOTIFY_ID = 1;

	/**
	 * Flag that indicates whether we're at Shuffle mode.
	 */
	private boolean shuffleMode = false;

	/**
	 * Random number generator for the Shuffle Mode.
	 */
	private Random random;

	private boolean repeatMode = false;

	/**
	 * Whenever we're created, reset the MusicPlayer.
	 */
	public void onCreate() {
		super.onCreate();

		currentSongPosition = 0;
		player = new MediaPlayer();

		initMusicPlayer();

		random = new Random();
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
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// Start playback
		player.start();

		if (kMP.settings.get("show_notification", true)) {
			// If the user clicks on the notification, let's spawn the
			// Now Playing screen.
			Intent notifyIntent = new Intent(this, ActivityNowPlaying.class);
			notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			PendingIntent pendingIntent = PendingIntent.getActivity(this,
					0,
					notifyIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			// Will create a Notification
			Notification.Builder builder = new Notification.Builder(this);

			builder.setContentIntent(pendingIntent)
			.setSmallIcon(R.drawable.play)
			.setTicker(currentSong.getTitle())
			.setOngoing(true)
			.setContentTitle(currentSong.getTitle())
			.setContentText(currentSong.getArtist());

			Notification notification = builder.build();

			// Sets the notification to run on the foreground.
			startForeground(NOTIFY_ID, notification);
		}

		// Can only send to last.fm when prepared.
		scrobbleCurrentSong(true);
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

		scrobbleCurrentSong(false);

		// Repeating current song if desired
		if (repeatMode) {
			playSong();
			return;
		}

		// Remember that by calling next(), if played
		// the last song on the list, will reset to the
		// first one.
		next();

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
		// Stops feeding the notification
		stopForeground(true);
		currentSong = null;

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
	public void previous() {

		scrobbleCurrentSong(false);

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
	public void next() {

		// TODO implement a queue of songs to prevent last songs
		//      to be played
		// TODO or maybe a playlist, whatever

		scrobbleCurrentSong(false);

		if (shuffleMode) {
			int newSongPosition = currentSongPosition;

			while (newSongPosition == currentSongPosition)
				newSongPosition = random.nextInt(songs.size());

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

	public void pausePlayer() {
		player.pause();

		scrobbleCurrentSong(false);
	}

	public void seekTo(int position) {
		player.seekTo(position);
	}

	public void start() {
		player.start();

		scrobbleCurrentSong(true);
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

	/**
	 * Last.fm support!
	 *
	 * We'll send our current song to ScrobbleDroid ONLY IF
	 * the preference for it is enabled.
	 *
	 * This needs to be called as often as possible - when pausing,
	 * resuming, when the track is going to be changed, when the
	 * track is changed...
	 *
	 * @note To avoid concurrency issues, make sure to clal
	 *       this method only when the music player is prepared!
	 * @see onPrepared()
	 */
	private void scrobbleCurrentSong(boolean isPlaying) {

		// Only scrobbling if the user lets us
		if (! kMP.settings.get("lastfm", false))
			return;

		Intent scrobble = new Intent("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");

		scrobble.putExtra("playing", isPlaying);
		scrobble.putExtra("id", getCurrentSongId());

		sendBroadcast(scrobble);
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
		MusicService getService() {
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
		long nowPlayingSongID = currentSong.getId();

		if (rule == "title")
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getTitle().compareTo(b.getTitle());
				}
			});

		else if (rule == "artist")
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getArtist().compareTo(b.getArtist());
				}
			});

		else if (rule == "album")
			Collections.sort(songs, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getAlbum().compareTo(b.getAlbum());
				}
			});

		else if (rule == "track") {
			// not implemented yet
		}

		else if (rule == "random") {
			Collections.shuffle(songs, random);
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
}
