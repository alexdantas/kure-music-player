package com.kure.musicplayer.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Maps ArrayList<String> to simple Layouts.
 */
public class MenuItemAdapter extends ArrayAdapter<String> {

	private final Context context;
	private final int resourceID;
	
	public MenuItemAdapter(Context context, int resource, ArrayList<String> items) {
		super(context, resource, items);
		
		this.context    = context;
		this.resourceID = resource;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View rowView = inflater.inflate(resourceID, parent, false);
		
		return rowView;
	}
}
