package com.bluegecko.sos.receive;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.AlarmReceiver;
import com.bluegecko.sos.utils.Preferences;
import com.bluegecko.sos.utils.Resources;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * A simple {@link Fragment} subclass.
 */
public class AvailabilityFragment extends DialogFragment {
	// Required empty public constructor
	public AvailabilityFragment() {}

	private AvailabilityListener mListener;
	private SharedPreferences prefs;
	private Button duringButton, untilButton;
	private LinearLayout durationLayout, untilLayout;

	private boolean modeDuring = true;
	private DateTime unavailable_until;
	private Spinner durationSpinner;
	private TextView untilDate_new;
	private TextView untilTime_new;
	private DateTimeFormatter dtf0, dtf_day, dtf_time;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View rootView =  inflater.inflate(R.layout.fragment_availability, container, false);
		// retrieve display dimensions
		Rect displayRectangle = new Rect();
		getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		// adjust dialog width
		rootView.setMinimumWidth((int)(displayRectangle.width() * 0.9f));
		// set top with 200 margin
		WindowManager.LayoutParams wlp = getDialog().getWindow().getAttributes();
		wlp.y = 200;
		//wlp.height = pos_bottom-pos_top;
		getDialog().getWindow().setAttributes(wlp);
		getDialog().getWindow().setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);

		prefs = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

		dtf0 = DateTimeFormat.forPattern("yyyyMMddHHmm");
		dtf_day = DateTimeFormat.forPattern("dd/MM");
		dtf_time = DateTimeFormat.forPattern("HH:mm");

		final LinearLayout newUnavailablePediodLayout = (LinearLayout) rootView.findViewById(R.id.newUnavailablePediodLayout);
		final LinearLayout currentUnavailablePeriodLayout = (LinearLayout) rootView.findViewById(R.id.currentUnavailablePeriodLayout);
		duringButton = (Button) rootView.findViewById(R.id.duringButton);
		untilButton = (Button) rootView.findViewById(R.id.untilButton);
		durationLayout = (LinearLayout) rootView.findViewById(R.id.durationLayout);
		untilLayout = (LinearLayout) rootView.findViewById(R.id.untilLayout);
		RadioButton rb0 = (RadioButton) rootView.findViewById(R.id.radioButton0);
		RadioButton rb1 = (RadioButton) rootView.findViewById(R.id.radioButton1);
		final LinearLayout dateTimeLayout = (LinearLayout) rootView.findViewById(R.id.dateTimeLayout);
		untilDate_new = (TextView) rootView.findViewById(R.id.untilDate0);
		untilTime_new = (TextView) rootView.findViewById(R.id.untilTime0);
		TextView untilDate_current = (TextView) rootView.findViewById(R.id.untilDate1);
		TextView untilTime_current = (TextView) rootView.findViewById(R.id.untilTime1);
		TextView unavailableEndlessTextView = (TextView) rootView.findViewById(R.id.unavailableEndlessTextView);
		LinearLayout unavailableUntilLayout = (LinearLayout) rootView.findViewById(R.id.unavailableUntilLayout);

		duringButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				modeDuring = true;
				v.setAlpha(1);
				untilButton.setAlpha(.25f);
				durationLayout.setVisibility(View.VISIBLE);
				untilLayout.setVisibility(View.GONE);
			}
		});
		untilButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				modeDuring = false;
				v.setAlpha(1);
				duringButton.setAlpha(.25f);
				durationLayout.setVisibility(View.GONE);
				untilLayout.setVisibility(View.VISIBLE);
			}
		});

		rb0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				dateTimeLayout.setVisibility(isChecked? View.VISIBLE: View.GONE);
			}
		});
		rb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				dateTimeLayout.setVisibility(isChecked? View.GONE: View.VISIBLE);
				SharedPreferences.Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean(Preferences.UNAVAILABLE_ENDLESS, isChecked).apply();
			}
		});
		rb1.setChecked(prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false));

		untilDate_new.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(getActivity(),
						datePickerListener,
						unavailable_until.getYear(), unavailable_until.getMonthOfYear()-1, unavailable_until.getDayOfMonth()
				).show();
			}
		});

		untilTime_new.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new TimePickerDialog(getActivity(),
						timePickerListener,
						unavailable_until.getHourOfDay(), unavailable_until.getMinuteOfHour(), true
				).show();
			}
		});

		durationSpinner = (Spinner) rootView.findViewById(R.id.durationSpinner);
		ArrayAdapter<CharSequence> durationAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.durations_array, R.layout.spinner_item_duration);
		durationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_list);
		durationSpinner.setAdapter(durationAdapter);

		modeDuring = prefs.getBoolean(Preferences.MODE_DURING, true);
		String until = prefs.getString(Preferences.UNAVAILABLE_UNTIL,"");
		if (until.isEmpty() || prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false))
			unavailable_until = DateTime.now();
		else
			unavailable_until = dtf0.parseDateTime(until);
		if (unavailable_until.isBefore(DateTime.now()))
			unavailable_until = DateTime.now();

		int unavailable_for = prefs.getInt(Preferences.UNAVAILABLE_FOR, 1);

		if (Resources.currentlyAvailable(getActivity())){
			newUnavailablePediodLayout.setVisibility(View.VISIBLE);
			currentUnavailablePeriodLayout.setVisibility(View.GONE);
			durationLayout.setVisibility(modeDuring ? View.VISIBLE : View.GONE);
			durationSpinner.setSelection(unavailable_for - 1);
			untilLayout.setVisibility(modeDuring ? View.GONE : View.VISIBLE);
			dateTimeLayout.setVisibility(rb0.isChecked() ? View.VISIBLE : View.GONE);
			untilDate_new.setText(dtf_day.print(unavailable_until));
			untilTime_new.setText(dtf_time.print(unavailable_until));
		} else {
			newUnavailablePediodLayout.setVisibility(View.GONE);
			currentUnavailablePeriodLayout.setVisibility(View.VISIBLE);
			untilDate_new.setText(dtf_day.print(unavailable_until));
			untilTime_new.setText(dtf_time.print(unavailable_until));
			untilDate_current.setText(dtf_day.print(unavailable_until));
			untilTime_current.setText(dtf_time.print(unavailable_until));
			unavailableEndlessTextView.setVisibility(prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false)? View.VISIBLE: View.GONE);
			unavailableUntilLayout.setVisibility(prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false)? View.GONE: View.VISIBLE);
		}

		Button okButton = (Button) rootView.findViewById(R.id.okButton);
		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean(Preferences.MODE_DURING, modeDuring).apply();
				prefsEditor.putInt(Preferences.UNAVAILABLE_FOR, durationSpinner.getSelectedItemPosition()+1).apply();
				if (modeDuring)
						unavailable_until = DateTime.now().plusHours(durationSpinner.getSelectedItemPosition()+1);
				prefsEditor.putString(Preferences.UNAVAILABLE_UNTIL, modeDuring?
					dtf0.print(DateTime.now().plusHours(durationSpinner.getSelectedItemPosition()+1)):
					dtf0.print(unavailable_until)
				).apply();
				mListener.onAvailabilty();
				if (!prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false) && unavailable_until.isAfter(DateTime.now()))
					StartPendingIntent();
				dismiss();
			}
		});

		Button modifyButton = (Button) rootView.findViewById(R.id.modifyButton);
		modifyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				newUnavailablePediodLayout.setVisibility(View.VISIBLE);
				currentUnavailablePeriodLayout.setVisibility(View.GONE);
			}
		});

		Button cancelButton = (Button) rootView.findViewById(R.id.cancelButton);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor prefsEditor = prefs.edit();
				if (prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false)){
					prefsEditor.putBoolean(Preferences.UNAVAILABLE_ENDLESS, false).apply();
				}
				prefsEditor.putString(Preferences.UNAVAILABLE_UNTIL, dtf0.print(DateTime.now().minusMinutes(1))).apply();
				StopPendingIntent();
				mListener.onAvailabilty();
				dismiss();

			}
		});

		return rootView;
	}

	private DatePickerDialog.OnDateSetListener datePickerListener
			= new DatePickerDialog.OnDateSetListener() {
		// when dialog box is closed, below method will be called.
		public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
			unavailable_until = new DateTime(selectedYear, selectedMonth+1, selectedDay, unavailable_until.getHourOfDay(), unavailable_until.getMinuteOfHour());
			untilDate_new.setText(dtf_day.print(unavailable_until));
		}
	};

	private TimePickerDialog.OnTimeSetListener timePickerListener
			= new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			unavailable_until = new DateTime(unavailable_until.getYear(), unavailable_until.getMonthOfYear(), unavailable_until.getDayOfMonth(), hourOfDay, minute);
			untilTime_new.setText(dtf_time.print(unavailable_until));
		}
	};

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		mListener = (AvailabilityListener) context;
	}

	@Override
	public void onDetach(){
		super.onDetach();
		mListener = null;
	}
	@Override
	public void onResume(){
		super.onResume();
		if (modeDuring) {
			duringButton.setAlpha(1);
			untilButton.setAlpha(.25f);
			durationLayout.setVisibility(View.VISIBLE);
			untilLayout.setVisibility(View.GONE);
		} else {
			untilButton.setAlpha(1);
			duringButton.setAlpha(.25f);
			durationLayout.setVisibility(View.GONE);
			untilLayout.setVisibility(View.VISIBLE);
		}

	}
	public interface AvailabilityListener {
		void onAvailabilty();
	}

	private void StartPendingIntent(){
		DateTimeFormatter dtf0 = DateTimeFormat.forPattern("yyyyMMddHHmm");
		String until = prefs.getString(Preferences.UNAVAILABLE_UNTIL, "");
		if (!until.isEmpty()){
			long delay  = dtf0.parseMillis(until);
			//long delay = DateTime.now().plusSeconds(5).getMillis();
			AlarmManager alarmMgr = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(getActivity(), AlarmReceiver.class);
			intent.putExtra("TYPE", AlarmReceiver.PENDING_AVAILABILITY);
			PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), AlarmReceiver.PENDING_AVAILABILITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				//alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, alarmIntent);
				alarmMgr.setExact(AlarmManager.RTC_WAKEUP, delay, alarmIntent);
			} else {
				//alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 1000, alarmIntent);
				alarmMgr.set(AlarmManager.RTC_WAKEUP, delay, alarmIntent);
			}}
	}

	private void StopPendingIntent() {
		Intent intent = new Intent(getActivity(), AlarmReceiver.class);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(getActivity(), AlarmReceiver.PENDING_AVAILABILITY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		alarmIntent.cancel();
	}

}
