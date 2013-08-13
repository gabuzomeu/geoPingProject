package eu.ttbox.geoping.service.receiver;

import android.app.IntentService;
import android.app.PendingIntent;
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
import eu.ttbox.geoping.domain.SmsLogProvider;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;


public class LogReadHistoryService extends IntentService {

    private static final String TAG = "LogReadHistoryService";

    public static final String ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ = "eu.ttbox.geoping.ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ";

    // ===========================================================
    // Constructor
    // ===========================================================


    public LogReadHistoryService() {
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
        Log.d(TAG, "### ############################################################### ###");
        Log.d(TAG, String.format("###  onHandleIntent for action %s : %s", action, intent));
        if (ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ.equals(action)) {
            // Log Uri
            Uri logUri = intent.getParcelableExtra(Intents.EXTRA_SMSLOG_URI);
            if (logUri != null) {
                markAsReadLog(this, logUri, Boolean.TRUE, null);
            }
            // Show Notification
            Intent serviceIntent = intent.getParcelableExtra(Intents.EXTRA_INTENT);
            Log.d(TAG, "### serviceIntent : " + serviceIntent);
            if (serviceIntent != null) {
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(MainActivity.class);
                stackBuilder.addNextIntent(new Intent(getApplicationContext(), MainActivity.class));
                stackBuilder.addNextIntent(serviceIntent);
                // Start activity
                stackBuilder.startActivities();
            }
        }
        Log.d(TAG, "### ############################################################### ###");
    }


    // ===========================================================
    // Message Read History
    // ===========================================================


    public static int getReadLogHistory(Context context, String phone, SmsLogSideEnum side) {
        NotificationCompat.InboxStyle inBoxStyle = null;
        Uri searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        ContentResolver cr = context.getContentResolver();
        String selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_IS_NOT_READ;
        String[] selectionArgs = null;
        if (side != null) {
            selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_ISNOTREAD_SIDE;
            selectionArgs = new String[]{String.valueOf(side.getDbCode())};
        }
        Cursor cursor = cr.query(searchUri, SmsLogDatabase.SmsLogColumns.ALL_COLS, selection, selectionArgs,
                SmsLogDatabase.SmsLogColumns.ORDER_BY_TIME_DESC);
        int count = 0;
        try {
            count = cursor.getCount();
            if (count > 0) {
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

    public static PendingIntent createClearLogPendingIntent(Context context, Intent wantedInted) {
        Intent readAction = new Intent(context, LogReadHistoryService.class);
        readAction.setAction(ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ);
        if (wantedInted != null) {
            readAction.putExtra(Intents.EXTRA_INTENT, wantedInted);
        }
        readAction.putExtra(Intents.EXTRA_SMSLOG_URI, SmsLogProvider.Constants.CONTENT_URI);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, readAction, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    public static void markAsReadLog(Context context, Uri entityUri, Boolean readStatus, String whereClase) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues(1);
        values.put(SmsLogDatabase.SmsLogColumns.COL_IS_READ, readStatus);
        long begin = System.currentTimeMillis();
        int count = cr.update(entityUri, values, SmsLogDatabase.SmsLogColumns.SELECT_BY_IS_NOT_READ, null);
        Log.d(TAG, "Reset Read " + count + " Logs in  " + (System.currentTimeMillis() - begin) + " ms.");
    }


    public void markAsReadLog(Context context, String phone) {
        Uri searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        markAsReadLog(context, searchUri, Boolean.TRUE, SmsLogDatabase.SmsLogColumns.SELECT_BY_IS_NOT_READ);
    }


}
