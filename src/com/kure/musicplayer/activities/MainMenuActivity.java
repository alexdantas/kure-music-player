package com.kure.musicplayer.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.kure.musicplayer.R;
import com.kure.musicplayer.kMP;

/**
 * First screen that the user sees - the Main Menu.
 * 
 * Must listen for clicks so we can change to the other
 * sub menus (Activities).
 * 
 * Thanks for providing a basic ListView navigation layout: 
 * http://stackoverflow.com/q/19476948
 */
public class MainMenuActivity extends Activity
	implements OnItemClickListener {

	/**
	 * All the possible items the user can select on this menu.
	 * 
	 * Will be initialized with default values on `onCreate`.
	 */
	public static final ArrayList<String> items = new ArrayList<String>();

	/**
	 * List that will be populated with all the items.
	 * 
	 * Look for it inside the res/layout xml files.
	 */
	ListView listView;
	
	/**
	 * Called when the activity is created for the first time.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_menu);
	
		// Adding all possible items on the menu.
		items.add(getString(R.string.menu_main_music));
		items.add(getString(R.string.menu_main_settings));
		items.add(getString(R.string.menu_main_shuffle));
		
		// List to be populated with items
		listView = (ListView)findViewById(R.id.activity_main_menu_list);
		
		// Adapter that will convert from Strings to List Items
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>
				(this, android.R.layout.simple_list_item_1, items);
		
		// Filling teh list with all the items
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(this);
		
		// Initializing the main program logic.
		kMP.initialize();
	}

	/**
	 * Will react to the user selecting an item.
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		// Gets the string value of the current item and
		// compares to all possible items.
		String currentItem = listView.getItemAtPosition(position).toString();
		
		if (currentItem == getString(R.string.menu_main_music)) {
			startActivity(new Intent(this, MusicMenuActivity.class));
		}
		else if (currentItem == getString(R.string.menu_main_settings)) {
			
		}
		else if (currentItem == getString(R.string.menu_main_shuffle)) {
			
		}		
		else {
			
		}
	}
	
	/**
	 * When destroying the Activity.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		// Need to clear all the items otherwise
		// they'll keep adding up.
		items.clear();
	}
}
