package com.bluegecko.sos.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.bluegecko.sos.R;
import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.send.SendSOS;
import com.bluegecko.sos.utils.Preferences;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.bluegecko.sos.utils.Resources.formatPhoneNumber;


public class IncomingSms extends BroadcastReceiver {
	private final String TAG = getClass().getSimpleName();

	private boolean isFist, hasFirstLocation, isCoarse, isToBeRefined, isRefined, isUnavailable;
	@Override
	public void onReceive(Context context, Intent intent) {
		final Bundle bundle = intent.getExtras();
		String message = "";
		if (bundle != null) {
			final Object[] pdusObj = (Object[]) bundle.get("pdus");
			if (pdusObj != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				assert pdus != null;
				final SmsMessage[] messages = new SmsMessage[pdus.length];
				Log.d(TAG, String.format("message count = %s", messages.length));
				for (int i = 0; i < pdus.length; i++) {
					//messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					messages[i] = getIncomingMessage(pdus[i], bundle);
					message += messages[i].getDisplayMessageBody();
				} // end for loop
				SmsMessage currentMessage = getIncomingMessage(pdus[0], bundle);
				String senderNum = currentMessage.getDisplayOriginatingAddress();
				String date = millisToDate(currentMessage.getTimestampMillis());
				// if it is a message from SOS app
				if (message.startsWith(context.getString(R.string.TAG))) {
					message = RemoveTag(context,message);
					message = RemoveSignature(context, message);
					SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

					// if it is a 'need help!' or a 'refined position' SMS  (we ar the recipient of a SOS)
					if (isFist || isRefined) {
						// if not alarm is currently running => turn it on
						if (!prefs.getBoolean("AlarmActivityRunning", false)) {
							Intent alarmActivity = new Intent(context, AlarmActivity.class);
							alarmActivity.putExtra("SENDER_NUM", senderNum);
							alarmActivity.putExtra("DATE", date);
							// if 'refined position' arrives 1st : message is empty
							if (isRefined) {
								alarmActivity.putExtra("TYPE", "refined");
								alarmActivity.putExtra("MESSAGE", "");
								alarmActivity.putExtra("HAS_LOCATION", true);
								alarmActivity.putExtra("TO_REFINE", false);
								alarmActivity.putExtra("COARSE_LOCATION", false);
								alarmActivity.putExtra("LOCATION_LINK", message.substring(message.indexOf("http")));
							} else {
								alarmActivity.putExtra("TYPE", "first");
								alarmActivity.putExtra("HAS_LOCATION", hasFirstLocation);
								alarmActivity.putExtra("TO_REFINE", isToBeRefined);
								alarmActivity.putExtra("COARSE_LOCATION", isCoarse);
								if (hasFirstLocation) {
									if (isCoarse) {
										// remove 'SMS_CoarseLocation" sentence (& 'to be refined')
										message = message.substring(0, message.indexOf(context.getString(R.string.SMS_CoarseLocation).substring(0, 10)));
									}
									int locationOffset = message.indexOf("http");
									alarmActivity.putExtra("LOCATION_LINK", message.substring(locationOffset));
									message = message.substring(0, message.indexOf(context.getString(R.string.positionMessageText).substring(0, 10)));
								}
								alarmActivity.putExtra("MESSAGE", message);
							}
							alarmActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							context.startActivity(alarmActivity);
						} else {
							// if '1st message' arrives after 'refined position' : set message
							if (isFist) {
								Intent setMessageIntent = new Intent("SET_MESSAGE");
								setMessageIntent.putExtra("MESSAGE", message);
								context.sendBroadcast(setMessageIntent);
								//AlarmActivity.alarmActivity.SetMessage(message);
								// if it is a 'refine position' message => replace location link in AlarmActivity
							} else {
								String newLocation = message.substring(message.indexOf("http"));                   // tronquer tout avant le début du lien
								AlarmActivity.locationLink = newLocation;
								Log.d(TAG, "new location: " + newLocation);
							}
						}

					// if it is an answer  (we are the sender of a SOS)
					} else {
						// get current recipient
						Recipients recipients = new Recipients(context);
						Recipients.Recipient recipient = recipients.getCurrentRecipientByNum(formatPhoneNumber(senderNum, true));
						int status = -1;
						if (recipient != null) {
							// set status & update
							if (isUnavailable)                  status = 0;
							else if (message.endsWith("GOT"))   status = 1;
							else if (message.endsWith("NO"))    status = 2;
							else if (message.endsWith("YES"))   status = 3;
							recipient.setStatus(status);
							recipients.updateRow(recipient);
							if (status==0 || status==2){                                // if unavailable or answer NO
								new SendSOS(context,recipient.getRank()+1).execute();   //      => call next recipient
							} else {
								// if not ManageAnswersActivity is running => turn it on
								if (!prefs.getBoolean(Preferences.MANAGE_ANSWERS_ACTIVITY, false)) {
									Intent manageAnswers = new Intent(context, ManageAnswersActivity.class);
									manageAnswers.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
									context.startActivity(manageAnswers);
								}
							}
						}
					}
				}// it is not an [SOS] message
			}// message is empty
		} // bundle is null
	}

	private String RemoveTag(Context context, String message){
		message = message.substring(context.getString(R.string.TAG).length());      // remove TAG
		String tag;
		// what kind of message ?
		if (message.startsWith("<")){
			tag = message.substring(1,message.indexOf(">"));
			isFist = tag.startsWith("I");
			isRefined = tag.startsWith("R");
			isUnavailable = tag.startsWith("U");
			hasFirstLocation = (isFist && tag.contains("l"));
			isCoarse = (isFist && tag.contains("c"));
			isToBeRefined = (isFist && tag.contains("r"));
			message = message.substring(tag.length()+2);
		}
		while(message.startsWith("\n")){
			message = message.substring(1);
		}
		return message;
	}

	private String RemoveSignature(Context context, String message){
		int signatureOffset = message.indexOf(context.getString(R.string.SMS_Signature).substring(0, 20));
		if (signatureOffset != -1)
			return message.substring(0, signatureOffset);
		else
			return message;
	}

	public static String millisToDate(long milliseconds) {
		DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
		DateTime dateTime = new DateTime(Long.valueOf(milliseconds));
		return fmt.print(dateTime);
	}

	@SuppressWarnings("deprecation")
	private SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
		SmsMessage currentSMS;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String format = bundle.getString("format");
			currentSMS = SmsMessage.createFromPdu((byte[]) aObject, format);
		} else {
			currentSMS = SmsMessage.createFromPdu((byte[]) aObject);
		}
		return currentSMS;
	}
}
