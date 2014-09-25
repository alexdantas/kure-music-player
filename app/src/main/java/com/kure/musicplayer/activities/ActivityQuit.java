package com.kure.musicplayer.activities;

import android.app.Activity;
import android.os.Bundle;

/**
 * Placeholder Activity that forcibly quits the application.
 *
 * This Activity is only used when we want to quit the
 * application (either by pressing "Quit" on the context
 * menu or by pressing back twice on the main menu).
 *
 * Do _not_ use it directly!
 * This Activity is only created when calling kMP.forceExit();
 * Go see it there.
 *
 * Created by kure on 9/24/2014.
 */
public class ActivityQuit extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		System.exit(0);
	}
}
