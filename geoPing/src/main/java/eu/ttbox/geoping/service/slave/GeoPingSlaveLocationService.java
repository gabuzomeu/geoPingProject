package eu.ttbox.geoping.service.slave;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase.GeoTrackColumns;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.service.core.WorkerService;
import eu.ttbox.geoping.utils.encoder.MessageEncoderHelper;
import eu.ttbox.osm.core.LocationUtils;
import eu.ttbox.osm.ui.map.mylocation.sensor.v2.OsmAndLocationProvider;
import eu.ttbox.osm.ui.map.mylocation.sensor.v2.OsmLocation;

public class GeoPingSlaveLocationService extends WorkerService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "GeoPingSlaveLocationService";

    private static final String LOCK_NAME = "GeoPingSlaveLocationService";

    private static final String ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING = "ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING";

    private final IBinder binder = new LocalBinder();

    // Services
    private TelephonyManager telephonyManager;
    private LocationManager locationManager;
    private OsmAndLocationProvider myLocation;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private SharedPreferences appPreferences;

    // Config
    private boolean saveInLocalDb = false;

    // Instance Data

    private MultiGeoRequestLocationListener multiGeoRequestListener = new MultiGeoRequestLocationListener();

    private int batterLevelInPercent = -1;

    // ===========================================================
    // Lock
    // ===========================================================

    private static volatile PowerManager.WakeLock lockStatic = null;

    // public static void runIntentInService(Context context, Intent intent) {
    // PowerManager.WakeLock lock = getLock(context);
    // lock.acquire();
    // intent.setClassName(context, GeoPingSlaveService.class.getName());
    // context.startService(intent);
    // }
    private synchronized static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME);
            lockStatic.setReferenceCounted(true);
        }
        return (lockStatic);
    }

    // ===========================================================
    // Constructors
    // ===========================================================

    public GeoPingSlaveLocationService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "#####################################");
        Log.d(TAG, "### GeoPing Location Service Started.");
     //   Log.d(TAG, "#####################################");
        // service
        this.appPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        this.telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        this.myLocation = new OsmAndLocationProvider((Application)getApplicationContext());

        loadPrefConfig();
        // register listener
        appPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void loadPrefConfig() {
        this.saveInLocalDb = appPreferences.getBoolean(AppConstants.PREFS_LOCAL_SAVE, false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppConstants.PREFS_LOCAL_SAVE)) {
            this.saveInLocalDb = appPreferences.getBoolean(AppConstants.PREFS_LOCAL_SAVE, false);
        }
    }

    @Override
    public void onDestroy() {
        appPreferences.unregisterOnSharedPreferenceChangeListener(this);
        this.myLocation.pauseAllUpdates(); //stopListening();
        multiGeoRequestListener.clear();
        super.onDestroy();
  //      Log.d(TAG, "#######################################");
        Log.d(TAG, "### GeoPing Location Service Destroyed.");
        Log.d(TAG, "#######################################");
    }

    // ===========================================================
    // Intent Handler
    // ===========================================================

    public static void runFindLocationAndSendInService(Context context, MessageActionEnum smsAction , String[] phone, Bundle params,  Bundle config) {
        // PowerManager.WakeLock lock = getLock(context);
        // lock.acquire();
        if (!smsAction.isConsumeMaster ) {
            throw new RuntimeException("It shoud be a Master consumer GeoPing Action Service for : " + smsAction);
        }
        
        Intent intent = new Intent(context, GeoPingSlaveLocationService.class);
        intent.putExtra(Intents.EXTRA_SMS_PHONE, phone);
        intent.putExtra(Intents.EXTRA_SMS_PARAMS, params);
        intent.putExtra(Intents.EXTRA_SMS_CONFIG, config);

        intent.putExtra(Intents.EXTRA_SMS_ACTION, smsAction.intentAction);
               
        intent.setAction(ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onHandleIntent : " + action);
        if (ACTION_FIND_LOCALISATION_AND_SEND_SMS_GEOPING.equals(action)) {
            String[] phone = intent.getStringArrayExtra(Intents.EXTRA_SMS_PHONE);
            Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
            Bundle config = intent.getBundleExtra(Intents.EXTRA_SMS_CONFIG);
            // Action
            // GeoPing Request
            String smsAction = intent.getStringExtra(Intents.EXTRA_SMS_ACTION);
            MessageActionEnum smsActionMsg = MessageActionEnum.getByIntentName(smsAction);
            registerGeoPingRequest(smsActionMsg, phone, params, config);
        }

    }

    // ===========================================================
    // Cell Id
    // ===========================================================

    /**
     * {link http://www.devx.com/wireless/Article/40524/0/page/2}
     */
    private int[] getCellId() {
        int[] cellId = new int[0];
        CellLocation cellLoc = telephonyManager.getCellLocation();
        if (cellLoc != null && (cellLoc instanceof GsmCellLocation)) {
            GsmCellLocation gsmLoc = (GsmCellLocation) cellLoc;
            gsmLoc.getPsc();
            // gsm cell id
            int cid = gsmLoc.getCid();
            // gsm location area code
            int lac = gsmLoc.getLac();
            // On a UMTS network, returns the primary scrambling code of the
            // serving cell.
            int psc = gsmLoc.getPsc();
            Log.d(TAG, String.format("Cell Id : %s  / Lac : %s  / Psc : %s", cid, lac, psc));
            if (psc > -1) {
                cellId = new int[3];
                cellId[2] = psc;
            } else {
                cellId = new int[2];
            }
            cellId[0] = cid;
            cellId[1] = lac;
        }
        return cellId;
    }

    private void getNeighboringCellId() {
        List<NeighboringCellInfo> neighCell = null;
        neighCell = telephonyManager.getNeighboringCellInfo();
        for (int i = 0; i < neighCell.size(); i++) {
            NeighboringCellInfo thisCell = neighCell.get(i);
            int cid = thisCell.getCid();
            int rssi = thisCell.getRssi();
            int psc = thisCell.getPsc();
            Log.d(TAG, " " + cid + " - " + rssi + " - " + psc);
        }
    }

    // ===========================================================
    // Geocoding Request
    // ===========================================================

    public boolean registerGeoPingRequest(MessageActionEnum smsAction ,  String[] phoneNumber, Bundle params,  Bundle config) {
        boolean locProviderEnabled = false;
        synchronized (multiGeoRequestListener) {
            // Acquire Lock
            PowerManager.WakeLock lock = getLock(this.getApplicationContext());
            lock.acquire();
            Log.d(TAG, "*** **************************************************************************** ***");
            Log.d(TAG, "*** Lock Acquire: " + LOCK_NAME + " " + lock);
            // Register request
            OsmLocation loc = myLocation.getLastKnownLocation();
            Location initLastLoc = loc!=null ? loc.getLocation() : null;
            GeoPingRequest request = new GeoPingRequest(smsAction, phoneNumber, params, config);
            multiGeoRequestListener.add(request);
            Log.d(TAG, "multiGeoRequestListener size : " + multiGeoRequestListener.size());
//            myLocation.addLocationListener(request);
            // TODO Bad for multi request
            myLocation.addLocationListener(multiGeoRequestListener);
            myLocation.resumeAllUpdates();
//            locProviderEnabled = myLocation.startListening(multiGeoRequestListener);
            // Schedule it for the time out 
            int timeOutInSeconde =  MessageEncoderHelper.readInt(params, MessageParamEnum.TIME_IN_S, 30);
            ScheduledFuture<Boolean> task = executorService.schedule(request, timeOutInSeconde, TimeUnit.SECONDS);
            request.meTask = task;
        }
        return locProviderEnabled;
    }

    public void unregisterGeoPingRequest(GeoPingRequest request) {
        synchronized (multiGeoRequestListener) {
            boolean isRemove = multiGeoRequestListener.remove(request);
            if (isRemove) {
                Log.d(TAG, "Remove GeoPing Request in list, do Stop Service");
            } else {
                Log.e(TAG, "###################################################################################");
                Log.e(TAG, "### Could not remove expected GeoPingRequest. /!\\ Emmergency Stop Service /!\\ ###");
                Log.e(TAG, "###################################################################################");
                // TODO ??? multiGeoRequestListener.clear();
            }
            // Release Lock
            PowerManager.WakeLock lock = getLock(this.getApplicationContext());
            if (lock.isHeld()) {
                lock.release();
            }
            Log.d(TAG, "*** Lock Release: " + LOCK_NAME + " " + lock);
            Log.d(TAG, "*** **************************************************************************** ***");
            // Stop Service if necessary
            if (multiGeoRequestListener.isEmpty()) {
                Log.d(TAG, "No GeoPing Request in list, do Stop Service");
                myLocation.pauseAllUpdates();
                // Stop Service
                stopSelf();
            } else {
                Log.d(TAG, "Do not stop service for waiting multiGeoRequestListener : " + multiGeoRequestListener.size());
            }
        }
    }

    // ===========================================================
    // Sensor Listener
    // ===========================================================

    /**
     * Computes the battery level by registering a receiver to the intent
     * triggered by a battery status/level change. <br/>
     * <a href="http://developer.android.com/training/monitoring-device-state/battery-monitoring.html">monitoring-device-state</a>
     */

    private void batteryLevel() {
        BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int level = -1;
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                Log.d(TAG, "Battery Level Remaining: " + level + "%");
                batterLevelInPercent = level;
            }
        };

        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
    }

    public class GeoPingRequest implements Callable<Boolean>, LocationListener, OsmAndLocationProvider.OsmAndLocationListener {

        private static final int LOCALISATION_SIGNIFICATY_NEWER_IN_MS = 1000 * 60 * 1;
        // Context
        private MessageActionEnum smsAction;
        private  String[] smsPhoneNumber;
        private Bundle params;
        // Config
        private int accuracyExpected = -1;
        private boolean isAccuracyExpectedCheck = false;
        // Task Reference
        ScheduledFuture<Boolean> meTask;

        public GeoPingRequest() {
            super();
        }

        public GeoPingRequest(MessageActionEnum smsAction,  String[] phoneNumber, Bundle params, Bundle config) {
            super();
            this.smsAction = smsAction;
            this.smsPhoneNumber = phoneNumber;
            this.params = params;
            // Read Config
            this.accuracyExpected =  MessageEncoderHelper.readInt(config, MessageParamEnum.ACCURACY, -1);
            this.isAccuracyExpectedCheck = accuracyExpected > -1;
            // register Listener for Battery Level
            batteryLevel();
        }

        @Override
        public Boolean call() throws Exception {
            Boolean result = Boolean.FALSE;
            try {
                Location lastLocation = myLocation.getLastFixAsLocation();
                Log.d(TAG, "Future Callable end with location : " + lastLocation);
                result = sendFoundLocation(lastLocation);
            } finally {
                unregisterGeoPingRequest(GeoPingRequest.this);
            }
            return result;
        }

        private Boolean sendFoundLocation(Location lastLocation) {
            Boolean isDone = Boolean.FALSE;
            // TODO Cell Id
           // int[] cellId = getCellId();
            // Location
            if (lastLocation != null) {
                Log.d(TAG, "sendFoundLocation with accuracy= " + lastLocation.getAccuracy()
                        + " / Time : "  + DateUtils.formatDateRange(getApplicationContext(),lastLocation.getTime(), lastLocation.getTime(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE ) + " : " + lastLocation);
                sendSmsLocation(smsAction, smsPhoneNumber, lastLocation, params);
                isDone = Boolean.TRUE;
            }
            return isDone;
        }

        @Override
        public void onLocationChanged(OsmLocation location) {
            Location loc = location!=null ? location.getLocation() : null;
            onLocationChanged(loc);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (isAccuracyExpectedCheck && location != null) {
                // Check expected accuracy
                int locAcc = (int) location.getAccuracy();
                if (locAcc <= accuracyExpected) {
                    // Test Date, in case of a very old Location
                    long now = System.currentTimeMillis();
                    Log.d(TAG, "The Current Time is : "
                            + DateUtils.formatDateRange(getApplicationContext(),now, now, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE )
                    );
                    if (LocationUtils.isLocationTooOld(location, now)) {
                        Log.d(TAG, "Ignore too old LocationChanged : "
                                + DateUtils.formatDateRange(getApplicationContext(),location.getTime(), location.getTime(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE )
                                + " : " + location.getProvider() + " +/-"+ locAcc+ "m");
                        return;
                    }
                    // Unregister Request
                    Log.d(TAG, "Keep enough accuracy LocationChanged : "
                            + DateUtils.formatDateRange(getApplicationContext(),location.getTime(), location.getTime(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE )
                            + " : " + location.getProvider() + " +/-"+ locAcc+ "m");

                    Boolean isDone = sendFoundLocation(location);
                    if (isDone.booleanValue()) {
                        // Ignore next match
                        isAccuracyExpectedCheck = false;
                        // Unregister event
                        boolean isMeTaskCancel = meTask.cancel(false);
                        Log.d(TAG, "Future Cancel Callable for expected accuracy [" +accuracyExpected +
                                "] end with location : " + location);
                        if (isMeTaskCancel) {
                            unregisterGeoPingRequest(GeoPingRequest.this);
                        }
                    }
                }
            }
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

    // ===========================================================
    // Sender Sms message
    // ===========================================================

    private void sendSmsLocation(MessageActionEnum smsAction,  String[] phones, Location location, Bundle extrasBundles) {
        GeoTrack geotrack = new GeoTrack(null, location);
        geotrack.batteryLevelInPercent = batterLevelInPercent;
        Bundle params = GeoTrackHelper.getBundleValues(geotrack); 
        // Add extrasBundles to params
        if (extrasBundles!=null && !extrasBundles.isEmpty()) {
            Bundle completParam = new Bundle();
            completParam.putAll(extrasBundles);
            completParam.putAll(params);
            // Switch Param
            params = completParam;
        }
        for (String phone : phones) {
            SmsSenderHelper.sendSmsAndLogIt(this, SmsLogSideEnum.SLAVE,  phone, smsAction, params);
            if (saveInLocalDb) {
                geotrack.requesterPersonPhone = phone;
                saveInLocalDb(geotrack, phone);
            }
        }
    }

    private void saveInLocalDb(GeoTrack geotrack, String phone) {
        if (geotrack == null) {
            return;
        }
        // Add Phone
        String previous =  geotrack.requesterPersonPhone;
        geotrack.requesterPersonPhone = phone;
        // Convert
        ContentValues values = GeoTrackHelper.getContentValues(geotrack);
        values.put(GeoTrackColumns.COL_PHONE, AppConstants.KEY_DB_LOCAL);
        getContentResolver().insert(GeoTrackerProvider.Constants.CONTENT_URI, values);
        // restore
        geotrack.requesterPersonPhone = previous;
    }

    // ===========================================================
    // Binder
    // ===========================================================

    public class LocalBinder extends Binder {
        public GeoPingSlaveLocationService getService() {
            return GeoPingSlaveLocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

}
