package com.bluegecko.sos.send;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.SmsManager;

import com.bluegecko.sos.R;
import com.bluegecko.sos.database.Recipients;

import java.util.ArrayList;

public class SimulateAnswersActivity extends AsyncTask<Void, Void, Void> {
	private String SOStag;
	private Recipients.Recipient recipient;

	public SimulateAnswersActivity(Context context, Recipients.Recipient recipient){
		SOStag = context.getString(R.string.TAG);
		this.recipient = recipient;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}
	@Override
	protected Void doInBackground(Void... params) {
		try {
			Thread.sleep(5*1000);
			SimulateAnswer(recipient, SOStag+"GOT");
			Thread.sleep(5*1000);
			SimulateAnswer(recipient, SOStag+"YES");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}
	@Override
	protected void onPostExecute(Void result) {
	}

	/**
	 * for debug only
	 */
	private void SimulateAnswer(Recipients.Recipient recipient, String message) {
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager.divideMessage(message);
		smsManager.sendMultipartTextMessage(recipient.getPhone(), null, parts, null, null);
	}
}
