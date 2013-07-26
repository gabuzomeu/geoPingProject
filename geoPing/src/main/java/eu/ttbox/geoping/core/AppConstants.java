package eu.ttbox.geoping.core;

import java.util.Calendar;

public class AppConstants {

    public static final double E6 = 1000000d;
    public static final int ONE_HOUR_IN_S= 3600 ;
    public static final int ONE_DAY_IN_S= ONE_HOUR_IN_S*24 ;
    // Constant
    public static final long UNSET_TIME = -1l;
    public static final long UNSET_ID = -1l;

    public static final long DATE_ZERO = getZeroDate();

    //  Config
    public static final String MARKET_LINK = "market://details?id=eu.ttbox.geoping"; // "https://play.google.com/store/apps/details?id=eu.ttbox.geoping";
    public static final String ANALYTICS_KEY = "UA-36410991-1";
    public static final  String BILLING_BASE64ENCODED_PUBLICKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAg6EIpPnbaQ73nK3psbyxspmlEBK4cE9MpDUIS492zPg0h++6tgx7bvSKNK8COrxtDCIUE3A4XxJkLoqxGupdpYBPWdwsNGP67VMDgjLaC2TP8EQRFEHEEZFUuIaY8LPKXsP5QhfEKKFTZxHs/fav0olvVDhZ1MnB+SO6ZbRw/GmZE4ILQMIURn5bypX248OMTwDwrESqVwWKH4165SzM9VeI8/iVAsxnDDG1VfQ8Gnfi4QjyZKG5U9jRyt0iIMnV3LOhkk549Zjv3oLS7R02kcjIfigBztB4P6+MXwZ/5DlN7CKmxn+5IiTACSb4LEoPrekw0DNG+bHaxdpz/fEimQIDAQAB";
    
    // "market://search?q=pname:eu.ttbox.geoping"
    // "market://search?q=pub:eu.ttbox.geoping"

    //
    public static final String LOCAL_DB_KEY = "local";
    public static final int SMS_MAX_SIZE_7BITS = 160;
    public static final int SMS_MAX_SIZE_8BITS = 140;
    public static final int SMS_MAX_BITS_ARRAY_SIZE =  1120;
    
    public static final char PHONE_SEP = ';';

    public static final int PER_PERSON_ID_MULTIPLICATOR = 10000;
    public static final String KEY_DB_LOCAL = "local";

    // Request Timeout
    public static final String PREFS_REQUEST_TIMEOUT_S = "requestTimeoutInS";
    public static final String PREFS_REQUEST_ACCURACY_M = "requestAccuracyExpectedInM";

    // Request Notification
    public static final String PREFS_SMS_DELETE_ON_MESSAGE = "smsDeleteOnMessage";
    public static final String PREFS_SHOW_GEOPING_NOTIFICATION = "showGeopingNotification";
    public static final String PREFS_LOCAL_SAVE = "localSave";
    public static final String PREFS_AUTHORIZE_GEOPING_PAIRING = "authorizeNewGeopingPairing";
    
    // Spy Notification TODO
    public static final String PREFS_EVENT_SPY_SHUTDOWN_SLEEP_IN_MS  = "spyEventShutdownSleepInMs";
    public static final String PREFS_EVENT_SPY_SIMCHANGE_PHONENUMBER  = "spyEventSimChangePhoneNumber";
    
    public static final String PREFS_ADD_BLOCKED = "addBlocked";
    // Map
    public static final String PREFS_KEY_MYLOCATION_DISPLAY_GEOLOC = eu.ttbox.osm.core.AppConstants.PREFS_KEY_MYLOCATION_DISPLAY_GEOLOC;
    public static final String PREFS_GEOPOINT_GEOCODING_AUTO = "geoPointGeocodingAuto";

    // TODO in prefs

    // public static final String PREFS_KEY_TILE_SOURCE = "KEY_TILE_SOURCE";

    public static final String PREFS_APP_LAUGHT_COUNT = "APP_LAUGHT_COUNT";
    public static final String PREFS_APP_LAUGHT_LASTDATE = "APP_LAUGHT_LASTDATE";
    public static final String PREFS_APP_LAUGHT_LASTVERSION = "APP_LAUGHT_LASTVERSION";
    public static final String PREFS_APP_LAUGHT_FIRSTDATE = "APP_LAUGHT_FIRSTDATE";

    private static long getZeroDate() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, 2012);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 22);
        // Midnight
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long timeAtMidnight = cal.getTimeInMillis();
        return timeAtMidnight;
    }
}
