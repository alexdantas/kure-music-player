package com.kure.musicplayer;

import java.util.ArrayList;
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
 * Note that it keeps the music playing even when the
 * device is locked.
 * For that, we must add a special permission on the
 * Manifest.
 * 
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
	
		
	// We'll compose the text to be shown on the notification
	// area based on those variables
	private String currentSongTitle  = "";
	private String currentSongArtist = "";
	
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
		
		currentSongTitle  = songToPlay.getTitle();
		currentSongArtist = songToPlay.getArtist();
		
		long songToPlayID = songToPlay.getId();
		
		// Append the external URI with our songs'
		Uri songToPlayURI = ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				songToPlayID);
		
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
			.setTicker(currentSongTitle)
			.setOngoing(true)
			.setContentTitle(currentSongTitle)
			.setContentText(currentSongArtist);

			Notification notification = builder.build();

			// Sets the notification to run on the foreground.
			startForeground(NOTIFY_ID, notification);
		}
		
		// Can only send to last.fm when prepared.
		scrobbleCurrentSong(true);
	}
	
	/**
	 * Let's call this when the user selects a song from the list.
	 * @param songIndex FUCK YEAH
	 */
	public void setSong(int songIndex) {
		currentSongPosition = songIndex;
	}

	/**
	 * Will be called when the music completes - either when the
	 * user presses 'next' or when the music ends or when the user
	 * selects another track.
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {
		
		if (player.getCurrentPosition() > 0) {
			
			if (repeatMode) {
				scrobbleCurrentSong(false);
				mp.reset();
				playSong();
			}
			else {
				mp.reset();
				next();
			}
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		mp.reset();
		return false;
	}
	
	@Override
	public void onDestroy() {
		// Stops feeding the notification
		stopForeground(true);
		
		super.onDestroy();
	}
	
	// These methods are to be called by the Activity
	// to work on the music-playing.
	
	public void previous() {
		
		scrobbleCurrentSong(false);
		
		currentSongPosition--;
		if (currentSongPosition < 0)
			currentSongPosition = songs.size() - 1;
		
		kMP.nowPlayingIndex = currentSongPosition;
		
		playSong();
	}
	
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
		
		// If we've reached the end of our current play queue
		boolean reachedEnd = false;
		
		if (currentSongPosition >= songs.size()) {
			currentSongPosition = 0;
			reachedEnd = true;
		}
		kMP.nowPlayingIndex = currentSongPosition;

		// Reached the end, should we restart playing
		// from the first song or simply stop?		
		if (reachedEnd) {
			if (kMP.settings.get("repeat_list", false))
				playSong();
			else
				stopSelf();
		}
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
}
