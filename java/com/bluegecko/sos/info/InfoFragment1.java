package com.bluegecko.sos.info;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bluegecko.sos.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoFragment1 extends DialogFragment {


	// Required empty public constructor
	public InfoFragment1() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_info1, container, false);

		ImageView howTo_widget = (ImageView) rootView.findViewById(R.id.howTo_widget);
		howTo_widget.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new HowToWidgetFragment().show(getChildFragmentManager(), "");
			}
		});

	return rootView;
	}

}
