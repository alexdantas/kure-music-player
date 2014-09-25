package com.kure.musicplayer.activities;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

/**
 * A menu that allows the user to change the application's
 * settings/preferences/configuration.
 *
 * This ListView is populated automatically from the
 * file `res/xml/preferences.xml`.
 *
 * Thanks:
 * - For teaching me how to build the Settings Activity:
 *   http://android-elements.blogspot.com.br/2011/06/creating-android-preferences-screen.html
 *
 * If the user changes the application's theme, the changes are
 * applied through all the application.
 *
 * This Activity listens for any change on the Theme, and if it
 * happens, we `recreate()` this Activity.
 *
 * Every other class handles it's theme according to the methods
 * inside `ActivityMaster`.
 */
public class ActivityMenuSettings extends PreferenceActivity
	implements OnSharedPreferenceChangeListener {

	/**
	 * Called when the activity is firstly created.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Setting the theme from kMP.Settings
		// right before creating the actual activity.
		//
		// TODO This is a hack - I've copied the code
		//      within `ActivityMaster`. I need to find
		//      a way to unify them. Currently I can't
		//      since I cant' subclass `ActivityMaster` here
		//      because it already needs to subclass
		//      PreferenceActivity.
		String theme = kMP.settings.get("themes", "default");

		if      (theme.equals("default"))         setTheme(R.style.Theme_Default);
		else if (theme.equals("light"))           setTheme(R.style.Theme_Light);
		else if (theme.equals("dark"))            setTheme(R.style.Theme_Dark);
		else if (theme.equals("solarized_dark"))  setTheme(R.style.Theme_Solarized_Dark);
		else if (theme.equals("wallpaper"))       setTheme(R.style.Theme_Wallpaper);
		else if (theme.equals("dialog_light"))    setTheme(R.style.Theme_DialogLight);
		else if (theme.equals("dialog_dark"))     setTheme(R.style.Theme_DialogDark);
		else if (theme.equals("light_simple"))    setTheme(R.style.Theme_LightSimple);
		else if (theme.equals("dark_simple"))     setTheme(R.style.Theme_DarkSimple);

		// This enables the "Up" button on the top Action Bar
		// Note that it returns to the parent Activity, specified
		// on `AndroidManifest`
		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		super.onCreate(savedInstanceState);

		// Populating the View with all the items from
		// file `res/xml/preferences.xml`.
		addPreferencesFromResource(R.xml.preferences);

		// We're asking to be notified when the user changes
		// any setting.
		PreferenceManager
			.getDefaultSharedPreferences(this)
			.registerOnSharedPreferenceChangeListener(this);

		// For every possible option inside this screen,
		// let's set what will happen when the user clicks on it.
		for(int x = 0; x < getPreferenceScreen().getPreferenceCount(); x++) {

			PreferenceCategory category = (PreferenceCategory) getPreferenceScreen().getPreference(x);

			for (int y = 0; y < category.getPreferenceCount(); y++) {
				Preference preference = category.getPreference(y);
				preference.setOnPreferenceClickListener(onClickPreference);

				// Initializing the default values
				if (preference.getKey().equals("version")) {
					preference.setSummary(kMP.versionName);
				}
			}
		}
	}

	/**
	 * This is what will happen when the user clicks
	 * any setting on this screen.
	 */
	public OnPreferenceClickListener onClickPreference = new OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference pref) {

			// When the user clicks the "Info" preference,
			// we should show it some information about the
			// music library.
			if (pref.getKey().equals("info")) {

				// We can only handle the user choice from now on
				// if we've successfully scanned the songs from the
				// device.
				if (! kMP.songs.isInitialized()) {

					Toast.makeText(ActivityMenuSettings.this,
					               getString(R.string.menu_music_proceed_error),
					               Toast.LENGTH_LONG).show();

					return false;
				}

				// Yay, showing the dialog!
				AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityMenuSettings.this);

				String dialogTitle = getString(R.string.menu_settings_info_dialog_title);
				String dialogText  = getString(
						R.string.menu_settings_info_dialog_text,
						kMP.songs.songs.size(),
						kMP.songs.getAlbums().size(),
						kMP.songs.getArtists().size(),
						kMP.songs.getPlaylistNames().size());

				dialog.setTitle(dialogTitle)
				      .setMessage(dialogText)
				      .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
				    	  @Override
				    	  public void onClick(DialogInterface dialog, int which) {

				    		  dialog.cancel();
				    	  }
				      });
				dialog.show();
			}

			// yay!
			if /* */(/* * */pref/* */./* * * * * * * */getKey/* */(/* */)/* */.equals/* */(/* */"version"/* */)/* */) /* */{/* *//* *//* *//* *//* *//* *//* */brianGriffin/* * */++/* */; /* */if /* */(brianGriffin >= 5) { brianGriffin = 0;startActivity(new Intent(ActivityMenuSettings.this, XXX.class)); }}

			if (pref.getKey().equals("misc")) {

				// Yay, showing the other dialog!
				AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityMenuSettings.this);

				String dialogTitle = getString(R.string.menu_settings_misc_dialog_title);
				String dialogText  = getString(R.string.menu_settings_misc_dialog_text);

				dialog.setTitle(dialogTitle)
				      .setMessage(dialogText)
				      .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

				    	  @Override
				    	  public void onClick(DialogInterface dialog, int which) {

				    		  dialog.cancel();
				    	  }

				      });
				dialog.show();
			}

			return false;
		}

	};

	/**
	 * Called when the user modifies any preference.
	 */
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		// If the user changed the theme, we'll restart
		// this activity. This way the changes are applied
		// immediately.
		//
		// This key's at `res/xml/preferences.xml`
		if (key.equals("themes")) {
			recreate();
			return;
		}

		// If user changed the notification setting
		if (key.equals("show_notification")) {

			// User just cancelled notification,
			// let's kill it
			if (! sharedPreferences.getBoolean("show_notification", false)) {
				if (kMP.musicService != null)
					kMP.musicService.cancelNotification();
			}
			// User just activated notification,
			// let's create it
			else {
				if (kMP.musicService != null)
					kMP.musicService.notifyCurrentSong();
			}
			return;
		}

		// If user changed lock screen widget setting
		if (key.equals("show_lock_widget")) {

			// User just cancelled widget, let's kill it
			if (! sharedPreferences.getBoolean("show_lock_widget", false)) {
				if (kMP.musicService != null)
					kMP.musicService.destroyLockScreenWidget();
			}
			// User just activated notification.
			else {
/*				if (kMP.musicService != null) {

					// MusicService's current state needs to be
					// used to correctly set the lock screen widget
					// (I know it's ugly, damn)
					int state =
							kMP.musicService.isPlaying() ?
							RemoteControlClient.PLAYSTATE_PLAYING :
							RemoteControlClient.PLAYSTATE_PAUSED;

					kMP.musicService.updateLockScreenWidget(kMP.musicService.currentSong, state);
				}*/
			}
		}
	}



	private int brianGriffin = 0;
}
