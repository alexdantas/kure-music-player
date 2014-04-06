package com.kure.musicplayer;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * The screen that will show our settings.
 * 
 * @see SettingsFragment.
 */
public class SettingsActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
}
