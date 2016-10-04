package chan.eddie.twittermap;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class UserLocator implements LocationListener {
	
    private static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    private static final long MINIMUM_TIME_BETWEEN_UPDATES = 100; // in Milliseconds

	// callback of activity on item click
	private OnLocationListener mCallback = null;

	private Activity activity;
	private LocationManager locationManager;

	public UserLocator(Activity activity) {
		this.activity = activity;
		locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
	}
	
    // Container Activity must implement this interface
    public interface OnLocationListener {
    	public void onLocationChanged(Location location);
    }
    
    public void setOnLocationListener(OnLocationListener listener) {
    	mCallback = listener;
    }

	public void onLocationChanged(Location location) {
		updateUILocation(location);
	}

	public void onProviderDisabled(String provider) {
		Toast.makeText(activity, "Provider disabled by the user. GPS turned off",
				Toast.LENGTH_LONG).show();
	}

	public void onProviderEnabled(String provider) {
		Toast.makeText(activity, "Provider enabled by the user. GPS turned on",
				Toast.LENGTH_LONG).show();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		Toast.makeText(activity, "Provider status changed", Toast.LENGTH_LONG)
				.show();
	}

    public void enableLocationUpdate() {
        Location gpsLocation = null;
        Location networkLocation = null;
     
        gpsLocation = requestUpdatesFromProvider(
                LocationManager.GPS_PROVIDER, "GPS provider not supported");
        networkLocation = requestUpdatesFromProvider(
                LocationManager.NETWORK_PROVIDER, "Network provider not supported");
     
        // If both providers return last known locations, compare the two and use the better
        // one to update the UI.  If only one provider returns a location, use it.
        if (gpsLocation != null && networkLocation != null) {
            updateUILocation(getBetterLocation(gpsLocation, networkLocation));
        } else if (gpsLocation != null) {
            updateUILocation(gpsLocation);
        } else if (networkLocation != null) {
            updateUILocation(networkLocation);
        }
    }
    
    public void disableLocationUpdate() {
    	locationManager.removeUpdates(this);
    }

	private Location requestUpdatesFromProvider(final String provider,
			final String errorRes) {
		Location location = null;
		if (locationManager.isProviderEnabled(provider)) {
			locationManager.requestLocationUpdates(provider,
					MINIMUM_TIME_BETWEEN_UPDATES,
					MINIMUM_DISTANCE_CHANGE_FOR_UPDATES, this);

			location = locationManager.getLastKnownLocation(provider);
		} else {
			Toast.makeText(activity, errorRes, Toast.LENGTH_LONG).show();
		}
		return location;
	}

    protected Location getBetterLocation(Location newLocation, 
           Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return newLocation;
        }
     
        // Check whether the new location fix is newer or older
        long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MINIMUM_TIME_BETWEEN_UPDATES;
        boolean isSignificantlyOlder = timeDelta < -MINIMUM_TIME_BETWEEN_UPDATES;
        boolean isNewer = timeDelta > 0;
     
        // If it's been more than minimum time since 
        // the current location, use the new location
        // because the user has likely moved.
        if (isSignificantlyNewer) {
            return newLocation;
            // If the new location is more than minimum time older, it must be worse
        } else if (isSignificantlyOlder) {
            return currentBestLocation;
        }
     
        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (newLocation.getAccuracy() 
                                   - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
     
        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
                currentBestLocation.getProvider());
     
        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return newLocation;
        } else if (isNewer && !isLessAccurate) {
            return newLocation;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return newLocation;
        }
        return currentBestLocation;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

	private void updateUILocation(Location location) {
		if (mCallback != null)
			mCallback.onLocationChanged(location);
	}
}
