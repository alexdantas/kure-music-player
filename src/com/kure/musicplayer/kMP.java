package com.kure.musicplayer;

import android.content.Context;


/**
 * Big class that contains the main logic behind kure Music Player.
 * 
 * This class contains the shared logic between all the Activities.
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
	 * Creates everything.
	 * 
	 * Must be called only once at the beginning
	 * of the program.
	 */
	public static void initialize(Context c) {

		songs.setContent("external");
		settings.load(c);
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
}
