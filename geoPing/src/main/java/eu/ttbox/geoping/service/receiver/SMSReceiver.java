package eu.ttbox.geoping.service.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.List;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.model.SmsLogTypeEnum;
import eu.ttbox.geoping.encoder.crypto.TextEncryptor;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.service.encoder.adpater.BundleEncoderAdapter;

/**
 *<a href="http://www.tutos-android.com/broadcast-receiver-android">broadcast-receiver-android</a>
 *<a href="http://mobiforge.com/developing/story/sms-messaging-android">sms-messaging-android</a>
  *
 */
public class SMSReceiver extends BroadcastReceiver {

	private static final String TAG = "SMSReceiver";

	public static final String ACTION_RECEIVE_SMS = "android.provider.Telephony.SMS_RECEIVED";
	public static final String EXTRA_PDUS = "pdus";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals(ACTION_RECEIVE_SMS)) {
			Log.d(TAG, "SMSReceiver : " + intent);
			Bundle bundle = intent.getExtras();
			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get(EXTRA_PDUS);

				final SmsMessage[] messages = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				}
				if (messages.length > 0) {
					SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
					boolean deleteSms = appPreferences.getBoolean(AppConstants.PREFS_SMS_DELETE_ON_MESSAGE, true);

					for (SmsMessage message : messages) {
						boolean isConsume = consumeMessage(context, message);
						if (!isConsume) {
							deleteSms = false;
						}
						if (deleteSms) {
							Log.d(TAG, "Cancel wanting abortBroadcast for unexpected Sms Message " + message.getMessageBody());
						}
					}
					if (deleteSms) {
						abortBroadcast();
					}
				}
			}
		}
	}

	// ===========================================================
	// Consume Sms message
	// ===========================================================

	private boolean consumeMessage(Context context, SmsMessage message) {
		final String messageBody = message.getMessageBody();
		final String phoneNumber = message.getDisplayOriginatingAddress();
		Log.d(TAG, "Consume SMS Geo Action : " + phoneNumber + " / " + messageBody);
		// Decrypt Msg
        // --------------------------
		TextEncryptor textEncryptor = null;
		if (MessageEncoderHelper.isGeoPingEncodedSmsMessageEncrypted(messageBody)) {
			// TODO Find Encryptor
		}
        List<BundleEncoderAdapter> geoMsgs = MessageEncoderHelper.decodeSmsMessage(phoneNumber, messageBody, textEncryptor);
        // Check Valid Conversion
        // --------------------------
        boolean isConsume = false;
        if (geoMsgs == null ||  geoMsgs.isEmpty()  || geoMsgs.get(0).getAction() == null) {
            Log.w(TAG, String.format("Ignore for No Action the GeoPingMessage : %s",  geoMsgs ));
            return isConsume;
        }
        // Consume Valid Message
        // --------------------------
        ContentResolver cr = context.getContentResolver();
        Uri logParentId = null;
        for (BundleEncoderAdapter msg : geoMsgs) {
            Intent intent = MessageEncoderHelper.convertSingleGeoPingMessageAsIntent(context, msg);
            if (intent != null) {
                isConsume = true;
                // Save Log Message
                // --------------------------
                SmsLogSideEnum side = msg.getAction().isConsumeMaster  ? SmsLogSideEnum.MASTER : SmsLogSideEnum.SLAVE;
                Uri insertUri = null;
                if (logParentId==null) {
                      insertUri = SmsSenderHelper.logSmsMessage( cr, side,   SmsLogTypeEnum.RECEIVE, msg, 1,    messageBody, null );
                    logParentId = insertUri;
                } else {
                      insertUri = SmsSenderHelper.logSmsMessage( cr, side,   SmsLogTypeEnum.RECEIVE, msg, 0, messageBody, logParentId );
                }
                // Start Service For Message
                // --------------------------
                intent.putExtra(Intents.EXTRA_SMSLOG_URI, insertUri);
                context.startService(intent);
            }
        }
		return isConsume;
	}

	// ===========================================================
	// Log Sms message
	// ===========================================================


}
