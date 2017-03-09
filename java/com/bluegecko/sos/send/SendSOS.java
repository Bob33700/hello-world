package com.bluegecko.sos.send;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.bluegecko.sos.R;
import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.receive.ManageAnswersActivity;
import com.bluegecko.sos.utils.MyLocation;
import com.bluegecko.sos.utils.Preferences;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

public class SendSOS extends AsyncTask<Void, Void, Void> {
	public static Recipients.Recipient currentRecipient;
	private static Recipients recipients;
	private Context mContext;
	private static MyLocation locator;
	private static Location location;
	private static String ok;
	private static MyLocation.LocationType requiredLocationType;

	public static String LOCATION_LINK = "http://maps.google.com/maps?daddr=%s,%s";
	public static SharedPreferences prefs;
	String SOStag;
	private int firstRank;

	private static boolean sendPosition, isCoarse, toBeRefined;


	public SendSOS(Context context, int firstRank){
		mContext = context;
		prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		SOStag = mContext.getString(R.string.TAG);
		recipients = new Recipients(mContext);
		this.firstRank = firstRank;
		// if new SOS => set all status to -1   and    'stop SOS' flag to false
		if (firstRank==0){
			prefs.edit().putBoolean(Preferences.STOP_SOS, false).apply();
			prefs.edit().putBoolean(Preferences.END_OF_LIST, false).apply();
			for (Recipients.Recipient recipient: recipients.getAllRows()) {
				recipient.setCalling(false);
				recipient.setStatus(-1);
				recipients.updateRow(recipient);
			}
		}
		// if not ManageAnswersActivity is running => turn it on
		if (!prefs.getBoolean(Preferences.MANAGE_ANSWERS_ACTIVITY, false)) {
			Intent manageAnswers = new Intent(context, ManageAnswersActivity.class);
			manageAnswers.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			context.startActivity(manageAnswers);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		locator = MyLocation.getInstance(mContext);
		locator.Start();
	}
	@Override
	protected Void doInBackground(Void... v) {
		// execute only if 'stopSOS' flag is false
		if (!prefs.getBoolean(Preferences.STOP_SOS, false)){
			// send SOS to the first recipient with a phone number (the list is sorted by rank asc)
			//Recipients recipients = new Recipients(mContext);
			Recipients.Recipient recipient;
			// look for 1st recipient with a phone number
			for (int rank=firstRank; rank<=recipients.getMaxRank(); rank++){
				recipient = recipients.getRecipientByRank(rank);
				if (recipient.getPhone()!=null && !recipient.getPhone().isEmpty()) {
					Send(mContext, recipient);
					currentRecipient = recipient;
					break;
				} else {
					currentRecipient = null;
					recipient.setStatus(0);         // indisponible
					recipients.updateRow(recipient);
				}
				if(recipient.getRank()==recipients.getMaxRank()) {
					prefs.edit().putBoolean(Preferences.END_OF_LIST, true).apply();
					recipients.updateRow(recipient);
				}

			}
		}

		return null;
	}
	@Override
	protected void onPostExecute(Void result) {
		// unregister location listener
		locator.Stop();
		// create a pending intent (answer timeout)
		if (currentRecipient!=null) {
			ManageAnswersActivity.SetAnswerTimeOut(mContext, currentRecipient, 60);                 // 60s
		}
/**
 * for debug only
 */
//		new SimulateAnswersActivity(mContext, currentRecipient).execute();
/****/


	}

	public static void Send(final Context context, Recipients.Recipient recipient) {

		ok = context.getString(R.string.SMSsentOK);
		String ko = context.getString(R.string.SMSsentKO);

		 sendPosition = recipient.getPosition();

		// vibrate
		Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		vibrator.vibrate(500);

		// si la géolocalisation est requise
		if (sendPosition){
			// vérifier que les systèmes sont OK pour obtenir la géolocalisation
			if (!locator.isGPS_ON() && !locator.isNetwork_ON()){
				// ask user to turn location service ON
				ShowToast(context,  context.getString(R.string.gps_dialog), true);
				ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 50);
				toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000); // 1000 is duration in ms
				Intent gpsOptionsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				context.startActivity(gpsOptionsIntent);
				toBeRefined = true;
			} else {
				toBeRefined = false;
			}
			/**
			 * find type of location witch is required
			 * if [GPS is ON]  => FINE location required
			 * If [GPS is OFF] => COARSE location required */
			requiredLocationType = locator.isGPS_ON()? MyLocation.LocationType.FINE: MyLocation.LocationType.COARSE;
			// get location
			location = locator.GetBestLastKnownLocation();

			// if location is not available => wait for 1st location (120s)
			if (location==null){
				ShowToast(context, context.getString(R.string.gps_wait), false);
				location = locator.WaitForLocation(12, null);      // wait n * 10s for 1st location (null means '1st location')
			}
			// if location is now available
			if (location!=null){
				//if FINE location required
				if (requiredLocationType==MyLocation.LocationType.FINE) {
					isCoarse = toBeRefined;
					// if systems were ON => location is good
					if (!toBeRefined){
						Send1SMS(context, recipient);    // standard message
					// if systems were OFF and just turned ON => location has to be refined
					} else {
						Send2SMS(context, recipient);
					}
				// if COARSE location required
				} else {
					isCoarse = true;
					Send1SMS(context, recipient);       // standard message
				}
			// if still unavailable => show failure message
			} else {
				ShowToast(context, ko, true);
				sendPosition = false;
				isCoarse = false;
				toBeRefined = false;
				Send1SMS(context, recipient);                   // message standard sans localisation
			}
		// si la géolocalisation n'est pas demandée
		} else {
			isCoarse = false;
			toBeRefined = false;
			Send1SMS(context, recipient);                   // message standard sans localisation
		}

		// update recipient's status
		recipient.setCalling(true);
		recipients.updateRow(recipient);
		if (recipient.getRank()==recipients.getMaxRank()){
			prefs.edit().putBoolean(Preferences.END_OF_LIST, true).apply();
		}
}

	/**
	 * Send standard message : only 1 SMS
	 * @param context: context
	 * @param recipient: SMS recipient
	 */
	private static void Send1SMS(Context context, Recipients.Recipient recipient){
		SendSMS(recipient.getPhone(), MakeMessage(context, recipient, location, 0));
		ShowToast(context, ok, true);
	}

	/**
	 * Send 2 SMS :
	 *      1st: with aproximated location to alert recipient
	 *      2nd: with refined location
	 * @param context: context
	 * @param recipient: SMS recipient
	 */
	private static void Send2SMS(final Context context, final Recipients.Recipient recipient){
		new Runnable(){
			@Override
			public void run() {
				int accuracy = (int) location.getAccuracy();
				SendSMS(recipient.getPhone(), MakeMessage(context, recipient, location, 1));                    // first message
				ShowToast(context, ok + " #1", true);
				Location newLocation = locator.WaitForLocation(12, location);                   // wait for better location (120s)
				//ShowToast(context, "new location "+location.toString(), true);
				if (newLocation!=null && !locator.isSameLocation(newLocation,location)) {
					SendSMS(recipient.getPhone(), MakeMessage(context, recipient, newLocation, 2));             // second message
					ShowToast(context, ok + " #2", true);
				} else {
					if (!Double.isNaN(accuracy))
						ShowToast(context, String.format(context.getString(R.string.gps_accuracy), accuracy), false);
				}
			}
		}.run();
	}

	public static void ShowToast(final Context context, final String message,final boolean length_long){
		try {
			((Activity) context).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(context, message, length_long ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private static String MakeMessage(Context context, Recipients.Recipient recipient, Location loc, int type){
		NumberFormat nf = new DecimalFormat("0.0000000");
		String message;
		// WITH location
		if (recipient.getPosition() && loc!=null) {
			// create location link
			String latitude = nf.format(loc.getLatitude()).replace(",", ".");
			String longitude = nf.format(loc.getLongitude()).replace(",", ".");
			String locationLink = String.format(LOCATION_LINK, latitude, longitude);
			String tag = "<";
			tag += (type==0 ||type==1)? "I" : "R>";
			if (type==0||type==1) {
				if (sendPosition){
					tag+="l";
					if (isCoarse) tag+="c";
					if (toBeRefined) tag+="r";
				}
				tag+=">";
			}

			message = context.getString(R.string.TAG) + tag +"\n";
			switch (type){
				// 1st message (aproximated location)
				case 1:
					if (recipient.getMessage()!=null && !recipient.getMessage().isEmpty()){
						message += recipient.getMessage();
					} else {
						message += (prefs.getString(Preferences.DEFAULT_MESSAGE,"").isEmpty() ? context.getString(R.string.defaultMessageText) : prefs.getString(Preferences.DEFAULT_MESSAGE,""));
					}
					message += context.getString(R.string.positionMessageText);
					message += locationLink;
					message+=context.getString(R.string.SMS_CoarseLocation);
					message+=context.getString(R.string.SMS_LocationToImprove);
					break;
				// 2nd message (refined location)
				case 2:
					message += String.format(
							context.getString(R.string.SMS_LocationImproved),
							locationLink);
					break;
				// standard message (fine location required and provider = GPS
				default:
					if (recipient.getMessage()!=null && !recipient.getMessage().isEmpty()){
						message += recipient.getMessage();
					} else {
						message += (prefs.getString(Preferences.DEFAULT_MESSAGE,"").isEmpty() ? context.getString(R.string.defaultMessageText) : prefs.getString(Preferences.DEFAULT_MESSAGE,""));
					}
					message += context.getString(R.string.positionMessageText);
					message += locationLink;
					if (requiredLocationType==MyLocation.LocationType.COARSE)
						message+=context.getString(R.string.SMS_CoarseLocation);
			}
		// WITHOUT location
		} else {
			String tag = "<I>";
			message = context.getString(R.string.TAG) + tag + "\n"
					+ (prefs.getString(Preferences.DEFAULT_MESSAGE,"").isEmpty() ? context.getString(R.string.defaultMessageText) : prefs.getString(Preferences.DEFAULT_MESSAGE,""));
		}
		// return whole message text + signature
		return message+context.getString(R.string.SMS_Signature);
	}


	private static void SendSMS (String number, String message){
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> parts = smsManager.divideMessage(message);
		smsManager.sendMultipartTextMessage(number, null, parts, null, null);

	}
}
