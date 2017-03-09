package com.bluegecko.sos.prepare;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.support.annotation.ColorInt;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import com.bluegecko.sos.R;
import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.utils.Permissions;
import com.bluegecko.sos.utils.Preferences;
import com.bluegecko.sos.utils.Resources;

import static com.bluegecko.sos.utils.Permissions.CheckPermission;
import static com.bluegecko.sos.utils.Resources.formatPhoneNumber;

import android.support.design.widget.TextInputLayout;

import java.lang.reflect.Field;

/**
 * A simple {@link Fragment} subclass.
 */
public class EditRecipientFragment extends DialogFragment {

	private EditRecipientListener mListener;

	private final String TAG = getClass().getSimpleName();
	public static final int REQUEST_CONTACT_NUMBER = 1;
	private Recipients.Recipient recipient;

	private TextInputLayout phoneHolder;
	private EditText phone ;
	private EditText name;
	private CheckBox positionCB;
	private EditText message;
	private boolean editRecipient = false;

	// Required empty public constructor
	public EditRecipientFragment() {}

	private Button validButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootVIew = inflater.inflate(R.layout.fragment_edit_recipient, container, false);
		SharedPreferences prefs = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

		ImageButton pickContactButton = (ImageButton) rootVIew.findViewById(R.id.pickContact);
		Button cancelButton = (Button) rootVIew.findViewById(R.id.cancelButton);
		validButton = (Button) rootVIew.findViewById(R.id.validButton);

		phoneHolder = (TextInputLayout) rootVIew.findViewById(R.id.phoneHolder);
		phone = (EditText) rootVIew.findViewById(R.id.phoneTextView);
		name = (EditText) rootVIew.findViewById(R.id.nameTextView);
		positionCB = (CheckBox) rootVIew.findViewById(R.id.positionCheckbox);
		message = (EditText) rootVIew.findViewById(R.id.messageTextView);

		phone.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus){
					setValidButton();
				}
			}
		});
		phone.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				setValidButton();
				return false;
			}
		});

		pickContactButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PickContact();
			}
		});
		pickContactButton.setVisibility(CheckPermission(getActivity(), Permissions.permission_CTX)? View.VISIBLE : View.GONE);

		positionCB.setChecked(CheckPermission(getActivity(), Permissions.permission_GPS)
							&& prefs.getBoolean(Preferences.SEND_LOCATION, true));

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		validButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Recipients recipients = new Recipients(getActivity());
				if (editRecipient){
					recipient.setPhone(phone.getText().toString());
					recipient.setName(name.getText().toString());
					recipient.setMessage(message.getText().toString());
					recipient.setPosition(positionCB.isChecked());
					recipients.updateRow(recipient);
					mListener.onEditRecipient("edit", recipient);
				} else {
					recipient = new Recipients.Recipient();
					recipient.setPhone(phone.getText().toString());
					recipient.setName(name.getText().toString());
					recipient.setMessage(message.getText().toString());
					recipient.setPosition(positionCB.isChecked());
					recipients.addRecipient(recipient);
					mListener.onEditRecipient("add", recipient);
				}
				dismiss();
			}
		});

		positionCB.setVisibility(CheckPermission(getActivity(), Permissions.permission_GPS)? View.VISIBLE : View.GONE);

		Bundle args = getArguments();
		if (args!=null) {
			try {
				final long id = args.getLong("ID");
				recipient = new Recipients(getActivity()).getRecipientById(id);
				phone.setText(recipient.getPhone());
				name.setText(recipient.getName());
				message.setText(recipient.getMessage());
				positionCB.setChecked(recipient.getPosition() && CheckPermission(getActivity(), Permissions.permission_GPS));
				editRecipient = true;
			} catch (Exception e){
				Log.d(TAG, e.toString());
			}
		} else {
			PickContact();
		}


		return rootVIew;
	}

	@Override
	public void onResume(){
		super.onResume();
		setValidButton();
	}

	private void setValidButton(){
		//phoneHolder.setSelected(!Resources.isValidPhoneNumber(phone.getText().toString()));
		//phoneHolder.getEditText().setHintTextColor(ContextCompat.getColor(getActivity(), Resources.isValidPhoneNumber(phone.getText().toString())? R.color.lightGray: R.color.redDark));
		setInputTextLayoutColor(
				phoneHolder,
				ContextCompat.getColor(getActivity(), Resources.isValidPhoneNumber(phone.getText().toString())? R.color.lightGray: R.color.colorAccent)
		);
		//phoneHolder.setDrawingCacheBackgroundColor(ContextCompat.getColor(getActivity(), Resources.isValidPhoneNumber(phone.getText().toString())? R.color.lightGray: R.color.redDark));
		//phone.setHintTextColor(ContextCompat.getColor(getActivity(), Resources.isValidPhoneNumber(phone.getText().toString())? R.color.lightGray: R.color.redDark));
		validButton.setEnabled(Resources.isValidPhoneNumber(phone.getText().toString()));
		validButton.setAlpha(Resources.isValidPhoneNumber(phone.getText().toString())? 1f: .25f);
	}
	public static void setInputTextLayoutColor(TextInputLayout til, @ColorInt int color) {
		try {
			Field fDefaultTextColor = TextInputLayout.class.getDeclaredField("mDefaultTextColor");
			fDefaultTextColor.setAccessible(true);
			fDefaultTextColor.set(til, new ColorStateList(new int[][]{{0}}, new int[]{ color }));

			Field fFocusedTextColor = TextInputLayout.class.getDeclaredField("mFocusedTextColor");
			fFocusedTextColor.setAccessible(true);
			fFocusedTextColor.set(til, new ColorStateList(new int[][]{{0}}, new int[]{ color }));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mListener = (EditRecipientListener) context;
	}

	@Override
	public void onDetach(){
		super.onDetach();
		mListener = null;
	}

	public interface EditRecipientListener {
		void onEditRecipient(String action, Recipients.Recipient recipient);
	}



	private void PickContact(){
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		getActivity().startActivityForResult(intent, REQUEST_CONTACT_NUMBER);
	}

	public void setContactData(Context context, String contactId){
		// find contact
		Cursor cursor = context.getContentResolver().query(
				Contacts.CONTENT_URI,
				null,
				Contacts._ID + "=?",
				new String[]{contactId},
				null
		);
		//if found
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				name.setText(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)));
				//retrieve mobile phone
				Cursor phones = context.getContentResolver().query(
						Phone.CONTENT_URI, null,
						Phone.CONTACT_ID + "=? and " + Phone.TYPE + "=" + Phone.TYPE_MOBILE,
						new String[]{contactId},
						null);
				if (phones!=null && phones.getCount()>0){
					phones.moveToFirst();
					phone.setText(formatPhoneNumber(phones.getString(phones.getColumnIndex(Phone.NUMBER)), false));
				}
				if (phones!=null) phones.close();
			}
			cursor.close();
		}

	}

}
