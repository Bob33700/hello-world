package com.bluegecko.sos.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.bluegecko.sos.R;

/**
 * Created by Bob on 25/08/2016
 */
public class Permissions {
	public static String[] permission_PHN = {Manifest.permission.CALL_PHONE};
	public static String[] permission_SMS = {Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS};
	public static String[] permission_GPS = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
	public static String[] permission_CTX = {Manifest.permission.READ_CONTACTS};
	public static final int SMS_RESULT_CODE = 100;
	public static final int GPS_RESULT_CODE = 101;
	public static final int CTX_RESULT_CODE = 102;
	public static final int PHN_RESULT_CODE = 103;

	public static boolean CheckPermission(Context context, String[] permission){
		boolean result = true;
		for (String p: permission){
			if (ActivityCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED){
				result = false;
				break;
			}
		}
		return result;
	}

	public static void GetPermissions(Context context){
		SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		SharedPreferences.Editor prefsEditor = prefs.edit();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (!CheckPermission(context, permission_SMS)) {
				ActivityCompat.requestPermissions((Activity) context, permission_SMS, SMS_RESULT_CODE);
			} else {
				prefsEditor.putBoolean(Preferences.FIRST_USE, false).apply();
				if (!CheckPermission(context, permission_GPS)) {
					ActivityCompat.requestPermissions((Activity) context, permission_GPS, GPS_RESULT_CODE);
				} else if (!CheckPermission(context, permission_CTX)) {
					ActivityCompat.requestPermissions((Activity) context, permission_CTX, CTX_RESULT_CODE);
				} else if (!CheckPermission(context, permission_PHN)) {
					ActivityCompat.requestPermissions((Activity) context, permission_PHN, PHN_RESULT_CODE);
				}
			}
		}
	}
}
