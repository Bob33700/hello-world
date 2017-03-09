package com.bluegecko.sos.info;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluegecko.sos.R;
import com.bluegecko.sos.utils.CircleIndicator;

public class InfoFragment extends Fragment {

	private InfoFragment1 infoFragment1;
	private InfoFragment2 infoFragment2;
	private InfoFragment3 infoFragment3;
	private InfoFragment4 infoFragment4;

	// Required empty public constructor
	public InfoFragment() {}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_info, container, false);

		// Instantiate a ViewPager and a PagerAdapter.
		ViewPager mPager = (ViewPager) rootView.findViewById(R.id.viewpager);
		mPager.setOffscreenPageLimit(3);
		final PagerAdapter adapter = new ScreenSlidePagerAdapter(getChildFragmentManager());
		mPager.setAdapter(adapter);
		CircleIndicator indicator = (CircleIndicator) rootView.findViewById(R.id.indicator);
		indicator.setViewPager(mPager);

		return rootView;
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
}
