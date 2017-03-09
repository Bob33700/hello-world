package com.bluegecko.sos.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.send.SendSOS;

/**
 * Created by Bob on 07/09/2016
 */
public class PhoneCall extends BroadcastReceiver {

	private final String TAG = getClass().getSimpleName();

	private Context mContext;
	private String incoming_nr;
	private int prev_state;
	private Recipients.Recipient recipient;
	private boolean suspended = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		TelephonyManager telephony = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); //TelephonyManager object
		MyPhoneStateListener customPhoneListener = new MyPhoneStateListener();
		telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);                        //Register our listener with TelephonyManager

		Bundle bundle = intent.getExtras();
		String phoneNr= bundle.getString("incoming_number");
		Log.v(TAG, "phoneNr: "+phoneNr);
		mContext=context;
	}

	/* Custom PhoneStateListener */
	public class MyPhoneStateListener extends PhoneStateListener {
		private static final String TAG = "MyPhoneStateListener";

		@Override
		public void onCallStateChanged(int state, String incomingNumber){

			if(incomingNumber!=null && incomingNumber.length()>0) incoming_nr=incomingNumber;
			if (SendSOS.currentRecipient!=null) recipient=SendSOS.currentRecipient;

			if (incomingNumber!=null && recipient!=null){
				switch(state){
					case TelephonyManager.CALL_STATE_RINGING:                                       // while ringing : nothing
						Log.d(TAG, "CALL_STATE_RINGING");
						prev_state=state;
						break;

					case TelephonyManager.CALL_STATE_OFFHOOK:                                       // if I answer
						Log.d(TAG, "CALL_STATE_OFFHOOK");
						prev_state=state;
						if (recipient.getPhone().equals(incomingNumber)){                           // if phone call from current recipient
							ManageAnswersActivity.CancelAnswerTimeOut(mContext);                    //      => suspend answer timeout
							suspended = true;
						}
						break;

					case TelephonyManager.CALL_STATE_IDLE:
						Log.d(TAG, "CALL_STATE_IDLE==>"+incoming_nr);
						if((prev_state==TelephonyManager.CALL_STATE_OFFHOOK)){                      // at end of an incomming call
							prev_state=state;
							if (suspended) {                                                        // if it was a call from current recipient
								ManageAnswersActivity.SetAnswerTimeOut(mContext, recipient, 30);    //      => start new answer timeout : 30s
								suspended = false;
							}
						}
						if((prev_state==TelephonyManager.CALL_STATE_RINGING)){                      // if I missed a call : nothing
							prev_state=state;
						}
						break;
				}
			}
		}
	}
}
