package com.kure.musicplayer;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * This was created so my project could forget about Fragments.
 * I have no idea what it does.
 * 
 * Source:http://stackoverflow.com/questions/22339554/how-to-prevent-eclipse-from-creating-fragment-main-xml
 *
 */
public class PlaceholderFragment extends Fragment {

	public PlaceholderFragment() {
		
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        return rootView;
    }
}
