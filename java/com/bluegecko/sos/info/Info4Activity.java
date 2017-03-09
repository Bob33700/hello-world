package com.bluegecko.sos.info;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.CircleIndicator;

public class Info4Activity extends AppCompatActivity implements InfoFragment4.Info4Listener {
	private InfoFragment1 infoFragment1;
	private InfoFragment2 infoFragment2;
	private InfoFragment3 infoFragment3;
	private InfoFragment4 infoFragment4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);

		// Instantiate a ViewPager and a PagerAdapter.
		ViewPager mPager = (ViewPager) findViewById(R.id.viewpager);
		mPager.setOffscreenPageLimit(3);
		final PagerAdapter adapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
		mPager.setAdapter(adapter);
		CircleIndicator indicator = (CircleIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(mPager);

	}

	/**
	 *  adaptateur du viewPager
	 */
	private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

		public ScreenSlidePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position){
				case 0:
					if (infoFragment1 ==null){
						infoFragment1 = new InfoFragment1();
					}
					return infoFragment1;
				case 1:
					if (infoFragment2 ==null){
						infoFragment2 = new InfoFragment2();
					}
					return infoFragment2;
				case 2:
					if (infoFragment3 ==null){
						infoFragment3 = new InfoFragment3();
					}
					return infoFragment3;
				case 3:
					if (infoFragment4 ==null){
						infoFragment4 = new InfoFragment4();
					}
					return infoFragment4;
				default:
					return null;
			}
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public int getItemPosition(Object object) {
			return super.getItemPosition(object);
		}
	}

	@Override
	public void onInfoOK() {
		finish();
	}
}
