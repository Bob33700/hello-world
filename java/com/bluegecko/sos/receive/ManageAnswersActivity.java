package com.bluegecko.sos.receive;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bluegecko.sos.R;
import com.bluegecko.sos.database.DataProvider;
import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.utils.AlarmReceiver;
import com.bluegecko.sos.utils.Preferences;
import com.bluegecko.sos.utils.Resources;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ManageAnswersActivity extends AppCompatActivity implements android.support.v4.app.LoaderManager.LoaderCallbacks<Cursor>{

	public static Activity answerManager;
	private static final int RECIPIENTS_LOADER_ID = 100;
	private SharedPreferences prefs;
	private SharedPreferences.Editor prefsEditor;

	private recipientAdapter adapter;
	private TextView endOfList;
	private Button cancelButton;
	private DateTimeFormatter fmt = DateTimeFormat.forPattern("HH:mm");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		answerManager = this;

		prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		prefsEditor = prefs.edit();
		prefsEditor.putBoolean(Preferences.MANAGE_ANSWERS_ACTIVITY, true).apply();

		setContentView(R.layout.activity_manage_answers);
		this.getWindow().setBackgroundDrawable(new ColorDrawable(0));

		TextView sent_at_text = (TextView) findViewById(R.id.sent_at_text);
		sent_at_text.setText(String.format(getString(R.string.sent_at), ""));

		TextView sent_at_time = (TextView) findViewById(R.id.sent_at_time);
		sent_at_time.setText(fmt.print(DateTime.now()));

		ListView answersList = (ListView) findViewById(R.id.answersLayout);
		adapter = new recipientAdapter(
				this,
				R.layout.list_recipient,                            // row layout
				null,                                               // cursor
				new String[]{Recipients.RECIPIENTS_ID},             // fields list
				new int[]{R.id.id}                                  // views list
		);
		answersList.setAdapter(adapter);

		endOfList = (TextView) findViewById(R.id.endOfList);

		cancelButton = (Button)findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				prefsEditor.putBoolean(Preferences.STOP_SOS, true).apply();
				finish();
			}
		});

		(findViewById(R.id.quitButton)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

	@Override
	public void onResume(){
		super.onResume();
		getSupportLoaderManager().initLoader(RECIPIENTS_LOADER_ID, null, this);
		endOfList.setVisibility(prefs.getBoolean(Preferences.END_OF_LIST, false)? View.VISIBLE: View.GONE);
		cancelButton.setVisibility(prefs.getBoolean(Preferences.END_OF_LIST, false)? View.GONE: View.VISIBLE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		prefsEditor.putBoolean(Preferences.MANAGE_ANSWERS_ACTIVITY, false).apply();
	}


	private class recipientAdapter extends SimpleCursorAdapter {
		public recipientAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
			super(context, layout, cursor, from, to, 0);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return LayoutInflater.from(context).inflate(R.layout.list_answer, parent, false);
		}
		@Override
		public void bindView(View view, Context context, Cursor cursor){
			super.bindView(view, context, cursor);
			Recipients.Recipient recipient = Recipients.cursorToRow(cursor);

			NumberFormat nf = new DecimalFormat("#0");
			((TextView)view.findViewById(R.id.rankTextView)).setText(nf.format(recipient.getRank()+1));
			((TextView)view.findViewById(R.id.nameTextView)).setText(recipient.getName());
			((TextView)view.findViewById(R.id.phoneTextView)).setText(Resources.formatPhoneNumber(recipient.getPhone(), false));

			ImageView answerPicture = (ImageView)view.findViewById(R.id.answerPicture);
			TextView answerTextView = (TextView)view.findViewById(R.id.answerTextView);
			switch (recipient.getStatus()) {       //-1:indéterminé; 0:indisponible; 1:vu; 2:refus; 3:j'arrive;
				case 0:
					answerPicture.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.answer_out));
					answerTextView.setText(context.getString(R.string.answer_unavailable));
					break;
				case 1:
					answerPicture.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.got_it));
					answerTextView.setText(context.getString(R.string.answer_got));
					break;
				case 2:
					answerPicture.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.answer_no));
					answerTextView.setText(context.getString(R.string.answer_no));
					break;
				case 3:
					answerPicture.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.answer_yes));
					answerTextView.setText(context.getString(R.string.answer_yes));
					break;
				default:
					answerPicture.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.answer_wait));
					answerTextView.setText(context.getString(R.string.answer_wait));
			}
		}
	}

	public static void CancelAnswerTimeOut(Context context){
		Intent intent = new Intent(context, AlarmReceiver.class);
		int type = AlarmReceiver.PENDING_ANSWER_MANAGER;
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, type, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmIntent.cancel();
	}

	public static void SetAnswerTimeOut(Context context, Recipients.Recipient recipient, int delay){
		AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmReceiver.class);
		int type = AlarmReceiver.PENDING_ANSWER_MANAGER;
		intent.putExtra("TYPE", type);
		intent.putExtra("RANK", recipient.getRank());
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, type, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay * 1000, alarmIntent);
		} else {
			alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay * 1000, alarmIntent);
		}

	}


	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = Uri.parse(DataProvider.RECIPIENTS_URL);
		CursorLoader c;
		switch (id) {
			case RECIPIENTS_LOADER_ID:
				// retrieve currently called recipients
				c = new CursorLoader(this, uri, null, Recipients.RECIPIENTS_CALLING + "=1", null, null);
				break;
			default:
				c = null;
		}
		return c;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		endOfList.setVisibility(prefs.getBoolean(Preferences.END_OF_LIST, false)? View.VISIBLE: View.GONE);
		cancelButton.setVisibility(prefs.getBoolean(Preferences.END_OF_LIST, false)? View.GONE: View.VISIBLE);
		adapter.swapCursor(data);
		//ShowRecipient();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

}
