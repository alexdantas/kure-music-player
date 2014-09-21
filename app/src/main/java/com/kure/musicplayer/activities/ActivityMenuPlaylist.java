package com.kure.musicplayer.activities;

import java.util.ArrayList;
import java.util.Collections;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

/**
 * Shows a menu with all the Playlists on the device.
 *
 */
public class ActivityMenuPlaylist extends ActivityMaster
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

		// This enables the "Up" button on the top Action Bar
		// Note that it returns to the parent Activity, specified
		// on `AndroidManifest`
		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		// List to be populated with items
		listView = (ListView)findViewById(R.id.activity_menu_artists_list);

		// All playlist names and sorting alphabetically
		items = kMP.songs.getPlaylistNames();
		Collections.sort(items);

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

		String selectedPlaylist = items.get(position);

		// Based on the selected playlist name, getting
		// all the songs and listing them.
		kMP.musicList = kMP.songs.getSongsByPlaylist(selectedPlaylist);

		Intent intent = new Intent(this, ActivityListSongs.class);

		intent.putExtra("title", selectedPlaylist);

		startActivity(intent);
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
