package com.bluegecko.sos.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

/**
 * Created by Bob on 03/07/2016
 */
public class MyLocation {
	private static Context mContext;

	public enum LocationType {FINE, COARSE}

	private static Location location;
	private static LocationManager locationManager;
	public static boolean isRunning = false;
	public static MyLocation instance;
	public static MyLocationListener myLocationListener;
	private static final int SECOND_MS = 1000;
	private static final int MINUTE_MS = 60 * SECOND_MS;
	private static final int LARGE_LOCATION_AGE_MS = 2 * MINUTE_MS;
	static Criteria criteria;

	public MyLocation(Context context) {
		mContext = context;
		if (locationManager==null)
			locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (myLocationListener==null)
			myLocationListener = new MyLocationListener();
		criteria = new Criteria();
		isRunning = false;
	}

	public static MyLocation getInstance(Context context) {
		if (instance != null) {
			return instance;
		} else {
			instance = new MyLocation(context);
			return instance;
		}
	}

	public void Start() {
		if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
		&& ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, myLocationListener);                  // update every 1s
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, myLocationListener);              // update every 1s
			//String bestProvider = locationManager.getBestProvider(criteria, true);
			isRunning = true;
		}
	}

	public void Stop() {
		if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
				|| ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
			locationManager.removeUpdates(myLocationListener);
			isRunning = false;
		}
	}

	public boolean isGPS_ON() {
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
	public boolean isNetwork_ON() {
		return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}


	public Location WaitForLocation(int delay, Location previousLocation) {
		if (previousLocation==null){
			while (delay!=0 && location==null){
				try {
					Thread.sleep(10*SECOND_MS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				delay--;
			}
			return location;
		} else {
			Location best = BestLocation(previousLocation, location);
			while (delay!=0 && isSameLocation(best,previousLocation)){
				try {
					Thread.sleep(10*SECOND_MS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				best = BestLocation(previousLocation, location);
				delay--;
			}
			return best;
		}
	}

	public boolean isSameLocation(Location loc1, Location loc2) {
		return (loc1 == null && loc2 == null)
				|| !((loc1 == null) || (loc2 == null))
					&& (loc1.getLatitude() == loc2.getLatitude() && loc1.getLongitude() == loc2.getLongitude());
	}
	/**
	 Gets the best already known location, if none exists, null is returned.
	 @return a location or null.
	 */
	public Location GetBestLastKnownLocation() {
		if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
		&& ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

			Location best;
			best = BestLocation(location, locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
			best = BestLocation(best, locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
			best = BestLocation(best, locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
			return best;
		} else {
			return null;
		}
	}

	/**
	* Determines whether one Location reading is better than the current
	* Location fix.
	* @param loc1 : 1st Location that you want to evaluate
	* @param loc2 : 2nd Location that you want to evaluate
	* @return the best location.
	*/
	protected static Location BestLocation(Location loc1, Location loc2) {

		if (loc1==null && loc2==null) return null;  // both are null => return null
		if (loc1==null) return loc2;                // loc1 is null, loc2 is not => return loc2
		if (loc2==null) return loc1;	            // loc2 is null, loc1 is not => return loc1
		// neither loc1 nor loc2 are null

		if (loc1.getLatitude()==loc2.getLatitude() && loc1.getLongitude()==loc2.getLongitude()) return loc1;

		// Check witch location fix is newer or older
		long timeDelta = loc1.getTime() - loc2.getTime();
		boolean loc1_isSignificantlyNewer = timeDelta > LARGE_LOCATION_AGE_MS;
		boolean loc2_isSignificantlyNewer = timeDelta < -LARGE_LOCATION_AGE_MS;
		boolean loc1_isNewer = timeDelta > 0;

		// If it's been more than two minutes between the 2 locations,
		// use the newer location because the user has likely moved
		if (loc1_isSignificantlyNewer) return loc1;
		if (loc2_isSignificantlyNewer) return loc2;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(loc1.getProvider(), loc2.getProvider());

		// if 2 locations have accuracy
		if (loc1.hasAccuracy() && loc2.hasAccuracy()) {
			// Check whether the new location fix is more or less accurate
			int accuracyDelta = (int) (loc1.getAccuracy() - loc2.getAccuracy());
			boolean loc1_isMoreAccurate = accuracyDelta < 0;
			boolean loc1_isSignificantlyMoreAccurate = accuracyDelta < -200;
			boolean loc2_isSignificantlyMoreAccurate = accuracyDelta > 200;

			// Determine location quality using a combination of timeliness and accuracy
			if (loc1_isNewer && loc1_isMoreAccurate) return loc1;
			if (!loc1_isNewer && !loc1_isMoreAccurate) return loc2;
			if (isFromSameProvider){
				if (loc1_isSignificantlyMoreAccurate) return loc1;
				if (loc2_isSignificantlyMoreAccurate) return loc2;
			}
		}
		// one or both locations don't have accuracy information
		// OR not from same provider
		// OR (from same provider AND not a very significant accuracy difference)
		if (loc1.getProvider().equals(LocationManager.GPS_PROVIDER)) {
			// if both locations are from GPS : prefer newer
			if (isFromSameProvider){
				if (loc1_isNewer) {
					return loc1;
				}
				return loc2;
			}
			// else prefer GPS
			return loc1;
		}
		return loc2;
	}

	// Checks whether two providers are the same
	private static boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	private class MyLocationListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			MyLocation.location = BestLocation(MyLocation.location, location);
			//Toast.makeText(mContext, "Location listener..." + location.toString(), Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onProviderDisabled(String provider) {
		}
	}


//	public static class Coords {
//		public double latitude;
//		public double longitude;
//		Coords(){
//			latitude = 0;
//			longitude = 0;
//		}
//		Coords(double latitude, double longitude){
//			this.latitude = latitude;
//			this.longitude = longitude;
//		}
//	}
}

