package com.kure.musicplayer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
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
 *
 * - Showing me how to get a music's full PATH:
 *   http://stackoverflow.com/a/21333187
 *
 * - Teaching me the queries to get Playlists
 *   and their songs:
 *   http://stackoverflow.com/q/11292125
 */
public class SongList {

	/**
	 * Big list with all the Songs found.
	 */
	public ArrayList<Song> songs = new ArrayList<Song>();

	/**
	 * Big list with all the Playlists found.
	 */
	public ArrayList<Playlist> playlists = new ArrayList<Playlist>();

	/**
	 * Maps song's genre IDs to song's genre names.
	 * @note It's only available after calling `scanSongs`.
	 */
	private HashMap<String, String> genreIdToGenreNameMap;

	/**
	 * Maps song's IDs to song genre IDs.
	 * @note It's only available after calling `scanSongs`.
	 */
	private HashMap<String, String> songIdToGenreIdMap;

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
	 * Inside it, we make a lot of queries to the system's
	 * databases - getting songs, genres and playlists.
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
		// There are separate URIs for music, genres and playlists. Go figure...
		//
		// Remember - internal is the phone memory, external is for the SD card.
		Uri musicUri = ((fromWhere == "internal") ?
				        android.provider.MediaStore.Audio.Media.INTERNAL_CONTENT_URI:
				        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
		Uri genreUri = ((fromWhere == "internal") ?
				        android.provider.MediaStore.Audio.Genres.INTERNAL_CONTENT_URI:
				        android.provider.MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI);
		Uri playlistUri = ((fromWhere == "internal") ?
		        android.provider.MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI:
		        android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);

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


		// These are the columns from the system databases.
		// They're the information I want to get from songs.
		String GENRE_ID      = MediaStore.Audio.Genres._ID;
		String GENRE_NAME    = MediaStore.Audio.Genres.NAME;
        String SONG_ID       = android.provider.MediaStore.Audio.Media._ID;
		String SONG_TITLE    = android.provider.MediaStore.Audio.Media.TITLE;
		String SONG_ARTIST   = android.provider.MediaStore.Audio.Media.ARTIST;
		String SONG_ALBUM    = android.provider.MediaStore.Audio.Media.ALBUM;
		String SONG_YEAR     = android.provider.MediaStore.Audio.Media.YEAR;
		String SONG_TRACK_NO = android.provider.MediaStore.Audio.Media.TRACK;
		String SONG_FILEPATH = android.provider.MediaStore.Audio.Media.DATA;
		String SONG_DURATION = android.provider.MediaStore.Audio.Media.DURATION;

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

        cursor.close();

        // Map from Songs IDs to Genre IDs
        songIdToGenreIdMap = new HashMap<String, String>();

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

        		songIdToGenreIdMap.put(Long.toString(currentSongID), genreID);
        	}
        	cursor.close();
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
				SONG_TRACK_NO,
				SONG_FILEPATH,
				SONG_DURATION
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

				song.setTitle      (cursor.getString(cursor.getColumnIndex(SONG_TITLE)));
				song.setArtist     (cursor.getString(cursor.getColumnIndex(SONG_ARTIST)));
				song.setAlbum      (cursor.getString(cursor.getColumnIndex(SONG_ALBUM)));
				song.setYear       (cursor.getInt   (cursor.getColumnIndex(SONG_YEAR)));
				song.setTrackNumber(cursor.getInt   (cursor.getColumnIndex(SONG_TRACK_NO)));
				song.setDuration   (cursor.getInt   (cursor.getColumnIndex(SONG_DURATION)));

				// Using the previously created genre maps
				// to fill the current song genre.
				String currentGenreID   = songIdToGenreIdMap.get(Long.toString(song.getId()));
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

		// Alright, now I'll get all the Playlists.
		// First I grab all playlist IDs and Names and then for each
		// one of those, getting all songs inside them.

		// As you know, the columns for the database.
		String PLAYLIST_ID      = MediaStore.Audio.Playlists._ID;
		String PLAYLIST_NAME    = MediaStore.Audio.Playlists.NAME;
		String PLAYLIST_SONG_ID = MediaStore.Audio.Playlists.Members.AUDIO_ID;

		// This is what I'll get for all playlists.
		String[] playlistColumns = {
				PLAYLIST_ID,
				PLAYLIST_NAME
		};

		// The actual query - takes a while.
		cursor = resolver.query(playlistUri, playlistColumns, null, null, null);

		// Going through all playlists, creating my class and populating
		// it with all the song IDs they have.
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {

			Playlist playlist = new Playlist(cursor.getLong(cursor.getColumnIndex(PLAYLIST_ID)),
			                                 cursor.getString(cursor.getColumnIndex(PLAYLIST_NAME)));

			// For each playlist, get all song IDs
			Uri currentUri = MediaStore.Audio.Playlists.Members.getContentUri(fromWhere, playlist.getID());

			Cursor cursor2 = resolver.query(currentUri,
			                                new String[] { PLAYLIST_SONG_ID },
			                                musicsOnly,
			                                null, null);

			// Adding each song's ID to it
			for (cursor2.moveToFirst(); !cursor2.isAfterLast(); cursor2.moveToNext())
				playlist.add(cursor2.getLong(cursor2.getColumnIndex(PLAYLIST_SONG_ID)));

			playlists.add(playlist);
			cursor2.close();
		}

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
	 * Returns an alphabetically sorted list with all
	 * existing genres on the scanned songs.
	 */
	public ArrayList<String> getGenres() {

		ArrayList<String> genres = new ArrayList<String>();

		for (String genre : genreIdToGenreNameMap.values())
			genres.add(genre);

		Collections.sort(genres);

		return genres;
	}

	/**
	 * Returns a list with all years your songs have.
	 *
	 * @note It is a list of Strings. To access the
	 *       years, do a `Integer.parseInt(string)`.
	 */
	public ArrayList<String> getYears() {

		ArrayList<String> years = new ArrayList<String>();

		for (Song song : songs) {
			String year = Integer.toString(song.getYear());

			if ((Integer.parseInt(year) > 0) && (! years.contains(year)))
				years.add(year);
		}

		// Making them alphabetically sorted
		Collections.sort(years);

		return years;
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
	 * Returns a new list with all songs.
	 *
	 * @note This is different than accessing `songs` directly
	 *       because it duplicates it - you can then mess with
	 *       it without worrying about changing the original.
	 */
	public ArrayList<Song> getSongs() {
		ArrayList<Song> list = new ArrayList<Song>();

		for (Song song : songs)
			list.add(song);

		return list;
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

	/**
	 * Returns a list with all songs that have the same `genre.`
	 */
	public ArrayList<Song> getSongsByGenre(String genreName) {

		ArrayList<Song> currentSongs = new ArrayList<Song>();

		for (Song song : songs) {

			String currentSongGenre = song.getGenre();

			if (currentSongGenre == genreName)
				currentSongs.add(song);
		}

		return currentSongs;
	}

	/**
	 * Returns a list with all songs composed at `year`.
	 */
	public ArrayList<Song> getSongsByYear(int year) {

		ArrayList<Song> currentSongs = new ArrayList<Song>();

		for (Song song : songs) {

			int currentYear = song.getYear();

			if (currentYear == year)
				currentSongs.add(song);
		}

		return currentSongs;
	}

	public ArrayList<String> getPlaylistNames() {

		ArrayList<String> names = new ArrayList<String>();

		for (Playlist playlist : playlists)
			names.add(playlist.getName());

		return names;
	}

	public Song getSongById(long id) {

		Song currentSong = null;

		for (Song song : songs)
			if (song.getId() == id) {
				currentSong = song;
				break;
			}

		return currentSong;
	}

	public ArrayList<Song> getSongsByPlaylist(String playlistName) {

		ArrayList<Long> songIDs = null;

		for (Playlist playlist : playlists)
			if (playlist.getName().equals(playlistName)) {
				songIDs = playlist.getSongIds();
				break;
			}

		ArrayList<Song> currentSongs = new ArrayList<Song>();

		for (Long songID : songIDs)
			currentSongs.add(getSongById(songID));

		return currentSongs;
	}

	/**
	 * Creates a new Playlist.
	 *
	 * @param c          Activity on which we're creating.
	 * @param fromWhere  "internal" or "external".
	 * @param name       Playlist name.
	 * @param songsToAdd List of song IDs to place on it.
	 */
	public void newPlaylist(Context c, String fromWhere, String name, ArrayList<Song> songsToAdd) {

		ContentResolver resolver = c.getContentResolver();

		Uri playlistUri = ((fromWhere == "internal") ?
		        android.provider.MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI:
		        android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);

		// CHECK IF PLAYLIST EXISTS!

		// Setting the new playlists' values
		ContentValues values = new ContentValues();
		values.put(MediaStore.Audio.Playlists.NAME, name);
		values.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());

		// Actually inserting the new playlist.
		Uri newPlaylistUri = resolver.insert(playlistUri, values);

		// Getting the new Playlist ID
		String PLAYLIST_ID      = MediaStore.Audio.Playlists._ID;
		String PLAYLIST_NAME    = MediaStore.Audio.Playlists.NAME;

		// This is what I'll get for all playlists.
		String[] playlistColumns = {
				PLAYLIST_ID,
				PLAYLIST_NAME
		};

		// The actual query - takes a while.
		Cursor cursor = resolver.query(playlistUri, playlistColumns, null, null, null);

		long playlistID = 0;

		// Going through all playlists, creating my class and populating
		// it with all the song IDs they have.
		for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
			if (name.equals(cursor.getString(cursor.getColumnIndex(PLAYLIST_NAME))))
				playlistID = cursor.getLong(cursor.getColumnIndex(PLAYLIST_ID));

		// Now, to it's songs
		Uri songUri = Uri.withAppendedPath(newPlaylistUri, MediaStore.Audio.Playlists.Members.CONTENT_DIRECTORY);
		int songOrder = 1;

		for (Song song : songsToAdd) {

			ContentValues songValues = new ContentValues();

			songValues.put(MediaStore.Audio.Playlists.Members.AUDIO_ID,   song.getId());
			songValues.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, songOrder);

			resolver.insert(songUri, songValues);
			songOrder++;
		}

		// Finally, we're updating our internal list of Playlists
		Playlist newPlaylist = new Playlist(playlistID, name);

		for (Song song : songsToAdd)
			newPlaylist.add(song.getId());

		playlists.add(newPlaylist);
	}
}
