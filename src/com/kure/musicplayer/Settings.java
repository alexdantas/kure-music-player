package com.kure.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Interface to the application's settings.
 *
 * Keep in mind that when we call `get()` we must supply a
 * key that exists on `res/xml/preferences.xml`!
 * 
 * As a matter of fact, go look there now. 
 * 
 * Thanks a lot again, you great sources:
 * - http://stackoverflow.com/a/3624358
 * - http://android-elements.blogspot.com.br/2011/06/creating-android-preferences-screen.html
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
	
	/**
	 * Resets all settings to default.
	 */
	public void reset() {
		preferences.edit().clear().commit();
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
