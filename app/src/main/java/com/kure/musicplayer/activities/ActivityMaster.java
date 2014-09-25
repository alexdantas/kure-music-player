package com.kure.musicplayer.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

/**
 * Master Activity from which every other Activity inherits
 * (except for `ActivityMenuSettings`).
 *
 * If contains some things they all have in common:
 *
 * - They can change the color theme at runtime;
 * - They all have the same context menu (bottom menu).
 *   (note that there's an extra item "Now Playing" that
 *    only appears if user started playing something)
 *
 * What we do is make each Activity keep track of which
 * theme it currently has.
 * Whenever they have focus, we test to see if the global theme
 * was changed by the user.
 * If it was, it `recreate()`s itself.
 *
 * @note We must call `Activity.setTheme()` BEFORE
 *       `Activity.setContentView()`!
 *
 * Sources that made me apply this idea, thank you so much:
 * - http://stackoverflow.com/a/4673209
 * - http://stackoverflow.com/a/11875930
 */
@SuppressLint("Registered") // No need to register this class on AndroidManifest
public class ActivityMaster extends Activity {

	/**
	 * Keeping track of the current theme name.
	 *
	 * @note It's name and valid values are defined on
	 *       `res/values/strings.xml`, at the fields
	 *       we can change on the Settings menu.
	 */
	protected String currentTheme = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Mandatory - when creating we don't have
		// a theme applied yet.
		refreshTheme();

		kMP.startMusicService(this);
	}

	/**
	 * Called when the user returns to this activity after leaving.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		// Every time the user focuses this Activity,
		// we need to check it.
		// It the theme changed, recreate ourselves.
		if (refreshTheme())
			recreate();
	}

	/**
	 * Tests if our current theme is the same as the one
	 * specified on `Settings`, reapplying the theme if
	 * not the case.
	 *
	 * @return Flag that tells if we've changed the theme.
	 */
	public boolean refreshTheme() {

		// Getting global theme name from the Settings.
		// Second argument is the default value, in case
		// something went wrong.
		String theme = kMP.settings.get("themes", "default");

		if (currentTheme != theme)
		{
			// Testing each possible theme name.
			// I repeat - all valid theme names are specified
			// at `res/strings.xml`, right at the Settings sub menu.
			if      (theme.equals("default"))         setTheme(R.style.Theme_Default);
			else if (theme.equals("light"))           setTheme(R.style.Theme_Light);
			else if (theme.equals("dark"))            setTheme(R.style.Theme_Dark);
			else if (theme.equals("solarized_dark"))  setTheme(R.style.Theme_Solarized_Dark);
			else if (theme.equals("wallpaper"))       setTheme(R.style.Theme_Wallpaper);
			else if (theme.equals("dialog_light"))    setTheme(R.style.Theme_DialogLight);
			else if (theme.equals("dialog_dark"))     setTheme(R.style.Theme_DialogDark);
			else if (theme.equals("light_simple"))    setTheme(R.style.Theme_LightSimple);
			else if (theme.equals("dark_simple"))     setTheme(R.style.Theme_DarkSimple);

			currentTheme = theme;
			return true;
		}
		return false;
	}

	/**
	 * Let's set a context menu (menu that appears when
	 * the user presses the "menu" button).
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Default options specified on the XML
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_context, menu);

		// Extra option to go to Now Playing screen
		// (only activated when there's an actual Now Playing screen)
		if (kMP.mainMenuHasNowPlayingItem)
			menu.findItem(R.id.context_menu_now_playing).setVisible(true);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * This method gets called whenever the user clicks an
	 * item on the context menu.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		// I know it's bad to force quiting the program,
		// but I just love when applications have this option
		case R.id.context_menu_end:
			kMP.forceExit(this);
			break;

		case R.id.context_menu_settings:
			startActivity(new Intent(this, ActivityMenuSettings.class));
			break;

		case R.id.context_menu_now_playing:
			Intent nowPlayingIntent = new Intent(this, ActivityNowPlaying.class);
			nowPlayingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			startActivity(nowPlayingIntent);
			break;
		}

		return super.onOptionsItemSelected(item);
	}
}

