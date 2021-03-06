package eu.ttbox.geoping;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.crypto.PRNGFixes;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.message.MessageDatabase;
import eu.ttbox.geoping.domain.pairing.GeoFenceDatabase;
import eu.ttbox.geoping.domain.pairing.PairingDatabase;
import eu.ttbox.geoping.domain.person.PersonDatabase;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;

//import eu.ttbox.geoping.domain.crypto.SecureDatabase;

public class GeoPingApplication extends Application {

    private String TAG = "GeoPingApplication";

    /* define your web property ID obtained after profile creation for the app */

    /* Analytics tracker instance */
//    private GoogleAnalytics tracker;

    private static GeoPingApplication APP_INSTANCE;

    private static final boolean DEVELOPPER_MODE = BuildConfig.DEBUG;

    // Cache
    private PhotoThumbmailCache photoCache;

    // DataBase
    private SmsLogDatabase smsLogDatabase;
    private GeoTrackDatabase geoTrackDatabase;
    // private SecureDatabase secureDatabase;
    private Tracker mTracker = null;

    private long lastActivityDate = Long.MIN_VALUE;

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate() {
        // Strict Mode
//        initStrictMode();
        // Create Application
        super.onCreate();
        APP_INSTANCE = this;
        // Security Path
        PRNGFixes.apply();

        // Perform the initialization that doesn't have to finish immediately.
        // We use an async task here just to avoid creating a new thread.
        (new DelayedInitializer(2000)).execute();
    }

    public static GeoPingApplication getInstance() {
        return APP_INSTANCE;
    }

    private class DelayedInitializer extends AsyncTask<Void, Void, Integer> {

        long delayInMs;

        public DelayedInitializer(long delayInMs) {
            super();
            this.delayInMs = delayInMs;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            final Context context = GeoPingApplication.this;
            try {
                Thread.sleep(delayInMs);
            } catch (InterruptedException e) {
                Log.d(TAG, "InterruptedException " + e.getMessage());
            }
            // Increment Counter Laught
            int launchCount = incrementApplicationLaunchCounter(context);
            Log.d(TAG, "================ Geoping Launch count = " + launchCount + "  ======================================");
            return launchCount;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

        }
    }


    // ===========================================================
    // Statistic
    // ===========================================================

    private int incrementApplicationLaunchCounter(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefEditor = settings.edit();
        // Launch count
        int counter = incrementKey(settings, prefEditor, AppConstants.PREFS_APP_LAUGHT_COUNT);
        // Last Launch version
        int appVersionCode = versionCode();
        int appPreviousVersionCode =  settings.getInt(AppConstants.PREFS_APP_LAUGHT_LASTVERSION, Integer.MIN_VALUE);
        if (appVersionCode>appPreviousVersionCode) {
            prefEditor.putInt(AppConstants.PREFS_APP_LAUGHT_LASTVERSION, appVersionCode);
            // TODO display release notes
        }
        // Launch date
        long firstDateLaugth = settings.getLong(AppConstants.PREFS_APP_LAUGHT_FIRSTDATE, Long.MIN_VALUE);
        long now = System.currentTimeMillis();
        prefEditor.putLong(AppConstants.PREFS_APP_LAUGHT_LASTDATE, now);
        if (Long.MIN_VALUE == firstDateLaugth) {
            prefEditor.putLong(AppConstants.PREFS_APP_LAUGHT_FIRSTDATE, now);
        }
        // Commit modifs
        prefEditor.commit();
        return counter;
    }

    private int incrementKey(SharedPreferences settings, SharedPreferences.Editor prefEditor, String pkey) {
        int previousCount = settings.getInt(pkey, 0);
        int incVal = 1 + previousCount;
        prefEditor.putInt(pkey, incVal);
        Log.d(TAG, "### Increment " + pkey + " : " + previousCount + " ===> " + incVal);
        return incVal;
    }

    // ===========================================================
    // Life Cycle
    // ===========================================================

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (photoCache != null) {
            photoCache.onLowMemory();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (photoCache != null) {
            photoCache.onTrimMemory(level);
        }
    }

    @Override
    public void onTerminate() {
      //  GAServiceManager.getInstance().dispatch();
        super.onTerminate();
    }

    // ===========================================================
    // Accessors
    // ===========================================================

    /**
     * Get Application Version
     *
     * @return
     */
    public String version() {
        return String.format("Version : %s/%s", getPackageName(), versionName());
    }

    public String versionPackageName() {
        return String.format("%s/%s", getPackageName(), versionName());
    }

    public String versionName() {
        try {
            final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } // try
        catch (PackageManager.NameNotFoundException nnfe) {
            return "Unknown";
        }
    }
    public int versionCode() {
        try {
            final PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionCode;
        } // try
        catch (PackageManager.NameNotFoundException nnfe) {
            return 0;
        }
    }


    // ===========================================================
    // Google Analytic
    // ===========================================================

    public Tracker getTracker() {
        if (mTracker==null) {
            createTracker();
        }
        return mTracker;
    }

    private synchronized Tracker createTracker() {
        if (mTracker==null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t =   analytics.newTracker(R.xml.analytics_global_tracker) ;
            mTracker = t;
        }
        return mTracker ;
    }

    // ===========================================================
    // Photo Cache
    // ===========================================================

    public PhotoThumbmailCache getPhotoThumbmailCache() {
        if (photoCache == null) {
            photoCache = initPhotoThumbmailCache();
        }
        return photoCache;
    }

    private synchronized PhotoThumbmailCache initPhotoThumbmailCache() {
        PhotoThumbmailCache photoCacheLocal = photoCache;
        if (photoCache == null) {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            int memoryClassBytes = am.getMemoryClass() * 1024 * 1024;
            int maxSizeBytes = memoryClassBytes / 8; // 307000 * 10
            photoCacheLocal = new PhotoThumbmailCache(maxSizeBytes);
            Log.d(TAG, "Create Cache of PhotoThumbmailCache wih size " + maxSizeBytes);
        }
        return photoCacheLocal;
    }

    // ===========================================================
    // Database instance
    // ===========================================================

    public SmsLogDatabase getSmsLogDatabase() {
        if (smsLogDatabase == null) {
            smsLogDatabase = new SmsLogDatabase(this);
        }
        return smsLogDatabase;
    }
    PersonDatabase personDatabase;
    PairingDatabase pairingDatabase;

    public PersonDatabase getPersonDatabase() {
        if (personDatabase == null) {
            personDatabase = new PersonDatabase(this);
        }
        return personDatabase;
    }

    public PairingDatabase getPairingDatabase() {
        if (pairingDatabase == null) {
            pairingDatabase = new PairingDatabase(this);
        }
        return pairingDatabase;
    }

    public GeoTrackDatabase getGeoTrackDatabase() {
        if (geoTrackDatabase == null) {
            geoTrackDatabase = new GeoTrackDatabase(this);
        }
        return geoTrackDatabase;
    }

    MessageDatabase messageDatabase;

    public MessageDatabase getMessageDatabase() {
        if (messageDatabase == null) {
            messageDatabase = new MessageDatabase(this);
        }
        return messageDatabase;
    }

    GeoFenceDatabase geoFenceDatabase;

    public GeoFenceDatabase getGeoFenceDatabase() {
        if (geoFenceDatabase == null) {
            geoFenceDatabase = new GeoFenceDatabase(this);
        }
        return geoFenceDatabase;
    }

   /* public SecureDatabase getSecureDatabase() {
        if (secureDatabase==null) {
            String password = "ddzsmj,rdzm,rmzkrz";
            secureDatabase = new SecureDatabase(this, password);
        }
        return secureDatabase;
    }*/


    // ===========================================================
    // Dev
    // ===========================================================

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initStrictMode() {
        if (VersionUtils.isHc11 && DEVELOPPER_MODE) {
            StrictMode.setThreadPolicy( //
                    new StrictMode.ThreadPolicy.Builder()//
                            .detectDiskReads()//
                            .detectDiskWrites() //
                            .detectNetwork() //
                            .penaltyFlashScreen() //
                            .penaltyLog()//
                            .build());
        }
    }

    // ===========================================================
    // Login
    // ===========================================================

    public long getLastActivityDate() {
        return this.lastActivityDate;
    }

  //  public boolean isLastActivityDateRecent() {
  //      return this.lastActivityDate;
  //  }


    public void  setLastActivityDate() {
        long now = System.currentTimeMillis();
        setLastActivityDate(now);
    }

    public void  setLastActivityDate(long date) {
        this.lastActivityDate = date;
    }

    // ===========================================================
    // Other
    // ===========================================================

    public static GeoPingApplication getGeoPingApplication(Context context) {
        return (GeoPingApplication) context.getApplicationContext();
    }

}
