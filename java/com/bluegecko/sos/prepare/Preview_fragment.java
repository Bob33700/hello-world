package com.bluegecko.sos.prepare;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.Preferences;


/**
 * A simple {@link Fragment} subclass.
 */
public class Preview_fragment extends DialogFragment {

	private Context mContext;

	public Preview_fragment() {
		// Required empty public constructor
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_preview, container, false);

		SharedPreferences prefs = mContext.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

		TextView messageTextView = (TextView) rootView.findViewById(R.id.messageTextView);
		String defString = prefs.getString(Preferences.DEFAULT_MESSAGE,"").isEmpty() ? getString(R.string.defaultMessageText) : prefs.getString(Preferences.DEFAULT_MESSAGE,"");
		String textString = getString(R.string.TAG) + "\n"+ defString;
		textString += getString(R.string.positionMessageText) + " lat, long";
		messageTextView.setText(textString);

		return rootView;
	}

	@Override
	public void onAttach(Context context){
		super.onAttach(context);
		mContext = context;
	}
}
