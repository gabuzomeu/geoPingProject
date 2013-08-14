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
import android.text.TextUtils;
import android.util.Log;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.SmsLogProvider;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;


public class LogReadHistoryService extends IntentService {

    private static final String TAG = "LogReadHistoryService";

    public static final String ACTION_SMSLOG_MARK_AS_READ = "eu.ttbox.geoping.ACTION_SMSLOG_MARK_AS_READ";

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
    public static PendingIntent createClearLogPendingIntent(Context context,  SmsLogSideEnum side, String phone, Intent wantedIntent) {
        return createClearLogPendingIntent(context, side, phone, wantedIntent, 0);
    }

    public static PendingIntent createClearLogPendingIntent(Context context,  SmsLogSideEnum side, String phone
            , Intent wantedIntent, int baseRequestCode) {
        Intent readAction = new Intent(context, LogReadHistoryService.class);
        readAction.setAction(ACTION_SMSLOG_MARK_AS_READ);
        // Filter Log
        int requestCode = baseRequestCode;
        if (side!=null) {
            requestCode += (1 + side.getDbCode()) ;
            readAction.putExtra(Intents.EXTRA_SMSLOG_SIDE, side.getDbCode());
        }
        // Redirect Intent
        if (wantedIntent != null) {
            readAction.putExtra(Intents.EXTRA_INTENT, wantedIntent);
            requestCode +=  wantedIntent.hashCode();
        }
        Uri searchUri = SmsLogProvider.Constants.CONTENT_URI;
        if (!TextUtils.isEmpty(phone)) {
            searchUri =  SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        }
        readAction.putExtra(Intents.EXTRA_SMSLOG_URI, searchUri);
        // Create Pending
        PendingIntent pendingIntent = PendingIntent.getService(context, requestCode, readAction, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }


    // ===========================================================
    // Handle Message
    // ===========================================================

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "### ############################################################### ###");
        Log.d(TAG, String.format("###  onHandleIntent for action %s : %s", action, intent));
        if (ACTION_SMSLOG_MARK_AS_READ.equals(action)) {
            // Log Uri
            Uri logUri = intent.getParcelableExtra(Intents.EXTRA_SMSLOG_URI);
            Log.d(TAG, "### logUri : " + logUri);
            if (logUri != null) {
                int sideCode = intent.getIntExtra(Intents.EXTRA_SMSLOG_SIDE, -1);
                SmsLogSideEnum side = sideCode!=-1 ? SmsLogSideEnum.getByDbCode(sideCode): null ;
                Log.d(TAG, "### side : " + side);
                markAsToReadLog(this, logUri, Boolean.FALSE, side);
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
     //   NotificationCompat.InboxStyle inBoxStyle = null;
        // Compute Uri
        Uri searchUri =SmsLogProvider.Constants.CONTENT_URI;
        if (!TextUtils.isEmpty(phone)) {
              searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        }
        // Compute Where Clause
        String selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TO_READ;
        String[] selectionArgs = null;
        if (side != null) {
            selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TOREAD_SIDE;
            selectionArgs = new String[]{String.valueOf(side.getDbCode())};
        }
        // Query
        long begin = System.currentTimeMillis();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(searchUri, SmsLogDatabase.SmsLogColumns.ALL_COLS, selection, selectionArgs,
                SmsLogDatabase.SmsLogColumns.ORDER_BY_TIME_DESC);
        int count = 0;
        try {
            count = cursor.getCount();
            if (count > 0) {
//                inBoxStyle = new NotificationCompat.InboxStyle();
//            if (cursor.moveToNext()) {
//                SmsLogHelper helper = new SmsLogHelper().initWrapper(cursor);
//            }
            }
        } finally {
            cursor.close();
        }
        Log.d(TAG, "Count To ReadLog " + count + " Logs in  " + (System.currentTimeMillis() - begin) + " ms.");
        return count;
    }



    private static void markAsToReadLog(Context context, Uri entityUri, Boolean toReadStatus, SmsLogSideEnum side) {
        ContentResolver cr = context.getContentResolver();
        // Values
        ContentValues values = new ContentValues(1);
        if (toReadStatus==null || Boolean.FALSE.equals(toReadStatus)) {
            values.putNull(SmsLogDatabase.SmsLogColumns.COL_TO_READ);
        } else {
            values.put(SmsLogDatabase.SmsLogColumns.COL_TO_READ, toReadStatus);
        }
        // Where Clause
        String selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TO_READ;
        String[] selectionArgs = null;
        if (side != null) {
            selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TOREAD_SIDE;
            selectionArgs = new String[]{String.valueOf(side.getDbCode())};
        }
        // Update
        long begin = System.currentTimeMillis();
        int count = cr.update(entityUri, values, selection, selectionArgs);
        Log.d(TAG, "Mark As ToRead (" +toReadStatus+
                ") " + count + " ReadLogs in  " + (System.currentTimeMillis() - begin) + " ms for Uri "+ entityUri);
    }


    public static void markAsToReadLog(Context context, Uri entityUri, Boolean toReadStatus) {
        markAsToReadLog(context, entityUri, toReadStatus, null);
    }



}
