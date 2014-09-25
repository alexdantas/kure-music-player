package com.kure.musicplayer.activities;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.adapters.AdapterSong;


/**
 * Shows a predefined list of songs, letting the user select
 * them to play.
 *
 * @note This class is a mess because, to decide which songs to
 *       display, it uses the member `kMP.musicList`.
 */
public class ActivityListSongs extends ActivityMaster
	implements OnItemClickListener {

	/**
	 * List of songs that will be shown to the user.
	 */
	private ListView songListView;

	@Override
	protected void onCreate(Bundle popcorn) {
		super.onCreate(popcorn);

		setContentView(R.layout.activity_list_songs);

		// Let's fill ourselves with all the songs
		// available on the device.
		songListView = (ListView)findViewById(R.id.activity_list_songs_list);

		// We'll get warned when the user clicks on an item.
		songListView.setOnItemClickListener(this);

		// If we got an extra with a title, we'll apply it
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if (bundle != null)
			this.setTitle((String)bundle.get("title"));

		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		if ((kMP.musicList != null) && (! kMP.musicList.isEmpty())) {
			AdapterSong songAdapter = new AdapterSong(this, kMP.musicList);
			songListView.setAdapter(songAdapter);
		}

		// This enables the "Up" button on the top Action Bar
		// Note that it returns to the parent Activity, specified
		// on `AndroidManifest`
		ActionBar actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		// If we press and hold on a Song, let's add to the current
		// playing queue.
		songListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {

				kMP.musicService.add(kMP.musicList.get(position));
				return true;
			}
		});

	}

	/**
	 * When the user selects an item from our list, we'll start playing.
	 *
	 * We'll play the current list, starting from the song the user
	 * just selected.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// We'll play the current song list
		kMP.nowPlayingList = kMP.musicList;

		// Sending the song index inside the now playing list.
		// See the documentation of `ActivityNowPLaying` class.
		Intent intent = new Intent(this, ActivityNowPlaying.class);

		intent.putExtra("song", position);

		startActivity(intent);
	}
}
