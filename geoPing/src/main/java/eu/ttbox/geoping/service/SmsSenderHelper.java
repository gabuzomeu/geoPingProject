package eu.ttbox.geoping.service;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.domain.SmsLogProvider;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.model.SmsLogTypeEnum;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase.SmsLogColumns;
import eu.ttbox.geoping.domain.smslog.SmsLogHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.encoder.params.MessageParamField;
import eu.ttbox.geoping.service.receiver.MessageAcknowledgeReceiver;
import eu.ttbox.geoping.utils.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.utils.encoder.adpater.BundleEncoderAdapter;

public class SmsSenderHelper {

    private static final String TAG = "SmsSenderHelper";


    public static final String EXTRA_MSG_PART_COUNT = MessageAcknowledgeReceiver.EXTRA_ACK_MSG_PART_COUNT;
    public static final String EXTRA_MSG_PART_ID = MessageAcknowledgeReceiver.EXTRA_ACK_MSG_PART_ID;

    public static Bundle completeRequestTimeOutFromPrefs(SharedPreferences appPreferences, Bundle params) {
        Bundle result = params == null ? new Bundle() : params;
        if (!result.containsKey(MessageParamField.TIME_IN_S.dbFieldName)) {
            int timeOut = appPreferences.getInt(AppConstants.PREFS_REQUEST_TIMEOUT_S, -1);
            if (timeOut > -1) {
                result.putInt(MessageParamField.TIME_IN_S.dbFieldName, timeOut);
            }
        }
        if (!result.containsKey(MessageParamField.LOC_ACCURACY.dbFieldName)) {
            int accuracy = appPreferences.getInt(AppConstants.PREFS_REQUEST_ACCURACY_M, -1);
            if (accuracy > -1) {
                result.putInt(MessageParamField.LOC_ACCURACY.dbFieldName, accuracy);
            }
        }
        return result;
    }

    public static ArrayList<String> splitNumbers(String phoneString) {
        ArrayList<String> phones = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(phoneString, ";");
        while (st.hasMoreTokens()) {
            String phone = st.nextToken();
            phones.add(phone);
        }
        return phones;
    }

    public static Uri[] sendSmsAndLogIt(Context context, SmsLogSideEnum side, String[] phones, MessageActionEnum action, Bundle eventParams) {
        int phoneSize = phones.length;
        Uri[] uris = new Uri[phoneSize];
        for (int i = 0; i < phoneSize; i++) {
            String phone = phones[i];
            uris[i] = SmsSenderHelper.sendSmsAndLogIt(context, side, phone, action, eventParams);
        }
        return uris;
    }

    public static Uri sendSmsAndLogIt(Context context, SmsLogSideEnum side, String phone, MessageActionEnum action, Bundle eventParams) {
        Uri isSend = null;
        Bundle params = eventParams == null ? new Bundle() : eventParams;
        // Complete with App version
        if (!MessageEncoderHelper.isToBundle(params, MessageParamEnum.APP_VERSION)) {
            int appVersion = GeoPingApplication.getGeoPingApplication(context).versionCode();
            MessageEncoderHelper.writeToBundle(params, MessageParamEnum.APP_VERSION, appVersion);
        }
        // Encode Message
        String encrypedMsg = MessageEncoderHelper.encodeSmsMessage(action, params);
        Log.d(TAG, String.format("Send Request SmsMessage to %s : %s (%s)", phone, action, encrypedMsg));

        if (encrypedMsg != null && encrypedMsg.length() > 0) {
            SmsManager smsManager = SmsManager.getDefault();
            // Compute Messages
            ArrayList<String> msgsplit = smsManager.divideMessage(encrypedMsg);
            int msgSplitCount = msgsplit.size();
            // Log It
            ContentResolver cr = context.getContentResolver();
            Uri logUri = logSmsMessage(cr, side, SmsLogTypeEnum.SEND_REQ, phone, action, params, msgSplitCount, encrypedMsg, false, null);

            // Shot Message Send
            if (msgSplitCount == 1) {
                sendTextMessage(context, smsManager, logUri, phone, encrypedMsg);
                isSend = logUri;
                Log.d(TAG, String.format("Send SmsMessage (%s chars, args) : %s", encrypedMsg.length(), encrypedMsg));
            } else {
                sendMultipartTextMessage(context, smsManager, logUri, phone, msgsplit);
                isSend = logUri;
                Log.d(TAG, String.format("Send Long SmsMessage (%s chars) : %s", encrypedMsg.length(), encrypedMsg));
            }
        }
        return isSend;
    }

    private static void sendTextMessage(Context context, SmsManager smsManager, Uri logUri, String phone, String encrypedMsg) {
        final int msgSplitCount = 1;
        // Acknowledge
        PendingIntent sendIntent = createPendingIntentAck(context, logUri, MessageAcknowledgeReceiver.ACTION_SEND_ACK, 1, msgSplitCount);
        PendingIntent deliveryIntent =  createPendingIntentAck(context, logUri, MessageAcknowledgeReceiver.ACTION_DELIVERY_ACK, 1, msgSplitCount);
        // Send Message
        smsManager.sendTextMessage(phone, null, encrypedMsg, sendIntent, deliveryIntent);
    }


    private static void sendMultipartTextMessage(Context context, SmsManager smsManager, Uri logUri, String phone, ArrayList<String> msgsplit) {
        int msgSplitCount = msgsplit.size();
        // Acknowledge
        ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>(msgSplitCount);
        ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>(msgSplitCount);
        for (int msgId = 1; msgId <= msgSplitCount; msgId++) {
            // Acknowledge
            PendingIntent sendIntent =  createPendingIntentAck(context, logUri, MessageAcknowledgeReceiver.ACTION_SEND_ACK, msgId, msgSplitCount);
            PendingIntent deliveryIntent = createPendingIntentAck(context, logUri, MessageAcknowledgeReceiver.ACTION_DELIVERY_ACK, msgId, msgSplitCount);
            sentIntents.add(sendIntent);
            deliveryIntents.add(deliveryIntent);
        }
        // Send Message
        smsManager.sendMultipartTextMessage(phone, null, msgsplit, sentIntents, deliveryIntents);
    }

    private static PendingIntent createPendingIntentAck(Context context, Uri logUri, String action, int msgId, int msgSplitCount) {
        PendingIntent sendIntent = PendingIntent.getBroadcast(context, 0, //
                new Intent(action).setData(logUri) //
                        .putExtra(EXTRA_MSG_PART_COUNT, msgSplitCount) //
                        .putExtra(EXTRA_MSG_PART_ID, msgId) //
                , 0
        ); //  PendingIntent.FLAG_CANCEL_CURRENT
        return sendIntent;

    }

    // ===========================================================
    // Re Send Sms Message
    // ===========================================================
    public static int reSendSmsMessage(Context context, Uri searchLogUri, String selection, String[] selectionArgs) {
        int result = 0;
        ContentResolver cr = context.getContentResolver();
        String[] projection = new String[] {
                SmsLogColumns.COL_ID,
                SmsLogColumns.COL_PHONE,
                SmsLogColumns.COL_MESSAGE  };
        String sortOrder = SmsLogDatabase.SmsLogColumns.ORDER_BY_TIME_ASC;
        Cursor cursor=  cr.query(searchLogUri, projection, selection, selectionArgs, sortOrder);
        try {
            int cursorSize = cursor.getCount();
            if (cursorSize > 0) {
                Log.d(TAG, "### Need Resent SMS : " + cursorSize + " SMS Messages");
                SmsLogHelper helper = new SmsLogHelper().initWrapper(cursor);
                while (cursor.moveToNext()) {
                    String smsMessage = helper.getMessage(cursor);
                    String phone = helper.getSmsLogPhone(cursor);
                    Uri logUri = helper.getSmsLogUri(cursor);
                    Log.d(TAG, "Resend SMS Message : " + smsMessage  );
                    reSendSmsMessage(context, logUri, phone, smsMessage);
                    result++;
                }
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private static void reSendSmsMessage(Context context, Uri logUri, String phone, String encrypedMsg ) {
        if (encrypedMsg != null && encrypedMsg.length() > 0) {
            SmsManager smsManager = SmsManager.getDefault();
            ContentResolver cr = context.getContentResolver();
            // Mark to Log To ReSend
            ContentValues values = new ContentValues();
            values.put(SmsLogColumns.COL_SMSLOG_TYPE, SmsLogTypeEnum.SEND_REQ.getCode());
            // Reset Status
            values.put(SmsLogColumns.COL_MSG_ACK_DELIVERY_TIME_MS, 0L);
            values.put(SmsLogColumns.COL_MSG_ACK_DELIVERY_MSG_COUNT, 0);
            values.put(SmsLogColumns.COL_MSG_ACK_DELIVERY_RESULT_MSG, "");
            values.put(SmsLogColumns.COL_MSG_ACK_SEND_TIME_MS, 0L);
            values.put(SmsLogColumns.COL_MSG_ACK_SEND_MSG_COUNT, 0);
            values.put(SmsLogColumns.COL_MSG_ACK_SEND_RESULT_MSG, "");
            // Do Update Status
            int updateCount = cr.update(logUri, values, null, null);
            // Compute Messages
            ArrayList<String> msgsplit = smsManager.divideMessage(encrypedMsg);
            int msgSplitCount = msgsplit.size();
            // Shot Message Send
            if (msgSplitCount == 1) {
                sendTextMessage(context, smsManager, logUri, phone, encrypedMsg);
                Log.d(TAG, String.format("Send SmsMessage (%s chars, args) : %s", encrypedMsg.length(), encrypedMsg));
            } else {
                sendMultipartTextMessage(context, smsManager, logUri, phone, msgsplit);
                Log.d(TAG, String.format("Send Long SmsMessage (%s chars) : %s", encrypedMsg.length(), encrypedMsg));
            }
        }
    }

    // ===========================================================
    // Log Sms message
    // ===========================================================

    /*
    @Deprecated
    public static Uri logSmsMessage(ContentResolver cr, SmsLogSideEnum side, SmsLogTypeEnum type, GeoPingMessage geoMessage, int smsWeight, String encrypedMsg) {
        return logSmsMessage(cr, side, type, geoMessage.phone, geoMessage.action, geoMessage.params, smsWeight, encrypedMsg);
    }

    @Deprecated
    public static Uri logSmsMessage(ContentResolver cr, SmsLogSideEnum side, SmsLogTypeEnum type, String phone, SmsMessageActionEnum action, Bundle params, int smsWeight, String encrypedMsg) {
        ContentValues values = SmsLogHelper.getContentValues(side, type, phone, action, params, encrypedMsg);
        values.put(SmsLogColumns.COL_MSG_COUNT, smsWeight);
        Uri logUri = cr.insert(SmsLogProvider.Constants.CONTENT_URI, values);
        return logUri;
    }
*/

    public static Uri logSmsMessage(ContentResolver cr, SmsLogSideEnum side, SmsLogTypeEnum type
            , BundleEncoderAdapter geoMessage
            , int smsWeight, String encrypedMsg
            , boolean markAsUnread, Uri parentUri) {
        return logSmsMessage(cr, side, type, geoMessage.getPhone(), geoMessage.getAction(), geoMessage.getMap(), smsWeight, encrypedMsg, markAsUnread, parentUri);
    }

    public static Uri logSmsMessage(ContentResolver cr, SmsLogSideEnum side, SmsLogTypeEnum type, String phone
            , MessageActionEnum action, Bundle params
            , int smsWeight, String encrypedMsg
            , boolean markAsToRead, Uri parentUri) {
        ContentValues values = SmsLogHelper.getContentValues(side, type, phone, action, params, encrypedMsg);
        values.put(SmsLogColumns.COL_MSG_COUNT, smsWeight);

        if (parentUri != null) {
            String logParentId = parentUri.getLastPathSegment();
            values.put(SmsLogColumns.COL_PARENT_ID, logParentId);
        }
        if (markAsToRead) {
            values.put(SmsLogColumns.COL_TO_READ, Boolean.TRUE);
        }
        Uri logUri = cr.insert(SmsLogProvider.Constants.CONTENT_URI, values);
        return logUri;
    }


    private void printContentValues(ContentValues values) {
        for (Map.Entry<String, Object> key : values.valueSet()) {
            Object val = key.getValue();
            Log.d(TAG, "SaveLog ContentValues : " + key.getKey() + " = " + val);
        }
    }
}
