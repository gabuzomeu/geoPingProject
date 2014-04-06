package eu.ttbox.geoping.utils.lastlocation;


import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.concurrent.Future;

public class LastLocationFinder implements ILastLocationFinder {

    private static final String TAG = "LastLocationFinder";

    private Context context;
    private ILastLocationFinder lastLocationFinder;


    // ===========================================================
    //   Factory
    // ===========================================================

    public static ILastLocationFinder getLastLocationFinder(Context context) {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(TAG, "### ILastLocationFinder with GooglePlayServiceLastLocationFinder ");
            return new GooglePlayServiceLastLocationFinder(context);
        } else {
            Log.d(TAG, "### ILastLocationFinder with GingerbreadLastLocationFinder ");
            return new GingerbreadLastLocationFinder(context);
        }

    }

    // ===========================================================
    //   Constructor
    // ===========================================================


    public LastLocationFinder(Context context) {
        super();
        this.context = context;
        this.lastLocationFinder = getLastLocationFinder(context);
    }


    // ===========================================================
    //   Implementation
    // ===========================================================


    @Override
    public void onStop() {
        lastLocationFinder.onStop();
    }

    @Override
    public Future<Location> getLastBestLocation(int minDistance, long minTime) {
        return lastLocationFinder.getLastBestLocation(minDistance, minTime);
    }

    @Override
    public Future<Location> getLastLocation() {
        return lastLocationFinder.getLastLocation();
    }
}
