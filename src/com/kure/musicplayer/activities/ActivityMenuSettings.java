package com.kure.musicplayer.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

/**
 * Let's the user interact with a Menu and change the
 * application's settings/preferences/configuration.
 * 
 * I don't need to build the View because  I'm subclassing
 * `PreferenceActivity` - it gives an an automatic ListView
 * based on our items on the `res/xml/preferences.xml` file.
 * 
 * Thanks a lot, you awesome source you!
 * http://android-elements.blogspot.com.br/2011/06/creating-android-preferences-screen.html
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
		String theme = kMP.settings.get("themes", "light");
		if (theme.equals("light"))
			setTheme(R.style.Theme_Light);
		
		else if (theme.equals("dark"))
			setTheme(R.style.Theme_Dark);
	
		else if (theme.equals("solariezd"))
			this.setTheme(R.style.Theme_Solarized);			
		
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		// We're asking to be notified when the user changes 
		// any setting.
		PreferenceManager
			.getDefaultSharedPreferences(this)
			.registerOnSharedPreferenceChangeListener(this);
	}
	
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
		if (key.equals("themes"))
			recreate();
	}
}
