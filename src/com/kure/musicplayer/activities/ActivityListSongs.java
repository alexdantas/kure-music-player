package com.kure.musicplayer.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.adapters.AdapterSong;


/**
 * Shows a predefined list of songs, letting the user select
 * them to play.
 * 
 * @note This class is a mess because, to decide which songs to
 *       display, it uses the member `kMP.musicList`.
 */
public class ActivityListSongs extends ActivityMaster {
	
	/**
	 * List of songs that will be shown to the user.
	 */
	private ListView songListView;
	
	@Override
	protected void onCreate(Bundle popcorn) {
		super.onCreate(popcorn);
		
		setContentView(R.layout.activity_list_songs);

		// Let's fill ourselves with all the songs
		// available on the device.
		songListView = (ListView)findViewById(R.id.activity_list_songs_list);
				
		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		if ((kMP.musicList != null) && (! kMP.musicList.isEmpty())) {
			AdapterSong songAdapter = new AdapterSong(this, kMP.musicList);
			songListView.setAdapter(songAdapter);	
		}
		
		// This enables the "Up" button on the top Action Bar
		// Note that it returns to the parent Activity, specified
		// on `AndroidManifest`
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);	
	}
	
	/**
	 * Gets called whenever the user touches the song on the View.
	 * It is defined on the Song Layout.
	 */
	public void songPickedByUser(View view) {
		// We're getting the song's index from the tag
		// set to the View on SongAdapter.
		kMP.musicService.setSong(Integer.parseInt(view.getTag().toString()));
		kMP.musicService.playSong();
	}
}
