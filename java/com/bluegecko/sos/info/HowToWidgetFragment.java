package com.bluegecko.sos.info;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluegecko.sos.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class HowToWidgetFragment extends DialogFragment {


	// Required empty public constructor
	public HowToWidgetFragment() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_how_to_widget, container, false);
	}

}
