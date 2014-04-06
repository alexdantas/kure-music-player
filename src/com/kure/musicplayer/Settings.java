package com.kure.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Interface to the application's settings.
 *
 */
public class Settings {

	/**
	 * Current app's preferences.
	 * They're read and saved on `res/xml/preferences.xml`.
	 */
	private SharedPreferences preferences = null;
	
	/**
	 * Initializes the internal settings
	 */
	public void load(Context c) {
        preferences = PreferenceManager.getDefaultSharedPreferences(c);
	}
	
	// QUERY METHODS
	
	public boolean get(String key, boolean defaultValue) {
		if (preferences == null)
			return defaultValue;
		
		return preferences.getBoolean(key, defaultValue);
	}
	
	public String get(String key, String defaultValue) {
		if (preferences == null)
			return defaultValue;
		
		return preferences.getString(key, defaultValue);
	}
	
	public int get(String key, int defaultValue) {
		if (preferences == null)
			return defaultValue;
		
		return preferences.getInt(key, defaultValue);
	}
}
