package com.kure.musicplayer;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * We'll map the ArrayList from our MainActivity into
 * multiple Artist/Title fields inside our activity_main Layout.
 *
 */
public class SongAdapter extends BaseAdapter {

	private ArrayList<Song> songs;
	private LayoutInflater songInflater;
	
	public SongAdapter(Context c, ArrayList<Song> theSongs) {
		songs = theSongs;
		songInflater = LayoutInflater.from(c);
	}
	
	@Override
	public int getCount() {
		return songs.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		// Will map from a Song to a Song layout
		LinearLayout songLayout = (LinearLayout)songInflater.inflate
				(R.layout.song, parent, false);
		
		TextView songView = (TextView)songLayout.findViewById(R.id.song_title);
		TextView artistView = (TextView)songLayout.findViewById(R.id.song_artist);
		
		Song currentSong = songs.get(position);
		
		songView.setText(currentSong.getTitle());
		artistView.setText(currentSong.getArtist());
		
		// Saving position as a tag.
		// Each Song layout has a onClick attribute,
		// which calls a function that plays a song
		// with that tag.
		songLayout.setTag(position);
		return songLayout;
	}

}
