package eu.ttbox.geoping.service.receiver;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * https://code.google.com/p/android-protips-location/source/browse/trunk/src/com/radioactiveyak/location_best_practices/receivers/LocationChangedReceiver.java
 */
public class LocationChangeReceiver extends BroadcastReceiver {

    protected static String TAG = "LocationChangedReceiver";


    private static final int REQUEST_CODE_NOT_USED = 0;


    // ===========================================================
    //   Static Accessor
    // ===========================================================


    public static PendingIntent createPendingIntent(Context context) {
        Intent i = new Intent(context, LocationChangeReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE_NOT_USED, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pi;
    }

    public static void requestLocationUpdates(Context context, LocationManager locationManager) {
        PendingIntent pi = createPendingIntent(context);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, pi);
    }

    public static void requestSingleUpdate(Context context, LocationManager locationManager) {
        PendingIntent pi = createPendingIntent(context);
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
        //criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.requestSingleUpdate(criteria, pi);
    }


    // ===========================================================
    //   Receiver
    // ===========================================================

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // Key
        String locationKey = LocationManager.KEY_LOCATION_CHANGED;
        String providerEnabledKey = LocationManager.KEY_PROVIDER_ENABLED;
        // Provider
        if (intent.hasExtra(providerEnabledKey)) {
            if (!intent.getBooleanExtra(providerEnabledKey, true)) {
               // Intent providerDisabledIntent = new Intent(PlacesConstants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
               // context.sendBroadcast(providerDisabledIntent);
            }
        }
        // Location
        if (intent.hasExtra(locationKey)) {
            Bundle b = intent.getExtras();
            Location location = (Location)b.get(locationKey);
            Log.d(TAG, "Actively Updating place list for Location : " + location);
        }
    }


}
