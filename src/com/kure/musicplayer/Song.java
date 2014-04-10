package com.kure.musicplayer;


/**
 * Represents a single audio file on the system.
 */
public class Song {

	/**
	 * Identifier for the song on the Android system.
	 * (so we can locate the file anywhere)
	 */
	private long   id;

	private String title;
	private String artist;
	private String album;
	private int    year;

	public Song(long songID, String title, String artist, String album, int year) {
		this.id     = songID;
		this.title  = title;
		this.artist = artist;
		this.album  = album;
		this.year   = year;
	}

	public long getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getArtist() {
		return artist;
	}
	public String getAlbum() {
		return album;
	}
	public int getYear() {
		return year;
	}

	private String path;

	public void setPath(String path) {
		this.path = path;
	}
	public String getPath() {
		return path;
	}
}
