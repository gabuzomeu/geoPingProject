package eu.ttbox.geoping.utils.lastlocation;

import android.app.AlarmManager;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import java.util.List;
import java.util.concurrent.Future;

import eu.ttbox.geoping.utils.lastlocation.retryfutur.LocationReceiverReplyFutur;
import eu.ttbox.geoping.utils.lastlocation.retryfutur.LocationReplyFutur;


/**
 * Optimized implementation of Last Location Finder for devices running Gingerbread
 * and above.
 * <p/>
 * This class let's you find the "best" (most accurate and timely) previously
 * detected location using whatever providers are available.
 * <p/>
 * Where a timely / accurate previous location is not detected it will
 * return the newest location (where one exists) and setup a oneshot
 * location update to find the current location.
 */
public class GingerbreadLastLocationFinder implements ILastLocationFinder {

    private static final String TAG = "GingerbreadLastLocationFinder";
    private static final int MAX_DISTANCE = 75;

    private LocationManager locationManager;
    private Context context;

    // ===========================================================
    //   Constructor
    // ===========================================================

    /**
     * Construct a new Gingerbread Last Location Finder.
     *
     * @param context Context
     */
    public GingerbreadLastLocationFinder(Context context) {
        super();
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void onStop() {
        // Disconnecting the client invalidates it.
        locationManager = null;
    }


    // ===========================================================
    //   Business
    // ===========================================================


    @Override
    public Future<Location> getLastLocation() {
        // TODO Read the preference
        int minDistance = MAX_DISTANCE;
        long minTime = System.currentTimeMillis()- AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        return getLastBestLocation(75, minTime);
    }

    /**
     * Returns the most accurate and timely previously detected location.
     * Where the last result is beyond the specified maximum distance or latency
     *
     * @param minDistance Minimum distance before we require a location update.
     * @param minTime     Minimum time required between location updates.
     * @return The most accurate and / or timely previously detected location.
     */
    public Future<Location> getLastBestLocation(int minDistance, long minTime) {
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestTime = Long.MIN_VALUE;

        // Iterate through all the providers on the system, keeping
        // note of the most accurate result within the acceptable time limit.
        // If no result is found within maxTime, return the newest Location.
        List<String> matchingProviders = locationManager.getAllProviders();
        for (String provider : matchingProviders) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();

                if ((time > minTime && accuracy < bestAccuracy)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                } else if (time < minTime && bestAccuracy == Float.MAX_VALUE && time > bestTime) {
                    bestResult = location;
                    bestTime = time;
                }
            }
        }

        // If the best result is beyond the allowed time limit, or the accuracy of the
        // best result is wider than the acceptable maximum distance, request a single update.
        // This check simply implements the same conditions we set when requesting regular
        // location updates every [minTime] and [minDistance].
        if (bestTime < minTime || bestAccuracy > minDistance) {
            return new LocationReceiverReplyFutur(context);
        } else {
            return new LocationReplyFutur(bestResult);
        }
    }


}