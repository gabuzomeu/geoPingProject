package eu.ttbox.geoping.service.receiver;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.SmsLogProvider;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;
import eu.ttbox.geoping.domain.smslog.SmsLogHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.core.MessageActionEnumLabelHelper;


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

    public static PendingIntent createClearLogPendingIntent(Context context, SmsLogSideEnum side, String phone, Intent wantedIntent) {
        return createClearLogPendingIntent(context, side, phone, wantedIntent, 0);
    }

    public static PendingIntent createClearLogPendingIntent(Context context, SmsLogSideEnum side, String phone
            , Intent wantedIntent, int baseRequestCode) {
        Intent readAction = new Intent(context, LogReadHistoryService.class);
        readAction.setAction(ACTION_SMSLOG_MARK_AS_READ);
        // Filter Log
        int requestCode = baseRequestCode;
        if (side != null) {
            requestCode += (1 + side.getDbCode());
            readAction.putExtra(Intents.EXTRA_SMSLOG_SIDE_DBCODE, side.getDbCode());
        }
        // Redirect Intent
        if (wantedIntent != null) {
            readAction.putExtra(Intents.EXTRA_INTENT, wantedIntent);
            requestCode += wantedIntent.hashCode();
        }
        Uri searchUri = SmsLogProvider.Constants.CONTENT_URI;
        if (!TextUtils.isEmpty(phone)) {
            searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
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
                int sideCode = intent.getIntExtra(Intents.EXTRA_SMSLOG_SIDE_DBCODE, -1);
                SmsLogSideEnum side = sideCode != -1 ? SmsLogSideEnum.getByDbCode(sideCode) : null;
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

    public static ArrayList<String> getReadLogHistoryGeofenceViolation(Context context, String phone, SmsLogSideEnum side) {
        Uri searchUri = SmsLogProvider.Constants.CONTENT_URI;
        if (!TextUtils.isEmpty(phone)) {
            searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        }
        // Compute Where Clause
        String[] projection = new String[]{
                SmsLogDatabase.SmsLogColumns.COL_ID, SmsLogDatabase.SmsLogColumns.COL_TIME
                , SmsLogDatabase.SmsLogColumns.COL_REQUEST_ID, SmsLogDatabase.SmsLogColumns.COL_ACTION
        }; //SmsLogDatabase.SmsLogColumns.ALL_COLS;
        String selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TO_READ;
        String[] selectionArgs = null;
        if (side != null) {
            selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TOREAD_SIDE;
            selectionArgs = new String[]{String.valueOf(side.getDbCode())};
        }
        selection += String.format(" and %s in ('%s', '%s')", SmsLogDatabase.SmsLogColumns.COL_ACTION,
                MessageActionEnum.GEOFENCE_ENTER.getDbCode(), MessageActionEnum.GEOFENCE_EXIT.getDbCode());
        // Query
        long begin = System.currentTimeMillis();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(searchUri, projection, selection, selectionArgs,
                SmsLogDatabase.SmsLogColumns.ORDER_BY_GEOFENCE_VIOLATION);
        ArrayList<String> notifLines = null;
        try {
            if (cursor.getCount()<1) {
                return null;
            }
            // Read Geofences
            notifLines = new ArrayList<String>();
            String lastRequestId = null;
            SmsLogHelper helper = new SmsLogHelper().initWrapper(cursor);
            while (cursor.moveToNext()) {
                String requestId = helper.getSmsLogGeofencRequestId(cursor);
                if (lastRequestId == null || !lastRequestId.equals(requestId)) {
                    MessageActionEnum actionEnum = helper.getSmsMessageActionEnum(cursor);
                    // TODO Label
                    String geofenceName = requestId;
                    String contentTitle = MessageActionEnumLabelHelper.getString(context, actionEnum, geofenceName);
                    notifLines.add(contentTitle);
                }
                lastRequestId = requestId;
            }
        } finally {
            cursor.close();
        }
        Log.d(TAG, "Geofences To ReadLog " + (notifLines!=null?notifLines.size():0) + " Logs in  " + (System.currentTimeMillis() - begin) + " ms.");
        return notifLines;
    }

    public static int getReadLogHistory(Context context, String phone, SmsLogSideEnum side) {
        //   NotificationCompat.InboxStyle inBoxStyle = null;
        // Compute Uri
        Uri searchUri = SmsLogProvider.Constants.CONTENT_URI;
        if (!TextUtils.isEmpty(phone)) {
            searchUri = SmsLogProvider.Constants.getContentUriPhoneFilter(phone);
        }
        // Compute Where Clause
        String[] projection = SmsLogDatabase.SmsLogColumns.ALL_COLS; // TODO Smaller
        String selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TO_READ;
        String[] selectionArgs = null;
        if (side != null) {
            selection = SmsLogDatabase.SmsLogColumns.SELECT_BY_TOREAD_SIDE;
            selectionArgs = new String[]{String.valueOf(side.getDbCode())};
        }
        // Query
        long begin = System.currentTimeMillis();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = cr.query(searchUri, projection, selection, selectionArgs,
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
        if (toReadStatus == null || Boolean.FALSE.equals(toReadStatus)) {
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
        Log.d(TAG, "Mark As ToRead (" + toReadStatus +
                ") " + count + " ReadLogs in  " + (System.currentTimeMillis() - begin) + " ms for Uri " + entityUri);
    }


    public static void markAsToReadLog(Context context, Uri entityUri, Boolean toReadStatus) {
        markAsToReadLog(context, entityUri, toReadStatus, null);
    }


}
