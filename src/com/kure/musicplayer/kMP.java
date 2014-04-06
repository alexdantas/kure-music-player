package com.kure.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

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
	 * Creates everything.
	 * 
	 * Must be called only once at the beginning
	 * of the program.
	 */
	public static void initialize(Context c) {
		
		songs.scanSongs(c);
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
