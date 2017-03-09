package com.bluegecko.sos.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.bluegecko.sos.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends Fragment {

	private SettingsListener mListener;
	private boolean newMessage = false;

	// Required empty public constructor
	public SettingsFragment() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

		final SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		final SharedPreferences.Editor prefsEditor = prefs.edit();
		final String defaultMessage = prefs.getString(Preferences.DEFAULT_MESSAGE, getString(R.string.defaultMessageText));

		final LinearLayout editMessageLayout = (LinearLayout) rootView.findViewById(R.id.editMessageLayout);

		final EditText messageTextView = (EditText) rootView.findViewById(R.id.messageTextView);
		messageTextView.setText(defaultMessage);
		messageTextView.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				newMessage = !((EditText)v).getText().toString().equals(defaultMessage);
				messageTextView.setBackgroundColor(ContextCompat.getColor(getActivity(),
						newMessage?
						R.color.modified:
						R.color.lightGray));
				return false;
			}
		});
		messageTextView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				newMessage = !messageTextView.getText().toString().equals(defaultMessage);
				messageTextView.setBackgroundColor(ContextCompat.getColor(getActivity(),
						newMessage?
								R.color.modified:
								R.color.lightGray));
			}
			@Override
			public void afterTextChanged(Editable s) {}

		});

		final Button OKbutton = (Button) rootView.findViewById(R.id.OKbutton);
		OKbutton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = messageTextView.getText().toString();
				if (newMessage)
					prefsEditor.putString(Preferences.DEFAULT_MESSAGE, message.isEmpty()? getActivity().getString(R.string.defaultMessageText) : message).apply();
				messageTextView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.lightGray));
				Resources.hideKeyboard(getActivity());
				editMessageLayout.requestFocus();
			}
		});

		Button permissionsButton = (Button) rootView.findViewById(R.id.permissionsButton);
		permissionsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Permissions_fragment().show(getChildFragmentManager(), "");
			}
		});

		final Button backButton = (Button) rootView.findViewById(R.id.backButton);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mListener.onSettings();
			}
		});

		rootView.findViewById(R.id.permissionsLayout)
				.setVisibility((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)?
						View.VISIBLE:
						View.GONE);


		return rootView;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mListener = (SettingsListener) context;
	}

	@Override
	public void onDetach(){
		super.onDetach();
		mListener = null;
	}

	public interface SettingsListener {
		void onSettings();
	}
}
