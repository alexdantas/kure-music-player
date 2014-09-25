package com.kure.musicplayer.services;

import java.io.IOException;
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
import com.kure.musicplayer.R;
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
 * - Keep watching for headphone/headset events with
 *   a Broadcast - and react accordingly.
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
 *
 * Thanks:
 * - Google's MediaPlayer guide - has info on AudioFocus,
 *   Services and lotsa stuff
 *   http://developer.android.com/guide/topics/media/mediaplayer.html
 */
public class ServicePlayMusic extends Service
	implements MediaPlayer.OnPreparedListener,
	           MediaPlayer.OnErrorListener,
	           MediaPlayer.OnCompletionListener,
	           AudioManager.OnAudioFocusChangeListener {

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

	/**
	 * Spawns an on-going notification with our current
	 * playing song.
	 */
	private NotificationMusic notification = null;

    // The tag we put on debug messages
    final static String TAG = "MusicService";

    // These are the Intent actions that we are prepared to handle. Notice that the fact these
    // constants exist in our class is a mere convenience: what really defines the actions our
    // service can handle are the <action> tags in the <intent-filters> tag for our service in
    // AndroidManifest.xml.
    public static final String BROADCAST_ORDER = "com.kure.musicplayer.MUSIC_SERVICE";
    public static final String BROADCAST_EXTRA_GET_ORDER = "com.kure.musicplayer.dasdas.MUSIC_SERVICE";

    public static final String BROADCAST_ORDER_PLAY            = "com.kure.musicplayer.action.PLAY";
    public static final String BROADCAST_ORDER_PAUSE           = "com.kure.musicplayer.action.PAUSE";
    public static final String BROADCAST_ORDER_TOGGLE_PLAYBACK = "dlsadasd";
    public static final String BROADCAST_ORDER_STOP            = "com.kure.musicplayer.action.STOP";
    public static final String BROADCAST_ORDER_SKIP            = "com.kure.musicplayer.action.SKIP";
    public static final String BROADCAST_ORDER_REWIND          = "com.kure.musicplayer.action.REWIND";


    /**
     * Possible states this Service can be on.
     */
    enum ServiceState {
    	// MediaPlayer is stopped and not prepared to play
    	Stopped,

    	// MediaPlayer is preparing...
    	Preparing,

    	// Playback active - media player ready!
    	// (but the media player may actually be paused in
    	// this state if we don't have audio focus).
    	Playing,

    	// So that we know we have to resume playback once we get focus back)
    	// playback paused (media player ready!)
    	Paused
    }

    /**
     * Current state of the Service.
     */
    ServiceState serviceState = ServiceState.Preparing;



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
    ComponentName mediaButtonEventReceiver;

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

		randomNumberGenerator = new Random();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		initMusicPlayer();

		Context context = getApplicationContext();

        // Starting the scrobbler service.
        Intent scrobblerIntent = new Intent(context, ServiceScrobbleMusic.class);
        context.startService(scrobblerIntent);

		// Registering our BroadcastReceiver to listen to orders
        // from inside our own application.
		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.registerReceiver(localBroadcastReceiver, new IntentFilter(ServicePlayMusic.BROADCAST_ORDER));

		// Registering the headset broadcaster for info related
		// to user plugging the headset.
		IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(headsetBroadcastReceiver, headsetFilter);

		Log.w(TAG, "onCreate");
	}

	/**
	 * Initializes the Android's internal MediaPlayer.
	 *
	 * @note We might call this function several times without
	 *       necessarily calling {@link #stopMusicPlayer()}.
	 */
	public void initMusicPlayer() {
		if (player == null)
			player = new MediaPlayer();

		// Assures the CPU continues running this service
		// even when the device is sleeping.
		player.setWakeMode(getApplicationContext(),
				PowerManager.PARTIAL_WAKE_LOCK);

		player.setAudioStreamType(AudioManager.STREAM_MUSIC);

		// These are the events that will "wake us up"
		player.setOnPreparedListener(this); // player initialized
		player.setOnCompletionListener(this); // song completed
		player.setOnErrorListener(this);

		Log.w(TAG, "initMusicPlayer");
	}
	/**
	 * Cleans resources from Android's native MediaPlayer.
	 *
	 * @note According to the MediaPlayer guide, you should release
	 *       the MediaPlayer as often as possible.
	 *       For example, when losing Audio Focus for an extended
	 *       period of time.
	 */
	public void stopMusicPlayer() {
		if (player == null)
			return;

		player.stop();
		player.release();
		player = null;

		Log.w(TAG, "stopMusicPlayer");
	}

	/**
	 * Sets the "Now Playing List"
	 *
	 * @param theSongs Songs list that will play from now on.
	 *
	 * @note Make sure to call {@link #playSong()} after this.
	 */
	public void setList(ArrayList<Song> theSongs) {
		songs = theSongs;
	}

	/**
	 * Appends a song to the end of the currently playing queue.
	 *
	 * @param song New song to put at the end.
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

    		Log.w(TAG, "external broadcast");

    		// Broadcasting orders to our MusicService
    		// locally (inside the application)
    		LocalBroadcastManager local = LocalBroadcastManager.getInstance(context);

			String action = intent.getAction();

    		// Headphones disconnected
    		if (action.equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {

    			// Will only pause the music if the Setting
    			// for it is enabled.
    			if (! kMP.settings.get("pause_headphone_off", true))
    				return;

    			// ADD SETTINGS HERE
    			String text = context.getString(R.string.service_music_play_headphone_off);
    			Toast.makeText(context, text, Toast.LENGTH_SHORT).show();

    			// send an intent to our MusicService to telling it to pause the audio
    			Intent broadcastIntent = new Intent(ServicePlayMusic.BROADCAST_ORDER);
				broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER, ServicePlayMusic.BROADCAST_ORDER_PAUSE);

				local.sendBroadcast(broadcastIntent);
    			Log.w(TAG, "becoming noisy");
    			return;
    		}

    		if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {

    			// Which media key was pressed
    			KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);

    			// Not interested on anything other than pressed keys.
    			if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
    				return;

    			String intentValue = null;

    			switch (keyEvent.getKeyCode()) {

    			case KeyEvent.KEYCODE_HEADSETHOOK:
    			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
    				intentValue = ServicePlayMusic.BROADCAST_ORDER_TOGGLE_PLAYBACK;
    				Log.w(TAG, "media play pause");
    				break;

    			case KeyEvent.KEYCODE_MEDIA_PLAY:
    				intentValue = ServicePlayMusic.BROADCAST_ORDER_PLAY;
    				Log.w(TAG, "media play");
    				break;

    			case KeyEvent.KEYCODE_MEDIA_PAUSE:
    				intentValue = ServicePlayMusic.BROADCAST_ORDER_PAUSE;
    				Log.w(TAG, "media pause");
    				break;

    			case KeyEvent.KEYCODE_MEDIA_NEXT:
    				intentValue = ServicePlayMusic.BROADCAST_ORDER_SKIP;
    				Log.w(TAG, "media next");
    				break;

    			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
    				// TODO: ensure that doing this in rapid succession actually plays the
    				// previous song
    				intentValue = ServicePlayMusic.BROADCAST_ORDER_REWIND;
    				Log.w(TAG, "media previous");
    				break;
    			}

    			// Actually sending the Intent
    			if (intentValue != null) {
    				Intent broadcastIntent = new Intent(ServicePlayMusic.BROADCAST_ORDER);
    				broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER, intentValue);

    				local.sendBroadcast(broadcastIntent);
    			}
    		}
    	}
    }

    /**
     * Will keep an eye on global broadcasts related to
     * the Headset.
     */
    BroadcastReceiver headsetBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();

			// Headphones just connected (or not)
			if (action.equals(Intent.ACTION_HEADSET_PLUG)) {

				Log.w(TAG, "headset plug");
				boolean connectedHeadphones = (intent.getIntExtra("state", 0) == 1);
				boolean connectedMicrophone = (intent.getIntExtra("microphone", 0) == 1) && connectedHeadphones;

				// User just connected headphone and the player was paused,
				// so we shoud restart the music.
				if (connectedMicrophone && (serviceState == ServiceState.Paused)) {

					// Will only do it if it's Setting is enabled, of course
					if (kMP.settings.get("play_headphone_on", true)) {
						LocalBroadcastManager local = LocalBroadcastManager.getInstance(context);

						Intent broadcastIntent = new Intent(ServicePlayMusic.BROADCAST_ORDER);
						broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_GET_ORDER, ServicePlayMusic.BROADCAST_ORDER_PLAY);

						local.sendBroadcast(broadcastIntent);
					}
				}

				// I wonder what's this for
				String headsetName = intent.getStringExtra("name");

				if (connectedHeadphones) {
					String text = context.getString(R.string.service_music_play_headphone_on, headsetName);

					Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
				}
			}
		}
    };

	/**
	 * The thing that will keep an eye on LocalBroadcasts
	 * for the MusicService.
	 */
	BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
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
	 * Asks the AudioManager for our application to
	 * have the audio focus.
	 *
	 * @return If we have it.
	 */
	private boolean requestAudioFocus() {
		//Request audio focus for playback
		int result = audioManager.requestAudioFocus(
				this,
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
	 *
	 * For example, when we receive a message, we lose the focus
	 * and when the ringer stops playing, we get the focus again.
	 *
	 * So we must avoid the bug that occurs when the user pauses
	 * the player but receives a message - and since after that
	 * we get the focus, the player will unpause.
	 */
	public void onAudioFocusChange(int focusChange) {

		switch (focusChange) {

		// Yay, gained audio focus! Either from losing it for
		// a long or short periods of time.
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.w(TAG, "audiofocus gain");

			if (player == null)
				initMusicPlayer();

			if (pausedTemporarilyDueToAudioFocus) {
				pausedTemporarilyDueToAudioFocus = false;
				unpausePlayer();
			}

			if (loweredVolumeDueToAudioFocus) {
				loweredVolumeDueToAudioFocus = false;
				player.setVolume(1.0f, 1.0f);
			}
			break;

		// Damn, lost the audio focus for a (presumable) long time
		case AudioManager.AUDIOFOCUS_LOSS:
			Log.w(TAG, "audiofocus loss");

			// Giving up everything
			//audioManager.unregisterMediaButtonEventReceiver(mediaButtonEventReceiver);
			//audioManager.abandonAudioFocus(this);

			//pausePlayer();
			stopMusicPlayer();
			break;

		// Just lost audio focus but will get it back shortly
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.w(TAG, "audiofocus loss transient");

			if (! isPaused()) {
				pausePlayer();
				pausedTemporarilyDueToAudioFocus = true;
			}
			break;

		// Temporarily lost audio focus but I can keep it playing
		// at a low volume instead of stopping completely
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.w(TAG, "audiofocus loss transient can duck");

			player.setVolume(0.1f, 0.1f);
			loweredVolumeDueToAudioFocus = true;
			break;
		}
	}
	// Internal flags for the function above {{
	private boolean pausedTemporarilyDueToAudioFocus = false;
	private boolean loweredVolumeDueToAudioFocus     = false;
	// }}

    /**
     * Updates the lock-screen widget (creating if non-existing).
     *
     * @param song  Where it will take metadata to display.
     *
     * @param state Which state is it into.
     *              Can be one of the following:
     *              {@link RemoteControlClient.PLAYSTATE_PLAYING }
     *              {@link RemoteControlClient.PLAYSTATE_PAUSED }
     *              {@link RemoteControlClient.PLAYSTATE_BUFFERING }
     *              {@link RemoteControlClient.PLAYSTATE_ERROR }
     *              {@link RemoteControlClient.PLAYSTATE_FAST_FORWARDING }
     *              {@link RemoteControlClient.PLAYSTATE_REWINDING }
     *              {@link RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS }
     *              {@link RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS }
     *              {@link RemoteControlClient.PLAYSTATE_STOPPED }
     */
	public void updateLockScreenWidget(Song song, int state) {

		// Only showing if the Setting is... well... set
		if (! kMP.settings.get("show_lock_widget", true))
			return;

		if (song == null)
			return;

		if (!requestAudioFocus()) {
		    //Stop the service.
		    stopSelf();
		    Toast.makeText(getApplicationContext(), "FUCK", Toast.LENGTH_LONG).show();
		    return;
		}

		Log.w("service", "audio_focus_granted");

		// The Lock-Screen widget was not created up until now.
		// (both of the null-checks below)
		if (mediaButtonEventReceiver == null)
			mediaButtonEventReceiver = new ComponentName(this, ExternalBroadcastReceiver.class);

        if (lockscreenController == null) {
            Intent audioButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            audioButtonIntent.setComponent(mediaButtonEventReceiver);

            PendingIntent pending = PendingIntent.getBroadcast(this, 0, audioButtonIntent, 0);

            lockscreenController = new RemoteControlClientCompat(pending);

            RemoteControlHelper.registerRemoteControlClient(audioManager, lockscreenController);
            audioManager.registerMediaButtonEventReceiver(mediaButtonEventReceiver);

            Log.w("service", "created control compat");
        }

        // Current state of the Lock-Screen Widget
        lockscreenController.setPlaybackState(state);

        // All buttons the Lock-Screen Widget supports
        // (will be broadcasts)
        lockscreenController.setTransportControlFlags(
        		RemoteControlClient.FLAG_KEY_MEDIA_PLAY     |
        		RemoteControlClient.FLAG_KEY_MEDIA_PAUSE    |
        		RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
        		RemoteControlClient.FLAG_KEY_MEDIA_NEXT);

        // Update the current song metadata
        // on the Lock-Screen Widget
        lockscreenController
        		// Starts editing (before #apply())
        		.editMetadata(true)

        		// Sending all metadata of the current song
                .putString(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST,   song.getArtist())
                .putString(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM,    song.getAlbum())
                .putString(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE,    song.getTitle())
                .putLong  (android.media.MediaMetadataRetriever.METADATA_KEY_DURATION, song.getDuration())

                // TODO: fetch real item artwork
                //.putBitmap(
                //        RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,
                //        mDummyAlbumArt)

                // Saves (after #editMetadata())
                .apply();

        Log.w("service", "remote control client applied");
	}

	public void destroyLockScreenWidget() {
		if ((audioManager != null) && (lockscreenController != null)) {
			//RemoteControlHelper.unregisterRemoteControlClient(audioManager, lockscreenController);
			lockscreenController = null;
		}

		if ((audioManager != null) && (mediaButtonEventReceiver != null)) {
			audioManager.unregisterMediaButtonEventReceiver(mediaButtonEventReceiver);
			mediaButtonEventReceiver = null;
		}
	}

	/**
	 * Called when the music is ready for playback.
	 */
	@Override
	public void onPrepared(MediaPlayer mp) {

		serviceState = ServiceState.Playing;

		// Start playback
		player.start();

		// If the user clicks on the notification, let's spawn the
		// Now Playing screen.
		notifyCurrentSong();
	}

	/**
	 * Sets a specific song, already within internal Now Playing List.
	 *
	 * @param songIndex Index of the song inside the Now Playing List.
	 */
	public void setSong(int songIndex) {

		if (songIndex < 0 || songIndex >= songs.size())
			currentSongPosition = 0;
		else
			currentSongPosition = songIndex;
	}

	/**
	 * Will be called when the music completes - either when the
	 * user presses 'next' or when the music ends or when the user
	 * selects another track.
	 */
	@Override
	public void onCompletion(MediaPlayer mp) {

		// Keep this state!
		serviceState = ServiceState.Playing;

		// TODO: Why do I need this?
/*		if (player.getCurrentPosition() <= 0)
			return;
*/
		broadcastState(ServicePlayMusic.BROADCAST_EXTRA_COMPLETED);

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
		if (currentSongPosition == 0) {
			if (kMP.settings.get("repeat_list", false))
				playSong();

			else
				destroySelf();

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
		Log.w(TAG, "onError");
		return false;
	}

	@Override
	public void onDestroy() {
		Context context = getApplicationContext();

		cancelNotification();

		currentSong = null;

		// Stopping the scrobbler service.
		Intent scrobblerIntent = new Intent(context, ServiceScrobbleMusic.class);
		context.stopService(scrobblerIntent);

		if (audioManager != null)
			audioManager.abandonAudioFocus(this);

		stopMusicPlayer();

		destroyLockScreenWidget();

		Log.w(TAG, "onDestroy");
		super.onDestroy();
	}

	/**
	 * Kills the service.
	 *
	 * @note Explicitly call this when the service is completed
	 *       or whatnot.
	 */
	private void destroySelf() {
		stopSelf();
		currentSong = null;
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
		if (serviceState != ServiceState.Paused && serviceState != ServiceState.Playing)
			return;

		if (userSkippedSong)
			broadcastState(ServicePlayMusic.BROADCAST_EXTRA_SKIP_PREVIOUS);

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
		if (serviceState != ServiceState.Paused && serviceState != ServiceState.Playing)
			return;

		// TODO implement a queue of songs to prevent last songs
		//      to be played
		// TODO or maybe a playlist, whatever

		if (userSkippedSong)
			broadcastState(ServicePlayMusic.BROADCAST_EXTRA_SKIP_NEXT);

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
		boolean returnValue = false;

		try {
			returnValue = player.isPlaying();
		}
		catch (IllegalStateException e) {
			player.reset();
			player.prepareAsync();
		}

		return returnValue;
	}

	public boolean isPaused() {
		return serviceState == ServiceState.Paused;
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
		catch(IOException io) {
			Log.e(TAG, "IOException: couldn't change the song", io);
			destroySelf();
		}
		catch(Exception e) {
			Log.e(TAG, "Error when changing the song", e);
			destroySelf();
		}

		// Prepare the MusicPlayer asynchronously.
		// When finished, will call `onPrepare`
		player.prepareAsync();
		serviceState = ServiceState.Preparing;

		broadcastState(ServicePlayMusic.BROADCAST_EXTRA_PLAYING);

		updateLockScreenWidget(currentSong, RemoteControlClient.PLAYSTATE_PLAYING);
		Log.w(TAG, "play song");
	}

	public void pausePlayer() {
		if (serviceState != ServiceState.Paused && serviceState != ServiceState.Playing)
			return;

		player.pause();
		serviceState = ServiceState.Paused;

		notification.notifyPaused(true);

        // Updates Lock-Screen Widget
		if (lockscreenController != null)
			lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);

		broadcastState(ServicePlayMusic.BROADCAST_EXTRA_PAUSED);
	}

	public void unpausePlayer() {
		if (serviceState != ServiceState.Paused && serviceState != ServiceState.Playing)
			return;

		player.start();
		serviceState = ServiceState.Playing;

		notification.notifyPaused(false);

		// Updates Lock-Screen Widget
		if (lockscreenController != null)
			lockscreenController.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

		broadcastState(ServicePlayMusic.BROADCAST_EXTRA_UNPAUSED);
	}

	/**
	 * Toggles between Pause and Unpause.
	 *
	 * @see pausePlayer()
	 * @see unpausePlayer()
	 */
	public void togglePlayback() {
		if (serviceState == ServiceState.Paused)
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

	/**
	 * Displays a notification on the status bar with the
	 * current song and some nice buttons.
	 */
	public void notifyCurrentSong() {
		if (! kMP.settings.get("show_notification", true))
			return;
		if (currentSong == null)
			return;

		if (notification == null)
			notification = new NotificationMusic();

		notification.notifySong(this, this, currentSong);
	}

	/**
	 * Disables the hability to notify things on the
	 * status bar.
	 *
	 * @see #notifyCurrentSong()
	 */
	public void cancelNotification() {
		if (notification == null)
			return;

		notification.cancel();
		notification = null;
	}

	/**
	 * Shouts the state of the Music Service.
	 *
	 * @note This broadcast is visible only inside this application.
	 *
	 * @note Will get received by listeners of `ServicePlayMusic.BROADCAST_ACTION`
	 *
	 * @param state Current state of the Music Service.
	 */
	private void broadcastState(String state) {
		if (currentSong == null)
			return;

		Intent broadcastIntent = new Intent(ServicePlayMusic.BROADCAST_ACTION);

		broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_STATE,   state);
		broadcastIntent.putExtra(ServicePlayMusic.BROADCAST_EXTRA_SONG_ID, currentSong.getId());

		LocalBroadcastManager
		.getInstance(getApplicationContext())
		.sendBroadcast(broadcastIntent);

		Log.w(TAG, "sentBroadcast");
	}
}
