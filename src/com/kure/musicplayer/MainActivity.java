package com.kure.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

import com.kure.musicplayer.MusicService.MusicBinder;

/**
 * Main screen that will be shown when the user starts.
 * Here's all the application logic, split into several classes.
 * 
 * The default behavior is to keep playing the music on
 * the background and only quit the app if the user
 * presses the button "End".
 * 
 * It has a nice media control.
 */
public class MainActivity extends Activity implements MediaPlayerControl {
	
	/**
	 * Big list of all songs on the device.
	 */
	private ArrayList<Song> songList;
	
	private ListView songView;
	
	/**
	 * Our custom service that allows the music to play.
	 * It'll be controlled here, on the MainActivity.
	 */
	private MusicService musicService;
	
	private Intent playIntent;
	
	/**
	 * Tells if we bound the Activity class to the Service. 
	 */
	private boolean musicBound = false;
	
	private boolean paused = false;
	private boolean playbackPaused = false;
	
	// THESE ARE THE METHODS THAT CONTROL THE ACTIVITY LIFECYCLE
	
	/**
	 * Activity is being created for the first time.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Let's fill ourselves with all the songs
		// available on the device.
		songView = (ListView)findViewById(R.id.song_list);
		songList = new ArrayList<Song>();
		
		fillSongList();
		sortSongListBy("Title");
		
		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		SongAdapter songAdapter = new SongAdapter(this, songList);
		songView.setAdapter(songAdapter);	
		
		setMusicController();
		
		// Loading default settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	}

	/**
	 * Activity is about to become visible.
	 * 
	 * Makes the Service start whenever the Activity is shown.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		
		if (playIntent == null) {
			// Start the MusicService with our list of musics
			playIntent = new Intent(this, MusicService.class);
			bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
			startService(playIntent);
		}
	};	
	
	/**
	 * Another Activity is taking focus.
	 * (either from user going to another Activity or home)
	 */
	@Override
	protected void onPause() {
		super.onPause();
		
		paused = true;
		playbackPaused = true;		
	}
	
	/**
	 * Activity has become visible.
	 * 
	 * @see onPause()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		if (paused) {
			// Ensure that the controller
			// is shown when the user returns to the app
			setMusicController();
			paused = false;
		}		
	}
	
	/**
	 * Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		musicController.hide();
		super.onStop();		
	}
	
	/**
	 * Activity is about to be destroyed.
	 */
	@Override
	protected void onDestroy() {
		// Cleaning up everything
		stopService(playIntent);
		musicService = null;
		
		super.onDestroy();
	}
	
	// END OF ACTIVITY LIFECYCLE METHODS
	
	/**
	 * The actual connection to the MusicService.
	 * We start it with an Intent.
	 * 
	 * These callbacks will bind the MusicService to our internal
	 * variables.
	 * We can only know it happened through our flag, `musicBound`. 
	 */
	private ServiceConnection musicConnection = new ServiceConnection() {
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			MusicBinder binder = (MusicBinder)service;
			
			// Here's where we finally create the MusicService
			// and set it's music list.
			musicService = binder.getService();
			musicService.setList(songList);
			musicBound = true;
		}; 
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			musicBound = false;
		}
		
	};
	
	/**
	 * Fills the ListView with all the songs found on the device.
	 */
	public void fillSongList() {
		// This will ask for details on music files
		ContentResolver musicResolver = getContentResolver();
		
		// This will contain the URI to music files.
		// We're trying to get music from the SD card - EXTERNAL_CONTENT
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		
		// Pointer to database results when querying a resolver
		Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
		
		if (musicCursor != null && musicCursor.moveToFirst())
		{
			// Getting pre-defined columns from the system
			// They're the data from all music found on the URI.
			int titleColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.TITLE);
			
			int idColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media._ID);
			
			int artistColumn = musicCursor.getColumnIndex
					(android.provider.MediaStore.Audio.Media.ARTIST);
			
			// Adding songs to the list
			do {
				long   thisId     = musicCursor.getLong(idColumn);
				String thisTitle  = musicCursor.getString(titleColumn);
				String thisArtist = musicCursor.getString(artistColumn);
				
				songList.add(new Song(thisId, thisTitle, thisArtist));
			}
			while (musicCursor.moveToNext());
		}
		else
		{
			// What do I do if I can't find any songs?
			songList.add(new Song(0, "No Songs Found", "On this Device"));
		}
	}
	
	/**
	 * Sorts all the songs.
	 * They can be sorted alphabetically by "Artist" or "Title".
	 */
	public void sortSongListBy(String way) {
		
		if (way == "Artist")
		{
			Collections.sort(songList, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getArtist().compareTo(b.getArtist());
				}
			});
		}
		else {
			Collections.sort(songList, new Comparator<Song>() {
				public int compare(Song a, Song b)
				{
					return a.getTitle().compareTo(b.getTitle());
				}
			});
		}
	}
	
	/**
	 * Called when creating the top menu.
	 * It adds all the buttons on the `res/menu/main.xml` file.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	/**
	 * This method gets called whenever the user clicks an
	 * item on the top menu.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
		
		case R.id.action_shuffle:
			musicService.toggleShuffleMode();
			return true;
			
		case R.id.action_end:
		case R.id.action_end2:
			// Let's stop playing the music and
			// quitting the App altogether
			stopService(playIntent);
			musicService = null;
			
			// This forces the system to kill the process, although
			// it's not a nice way to do it.
			//
			// Later implement FinActivity:
			// http://stackoverflow.com/a/4737595
			System.exit(0);
			break;
			
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Gets called whenever the user touches the song on the View.
	 * It is defined on the Song Layout.
	 */
	public void songPickedByUser(View view) {
		// We're getting the song's index from the tag
		// set to the View on SongAdapter.
		musicService.setSong(Integer.parseInt(view.getTag().toString()));
		musicService.playSong();
		
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
		
		musicController.show(0);
	}

	
	// These are the methods to implement for the MediaPlayerControl
	// They're called by the Android System.
	
	private MusicController musicController;

	/**
	 * (Re)Starts the MusicController.
	 */
	private void setMusicController() {
		musicController = new MusicController(this);
		
		// What will happen when the user presses the
		// next/previous buttons?
		musicController.setPrevNextListeners(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Calling method defined on MainActivity
				playNext();
			}
		}, new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// Calling method defined on MainActivity
				playPrevious();
			}
		});
		
		// Binding to our media player
		musicController.setMediaPlayer(this);
		musicController.setAnchorView(findViewById(R.id.song_list));
		musicController.setEnabled(true);
	}
	
	@Override
	public void start() {
		musicService.start();
	}

	@Override
	public void pause() {
		musicService.pausePlayer();
	}

	@Override
	public int getDuration() {
		if (musicService != null && musicBound && musicService.isPlaying())
			return musicService.getDuration();
		else
			return 0;
	}

	@Override
	public int getCurrentPosition() {
		if (musicService != null && musicBound && musicService.isPlaying())
			return musicService.getPosition();
		else
			return 0;
	}

	@Override
	public void seekTo(int position) {
		musicService.seekTo(position);
	}

	@Override
	public boolean isPlaying() {
		if (musicService != null && musicBound)
			return musicService.isPlaying();
		
		return false;
	}

	@Override
	public int getBufferPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getAudioSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// Back to the normal methods
	
	private void playNext() {
		musicService.next();
		musicService.playSong();
		
		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.		
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
		
		musicController.show(0); //immediately
	}
	private void playPrevious() {
		musicService.previous();
		musicService.playSong();
		
		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
		
		musicController.show(0); //immediately
	}
}
