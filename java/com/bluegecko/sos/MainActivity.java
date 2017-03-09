package com.bluegecko.sos;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bluegecko.sos.database.Recipients;
import com.bluegecko.sos.info.Info4Activity;
import com.bluegecko.sos.info.InfoFragment;
import com.bluegecko.sos.info.InfoFragment4;
import com.bluegecko.sos.prepare.EditRecipientFragment;
import com.bluegecko.sos.prepare.Preview_fragment;
import com.bluegecko.sos.receive.AvailabilityFragment;
import com.bluegecko.sos.send.SendSOS;
import com.bluegecko.sos.utils.Permissions;
import com.bluegecko.sos.utils.Permissions_fragment;
import com.bluegecko.sos.utils.Preferences;
import com.bluegecko.sos.utils.Resources;
import com.bluegecko.sos.utils.ReverseInterpolator;
import com.bluegecko.sos.utils.SeekBarHint;
import com.bluegecko.sos.utils.SettingsFragment;
import com.bluegecko.sos.utils.SplashActivity;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;

import static com.bluegecko.sos.utils.Permissions.CheckPermission;
import static com.bluegecko.sos.utils.Permissions.GetPermissions;

import static com.bluegecko.sos.utils.Permissions.permission_SMS;
import static com.bluegecko.sos.utils.Permissions.permission_GPS;
import static com.bluegecko.sos.utils.Permissions.permission_CTX;
import static com.bluegecko.sos.utils.Permissions.permission_PHN;
import static com.bluegecko.sos.utils.Permissions.SMS_RESULT_CODE;
import static com.bluegecko.sos.utils.Permissions.GPS_RESULT_CODE;
import static com.bluegecko.sos.utils.Permissions.CTX_RESULT_CODE;
import static com.bluegecko.sos.utils.Permissions.PHN_RESULT_CODE;

public class MainActivity extends AppCompatActivity implements
		Permissions_fragment.PermissionsListener,
		AvailabilityFragment.AvailabilityListener,
		InfoFragment4.Info4Listener,
		SettingsFragment.SettingsListener,
		EditRecipientFragment.EditRecipientListener {

	boolean firstUse;

	private SharedPreferences prefs;
	private SharedPreferences.Editor prefsEditor;
	private final String TAG = getClass().getSimpleName();
	private Recipients recipients;
	private LinearLayout content;
	private LinearLayout mask, sideLayout;
	private TranslateAnimation anim;
	private EditRecipientFragment rFragment;
	private TextView recipientsLabel;
	private TextView outUntil;
	private ImageView unavailablePicture;

	private ImageView infoMenuIcon, settingsMenuIcon, placeHolder;
	private TextView selectedIconText;

	private recipientAdapter adapter;
	private List<Recipients.Recipient> recipientsList;
	public boolean dragEnabled = true;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		placeHolder = (ImageView) findViewById(R.id.placeHolder);
		infoMenuIcon = (ImageView) findViewById(R.id.infoButton);
		infoMenuIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TranslateIcon(infoMenuIcon, settingsMenuIcon, getString(R.string.infoTitle));
				ShowSideLayout(new InfoFragment());
			}
		});

		settingsMenuIcon = (ImageView) findViewById(R.id.settingsButton);
		settingsMenuIcon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TranslateIcon(settingsMenuIcon, infoMenuIcon, getString(R.string.settings_title));
				ShowSideLayout(new SettingsFragment());
			}
		});

		selectedIconText = (TextView) findViewById(R.id.selectedIconText);

		content = (LinearLayout) findViewById(R.id.content);
		mask = (LinearLayout) findViewById(R.id.mask);
		sideLayout = (LinearLayout) findViewById(R.id.sideLayout);

		prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
		prefsEditor = prefs.edit();
		firstUse = !CheckPermission(this, Permissions.permission_SMS) || prefs.getBoolean(Preferences.FIRST_USE, true);
		prefsEditor.putBoolean("AlarmActivityRunning", false).apply();
		prefsEditor.putBoolean(Preferences.MANAGE_ANSWERS_ACTIVITY, false).apply();

		if (firstUse) {
			if (!prefs.getBoolean(Preferences.HAS_SHORTCUT, false)) addShortcut();
			prefsEditor.putBoolean(Preferences.FIRST_USE, false).apply();
			startActivity(new Intent(this, Info4Activity.class));
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				ShowSideLayout(new Permissions_fragment());
		}

		recipients = new Recipients(this);

		rFragment = new EditRecipientFragment();
		LinearLayout addButton = (LinearLayout) findViewById(R.id.add);
		addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rFragment = new EditRecipientFragment();
				rFragment.show(getSupportFragmentManager(), "");
			}
		});

		recipientsLabel = (TextView) findViewById(R.id.recipientsLabel);

		final DragSortListView recipientsListView = (DragSortListView) findViewById(R.id.recipientsLayout);
		recipientsList = recipients.getAllRows();

		adapter = new recipientAdapter(this, R.layout.list_recipient, recipientsList);




		recipientsListView.setAdapter(adapter);
		recipientsListView.setDropListener(new DragSortListView.DropListener() {
			@Override
			public void drop(int from, int to) {
				if (from!=to) {
					Recipients.Recipient rFrom = recipients.getRecipientByRank(from);
					rFrom.setRank(to);
					Recipients.Recipient rTo = recipients.getRecipientByRank(to);
					rTo.setRank(from);
					recipients.updateRow(rFrom);
					recipients.updateRow(rTo);
					adapter.clear();
					adapter.addAll(recipients.getAllRows());
				}
			}
		});
		recipientsLabel.setText(String.format(getString(R.string.recipients), recipientsList.size()));
		DragSortController mController = new MyDSController(recipientsListView);
		recipientsListView.setFloatViewManager(mController);
		recipientsListView.setOnTouchListener(mController);
		recipientsListView.setDragEnabled(dragEnabled);


		final SeekBarHint seekBar = (SeekBarHint) findViewById(R.id.seekBar);
		if (seekBar != null) {
			seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				int originalProgress;
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
					if(fromTouch){
						// only allow changes by 1 up or down
						if ((progress > (originalProgress+24))
								|| (progress < (originalProgress-24))) {
							seekBar.setProgress( originalProgress);
						} else {
							originalProgress = progress;
						}
					}
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					originalProgress=seekBar.getProgress();
				}
				@Override
				public void onStopTrackingTouch(final SeekBar seekBar) {
					if (seekBar.getProgress() > 95) {
						if (recipients.size>0) {
							SendSOS sender = new SendSOS(MainActivity.this, 0);
							sender.execute();                                       // send SOS
							finish();
						} else {
							Toast.makeText(MainActivity.this, getString(R.string.no_recipient), Toast.LENGTH_SHORT).show();
							ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), 0);
							anim.setDuration(200);
							anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
								@Override
								public void onAnimationUpdate(ValueAnimator animation) {
									int animProgress = (Integer) animation.getAnimatedValue();
									seekBar.setProgress(animProgress);
								}
							});
							anim.start();
						}
					} else {
						ValueAnimator anim = ValueAnimator.ofInt(seekBar.getProgress(), 0);
						anim.setDuration(200);
						anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
							@Override
							public void onAnimationUpdate(ValueAnimator animation) {
								int animProgress = (Integer) animation.getAnimatedValue();
								seekBar.setProgress(animProgress);
							}
						});
						anim.start();
					}
				}
			});
		}

		Button previewButton = (Button) findViewById(R.id.previewButton);
		previewButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Preview_fragment vFragment = new Preview_fragment();
				vFragment.show(getSupportFragmentManager(), "");
			}
		});

		RelativeLayout absentButton = (RelativeLayout) findViewById(R.id.absentButton);
		absentButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AvailabilityFragment aFragment = new AvailabilityFragment();
				aFragment.show(getSupportFragmentManager(), "");
			}
		});
		unavailablePicture = (ImageView) findViewById(R.id.unavailablePicture);
		outUntil = (TextView) findViewById(R.id.outUntil);
		Button quitButton = (Button) findViewById(R.id.quitButton);
		quitButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				content.requestFocus();
				finish();
			}
		});

		registerReceiver(broadcastReceiver, new IntentFilter("NOW_AVAILABLE"));

	}

	private class MyDSController extends DragSortController {
		DragSortListView mDslv;

		public MyDSController(DragSortListView dslv) {
			super(dslv);
			setDragHandleId(R.id.handler);
			mDslv = dslv;
		}

		@Override
		public View onCreateFloatView(int position) {
			ImageView floatView = (ImageView) super.onCreateFloatView(position);
			floatView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.floatView));
			floatView.setImageAlpha(96);
			return floatView;
		}

		@Override
		public void onDestroyFloatView(View floatView) {
			//do nothing; block super from crashing
		}

		@Override
		public int startDragPosition(MotionEvent ev) {
			//int width = mDslv.getWidth();
			return super.dragHandleHitPosition(ev);
		}
	}

	@Override
	public void onPostCreate(Bundle bundle){
		super.onPostCreate(bundle);
	}
	@Override
	public void onResume(){
		super.onResume();
		ShowAvailability();
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(broadcastReceiver);
	}

	@Override
	public void onBackPressed() {
		if (sideLayout.getVisibility()==View.VISIBLE){
			if (!CheckPermission(this, Permissions.permission_SMS)){
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setCancelable(false)
						.setTitle(R.string.warningTitle)
						.setMessage( R.string.SMSpermissionRefused)
						.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						})
						.setNegativeButton(getString(R.string.quit), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								finish();
							}
						})
						.create()
						.show();
			} else {
				HideSideLayout();
			}
		} else {
			super.onBackPressed();
		}
	}

	private void ShowSideLayout(Fragment fragment){
		mask.setVisibility(View.VISIBLE);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.sideLayout, fragment)
				.commit();
		sideLayout.setVisibility(View.VISIBLE);
		sideLayout.startAnimation(SideLayoutAnimation(true));
	}
	private void HideSideLayout(){
		sideLayout.startAnimation(SideLayoutAnimation(false));
		sideLayout.setVisibility(View.GONE);
		final ValueAnimator maskAnimation = ValueAnimator.ofFloat(1f, 0f);
		maskAnimation.setDuration(500); // milliseconds
		maskAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				mask.setAlpha((float) animator.getAnimatedValue());
			}
		});
		maskAnimation.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				super.onAnimationEnd(animation);
				mask.setVisibility(View.GONE);
			}
		});
		maskAnimation.start();
		ResetMenuIcons();
	}

	private TranslateAnimation SideLayoutAnimation(final boolean in){
		anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF, 0,
				Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0);
		anim.setDuration(300);
		if (in)
			anim.setInterpolator(new AccelerateDecelerateInterpolator());
		else
			anim.setInterpolator(new ReverseInterpolator(new AccelerateDecelerateInterpolator()));
		return anim;
	}

	private void TranslateIcon(final View selectedIcon, final View unselectedIcon, String text){
		int slideDuration = 300;
		int fadeDuration = 300;
		int fromPos[] = new int[2];
		int toPos[] = new int[2];
		// get positions (from & to)
		placeHolder.getLocationOnScreen(toPos);
		if (selectedIcon==infoMenuIcon){
			infoMenuIcon.getLocationOnScreen(fromPos);
		} else {
			settingsMenuIcon.getLocationOnScreen(fromPos);
		}
		// remove sensible zones & hide unselected icon
		selectedIcon.setClickable(false);
		unselectedIcon.setAlpha(0);
		// prepare textView
		selectedIconText.setAlpha(0);
		selectedIconText.setVisibility(View.VISIBLE);
		selectedIconText.setText(text);
		// prepare textView alpha animation
		final ValueAnimator textAnimation = Resources.ViewAlphaAnimation(selectedIconText, 0, 1, fadeDuration);
		//textAnimation.setStartDelay(200);
		// prepare selected icon animation
		final ValueAnimator iconAlphaAnimation = Resources.ViewAlphaAnimation(selectedIcon, 0.5f, 1, slideDuration);
		anim = Resources.ViewTranslateAnimation(fromPos, toPos, slideDuration, new AccelerateDecelerateInterpolator(), true);
		anim.setAnimationListener(new Animation.AnimationListener() {
			@Override public void onAnimationStart(Animation animation) {}
			@Override public void onAnimationEnd(Animation animation) {
				textAnimation.start();
			}
			@Override public void onAnimationRepeat(Animation animation) {}
		});
		// start icon && textView animation
		iconAlphaAnimation.start();
		selectedIcon.startAnimation(anim);
	}


	private void ResetMenuIcons(){
		int fadeDuration = 300;
		infoMenuIcon.setAlpha(0f);
		settingsMenuIcon.setAlpha(0f);
		infoMenuIcon.setVisibility(View.VISIBLE);
		settingsMenuIcon.setVisibility(View.VISIBLE);
		selectedIconText.setVisibility(View.INVISIBLE);
		// restore icons position
		Resources.ResetTranslateAnimation(infoMenuIcon);
		Resources.ResetTranslateAnimation(settingsMenuIcon);
		// animate icons alpha
		Resources.ViewAlphaAnimation(new View[]{infoMenuIcon, settingsMenuIcon}, 0, .5f, fadeDuration).start();
		// restore sensible zones
		infoMenuIcon.setClickable(true);
		settingsMenuIcon.setClickable(true);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode){
			case EditRecipientFragment.REQUEST_CONTACT_NUMBER:
				if (resultCode == RESULT_OK) {
					if (data != null ) {//&& requestCode == REQUEST_CONTACT_NUMBER
						Uri contactRecordURI = data.getData();
						String contactId = contactRecordURI.getLastPathSegment();
						rFragment.setContactData(this, contactId);
					} else {
						Log.w(TAG, "WARNING: Corrupted request response");
					}
				} else if (resultCode == RESULT_CANCELED) {
					Log.i(TAG, "Popup canceled by user.");
				} else {
					Log.w(TAG, "WARNING: Unknown resultCode");
				}
			default:
		}
	}

	private class recipientAdapter extends ArrayAdapter<Recipients.Recipient> {
		private Context mContext;
		private List<Recipients.Recipient> list;

		public recipientAdapter(Context context, int layout, List<Recipients.Recipient> from) {
			super(context, layout, R.id.id, from);
			mContext = context;
			list = from;
		}
		public void update(){
			recipients = new Recipients(MainActivity.this);
			recipientsList.clear();
			recipientsList.addAll(recipients.getAllRows());
			notifyDataSetChanged();
		}

		@Override
		public void add(Recipients.Recipient recipient) {
			recipients = new Recipients(MainActivity.this);
			recipientsList.add(recipient);
			notifyDataSetChanged();
			recipientsLabel.setText(String.format(getString(R.string.recipients), recipientsList.size()));
		}
		@Override
		public void remove(Recipients.Recipient recipient) {
			recipients = new Recipients(MainActivity.this);
			recipientsList.remove(recipient);
			notifyDataSetChanged();
			recipientsLabel.setText(String.format(getString(R.string.recipients), recipientsList.size()));
		}
		@Override
		public void clear() {
			recipients = new Recipients(MainActivity.this);
			recipientsList.clear();
			notifyDataSetChanged();
		}
		@Override
		public void addAll(Collection<? extends Recipients.Recipient> newList) {
			recipients = new Recipients(MainActivity.this);
			recipientsList.addAll(newList);
			notifyDataSetChanged();
		}


		@Override
		public View getView(final int position, View convertView, ViewGroup parent){
			View view = super.getView(position, convertView, parent);
			final Recipients recipients = new Recipients(mContext);

			boolean GPSisON = CheckPermission(mContext, permission_GPS);
			final Recipients.Recipient recipient = list.get(position);
			final Long id = recipient.getId();

			ImageButton delButton = (ImageButton) view.findViewById(R.id.delButton);
			delButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new Recipients(mContext).delRow(id);
					adapter.remove(recipient);
				}
			});

			TextView rankTextView = (TextView) view.findViewById(R.id.rankTextView);
			TextView phone = (TextView) view.findViewById(R.id.phoneTextView);
			TextView name = (TextView) view.findViewById(R.id.nameTextView);
			CheckBox positionCB = (CheckBox) view.findViewById(R.id.positionCheckbox);
			TextView message = (TextView) view.findViewById(R.id.messageTextView);
			LinearLayout content = (LinearLayout) view.findViewById(R.id.content);
			NumberFormat nf = new DecimalFormat("#0");
			rankTextView.setText(nf.format(position +1));
			phone.setText(Resources.formatPhoneNumber(recipient.getPhone(), false));
			name.setText(recipient.getName());
			message.setText(recipient.getMessage());
			positionCB.setVisibility(GPSisON? View.VISIBLE : View.GONE);
			positionCB.setChecked(recipient.getPosition());
			positionCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					recipient.setPosition(isChecked);
					recipients.updateRow(recipient);
				}
			});
			message.setVisibility(message.getText().toString().isEmpty()? View.GONE : View.VISIBLE);

			content.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					EditRecipientFragment rFragment = new EditRecipientFragment();
					Bundle bundle = new Bundle();
					bundle.putLong("ID", id);
					rFragment.setArguments(bundle);
					rFragment.show(getSupportFragmentManager(), "");
				}
			});

			phone.setHintTextColor(ContextCompat.getColor(mContext, phone.getText().toString().isEmpty()? R.color.redDark: android.R.color.black));
			view.setAlpha(phone.getText().toString().isEmpty()? .25f: 1f);

			return view;
		}
	}


	// selon la réponse de l'utilisateur à la demande de permissions
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch(requestCode){
			case SMS_RESULT_CODE:
				if (!CheckPermission(this, permission_SMS)) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setCancelable(false)
							.setTitle(R.string.warningTitle)
							.setMessage( R.string.SMSpermissionRefused)
							.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									GetPermissions(MainActivity.this);
									dialog.cancel();
								}
							})
							.setNegativeButton(getString(R.string.quit), new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.cancel();
									finish();
								}
							})
							.create()
							.show();
				} else {
					prefsEditor.putBoolean(Preferences.FIRST_USE, false).apply();
					if (!CheckPermission(this, permission_GPS)) {
						ActivityCompat.requestPermissions(this, permission_GPS, GPS_RESULT_CODE);
					} else if (!CheckPermission(this, permission_CTX)) {
						ActivityCompat.requestPermissions(this, permission_CTX, CTX_RESULT_CODE);
					} else if (!CheckPermission(this, permission_PHN)) {
						ActivityCompat.requestPermissions(this, permission_PHN, PHN_RESULT_CODE);
					}
				}
				break;
			case GPS_RESULT_CODE:
				if (!CheckPermission(this, permission_CTX)) {
					ActivityCompat.requestPermissions(this, permission_CTX, CTX_RESULT_CODE);
				} else if (!CheckPermission(this, permission_PHN)) {
					ActivityCompat.requestPermissions(this, permission_PHN, PHN_RESULT_CODE);
				}
				break;
			case CTX_RESULT_CODE:
				if (!CheckPermission(this, permission_PHN)) {
					ActivityCompat.requestPermissions(this, permission_PHN, PHN_RESULT_CODE);
				}
				break;
			default:
		}
	}

	private void addShortcut() {
		Intent shortcutIntent = new Intent(getApplicationContext(), SplashActivity.class);
		shortcutIntent.setAction(Intent.ACTION_MAIN);
		shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT;
		shortcutIntent.addFlags(flags);

		Intent addIntent = new Intent();
		addIntent.putExtra("duplicate", false);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getResources().getString(R.string.app_name));
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource
				.fromContext(getApplicationContext(), R.drawable.sos_app_icon5));
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		getApplicationContext().sendBroadcast(addIntent);
		prefsEditor.putBoolean(Preferences.HAS_SHORTCUT, true).apply();

	}


	/**
	 * Fragments interfaces
	 */
	@Override
	public void onPermissionsOK() {
		GetPermissions(this);
	}
	@Override
	public void onPermissionsHide() {
		HideSideLayout();
	}
	@Override
	public void onAvailabilty() {
		ShowAvailability();
	}

	public void ShowAvailability(){
		boolean available = Resources.currentlyAvailable(this);
		boolean endless = prefs.getBoolean(Preferences.UNAVAILABLE_ENDLESS, false);
		unavailablePicture.setImageDrawable(ContextCompat.getDrawable(this, available? R.drawable.here: R.drawable.not_here));
		outUntil.setVisibility(available? View.INVISIBLE: View.VISIBLE);
		if (!available && !endless) {
			DateTimeFormatter dtf0 = DateTimeFormat.forPattern("yyyyMMddHHmm");
			DateTimeFormatter dtf1 = DateTimeFormat.forPattern("dd/MM-HH:mm");
			DateTime until = dtf0.parseDateTime(prefs.getString(Preferences.UNAVAILABLE_UNTIL,""));
			outUntil.setText(dtf1.print(until));
		} else {
			outUntil.setText(getString(R.string.absent_until1));
		}
	}

//	public static void Release(){
//		if(thisActivity!=null) {
//			SharedPreferences prefs = thisActivity.getSharedPreferences(thisActivity.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
//			SharedPreferences.Editor prefsEditor = prefs.edit();
//			DateTimeFormatter dtf0 = DateTimeFormat.forPattern("yyyyMMddHHmm");
//			prefsEditor.putBoolean(Preferences.UNAVAILABLE_ENDLESS, false);
//			prefsEditor.putString(Preferences.UNAVAILABLE_UNTIL, dtf0.print(DateTime.now().minusMinutes(1))).apply();
//			thisActivity.ShowAvailability();
//		}
//	}

	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			DateTimeFormatter dtf0 = DateTimeFormat.forPattern("yyyyMMddHHmm");
			prefsEditor.putBoolean(Preferences.UNAVAILABLE_ENDLESS, false);
			prefsEditor.putString(Preferences.UNAVAILABLE_UNTIL, dtf0.print(DateTime.now().minusMinutes(1))).apply();
			ShowAvailability();
		}
	};
	@Override
	public void onInfoOK() {
		HideSideLayout();
	}

	@Override
	public void onSettings() {
		HideSideLayout();
	}

	@Override
	public void onEditRecipient(String action, Recipients.Recipient recipient) {
		switch (action){
			case "add":
				adapter.add(recipient);
				break;
			case "edit":
				adapter.update();
				break;
			default:
		}
		recipientsLabel.setText(String.format(getString(R.string.recipients), recipientsList.size()));
	}

}
