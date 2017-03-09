package com.bluegecko.sos.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;

import com.bluegecko.sos.R;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/**
 * Created by Bob on 25/08/2016
 */
public class Resources {
	public static void hideKeyboard(Activity activity) {
		InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		View view = activity.getCurrentFocus();
		if (view == null) {
			view = new View(activity);
		}
		imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static String formatPhoneNumber(String number, boolean international){
		String formattedPhoneNumber = null;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			formattedPhoneNumber = PhoneNumberUtils.normalizeNumber(number);
			if (international)
				formattedPhoneNumber = PhoneNumberUtils.formatNumberToE164(formattedPhoneNumber, Locale.getDefault().getCountry());
			else
				formattedPhoneNumber = PhoneNumberUtils.formatNumber(formattedPhoneNumber, Locale.getDefault().getCountry());
		} else {
			if (!number.isEmpty())
//			formattedPhoneNumber = PhoneNumberUtils.formatNumber(number); //Deprecated method
				try {
					PhoneNumberUtil instance = PhoneNumberUtil.getInstance();
					Phonenumber.PhoneNumber phoneNumber = instance.parse(number, Locale.getDefault().getCountry());
					formattedPhoneNumber = instance.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);

				} catch (NumberParseException e) {
					formattedPhoneNumber = null;
				}
		}
		return formattedPhoneNumber;
	}

	public static boolean isValidPhoneNumber(String num){
		return formatPhoneNumber(num,true)!=null;
	}

	public static boolean currentlyAvailable(Context context){
		DateTime unavailable_until;
		DateTimeFormatter dtf0 = DateTimeFormat.forPattern("yyyyMMddHHmm");
		SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

		if (prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false))
			return false;

		String until = prefs.getString(Preferences.UNAVAILABLE_UNTIL,"");
		if (until.isEmpty())
			unavailable_until = DateTime.now().minusMillis(1);
		else
			unavailable_until = dtf0.parseDateTime(until);
		return  DateTime.now().isAfter(unavailable_until);
	}

	public static ValueAnimator ViewAlphaAnimation(final View[] views, float from, float to, int duration){
		ValueAnimator anim = ValueAnimator.ofFloat(from, to);
		anim.setDuration(duration); // milliseconds
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {for(View v: views) v.setAlpha((float) animator.getAnimatedValue());}
		});
		return anim;
	}
	public static ValueAnimator ViewAlphaAnimation(final View view, float from, float to, int duration){
		ValueAnimator anim = ValueAnimator.ofFloat(from, to);
		anim.setDuration(duration); // milliseconds
		anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				view.setAlpha((float) animator.getAnimatedValue());
			}
		});
		return anim;
	}

	public static TranslateAnimation ViewTranslateAnimation(int[] from, int[] to, int duration, Interpolator interpolator, boolean fillAfter){
		TranslateAnimation anim = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, to[0]-from[0],
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, to[1]-from[1]);
		anim.setInterpolator(interpolator);
		anim.setDuration(duration);
		anim.setFillAfter(fillAfter);
		return anim;
	}
	public static void ResetTranslateAnimation(View v){
		TranslateAnimation anim = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.ABSOLUTE, 0);
		anim.setInterpolator(new AccelerateDecelerateInterpolator());
		anim.setDuration(0);
		anim.setFillAfter(true);
		v.startAnimation(anim);
	}
}
