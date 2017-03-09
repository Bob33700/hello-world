package com.bluegecko.sos.receive;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.Resources;

import java.io.IOException;
import java.io.InputStream;

import static com.bluegecko.sos.utils.Permissions.CheckPermission;
import static com.bluegecko.sos.utils.Permissions.permission_PHN;

public class AlarmActivity extends Activity {

	public static String locationLink;
	//private final String TAG = getClass().getSimpleName();

	private SharedPreferences.Editor prefsEditor;
	private String[] permission_CTX = {Manifest.permission.READ_CONTACTS};
	private TextView senderName, messageTextView;

	private ToneGenerator toneG;
	private Vibrator vibrator;
	private String number = "";
	private boolean watchedToLocation = false;
	private boolean got = false;
	private boolean missed;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		prefsEditor = prefs.edit();

		missed = !Resources.currentlyAvailable(this);

		// inflate alarm layout
		setContentView(R.layout.activity_alarm);

		LinearLayout content = (LinearLayout) findViewById(R.id.content);
		RelativeLayout senderLayout = (RelativeLayout) findViewById(R.id.senderLayout);
		// call-back
		senderLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!got) Answer("GOT");
				if (ActivityCompat.checkSelfPermission(AlarmActivity.this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
					Intent callIntent = new Intent(Intent.ACTION_CALL);
					callIntent.setData(Uri.parse("tel:"+number));
					startActivity(callIntent);
				}
			}
		});
		findViewById(R.id.recallAdvice).setVisibility(CheckPermission(this, permission_PHN) ? View.VISIBLE : View.GONE);

		// stop tone
		LinearLayout stopSoundButton = (LinearLayout) findViewById(R.id.stopSoundButton);
		stopSoundButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!got) Answer("GOT");
			}
		});

		// show sender's identity
		ImageView contactPicture = (ImageView) findViewById(R.id.contactPicture);
		senderName = (TextView) findViewById(R.id.senderName);
		TextView senderNumber = (TextView) findViewById(R.id.senderNumber);
		messageTextView = (TextView) findViewById(R.id.messageTextView);

		// show position and way to go
		ImageButton positionButton = (ImageButton) findViewById(R.id.positionButton);
		positionButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!got) Answer("GOT");
				try {
					Uri uri = Uri.parse(locationLink);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
					watchedToLocation = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		TextView warningText = (TextView) findViewById(R.id.warningText);
		ImageView warningPicture = (ImageView) findViewById(R.id.warningPicture);

		LinearLayout yesButton = (LinearLayout) findViewById(R.id.yesButton);
		// answer YES
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Answer("YES");
				if (!watchedToLocation){
					Uri uri = Uri.parse(locationLink);
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					startActivity(intent);
				}
				finish();
			}
		});

		LinearLayout noButton = (LinearLayout) findViewById(R.id.noButton);
		// answer NO
		noButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Answer("NO");
				finish();
			}
		});

		LinearLayout gotButton = (LinearLayout) findViewById(R.id.gotButton);
		// answer GOT
		gotButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prefsEditor.putBoolean("MissedAlarm", true).apply();
				finish();
			}
		});

		try {
			number = getIntent().getStringExtra("SENDER_NUM");
			String date = getIntent().getStringExtra("DATE");
			String message = getIntent().getStringExtra("MESSAGE");
			boolean hasLocation = getIntent().getBooleanExtra("HAS_LOCATION", false);
			boolean coarseLocation = getIntent().getBooleanExtra("COARSE_LOCATION", false);
			locationLink = getIntent().getStringExtra("LOCATION_LINK");

			positionButton.setVisibility(hasLocation? View.VISIBLE : View.GONE);
			warningText.setVisibility(coarseLocation? View.VISIBLE : View.GONE);
			warningPicture.setVisibility(coarseLocation? View.VISIBLE : View.GONE);

			if (CheckPermission(this, permission_CTX)){
				contactPicture.setImageBitmap(retrieveContactPhoto(this, number));
			} else {
				senderName.setVisibility(View.GONE);
			}
			senderNumber.setText(number);
			((TextView)findViewById(R.id.dateTextView)).setText(date);
			messageTextView.setText(message);

		} catch (Exception e) {
			e.printStackTrace();
		}

		// if available
		if (!missed) {
			// set flag 'alarm running'
			prefsEditor.putBoolean("AlarmActivityRunning", true).apply();
			// start sound
			toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 80);
			toneG.startTone(ToneGenerator.TONE_CDMA_MED_PBX_SSL, 10000); // 10000 is duration in ms
			// start vibration
			int dash = 500;         // Length of a Morse Code "dash" in milliseconds
			int short_gap = 200;    // Length of Gap Between Letters
			long[] pattern = {0, dash, short_gap, dash, short_gap, dash, short_gap};
			vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vibrator.vibrate(pattern, -1);
		// if unavailable
		} else {
			// set flag 'alarm missed'
			prefsEditor.putBoolean("MissedAlarm", true).apply();
			// modify alarm layout
			content.setBackgroundColor(ContextCompat.getColor(this, R.color.lightGray_bg));         // fond gris
			stopSoundButton.setVisibility(View.GONE);                                               // bouton 'stop' masqué
			noButton.setVisibility(View.GONE);                                                      // bouton 'NON' masqué
			yesButton.setVisibility(View.INVISIBLE);                                                // bouton 'YES' masqué
			gotButton.setVisibility(View.VISIBLE);                                                  // bouton 'GOT' visible
			// send 'unavailable answer'
			SmsManager.getDefault().sendTextMessage(number, null, getString(R.string.TAG) + "<U>\n" + getString(R.string.unavailable), null, null);
		}

		registerReceiver(broadcastReceiver, new IntentFilter("SET_MESSAGE"));

	}

	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				messageTextView.setText(intent.getStringExtra("MESSAGE"));
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	};

	private void Answer(String message) {
		if (!missed) {
			toneG.stopTone();
			vibrator.cancel();
			SmsManager.getDefault().sendTextMessage(number, null, getString(R.string.TAG) + message, null, null);
			got = true;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		prefsEditor.putBoolean("AlarmActivityRunning", false).apply();
	}

	public Bitmap retrieveContactPhoto(Context context, String number) {
		ContentResolver contentResolver = context.getContentResolver();
		String contactId = null;
		Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

		String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};

		Cursor cursor =
				contentResolver.query(
						uri,
						projection,
						null,
						null,
						null);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				senderName.setText(cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)));
				contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
			}
			cursor.close();
		}

		Bitmap photo = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.contact_icon);

		try {
			InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
					ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId)));

			if (inputStream != null) {
				photo = BitmapFactory.decodeStream(inputStream);
			}

			assert inputStream != null;
			inputStream.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return photo;
	}
}
