package com.bluegecko.sos.utils;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.bluegecko.sos.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class Permissions_fragment extends DialogFragment {

	private PermissionsListener mListener;
	private Context mContext;
	private boolean firstUse;
	private TextView permissionNotice;

	// Required empty public constructor
	public Permissions_fragment() {}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Inflate the layout for this fragment
		final View rootView = inflater.inflate(R.layout.fragment_permissions, container, false);

		// retrieve display dimensions
		Rect displayRectangle = new Rect();
		Window window = getActivity().getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		// adjust dialog width
		rootView.setMinimumWidth((int)(displayRectangle.width() * 0.9f));


		SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		firstUse =  prefs.getBoolean(Preferences.FIRST_USE, false);
		permissionNotice = (TextView) rootView.findViewById(R.id.permissionsNotice);
		permissionNotice.setVisibility(firstUse ? View.GONE : View.VISIBLE);

		Button button = (Button) rootView.findViewById(R.id.permissionsButton);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				permissionNotice.setVisibility(firstUse ? View.GONE : View.VISIBLE);
				if (firstUse){
					mListener.onPermissionsOK();
					mListener.onPermissionsHide();
					dismiss();
				} else {
					final Intent i = new Intent();
					i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
					i.addCategory(Intent.CATEGORY_DEFAULT);
					i.setData(Uri.parse("package:" + mContext.getPackageName()));
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					mContext.startActivity(i);
					mListener.onPermissionsHide();
					dismiss();
				}
			}
		});
		return rootView;
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mContext = context;
		mListener = (PermissionsListener) context;
	}

	@Override
	public void onDetach(){
		super.onDetach();
		mListener = null;
	}

	public interface PermissionsListener {
		void onPermissionsOK();
		void onPermissionsHide();
	}
}
