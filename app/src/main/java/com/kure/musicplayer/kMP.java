package com.kure.musicplayer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

import com.kure.musicplayer.activities.ActivityQuit;
import com.kure.musicplayer.model.Song;
import com.kure.musicplayer.model.SongList;
import com.kure.musicplayer.services.ServicePlayMusic;
import com.kure.musicplayer.services.ServicePlayMusic.MusicBinder;


/**
 * Big class that contains the main logic behind kure Music Player.
 *
 * This class contains the shared logic between all the Activities.
 *
 * Thanks:
 * - For telling how to get the application version:
 *   http://stackoverflow.com/a/6593822
 */
public class kMP {

	/**
	 * All the songs on the device.
	 */
	public static SongList songs = new SongList();

	/**
	 * All the app's configurations/preferences/settings.
	 */
	public static Settings settings = new Settings();

	/**
	 * Our custom service that allows the music to play
	 * even when the app is not on focus.
	 */
	public static ServicePlayMusic musicService = null;

	/**
	 * Contains the songs that are going to be shown to
	 * the user on a particular menu.
	 *
	 * @note IGNORE THIS - don't mess with it.
	 *
	 * Every `ActivityMenu*` uses this temporary variable to
	 * store subsections of `SongList` and set `ActivityListSongs`
	 * to display it.
	 */
	public static ArrayList<Song> musicList = null;

	/**
	 * List of the songs being currently played by the user.
	 *
	 * (independent of the UI)
	 *
	 * TODO remove this shit
	 */
	public static ArrayList<Song> nowPlayingList = null;

	/**
	 * Flag that tells if the Main Menu has an item that
	 * sends the user to the Now Playing Activity.
	 *
	 * It's here because when firstly initializing the
	 * application, there's no Now Playing Activity.
	 */
	public static boolean mainMenuHasNowPlayingItem = false;

	// GENERAL PROGRAM INFO
	public static String applicationName = "kure Music Player";
	public static String packageName = "<unknown>";
	public static String versionName = "<unknown>";
	public static int    versionCode = -1;
	public static long   firstInstalledTime = -1;
	public static long   lastUpdatedTime    = -1;

	/**
	 * Creates everything.
	 *
	 * Must be called only once at the beginning
	 * of the program.
	 */
	public static void initialize(Context c) {

		kMP.packageName = c.getPackageName();

		try {
			// Retrieving several information
			PackageInfo info = c.getPackageManager().getPackageInfo(kMP.packageName, 0);

			kMP.versionName        = info.versionName;
			kMP.versionCode        = info.versionCode;
			kMP.firstInstalledTime = info.firstInstallTime;
			kMP.lastUpdatedTime    = info.lastUpdateTime;

		} catch (PackageManager.NameNotFoundException e) {
			// Couldn't get package information
			//
			// Won't do anything, since variables are
			// already started with default values.
		}
	}

	/**
	 * Destroys everything.
	 *
	 * Must be called only once when the program
	 * being destroyed.
	 */
	public static void destroy() {
		songs.destroy();
	}

	/**
	 * The actual connection to the MusicService.
	 * We start it with an Intent.
	 *
	 * These callbacks will bind the MusicService to our internal
	 * variables.
	 * We can only know it happened through our flag, `musicBound`.
	 */
	public static ServiceConnection musicConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MusicBinder binder = (MusicBinder)service;

			// Here's where we finally create the MusicService
			musicService = binder.getService();
			musicService.setList(kMP.songs.songs);
			musicService.musicBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			musicService.musicBound = false;
		}
	};

	/**
	 * Our will to start a new music Service.
	 * Android requires that we start a service through an Intent.
	 */
	private static Intent musicServiceIntent = null;

	/**
	 * Initializes the Music Service at Activity/Context c.
	 *
	 * @note Only starts the service once - does nothing when
	 *       called multiple times.
	 */
	public static void startMusicService(Context c) {

		if (musicServiceIntent != null)
			return;

		if (kMP.musicService != null)
			return;

		// Create an intent to bind our Music Connection to
		// the MusicService.
		musicServiceIntent = new Intent(c, ServicePlayMusic.class);
		c.bindService(musicServiceIntent, musicConnection, Context.BIND_AUTO_CREATE);
		c.startService(musicServiceIntent);
	}

	/**
	 * Makes the music Service stop and clean itself at
	 * Activity/Context c.
	 */
	public static void stopMusicService(Context c) {

		if (musicServiceIntent == null)
			return;

		c.stopService(musicServiceIntent);
		musicServiceIntent = null;

		kMP.musicService = null;
	}

	/**
	 * Forces the whole application to quit.
	 *
	 * Please read more info on this StackOverflow answer:
	 * http://stackoverflow.com/a/4737595
	 *
	 * @note This is dangerous, make sure to cleanup
	 *       everything before calling this.
	 */
	public static void forceExit(Activity c) {

		// Instead of just calling `System.exit(0)` we use
		// a temporary Activity do to the dirty job for us
		// (clearing all other Activities and finishing() itself).
		Intent intent = new Intent(c, ActivityQuit.class);

		// Clear all other Activities
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		c.startActivity(intent);

		// Clear the Activity calling this function
		c.finish();
	}
}
