package com.kure.musicplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;

import com.kure.musicplayer.MusicController;
import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;
import com.kure.musicplayer.adapters.AdapterSong;


/**
 * Shows the "Now Playing Queue", with all songs to be played, plus
 * letting the user change between songs with a MediaPlayerControl.
 */
public class ActivityNowPlaying extends ActivityMaster
	implements MediaPlayerControl, OnItemClickListener {
	
	/**
	 * List that will display all the songs.
	 */
	private ListView songListView;

	private boolean paused = false;
	private boolean playbackPaused = false;
	
	private MusicController musicController;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);

		songListView = (ListView)findViewById(R.id.activity_now_playing_song_list);
		
		// We'll play this pre-defined list
		kMP.musicService.setList(kMP.nowPlayingList);
				
		// Connects the song list to an adapter
		// (thing that creates several Layouts from the song list)
		AdapterSong songAdapter = new AdapterSong(this, kMP.nowPlayingList);
		songListView.setAdapter(songAdapter);	

		// We'll get warned when the user clicks on an item.
		songListView.setOnItemClickListener(this);
	
		// We expect an extra with the song to start playing
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		
		if (bundle == null)
			throw new RuntimeException("Expected Song Index");
		
		// This is the song we'll play!
		int index = (int)bundle.get("current");
		
		// Prepare the music service to play the song.
		kMP.musicService.setSong(index);
		
		// Scroll the list view to the current song.
		songListView.setSelection(index);
		
		// Attempt to change the background of current song
		/*
		View view = songListView.getChildAt(index - songListView.getFirstVisiblePosition());
		if (view != null)
		{
			view.setBackgroundColor(Color.RED);
		}
		*/
		
		setMusicController();
		
		kMP.musicService.playSong();

		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
	}
	
	@Override
	protected void onStart() {
		
		super.onStart();
		
	//	if (!this.isFinishing()) {
	//	    musicController.show(5000);
	//	}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if(event.getAction() == KeyEvent.ACTION_DOWN) 
			if (keyCode == KeyEvent.KEYCODE_MENU)
				musicController.show();
		
		return super.onKeyDown(keyCode, event);
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

	/**
	 * (Re)Starts the musicController.
	 */
	private void setMusicController() {
		musicController = new MusicController(ActivityNowPlaying.this);
		
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
		musicController.setAnchorView(findViewById(R.id.activity_now_playing_song_list));
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		// Prepare the music service to play the song.
		kMP.musicService.setSong(position);
		
		// Scroll the list view to the current song.
		songListView.setSelection(position);

		
		kMP.musicService.playSong();

		if (playbackPaused) {
			setMusicController();
			playbackPaused = false;
		}
	}
}

