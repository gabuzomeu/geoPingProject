package eu.ttbox.geoping.service.receiver;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import eu.ttbox.geoping.BuildConfig;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.utils.sensor.BatterySensorReplyFutur;

/**
 * https://code.google.com/p/android-protips-location/source/browse/trunk/src/com/radioactiveyak/location_best_practices/receivers/LocationChangedReceiver.java
 */
public class LocationChangeReceiver extends BroadcastReceiver {

    protected static String TAG = "LocationChangedReceiver";


    private static final int REQUEST_CODE_NOT_USED = 0;


    // ===========================================================
    //   Static Accessor
    // ===========================================================


    public static PendingIntent createPendingIntent(Context context, String[] phones, MessageActionEnum smsAction, Bundle eventParams) {
        Intent intent = new Intent(context, LocationChangeReceiver.class);
        intent.putExtra(Intents.EXTRA_SMS_ACTION, smsAction.intentAction);
        intent.putExtra(Intents.EXTRA_SMS_PHONE, phones);
        intent.putExtra(Intents.EXTRA_SMS_PARAMS, eventParams);
        // Pending
        PendingIntent pi = PendingIntent.getBroadcast(context, REQUEST_CODE_NOT_USED, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return pi;
    }

    public static void requestLocationUpdates(Context context, LocationManager locationManager, String[] phones, MessageActionEnum smsAction, Bundle eventParams) {
        PendingIntent pi = createPendingIntent(context, phones, smsAction, eventParams);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, pi);
    }

    public static void requestSingleUpdate(Context context, LocationManager locationManager, String[] phones, MessageActionEnum smsAction, Bundle eventParams) {
        PendingIntent pi = createPendingIntent(context, phones, smsAction, eventParams);
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
        // Battery
        // FIXME ReceiverCallNotAllowedException: IntentReceiver components are not allowed to register to receive intents
        BatterySensorReplyFutur battery = null;//new BatterySensorReplyFutur(context);


        // Action
        String smsAction = intent.getStringExtra(Intents.EXTRA_SMS_ACTION);
        MessageActionEnum eventType = MessageActionEnum.getByIntentName(smsAction);
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
            // Read Battery
            if (battery!=null) {
                geotrack.batteryLevelInPercent = battery.getOrNull(1, TimeUnit.SECONDS);
            }
            // Convert Result
            Bundle params = GeoTrackHelper.getBundleValues(geotrack);
            // Add All Specific extra values
            if (extrasBundles != null && !extrasBundles.isEmpty()) {
                params.putAll(extrasBundles);
            }
            SmsSenderHelper.sendSmsAndLogIt(context, SmsLogSideEnum.SLAVE, phones, eventType, params);
            saveInLocalDb(context, geotrack, phones);
        }
    }

    // ===========================================================
    //   Local Db Saver
    // ===========================================================


    private static boolean isSaveInLocalDb(Context context) {
        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return appPreferences.getBoolean(AppConstants.PREFS_LOCAL_SAVE, false);
    }

    private static Uri[] saveInLocalDb(Context context, GeoTrack geotrack, String[] phones) {
        Uri[] result = null;
        if (geotrack == null || !isSaveInLocalDb(context)) {
            return result;
        }
        // Add Phone
        if (phones != null && phones.length > 0) {
            int phoneSize = phones.length;
            result = new Uri[phoneSize];
            // Preserve Previous Model
            String previous = geotrack.requesterPersonPhone;
            for (int i = 0; i < phoneSize; i++) {
                String phone = phones[i];
                geotrack.requesterPersonPhone = phone;
                // Save in DB
                ContentValues values = GeoTrackHelper.getContentValues(geotrack);
                values.put(GeoTrackDatabase.GeoTrackColumns.COL_PHONE, AppConstants.KEY_DB_LOCAL);
                result[i] = context.getContentResolver().insert(GeoTrackerProvider.Constants.CONTENT_URI, values);
            }
            // restore
            geotrack.requesterPersonPhone = previous;
        }
        return result;
    }

    // ===========================================================
    //  Other
    // ===========================================================


}