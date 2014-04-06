package com.kure.musicplayer.activities;

import java.util.ArrayList;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Shows a menu with all the artists of the songs
 * on SongList, allowing the user to choose one of
 * them and going to a specific artist menu.
 *
 */
public class ActivityMenuArtist extends Activity
	implements OnItemClickListener {

	/**
	 * All the possible items the user can select on this menu.
	 * 
	 * Will be initialized with default values on `onCreate`.
	 */
	public static ArrayList<String> items;

	/**
	 * List that will be populated with all the items.
	 * 
	 * Look for it inside the res/layout xml files.
	 */
	ListView listView;
	
	/**
	 * Called when the activity is created for the first time.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu_artists);
		
		// List to be populated with items
		listView = (ListView)findViewById(R.id.activity_menu_artists_list);
		
		items = kMP.songs.getArtists();
		
		// Adapter that will convert from Strings to List Items
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>
				(this, android.R.layout.simple_list_item_1, items);
		
		// Filling teh list with all the items
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(this);
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
		
		String selectedArtist = items.get(position);
		
		Toast.makeText(this,
				selectedArtist,
				Toast.LENGTH_LONG).show();		
	}
	
	/**
	 * When destroying the Activity.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// Need to clear all the items otherwise
		// they'll keep adding up.
		items.clear();
	}	
}
