package com.bluegecko.sos.prepare;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.Preferences;

/**
 * A simple {@link Fragment} subclass.
 */
public class EditMessageFragment extends DialogFragment {


	// Required empty public constructor
	public EditMessageFragment() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_edit_message, container, false);

		// retrieve display dimensions
		Rect displayRectangle = new Rect();
		Window window = getActivity().getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		// adjust dialog width
		rootView.setMinimumWidth((int)(displayRectangle.width() * 0.8f));

		SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		final SharedPreferences.Editor prefsEditor = prefs.edit();

		final EditText messageTextView = (EditText) rootView.findViewById(R.id.messageTextView);
		messageTextView.setText(prefs.getString(Preferences.DEFAULT_MESSAGE, getString(R.string.defaultMessageText)));
		final Button OKbutton = (Button) rootView.findViewById(R.id.OKbutton);
		OKbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = messageTextView.getText().toString();
				prefsEditor.putString(Preferences.DEFAULT_MESSAGE, message.isEmpty()? getActivity().getString(R.string.defaultMessageText) : message)
						.apply();
				dismiss();
			}
		});

		return rootView;
	}

	@Override
	public void onAttach(Context context){
		super.onAttach(context);
	}

}
