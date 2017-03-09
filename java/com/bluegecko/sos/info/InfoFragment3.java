package com.bluegecko.sos.info;


import android.os.Build;
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
public class InfoFragment3 extends DialogFragment {


	// Required empty public constructor
	public InfoFragment3() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_info3, container, false);

		rootView.findViewById(R.id.permissionsLayout)
				.setVisibility((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)?
									View.VISIBLE:
									View.GONE);

	return rootView;
	}

}
