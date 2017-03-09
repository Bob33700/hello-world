package com.bluegecko.sos.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.bluegecko.sos.R;
import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.receive.ManageAnswersActivity;
import com.bluegecko.sos.send.SendSOS;

public class AlarmReceiver extends WakefulBroadcastReceiver {

	public final static int PENDING_AVAILABILITY = 1;
	public final static int PENDING_ANSWER_MANAGER = 2;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        try {
	        int type = intent.getIntExtra("TYPE", -1);
	        switch (type){
		        case PENDING_AVAILABILITY:
			        // jouer le son de notification
			        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			        Ringtone r = RingtoneManager.getRingtone(context, notification);
			        r.play();
			        // vibrer 3 fois
			        int dash = 500;         // Length of a Morse Code "dash" in milliseconds
			        int short_gap = 200;    // Length of Gap Between Letters
			        long[] pattern = {0, dash, short_gap, dash, short_gap, dash, short_gap};
			        v.vibrate(pattern, -1);
			        // rendre disponible et mettre l'image du bouton à jour
			        context.sendBroadcast(new Intent("NOW_AVAILABLE"));
			        break;
		        case PENDING_ANSWER_MANAGER:
			        int rank = intent.getIntExtra("RANK", -1);
			        Recipients recipients = new Recipients(context);
			        Recipients.Recipient recipient = recipients.getRecipientByRank(rank);
			        if (recipient!=null){                                               // if recipient found
				        int status = recipient.getStatus();
				        if (status==-1 || status==1){                                   // if his status is 'waiting for answer' or just 'got'
					        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
					        rank++;
					        if (rank <recipients.size)
						        new SendSOS(context, rank).execute();                   //  => skip to next recipient
					        else {
						        // if not ManageAnswersActivity is running => turn it on
						        if (!prefs.getBoolean(Preferences.MANAGE_ANSWERS_ACTIVITY, false)) {
							        Intent manageAnswers = new Intent(context, ManageAnswersActivity.class);
							        manageAnswers.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							        context.startActivity(manageAnswers);
						        }
						        recipient.setStatus(0);
						        recipients.updateRow(recipient);
					        }
				        }
			        }
			        break;
		        default:
	        }
         } catch (Exception e) {
            Log.d("AlarmReceiver:", "onReceive error");
        }
    }


}
