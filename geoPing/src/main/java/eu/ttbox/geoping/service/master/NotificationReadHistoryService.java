package eu.ttbox.geoping.service.master;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.SmsLogProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;


public class NotificationReadHistoryService extends IntentService  {

    private static final String TAG = "NotificationReadHistoryService";


    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationReadHistoryService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // service

    }


    // ===========================================================
    // Handle Message
    // ===========================================================

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, String.format("###  onHandleIntent for action %s : %s", action, intent));
        // Log Uri
        Uri logUri = extraSmsLogUri(intent);
        if (logUri!=null) {
            resetReadLog(logUri, Boolean.FALSE, null);
        }
        // Show Notification
       Intent serviceIntent =  intent.getParcelableExtra(Intents.EXTRA_INTENT);
        if (serviceIntent!=null) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(MainActivity.class);
            stackBuilder.addNextIntent(new Intent( getApplicationContext(), MainActivity.class));
            stackBuilder.addNextIntent(serviceIntent);
        }

    }

    private Uri extraSmsLogUri(Intent intent) {
        Uri logUri = null;
        String logUriString = intent.getStringExtra(Intents.EXTRA_SMSLOG_URI);
        if (logUriString!=null) {
            logUri = Uri.parse(logUriString);
        }
        return logUri;
    }

    // ===========================================================
    // Message Read History
    // ===========================================================


    private void resetReadLog(String phone) {
        Uri searchUri = GeoTrackerProvider.Constants.getContentUriPhoneFilter(phone);
        resetReadLog(searchUri, Boolean.TRUE, SmsLogDatabase.SmsLogColumns.SELECT_BY_IS_NOT_READ);
    }

    private void resetReadLog(Uri entityUri, Boolean readStatus, String whereClase) {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues(1);
        values.put(SmsLogDatabase.SmsLogColumns.COL_IS_READ, readStatus);
        long begin = System.currentTimeMillis();
        int count =  cr.update(entityUri, values, SmsLogDatabase.SmsLogColumns.SELECT_BY_IS_NOT_READ , null );
        Log.d(TAG, "Reset Read " +count+  " Logs in  " + (System.currentTimeMillis()-begin) + " ms.");
    }


    private static int getReadLogHistory( Context context, String phone) {
        NotificationCompat.InboxStyle inBoxStyle = null;
        Uri searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(searchUri, SmsLogDatabase.SmsLogColumns.ALL_COLS, SmsLogDatabase.SmsLogColumns.SELECT_BY_IS_NOT_READ, null,
                SmsLogDatabase.SmsLogColumns.ORDER_BY_TIME_DESC);
        int count = 0;
        try {
            count = cursor.getCount();
            if (count>0) {
                inBoxStyle = new NotificationCompat.InboxStyle();

//            if (cursor.moveToNext()) {
//                GeoTrackHelper helper = new GeoTrackHelper().initWrapper(cursor);
//            }
            }
        } finally {
            cursor.close();
        }
        return count;
    }


}
