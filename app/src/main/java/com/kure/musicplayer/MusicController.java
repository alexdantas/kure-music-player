package com.kure.musicplayer;

import android.content.Context;
import android.widget.MediaController;

/**
 * Widget that shows some cute buttons to control
 * the music playback.
 * 
 * The actual code for changing the music resides
 * at MusicService, we're only changing the appearance
 * of things.
 */
public class MusicController extends MediaController {

	public MusicController(Context c) {
		super(c);
	}
	
	/**
	 * We're overriding the parent's `hide` method, so we
	 * can prevent the controls from hiding after 3 seconds.
	 */
	/*public void hide() { }*/
}
