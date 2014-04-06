package com.kure.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Global interface to all the songs this app can see.
 *
 * TODO
 * We can configure it to look out on the internal storage
 * (phone) or the external storage (sd card).
 * 
 */
public class SongList {
	
	/**
	 * Big list with all the songs on the device.
	 */
	public ArrayList<Song> songs;

	/**
	 * Internal flag to tell if we've successfuly scanned
	 * all songs on the device.
	 * 
	 * We cannot operate otherwise.
	 */
	private boolean scannedSongs;
	
	/**
	 * Where we'll scan for songs.
	 * 
	 * It can be "internal" (for songs inside the phone) or
	 * "external" (for songs on the SD card).
	 */
	private String songSource;
	
	/**
	 * Default constructor, setting everything to default.
	 */
	public SongList() {
		songs = new ArrayList<Song>();
		
		scannedSongs = false;
		
		songSource = "internal";
	}
	
	/**
	 * Tells if we've scanned all songs on the device.
	 * 
	 * Note that we cannot operate otherwise.
	 */
	public boolean isInitialized() {
		return scannedSongs;
	}
	
	public void setContent(String where) {
		if (where == "internal" || where == "external")
			songSource = where;
	}
	
	/**
	 * Scans the device for songs, filling our internal lists.
	 * 
	 * @param c The current Activity's Context.
	 */
	public void scanSongs(Context c) {
		
		// This will ask for details on music files
		ContentResolver musicResolver = c.getContentResolver();
		
		// This will contain the URI to music files.
		// We're trying to get music from the SD card - EXTERNAL_CONTENT
		Uri musicUri = ((songSource == "internal") ?
				        android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI:
				        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		
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
				
				songs.add(new Song(thisId, thisTitle, thisArtist));
			}
			while (musicCursor.moveToNext());
		}
		else
		{
			// What do I do if I can't find any songs?
			songs.add(new Song(0, "No Songs Found", "On this Device"));
		}
		
		// Finally, let's sort the song list alphabetically
		Collections.sort(songs, new Comparator<Song>() {
			public int compare(Song a, Song b)
			{
				return a.getTitle().compareTo(b.getTitle());
			}
		});
		
		scannedSongs = true;
	}

	public void destroy() {
		songs.clear();
	}
}
