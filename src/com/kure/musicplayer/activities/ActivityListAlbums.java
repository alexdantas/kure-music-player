package com.kure.musicplayer.activities;

import java.util.ArrayList;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;


/**
 * Shows a list of albums from a specified artist.
 */
public class ActivityListAlbums extends ActivityMaster
	implements OnItemClickListener {
	
	/**
	 * List of songs that will be shown to the user.
	 */
	private ListView songListView;
	
	/**
	 * Raw items that will be shown on ListView.
	 */
	private ArrayList<String> items;
	
	@Override
	protected void onCreate(Bundle tableSex) {
		super.onCreate(tableSex);
		
		setContentView(R.layout.activity_list_songs);

		songListView = (ListView)findViewById(R.id.activity_list_songs_list);

		// We expect an extra with the artist name
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		
		if (bundle == null)
			throw new RuntimeException("Expected Artist Name");
		
		// This is the artist that we'll display the albums.
		String artist = (String)bundle.get("artist");
		
		if (artist == null || artist.isEmpty())
			throw new RuntimeException("Expected Artist Name");
		
		this.setTitle(artist);
		
		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		if ((kMP.musicList != null) && (! kMP.musicList.isEmpty())) {
			
			items = kMP.songs.getAlbumsByArtist(artist);
			
			// Adapter that will convert from Strings to List Items
			final ArrayAdapter<String> adapter = new ArrayAdapter<String>
					(this, android.R.layout.simple_list_item_1, items);	
			
			// Filling teh list with all the items
			songListView.setAdapter(adapter);
			
			songListView.setOnItemClickListener(this);
		}
		
		// This enables the "Up" button on the top Action Bar
		// Note that it returns to the parent Activity, specified
		// on `AndroidManifest`
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);	
	}
	
	/**
	 * Will react to the user selecting an item.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// We can only handle the user choice from now on
		// if we've successfully scanned the songs from the
		// device.
		if (! kMP.songs.isInitialized())
			return;
		
		String selectedAlbum = items.get(position);
		
		kMP.musicList = kMP.songs.getSongsByAlbum(selectedAlbum);
		
		Intent intent = new Intent(this, ActivityListSongs.class);
		
		intent.putExtra("title", selectedAlbum);
		
		startActivity(intent);
	}
}
