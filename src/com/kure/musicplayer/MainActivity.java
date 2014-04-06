package com.kure.musicplayer;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;


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
	
	private ListView songView;

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
				
		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		SongAdapter songAdapter = new SongAdapter(this, kMP.songs.songs);
		songView.setAdapter(songAdapter);	
		
		setMusicController();
	}

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


	// END OF ACTIVITY LIFECYCLE METHODS
	
	/**
	 * Gets called whenever the user touches the song on the View.
	 * It is defined on the Song Layout.
	 */
	public void songPickedByUser(View view) {
		// We're getting the song's index from the tag
		// set to the View on SongAdapter.
		kMP.musicService.setSong(Integer.parseInt(view.getTag().toString()));
		kMP.musicService.playSong();
		
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
		kMP.musicService.start();
	}

	@Override
	public void pause() {
		kMP.musicService.pausePlayer();
	}

	@Override
	public int getDuration() {
		if (kMP.musicService != null && kMP.musicService.musicBound && kMP.musicService.isPlaying())
			return kMP.musicService.getDuration();
		else
			return 0;
	}

	@Override
	public int getCurrentPosition() {
		if (kMP.musicService != null && kMP.musicService.musicBound && kMP.musicService.isPlaying())
			return kMP.musicService.getPosition();
		else
			return 0;
	}

	@Override
	public void seekTo(int position) {
		kMP.musicService.seekTo(position);
	}

	@Override
	public boolean isPlaying() {
		if (kMP.musicService != null && kMP.musicService.musicBound)
			return kMP.musicService.isPlaying();
		
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
		kMP.musicService.next();
		kMP.musicService.playSong();
		
		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.		
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
		
		musicController.show(0); //immediately
	}
	private void playPrevious() {
		kMP.musicService.previous();
		kMP.musicService.playSong();
		
		// To prevent the MusicPlayer from behaving
		// unexpectedly when we pause the song playback.
		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
		
		musicController.show(0); //immediately
	}
}
