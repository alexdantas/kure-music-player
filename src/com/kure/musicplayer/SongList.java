package com.kure.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Global interface to all the songs this application can see.
 *
 * Tasks:
 * - Scans for songs on the device
 *   (both internal and external memories)
 * - Has query functions to songs and their attributes.
 *
 * Thanks:
 * - Showing me how to get a music's full PATH:
 *   http://stackoverflow.com/a/21333187
 */
public class SongList {

	/**
	 * Big list with all the Songs found.
	 */
	public ArrayList<Song> songs = new ArrayList<Song>();

	/**
	 * Maps song's genre IDs to song's genre names.
	 * @note It's only available after calling `scanSongs`.
	 */
	private HashMap<String, String> genreIdToGenreNameMap;

	/**
	 * Maps song's genre IDs to song's IDs.
	 * @note It's only available after calling `scanSongs`.
	 */
	private HashMap<String, String> genreIdToSongIdMap;

	/**
	 * Flag that tells if successfully scanned all songs.
	 */
	private boolean scannedSongs;

	/**
	 * Flag that tells if we're scanning songs right now.
	 */
	private boolean scanningSongs;

	/**
	 * Tells if we've successfully scanned all songs on
	 * the device.
	 *
	 * This will return `false` both while we're scanning
	 * for songs and if some error happened while scanning.
	 */
	public boolean isInitialized() {
		return scannedSongs;
	}

	/**
	 * Tells if we're currently scanning songs on the device.
	 */
	public boolean isScanning() {
		return scanningSongs;
	}

	/**
	 * Scans the device for songs.
	 *
	 * This function takes a lot of time to execute and
	 * blocks the program UI.
	 * So you should call it on a separate thread and
	 * query `isInitialized` when needed.
	 *
	 * @note If you call this function twice, it rescans
	 *       the songs, refreshing internal lists.
	 *       It doesn't add up songs.
	 *
	 * @param c         The current Activity's Context.
	 * @param fromWhere Where should we scan for songs.
	 *
	 * Accepted values to `fromWhere` are:
	 * - "internal" To scan for songs on the phone's memory.
	 * - "external" To scan for songs on the SD card.
	 * - "both"     To scan for songs anywhere.
	 */
	public void scanSongs(Context c, String fromWhere) {

		// This is a rather complex function that interacts with
		// the underlying Android database.
		// Grab some coffee and stick to the comments.

		// Not implemented yet.
		if (fromWhere == "both")
			throw new RuntimeException("Can't scan from both locations - not implemented");

		// Checking for flags so we don't get called twice
		// Fucking Java that doesn't allow local static variables.
		if (scanningSongs)
			return;
		scanningSongs = true;

		// The URIs that tells where we should scan for files.
		// There are separate URIs for music and genres. Go figure...
		//
		// Remember - internal is the phone memory, external is for the SD card.
		Uri musicUri = ((fromWhere == "internal") ?
				        android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI:
				        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		Uri genreUri = ((fromWhere == "internal") ?
				        android.provider.MediaStore.Audio.Genres.INTERNAL_CONTENT_URI:
				        android.provider.MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI);

		// Gives us access to query for files on the system.
		ContentResolver resolver = c.getContentResolver();

		// We use this thing to iterate through the results
		// of a SQLite database query.
		Cursor cursor;

		// OK, this is where we start.
		//
		// First, before even touching the songs, we'll save all the
		// music genres (like "Rock", "Jazz" and such).
		// That's because Android doesn't allow getting a song genre
		// from the song file itself.
		//
		// To get the genres, we make queries to the system's SQLite
		// database. It involves genre IDs, music IDs and such.
		//
		// We're creating two maps:
		//
		// 1. Genre ID -> Genre Names
		// 2. Song ID -> Genre ID
		//
		// This way, we have a connection from a Song ID to a Genre Name.
		//
		// Then we finally get the songs!
		// We make queries to the database, getting all possible song
		// metadata - like artist, album and such.


		// Some local variables to ease typing.
		// They represent columns on the underlying system databases.
		String GENRE_ID      = MediaStore.Audio.Genres._ID;
		String GENRE_NAME    = MediaStore.Audio.Genres.NAME;
        String SONG_ID       = android.provider.MediaStore.Audio.Media._ID;
		String SONG_TITLE    = android.provider.MediaStore.Audio.Media.TITLE;
		String SONG_ARTIST   = android.provider.MediaStore.Audio.Media.ARTIST;
		String SONG_ALBUM    = android.provider.MediaStore.Audio.Media.ALBUM;
		String SONG_YEAR     = android.provider.MediaStore.Audio.Media.YEAR;
		String SONG_FILEPATH = android.provider.MediaStore.Audio.Media.DATA;


		// Creating the map  "Genre IDs" -> "Genre Names"
		genreIdToGenreNameMap = new HashMap<String, String>();

		// This is what we'll ask of the genres
		String[] genreColumns = {
				GENRE_ID,
				GENRE_NAME
		};

		// Actually querying the genres database
        cursor = resolver.query(genreUri, genreColumns, null, null, null);

		// Iterating through the results and filling the map.
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
            genreIdToGenreNameMap.put(cursor.getString(0), cursor.getString(1));

        // Map from Songs IDs to Genre IDs
        genreIdToSongIdMap = new HashMap<String, String>();

        // UPDATE URI HERE
    	if (fromWhere == "both")
    		throw new RuntimeException("Can't scan from both locations - not implemented");

    	// For each genre, we'll query the databases to get
    	// all songs's IDs that have it as a genre.
    	for (String genreID : genreIdToGenreNameMap.keySet()) {

        	Uri uri = MediaStore.Audio.Genres.Members.getContentUri(fromWhere,
        			                                                Long.parseLong(genreID));

        	cursor = resolver.query(uri, new String[] { SONG_ID }, null, null, null);

        	// Iterating through the results, populating the map
        	for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

        		long currentSongID = cursor.getLong(cursor.getColumnIndex(SONG_ID));

        		genreIdToSongIdMap.put(Long.toString(currentSongID), genreID);
        	}
        }

    	// Finished getting the Genres.
    	// Let's go get dem songzz.

		// Columns I'll retrieve from the song table
		String[] columns = {
				SONG_ID,
				SONG_TITLE,
				SONG_ARTIST,
				SONG_ALBUM,
				SONG_YEAR,
				SONG_FILEPATH
		};

		// Thing that limits results to only show music files.
		//
		// It's a SQL "WHERE" clause - it becomes `WHERE IS_MUSIC=1`.
		//
		// (note: using `IS_MUSIC!=0` takes a fuckload of time)
		final String musicsOnly = MediaStore.Audio.Media.IS_MUSIC + "=1";

		// Actually querying the system
		cursor = resolver.query(musicUri, columns, musicsOnly, null, null);

		if (cursor != null && cursor.moveToFirst())
		{
			// NOTE: I tried to use MediaMetadataRetriever, but it was too slow.
			//       Even with 10 songs, it took like 13 seconds,
			//       No way I'm releasing it this way - I have like 4.260 songs!

			do {
				// Creating a song from the values on the row
				Song song = new Song(cursor.getInt(cursor.getColumnIndex(SONG_ID)),
						             cursor.getString(cursor.getColumnIndex(SONG_FILEPATH)));

				song.setTitle(cursor.getString(cursor.getColumnIndex(SONG_TITLE)));
				song.setArtist(cursor.getString(cursor.getColumnIndex(SONG_ARTIST)));
				song.setAlbum(cursor.getString(cursor.getColumnIndex(SONG_ALBUM)));
				song.setYear(cursor.getInt(cursor.getColumnIndex(SONG_YEAR)));

				// Using the previously created genre maps
				// to fill the current song genre.
				String currentGenreID   = genreIdToSongIdMap.get(Long.toString(song.getId()));
				String currentGenreName = genreIdToGenreNameMap.get(currentGenreID);
				song.setGenre(currentGenreName);

				// Adding the song to the global list
				songs.add(song);
			}
			while (cursor.moveToNext());
		}
		else
		{
			// What do I do if I can't find any songs?
		}
		cursor.close();

		// Finally, let's sort the song list alphabetically
		// based on the song title.
		Collections.sort(songs, new Comparator<Song>() {
			public int compare(Song a, Song b)
			{
				return a.getTitle().compareTo(b.getTitle());
			}
		});

		scannedSongs  = true;
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

	/**
	 * Returns a list of Songs belonging to a specified artist.
	 */
	public ArrayList<Song> getSongsByArtist(String desiredArtist) {
		ArrayList<Song> songsByArtist = new ArrayList<Song>();

		for (Song song : songs) {
			String currentArtist = song.getArtist();

			if (currentArtist.equals(desiredArtist))
				songsByArtist.add(song);
		}

		// Sorting resulting list by Album
		Collections.sort(songsByArtist, new Comparator<Song>() {
			public int compare(Song a, Song b)
			{
				return a.getAlbum().compareTo(b.getAlbum());
			}
		});

		return songsByArtist;
	}

	/**
	 * Returns a list of album names belonging to a specified artist.
	 */
	public ArrayList<String> getAlbumsByArtist(String desiredArtist) {
		ArrayList<String> albumsByArtist = new ArrayList<String>();

		for (Song song : songs) {
			String currentArtist = song.getArtist();
			String currentAlbum  = song.getAlbum();

			if (currentArtist.equals(desiredArtist))
				if (! albumsByArtist.contains(currentAlbum))
					albumsByArtist.add(currentAlbum);
		}

		// Sorting alphabetically
		Collections.sort(albumsByArtist);

		return albumsByArtist;
	}

	/**
	 * Returns a list of Songs belonging to a specified album.
	 */
	public ArrayList<Song> getSongsByAlbum(String desiredAlbum) {
		ArrayList<Song> songsByAlbum = new ArrayList<Song>();

		for (Song song : songs) {
			String currentAlbum = song.getAlbum();

			if (currentAlbum.equals(desiredAlbum))
				songsByAlbum.add(song);
		}

		return songsByAlbum;
	}
}
