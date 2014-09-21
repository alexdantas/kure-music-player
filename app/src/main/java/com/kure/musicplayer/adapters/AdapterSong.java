package com.kure.musicplayer.adapters;

import java.util.ArrayList;

import com.kure.musicplayer.R;
import com.kure.musicplayer.model.Song;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Maps `Songs` inside `ArrayLists` into `TextView` fields.
 * 
 * We'll map the ArrayList from our MainActivity into
 * multiple Artist/Title fields inside our activity_main Layout.
 *
 */
public class AdapterSong extends BaseAdapter {

	private ArrayList<Song> songs;
	private LayoutInflater songInflater;
	
	public AdapterSong(Context c, ArrayList<Song> theSongs) {
		songs = theSongs;
		songInflater = LayoutInflater.from(c);
	}
	
	@Override
	public int getCount() {
		return songs.size();
	}

	@Override
	public Object getItem(int position) {
		return songs.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		// Will map from a Song to a Song layout
		LinearLayout songLayout = (LinearLayout)songInflater.inflate(R.layout.menu_item_song,
				                                                     parent,
				                                                     false);
		
		TextView titleView  = (TextView)songLayout.findViewById(R.id.menu_item_song_title);
		TextView artistView = (TextView)songLayout.findViewById(R.id.menu_item_song_artist);
		TextView albumView  = (TextView)songLayout.findViewById(R.id.menu_item_song_album);
		
		Song currentSong = songs.get(position);
		
		String title = currentSong.getTitle();
		if (title.isEmpty())
			titleView.setText("<unknown>");
		else
			titleView.setText(currentSong.getTitle());
		
		String artist = currentSong.getArtist();
		if (artist.isEmpty())
			artistView.setText("<unknown>");
		else
			artistView.setText(currentSong.getArtist());
		
		String album = currentSong.getAlbum();
		if (album.isEmpty())
			albumView.setText("<unknown>");
		else
			albumView.setText(currentSong.getAlbum());
		
		// Saving position as a tag.
		// Each Song layout has a onClick attribute,
		// which calls a function that plays a song
		// with that tag.
		songLayout.setTag(position);
		return songLayout;
	}
}
