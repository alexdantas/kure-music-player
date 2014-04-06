package com.kure.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

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
	 * Safety measure to avoid scanning songs twice on
	 * different threads.
	 */
	private boolean scanningSongs;
	
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
		
		scannedSongs  = false;
		scanningSongs = false;
		
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
		
		// "Thread Safety"
		if (scanningSongs)
			return;
		scanningSongs = true;
		
		// This will ask for details on music files
		ContentResolver musicResolver = c.getContentResolver();
		
		// This will contain the URI to music files.
		// We're trying to get music from the SD card - EXTERNAL_CONTENT
		Uri musicUri = ((songSource == "internal") ?
				        android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI:
				        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		
		// Columns I'll retrieve from the song table
		String[] columns = {
				android.provider.MediaStore.Audio.Media._ID,
				android.provider.MediaStore.Audio.Media.TITLE,
				android.provider.MediaStore.Audio.Media.ARTIST,
				android.provider.MediaStore.Audio.Media.ALBUM,
				android.provider.MediaStore.Audio.Media.YEAR
		};
		
		// Limiter that will only get rows with music files
		// It's a SQL "WHERE" clause.
		final String musicsOnly = MediaStore.Audio.Media.IS_MUSIC + "=1";
		
		// Thing that'll go through the table getting the rows
		Cursor musicCursor = musicResolver.query(musicUri, columns, musicsOnly, null, null);
		
		if (musicCursor != null && musicCursor.moveToFirst())
		{
			// We'll use those to retrieve specific columns
			int idColumn     = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
			int titleColumn  = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
			int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
			int albumColumn  = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM);
			int yearColumn   = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.YEAR);

			// Adding all songs to the list, one by row
			do {
				// Creating a song from the values on the row
				Song song = new Song(musicCursor.getInt(idColumn),
						             musicCursor.getString(titleColumn),
						             musicCursor.getString(artistColumn),
						             musicCursor.getString(albumColumn),
						             musicCursor.getInt(yearColumn));
				
				songs.add(song);
			}
			while (musicCursor.moveToNext());
		}
		else
		{
			// What do I do if I can't find any songs?
			songs.add(new Song(0,
					           "No Songs Found",
					           "On this Device",
					           "kMP",
					           2014));
		}
		musicCursor.close();
		
		// Finally, let's sort the song list alphabetically
		// based on the song title.
		Collections.sort(songs, new Comparator<Song>() {
			public int compare(Song a, Song b)
			{
				return a.getTitle().compareTo(b.getTitle());
			}
		});
		
		scannedSongs = true;
		scanningSongs = false;
	}
	
	public void destroy() {
		songs.clear();
	}

	/**
	 * Returns an alphabetically sorted list with all the
	 * artists of the scanned songs.
	 * 
	 * @note This method might take a while depending on how
	 *       many songs you have.
	 */
	public ArrayList<String> getArtists() {
		
		ArrayList<String> artists = new ArrayList<String>();
		
		for (Song song : songs) {
			String artist = song.getArtist();
			
			if ((artist != null) && (! artists.contains(artist)))
				artists.add(artist);
		}
		
		// Making them alphabetically sorted
		Collections.sort(artists);
		
		return artists;
	}
	
	/**
	 * Returns an alphabetically sorted list with all the
	 * albums of the scanned songs.
	 * 
	 * @note This method might take a while depending on how
	 *       many songs you have.
	 */
	public ArrayList<String> getAlbums() {
		
		ArrayList<String> albums = new ArrayList<String>();
		
		for (Song song : songs) {
			String album = song.getAlbum();
			
			if ((album != null) && (! albums.contains(album)))
				albums.add(album);
		}
		
		// Making them alphabetically sorted
		Collections.sort(albums);
		
		return albums;
	}	
}
