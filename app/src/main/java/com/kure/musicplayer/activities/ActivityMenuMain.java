package com.kure.musicplayer.activities;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.kure.musicplayer.NotificationMusic;
import com.kure.musicplayer.R;
import com.kure.musicplayer.helpers.SingleToast;
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

	// Adapter that will convert from Strings to List Items
	public static ArrayAdapter<String> adapter = null;

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

	// These variables are used to allow allow user to
	// press twice to exit the program
	// (showing a message when pressing the first time).

	private boolean backPressedOnce = false;
	private Handler backPressedHandler = new Handler();

	/** How long to wait to disable double-pressing to quit */
	private static final int BACK_PRESSED_DELAY = 2000;

	/** Action that actually disables double-pressing to quit */
	private final Runnable backPressedTimeoutAction = new Runnable() {
		@Override
		public void run() {
			backPressedOnce = false;
		}
	};

	/**
	 * Called when the activity is created for the first time.
	 */
	@Override
	protected void onCreate(Bundle seventhSonOfASeventhSon) {

		// We need to load the settings right before creating
		// the first activity so that the user-selected theme
		// will be applied to the first screen.
		//
		// Loading default settings at the first time the app;
		// is loaded.
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		kMP.settings.load(this);

		super.onCreate(seventhSonOfASeventhSon);
		setContentView(R.layout.activity_main_menu);

		// Adding all possible items on the main menu.
		items.add(getString(R.string.menu_main_music));
		items.add(getString(R.string.menu_main_settings));
		items.add(getString(R.string.menu_main_shuffle));

		if (kMP.mainMenuHasNowPlayingItem)
			items.add(getString(R.string.menu_main_now_playing));

		// ListView to be populated with the menu items
		listView = (ListView)findViewById(R.id.activity_main_menu_list);

		// Thing that converts the menu items to the ListView
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);

		// Filling teh list with all the items
		listView.setAdapter(adapter);

		// We'll get warned when the user clicks on an item.
		listView.setOnItemClickListener(this);

		// Initializing the main program logic.
		kMP.initialize(this);

		scanSongs(false);
	}

	/**
	 * Starts the background process of scanning the songs.
	 *
	 * @param forceScan If we should scan again. You should set
	 *                  this to true if you want to scan again
	 *                  the database.
	 *                  Otherwise, leave it `false` so we don't
	 *                  rescan the songs when this Activity
	 *                  is created again for some reason.
	 */
	void scanSongs(boolean forceScan) {

		// Loading all the songs from the device on a different thread.
		// We'll only actually do it if they weren't loaded already
		//
		// See the implementation right at the end of this class.
		if ((forceScan) || (! kMP.songs.isInitialized())) {

			SingleToast.show(ActivityMenuMain.this,
					getString(R.string.menu_main_scanning),
					Toast.LENGTH_LONG);

			new ScanSongs().execute();
		}
	}

	/**
	 * Will react to the user selecting an item.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// Gets the string value of the current item and
		// compares to all possible items.
		String currentItem = listView.getItemAtPosition(position).toString();

		if (currentItem.equals(getString(R.string.menu_main_music))) {
			startActivity(new Intent(this, ActivityMenuMusic.class));
		}
		else if (currentItem.equals(getString(R.string.menu_main_settings))) {

			// Let's start the settings screen.
			// While doing so, we need to know if the user have
			// changed the theme.
			// If he did, we'll refresh the screen.
			// See `onActivityResult()`
			Intent settingsIntent = new Intent(this, ActivityMenuSettings.class);
			startActivityForResult(settingsIntent, USER_CHANGED_THEME);

		}
		else if (currentItem.equals(getString(R.string.menu_main_shuffle))) {

			// Can only jump to shuffle all songs if we've
			// scanned all the songs from the device.
			if (! kMP.songs.isInitialized()) {
				SingleToast.show(this,
			               getString(R.string.menu_music_proceed_error),
			               Toast.LENGTH_LONG);
				return;
			}

			// Shuffle all songs

			kMP.nowPlayingList = kMP.songs.getSongs();

			Intent nowPlayingIntent = new Intent(this, ActivityNowPlaying.class);
			nowPlayingIntent.putExtra("sort", "random");
			nowPlayingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			startActivity(nowPlayingIntent);
		}
		else if (currentItem.equals(getString(R.string.menu_main_now_playing))) {
			// Jump to Now Playing screen
			startActivity(new Intent(this, ActivityNowPlaying.class));
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

		if (backPressedHandler != null)
			backPressedHandler.removeCallbacks(backPressedTimeoutAction);

		// Need to clear all the items otherwise
		// they'll keep adding up.
		items.clear();

		// Cancell all thrown Notifications
		NotificationMusic.cancelAll(this);

		kMP.stopMusicService(this);
	}

	/**
	 * We're overriding the default behavior for when the
	 * user presses the back button.
	 *
	 * This way, it will show "Please click BACK again to exit"
     * and if the user presses again it will quit.
     *
     * Thanks, guys at StackOverflow:
     * http://stackoverflow.com/a/13578600
	 */
	@Override
	public void onBackPressed() {

        if (this.backPressedOnce) {
            // Default behavior, quit it
            super.onBackPressed();
			kMP.forceExit(this);
            return;
        }

        this.backPressedOnce = true;

		SingleToast.show(this, getString(R.string.menu_main_back_to_exit), Toast.LENGTH_SHORT);

		backPressedHandler.postDelayed(backPressedTimeoutAction, BACK_PRESSED_DELAY);
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
				kMP.songs.scanSongs(ActivityMenuMain.this, "external");
				return ActivityMenuMain.this.getString(R.string.menu_main_scanning_ok);
			}
			catch (Exception e) {
				Log.e("Couldn't execute background task", e.toString());
				e.printStackTrace();
				return ActivityMenuMain.this.getString(R.string.menu_main_scanning_not_ok);
			}
		}

		/**
		 * Called once the background processing is done.
		 */
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			SingleToast.show(ActivityMenuMain.this,
			               result,
			               Toast.LENGTH_LONG);
		}
	}

	/**
	 * Adds a new item "Now Playing" on the main menu, if
	 * it ain't there yet.
	 */
	public static void addNowPlayingItem(Context c) {

		if (kMP.mainMenuHasNowPlayingItem)
			return;

		ActivityMenuMain.items.add(c.getString(R.string.menu_main_now_playing));

		kMP.mainMenuHasNowPlayingItem = true;

		// Refresh ListView
		adapter.notifyDataSetChanged();
	}
}
