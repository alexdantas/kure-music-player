package com.kure.musicplayer.activities;

import java.util.ArrayList;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

/**
 * First screen that the user sees - the Main Menu.
 * 
 * Must listen for clicks so we can change to the other
 * sub menus (Activities).
 * 
 * Thanks for providing a basic ListView navigation layout: 
 * http://stackoverflow.com/q/19476948
 */
public class ActivityMenuMain extends ActivityMaster
	implements OnItemClickListener {

	/**
	 * All the possible items the user can select on this menu.
	 * 
	 * Will be initialized with default values on `onCreate`.
	 */
	public static final ArrayList<String> items = new ArrayList<String>();

	/**
	 * List that will be populated with all the items.
	 * 
	 * Look for it inside the res/layout xml files.
	 */
	ListView listView;
	
	/**
	 * ID we'll use when calling the settings window.
	 * It'll say if the user changed theme or not.
	 * 
	 * @see onActivityResult()
	 */
	static final int USER_CHANGED_THEME = 1;
	
	/**
	 * Called when the activity is created for the first time.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// We need to load the settings right before creating
		// the first activity so that the user-selected theme
		// will be applied to the first screen.
		//
		// Loading default settings at the first time the app;
		// is loaded.
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		kMP.settings.load(this);
				
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
	
		// Adding all possible items on the main menu.
		items.add(getString(R.string.menu_main_music));
		items.add(getString(R.string.menu_main_settings));
		items.add(getString(R.string.menu_main_shuffle));
		
		// ListView to be populated with the menu items
		listView = (ListView)findViewById(R.id.activity_main_menu_list);
		
		// Adapter that will convert from Strings to List Items
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>
				(this, android.R.layout.simple_list_item_1, items);
		
		// Filling teh list with all the items
		listView.setAdapter(adapter);
		
		// We'll get warned when the user clicks on an item.
		listView.setOnItemClickListener(this);
		
		// Initializing the main program logic.
		kMP.initialize(this);
		
		// Loading all the songs from the device on a different thread.
		// We'll only actually do it if they weren't loaded already
		//
		// See the implementation right at the end of this class.
		if (! kMP.songs.isInitialized())
			new ScanSongs().execute();
	}

	/**
	 * Will react to the user selecting an item.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// Gets the string value of the current item and
		// compares to all possible items.
		String currentItem = listView.getItemAtPosition(position).toString();
		
		if (currentItem == getString(R.string.menu_main_music)) {
			startActivity(new Intent(this, ActivityMenuMusic.class));
		}
		else if (currentItem == getString(R.string.menu_main_settings)) {
			
			// Let's start the settings screen.
			// While doing so, we need to know if the user have
			// changed the theme.
			// If he did, we'll refresh the screen.
			// See `onActivityResult()`
			Intent settingsIntent = new Intent(this, ActivityMenuSettings.class);
			startActivityForResult(settingsIntent, USER_CHANGED_THEME);
			
		}
		else if (currentItem == getString(R.string.menu_main_shuffle)) {
			
			// Can only jump to shuffle all songs if we've
			// scanned all the songs from the device.
			if (kMP.songs.isInitialized()) {
				
			}
		}		
		else {
			
		}
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
		
		kMP.stopMusicService(this);
	}

	/**
	 * We're overriding the default behavior for when the
	 * user presses the back button.
	 * 
	 * This way, it'll not react at all.
	 * 
	 * TODO Improve on this - make it so that when doing this
	 *      will just keep the app running on the background.
	 */
	@Override
	public void onBackPressed() {
		// default behavior:
		// finish();
	}
	
	/**
	 * Activity is about to become visible - let's start the music
	 * service.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		kMP.startMusicService(this);
	}
	
	/**
	 * Let's create the action bar (menu on the top).
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		// Built based on the `res/menu/main.xml`
		MenuInflater inflater= getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	
	// HELPER METHODS
	
	/**
	 * Does an action on another Thread.
	 * 
	 * On this case, we'll scan the songs on the Android device
	 * without blocking the main Thread.
	 * 
	 * It gives a nice pop-up when finishes.
	 * 
	 * Source:
	 * http://answers.oreilly.com/topic/2699-how-to-handle-threads-in-android-and-what-you-need-to-watch-for/
	 */
	class ScanSongs extends AsyncTask<String, Integer, String> {

		/**
		 * The action we'll do in the background.
		 */
		@Override
		protected String doInBackground(String... params) {
			
			try {
				// Will scan all songs on the device
				kMP.songs.scanSongs(ActivityMenuMain.this);
				return "Finished scanning songs";
			}
			catch (Exception e) {
				Log.e("Couldn't execute background task", e.toString());
				e.printStackTrace();
				return "Failed to scan songs";
			}
		}
		
		/**
		 * Called once the background processing is done.
		 */
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			Toast.makeText(ActivityMenuMain.this,
				       result,
				       Toast.LENGTH_LONG).show();
		}
	}
}
