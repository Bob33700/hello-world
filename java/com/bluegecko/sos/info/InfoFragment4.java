package com.bluegecko.sos.info;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.bluegecko.sos.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class InfoFragment4 extends DialogFragment {

	private Info4Listener mListener;


	// Required empty public constructor
	public InfoFragment4() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_info4, container, false);

		Button OKbutton = (Button) rootView.findViewById(R.id.OKbutton);
		OKbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mListener.onInfoOK();
			}
		});

	return rootView;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mListener = (Info4Listener) context;
	}

	@Override
	public void onDetach(){
		super.onDetach();
		mListener = null;
	}

	public interface Info4Listener {
		void onInfoOK();
	}

}
