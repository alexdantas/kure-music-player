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
	 * Big list of all songs on the device.
	 */
	public static ArrayList<Song> songList;
	
	/**
	 * Creates everything.
	 * 
	 * Must be called only once at the beginning
	 * of the program.
	 */
	public static void initialize(Context c) {
		

		songList = new ArrayList<Song>();
		
		fillSongList(c);
		sortSongListBy("Title");
	}
	
	/**
	 * Destroys everything.
	 * 
	 * Must be called only once when the program
	 * being destroyed.
	 */
	public static void destroy() {
		
	}
	
	/**
	 * Fills the ListView with all the songs found on the device.
	 */
	public static void fillSongList(Context c) {
		// This will ask for details on music files
		ContentResolver musicResolver = c.getContentResolver();
		
		// This will contain the URI to music files.
		// We're trying to get music from the SD card - EXTERNAL_CONTENT
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		
		// Pointer to database results when querying a resolver
		Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
		
		if (musicCursor != null && musicCursor.moveToFirst())
		{
			// Getting pre-defined columns from the system
			// They're the data from all music found on the URI.
			int titleColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.TITLE);
			
			int idColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media._ID);
			
			int artistColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.ARTIST);
			
			// Adding songs to the list
			do {
				long   thisId     = musicCursor.getLong(idColumn);
				String thisTitle  = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				
				songList.add(new Song(thisId, thisTitle, thisArtist));
			}
			while (musicCursor.moveToNext());
		}
		else
		{
			// What do I do if I can't find any songs?
			songList.add(new Song(0, "No Songs Found", "On this Device"));
		}
	}
	
	/**
	 * Sorts all the songs.
	 * They can be sorted alphabetically by "Artist" or "Title".
	 */
	public static void sortSongListBy(String way) {
		
		if (way == "Artist")
		{
			Collections.sort(songList, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getArtist().compareTo(b.getArtist());
				}
			});
		}
		else {
			Collections.sort(songList, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getTitle().compareTo(b.getTitle());
				}
			});
		}
	}
}
