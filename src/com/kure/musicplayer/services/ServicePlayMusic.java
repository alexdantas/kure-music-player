package com.kure.musicplayer.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.kure.musicplayer.NotificationMusic;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.external.RemoteControlClientCompat;
import com.kure.musicplayer.external.RemoteControlHelper;
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
public class ServicePlayMusic extends Service
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

    // The tag we put on debug messages
    final static String TAG = "MusicService";

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String BROADCAST_ORDER = "com.kure.musicplayer.MUSIC_SERVICE";
    public static final String BROADCAST_EXTRA_GET_ORDER = "com.kure.musicplayer.dasdas.MUSIC_SERVICE";

    public static final String BROADCAST_ORDER_PLAY = "com.kure.musicplayer.action.PLAY";
    public static final String BROADCAST_ORDER_PAUSE = "com.kure.musicplayer.action.PAUSE";
    public static final String BROADCAST_ORDER_TOGGLE_PLAYBACK = "dlsadasd";
    public static final String BROADCAST_ORDER_STOP = "com.kure.musicplayer.action.STOP";
    public static final String BROADCAST_ORDER_SKIP = "com.kure.musicplayer.action.SKIP";
    public static final String BROADCAST_ORDER_REWIND = "com.kure.musicplayer.action.REWIND";




    /**
     * Controller that communicates with the lock screen,
     * providing that fancy widget.
     */
    RemoteControlClientCompat lockscreenController = null;

    /**
     * We use this to get the media buttons' Broadcasts and
     * to control the lock screen widget.
     *
     * Component name of the MusicIntentReceiver.
     */
    ComponentName mediaButtonReceiver;

    /**
     * Use this to get audio focus:
     *
     * 1. Making sure other music apps don't play
     *    at the same time;
     * 2. Guaranteeing the lock screen widget will
     *    be controlled by us;
     */
    AudioManager audioManager;



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

		// Starting the scrobbler service.
		Context context = getApplicationContext();

		Intent scrobblerIntent = new Intent(context, ServiceScrobbleMusic.class);
		context.startService(scrobblerIntent);


        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mediaButtonReceiver = new ComponentName(this, ExternalBroadcastReceiver.class);


		// Registering our BroadcastReceiver to listen to orders.
		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.registerReceiver(localBroadcastReceiver, new IntentFilter(ServicePlayMusic.BROADCAST_ORDER));
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
     * Receives external Broadcasts and gives our MusicService
     * orders based on them.
     *
     * It is the bridge between our application and the external
     * world. It receives Broadcasts and launches Internal Broadcasts.
     *
     * It acts on music events (such as disconnecting headphone)
     * and music controls (the lockscreen widget).
     *
	 * @note This class works because we are declaring it in a
	 *       `receiver` tag in `AndroidManifest.xml`.
	 *
	 * @note It is static so we can look out for external broadcasts
	 *       even when the service is offline.
	 */
    public static class ExternalBroadcastReceiver extends BroadcastReceiver {

    	@Override
    	public void onReceive(Context context, Intent intent) {

    		Log.w("service", "received audio controls");

    		// Broadcasting orders to our MusicService
    		// locally (inside the application)
			LocalBroadcastManager local = LocalBroadcastManager.getInstance(context);

    		// Headphones disconnected
    		if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {

    			Toast.makeText(context, "Headphones disconnected.", Toast.LENGTH_SHORT).show();

    			// send an intent to our MusicService to telling it to pause the audio
    			local.sendBroadcast(new Intent(ServicePlayMusic.BROADCAST_ORDER_PAUSE));
    			return;
    		}

    		if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {

    			// Which media key was pressed
    			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

    			// Not interested on anything other than pressed keys.
    			if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
    				return;


    			switch (keyEvent.getKeyCode()) {

    			case KeyEvent.KEYCODE_HEADSETHOOK:
    			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
    				local.sendBroadcast(
    						new Intent(
    								ServicePlayMusic.BROADCAST_ORDER)
    						.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
    								ServicePlayMusic.BROADCAST_ORDER_TOGGLE_PLAYBACK));
    				break;

    			case KeyEvent.KEYCODE_MEDIA_PLAY:
    				local.sendBroadcast(
    						new Intent(
    								ServicePlayMusic.BROADCAST_ORDER)
    						.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
    								ServicePlayMusic.BROADCAST_ORDER_PLAY));
    				break;

    			case KeyEvent.KEYCODE_MEDIA_PAUSE:
    				local.sendBroadcast(
    						new Intent(
    								ServicePlayMusic.BROADCAST_ORDER)
    						.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
    								ServicePlayMusic.BROADCAST_ORDER_PAUSE));

    				break;

    			case KeyEvent.KEYCODE_MEDIA_STOP:
    				local.sendBroadcast(
    						new Intent(
    								ServicePlayMusic.BROADCAST_ORDER)
    						.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
    								ServicePlayMusic.BROADCAST_ORDER_STOP));

    				break;

    			case KeyEvent.KEYCODE_MEDIA_NEXT:
    				local.sendBroadcast(
    						new Intent(
    								ServicePlayMusic.BROADCAST_ORDER)
    						.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
    								ServicePlayMusic.BROADCAST_ORDER_SKIP));

    				break;

    			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
    				// TODO: ensure that doing this in rapid succession actually plays the
    				// previous song
    				local.sendBroadcast(
    						new Intent(
    								ServicePlayMusic.BROADCAST_ORDER)
    						.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
    								ServicePlayMusic.BROADCAST_ORDER_REWIND));

    				break;
    			}
    		}
    	}
    };


	/**
	 * The thing that will keep an eye on LocalBroadcasts
	 * for the MusicService.
	 */
	BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {

		/**
		 * What it'll do when receiving a message from the
		 * MusicService?
		 */
		@Override
		public void onReceive(Context context, Intent intent) {

			// Getting the information sent by the MusicService
			// (and ignoring it if invalid)
			String order = intent.getStringExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER);

			// What?
			if (order == null)
				return;

			if (order.equals(ServicePlayMusic.BROADCAST_ORDER_PAUSE)) {
				pausePlayer();
			}
			else if (order.equals(ServicePlayMusic.BROADCAST_ORDER_PLAY)) {
				unpausePlayer();
			}
			else if (order.equals(ServicePlayMusic.BROADCAST_ORDER_TOGGLE_PLAYBACK)) {
				togglePlayback();
			}
			else if (order.equals(ServicePlayMusic.BROADCAST_ORDER_SKIP)) {
				next(true);
				playSong();
			}
			else if (order.equals(ServicePlayMusic.BROADCAST_ORDER_REWIND)) {
				previous(true);
				playSong();
			}

			Log.w(TAG, "local broadcast received");
		}
	};










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

		broadcastCurrentState(ServicePlayMusic.BROADCAST_EXTRA_PLAYING);

		updateLockScreenWidget();
	}

	/**
	 * Asks the AudioManager for our application to
	 * have the audio focus.
	 *
	 * @return If we have it.
	 */
	private boolean requestAudioFocus() {
		//Request audio focus for playback
		int result = audioManager.requestAudioFocus(
				audioFocusChangeListener,
				AudioManager.STREAM_MUSIC,
				AudioManager.AUDIOFOCUS_GAIN);

		//Check if audio focus was granted. If not, stop the service.
		return (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
	}

	/**
	 * Does something when the audio focus state changed
	 *
	 * @note Meaning it runs when we get and when we don't get
	 *       the audio focus from `#requestAudioFocus()`.
	 */
    OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {

            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                pausePlayer();
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                unpausePlayer();
            }
            else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {

            	// Giving up everything and stopping playback
                audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiver);
                audioManager.abandonAudioFocus(audioFocusChangeListener);

                Intent stopPlayback = new Intent(ServicePlayMusic.BROADCAST_ORDER);

                stopPlayback.putExtra(
                		ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER,
                		ServicePlayMusic.BROADCAST_ORDER_PAUSE);

        		// Broadcasting orders to our MusicService
        		// locally (inside the application)
    			LocalBroadcastManager local = LocalBroadcastManager.getInstance(getApplicationContext());

				local.sendBroadcast(stopPlayback);
            }

            Log.w("service", "on audio focus change listener");
        }
    };

	private void updateLockScreenWidget() {

		if (!requestAudioFocus()) {
		    //Stop the service.
		    stopSelf();
		    Toast.makeText(getApplicationContext(), "FUCK", Toast.LENGTH_LONG).show();
		    return;
		}

		Log.w("service", "audio_focus_granted");

		// The Lock-Screen widget was not created
		// up until now.
        if (lockscreenController == null) {
            Intent audioButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            audioButtonIntent.setComponent(mediaButtonReceiver);

            PendingIntent pending = PendingIntent.getBroadcast(this, 0, audioButtonIntent, 0);

            lockscreenController = new RemoteControlClientCompat(pending);

            RemoteControlHelper.registerRemoteControlClient(audioManager, lockscreenController);
            audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver);

            Log.w("service", "created control compat");
        }

        // Current state of the Lock-Screen Widget
        lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

        // All buttons the Lock-Screen Widget supports
        // (will be broadcasts)
        lockscreenController.setTransportControlFlags(
        		RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
        		RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
        		RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
        		RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
        		RemoteControlClient.FLAG_KEY_MEDIA_STOP);

        // Update the current song metadata
        // on the Lock-Screen Widget
        lockscreenController
        		// Starts editing (before #apply())
        		.editMetadata(true)

        		// Sending all metadata of the current song
                .putString(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST,   currentSong.getArtist())
                .putString(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM,    currentSong.getAlbum())
                .putString(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE,    currentSong.getTitle())
                .putLong  (android.media.MediaMetadataRetriever.METADATA_KEY_DURATION, currentSong.getDuration())

                // TODO: fetch real item artwork
                //.putBitmap(
                //        RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                //        mDummyAlbumArt)

                // Saves (after #editMetadata())
                .apply();

        Log.w("service", "remote control client applied");
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

		broadcastCurrentState(ServicePlayMusic.BROADCAST_EXTRA_COMPLETED);

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
		cancelNotification();
		currentSong = null;

		// Stopping the scrobbler service.
		Context context = getApplicationContext();

		Intent scrobblerIntent = new Intent(context, ServiceScrobbleMusic.class);
		context.stopService(scrobblerIntent);





		audioManager.abandonAudioFocus(audioFocusChangeListener);





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
			broadcastCurrentState(ServicePlayMusic.BROADCAST_EXTRA_SKIP_PREVIOUS);

		// Updates Lock-Screen Widget
		if (lockscreenController != null)
			lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS);

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
			broadcastCurrentState(ServicePlayMusic.BROADCAST_EXTRA_SKIP_NEXT);

		// Updates Lock-Screen Widget
		if (lockscreenController != null)
			lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS);

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

        // Updates Lock-Screen Widget
		if (lockscreenController != null)
			lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);

		broadcastCurrentState(ServicePlayMusic.BROADCAST_EXTRA_PAUSED);
	}

	public void unpausePlayer() {
		player.start();
		paused = !paused;

		notification.notifyPaused(false);

		// Updates Lock-Screen Widget
		if (lockscreenController != null)
			lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

		broadcastCurrentState(ServicePlayMusic.BROADCAST_EXTRA_UNPAUSED);
	}

	/**
	 * Toggles between Pause and Unpause.
	 *
	 * @see pausePlayer()
	 * @see unpausePlayer()
	 */
	public void togglePlayback() {
		if (paused)
			unpausePlayer();
		else
			pausePlayer();
	}

	public void seekTo(int position) {
		player.seekTo(position);
	}

	/**
	 * Toggles the Shuffle mode
	 * (if will play songs in random order).
	 */
	public void toggleShuffle() {
		shuffleMode = !shuffleMode;
	}

	/**
	 * Shuffle mode state.
	 * @return If Shuffle mode is on/off.
	 */
	public boolean isShuffle() {
		return shuffleMode;
	}

	/**
	 * Toggles the Repeat mode
	 * (if the current song will play again
	 *  when completed).
	 */
	public void toggleRepeat() {
		repeatMode = ! repeatMode;
	}

	/**
	 * Repeat mode state.
	 * @return If Repeat mode is on/off.
	 */
	public boolean isRepeat() {
		return repeatMode;
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
		public ServicePlayMusic getService() {
			return ServicePlayMusic.this;
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

	/**
	 * Shouts the current state of the Music Service.
	 *
	 * @note This broadcast is visible only inside this application.
	 *
	 * @note Will get received by listeners of `ServicePlayMusic.BROADCAST_ACTION`
	 *
	 * @param state Current state of the Music Service.
	 */
	private void broadcastCurrentState(String state) {

		Intent broadcastIntent = new Intent(ServicePlayMusic.BROADCAST_ACTION);

		broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_STATE,   state);
		broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_SONG_ID, currentSong.getId());

		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.sendBroadcast(broadcastIntent);

		Log.w(TAG, "sentBroadcast");
	}
}
