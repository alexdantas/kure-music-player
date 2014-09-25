package com.kure.musicplayer.helpers;

import android.content.Context;
import android.widget.Toast;

/**
 * Makes sure that a single Toast (text message) is displayed
 * on the application.
 *
 * Normally when you call several Toasts, they wait for the
 * others to finish.
 * We don't want that!
 *
 * With this class, you get an immediate Toast right away.
 *
 * Thanks to the awesome guys at StackOverflow:
 * http://stackoverflow.com/a/18676736
 *
 * Created by kure on 9/24/2014.
 */
public class SingleToast {

	private static Toast singleToast = null;

	/**
	 * Immediately shows a text message.
	 * Use this the same way you would call `Toast`.
	 *
	 * @note It calls "show()" immediately.
	 */
	public static void show(Context c, String text, int duration) {

		if (singleToast != null)
			singleToast.cancel(); // override current Toast, mate!

		singleToast = Toast.makeText(c, text, duration);
		singleToast.show();
	}
}
