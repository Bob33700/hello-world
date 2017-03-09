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
public class InfoFragment2 extends DialogFragment {


	// Required empty public constructor
	public InfoFragment2() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_info2, container, false);
	}

}
