package com.kure.musicplayer.activities;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.kure.musicplayer.R;

/**
 * Menu that will show our settings.
 * 
 * We get an automatic ListView based on our items on the
 * `res/xml/preferences.xml` file.
 * 
 * Thanks a lot, you awesome source you!
 * http://android-elements.blogspot.com.br/2011/06/creating-android-preferences-screen.html
 * 
 * @see SettingsFragment.
 */
public class ActivityMenuSettings extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
}
