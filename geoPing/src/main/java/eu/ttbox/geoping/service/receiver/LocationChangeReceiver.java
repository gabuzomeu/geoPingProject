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

import eu.ttbox.geoping.BuildConfig;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;

/**
 * https://code.google.com/p/android-protips-location/source/browse/trunk/src/com/radioactiveyak/location_best_practices/receivers/LocationChangedReceiver.java
 */
public class LocationChangeReceiver extends BroadcastReceiver {

    protected static String TAG = "LocationChangedReceiver";


    private static final int REQUEST_CODE_NOT_USED = 0;


    // ===========================================================
    //   Static Accessor
    // ===========================================================


    public static PendingIntent createPendingIntent(Context context, String[] phones, Bundle eventParams) {
        Intent i = new Intent(context, LocationChangeReceiver.class);
        // TODO        i.setAction()
        i.putExtra(Intents.EXTRA_SMS_PHONE, phones);
        i.putExtra(Intents.EXTRA_SMS_PARAMS, eventParams);
        // Pending
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE_NOT_USED, i,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pi;
    }

    public static void requestLocationUpdates(Context context, LocationManager locationManager, String[] phones, Bundle eventParams) {
        PendingIntent pi = createPendingIntent(context, phones, eventParams);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, pi);
    }

    public static void requestSingleUpdate(Context context, LocationManager locationManager, String[] phones, Bundle eventParams) {
        PendingIntent pi = createPendingIntent(context, phones, eventParams);
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(false);
        //criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationManager.requestSingleUpdate(criteria, pi);
    }



    // ===========================================================
    //   Printer
    // ===========================================================

    private void printExtras(Bundle extras) {
        if (BuildConfig.DEBUG) {
            Intents.printExtras(TAG, extras);
        }
    }


    // ===========================================================
    //   Receiver
    // ===========================================================

    @Override
    public void onReceive(Context context, Intent intent) {
        //
        String action = intent.getAction();
        Bundle b = intent.getExtras();
        // Log
        Log.d(TAG, "--- ------------------------------------------------------- ---");
        Log.d(TAG, "--- Location onReceive : " + intent);
        printExtras(intent.getExtras());
        Log.d(TAG, "--- ------------------------------------------------------- ---");

        // Action
        MessageActionEnum eventType = null;// TODO
        // Read Intent
        String[] phones = b.getStringArray(Intents.EXTRA_SMS_PHONE);
        Bundle extrasBundles = b.getBundle(Intents.EXTRA_SMS_PARAMS);
        // Location
        Location location = null;
        if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
            location = (Location) b.get(LocationManager.KEY_LOCATION_CHANGED);
            Log.d(TAG, "### Receive Location : " + location);
        }
        // Provider
        if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) {
            if (!intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, true)) {
                // Intent providerDisabledIntent = new Intent(PlacesConstants.ACTIVE_LOCATION_UPDATE_PROVIDER_DISABLED);
                // context.sendBroadcast(providerDisabledIntent);
            }
        }

        // Send Sms
        if (location != null) {
            // Converter Location
            GeoTrack geotrack = new GeoTrack(null, location);
            Bundle params = GeoTrackHelper.getBundleValues(geotrack);
            // Add All Specific extra values
            if (extrasBundles != null && !extrasBundles.isEmpty()) {
                params.putAll(extrasBundles);
            }
            SmsSenderHelper.sendSmsAndLogIt(context, SmsLogSideEnum.SLAVE, phones, eventType, params);
            // TODO saveInLocalDb

        }


    }
}