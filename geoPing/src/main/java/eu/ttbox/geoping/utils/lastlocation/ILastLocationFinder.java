package eu.ttbox.geoping.utils.lastlocation;


import android.location.Location;

import java.util.concurrent.Future;

/**
 * Interface definition for a Last Location Finder.
 *
 * Classes that implement this interface must provide methods to
 * find the "best" (most accurate and timely) previously detected
 * location using whatever providers are available.
 *
 * Where a timely / accurate previous location is not detected, classes
 * should return the last location and create a one-shot update to find
 * the current location. The one-shot update should be returned via the
 * Location Listener passed in through setChangedLocationListener.
 *
 * https://code.google.com/p/android-protips-location/source/browse/trunk/src/com/radioactiveyak/location_best_practices/#location_best_practices%2Futils
 */
public interface ILastLocationFinder {

    /**
     * Find the most accurate and timely previously detected location
     * using all the location providers. Where the last result is beyond
     * the acceptable maximum distance or latency create a one-shot update
     * of the current location to be returned using the {@link android.location.LocationListener}
     * passed in through {@link setChangedLocationListener}
     * @param minDistance Minimum distance before we require a location update.
     * @param minTime Minimum time required between location updates.
     * @return The most accurate and / or timely previously detected location.
     */
    public Future<Location> getLastBestLocation(int minDistance, long minTime);

    public Future<Location> getLastLocation();

    public void onStop();
}
