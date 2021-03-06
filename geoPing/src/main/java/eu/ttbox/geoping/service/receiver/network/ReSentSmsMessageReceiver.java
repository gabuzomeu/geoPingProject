package eu.ttbox.geoping.service.receiver.network;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.SmsLogProvider;
import eu.ttbox.geoping.domain.model.SmsLogTypeEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;

/**
 * platform_frameworks_base : com.android.internal.policy.impl.keyguard.KeyguardUpdateMonitor
 * Use http://developer.android.com/reference/android/telephony/SignalStrength.html ?
 * http://stackoverflow.com/questions/15039728/listening-to-signalstrength-when-phone-sleeps
 * http://alvinalexander.com/java/jwarehouse/android/telephony/java/com/android/internal/telephony/PhoneStateIntentReceiver.java.shtml
 */
public class ReSentSmsMessageReceiver extends BroadcastReceiver {

    private static final String TAG = "ReSentSmsMessageReceiver";

    static class TelephonyIntents {
        public static final String SPN_STRINGS_UPDATED_ACTION = "android.provider.Telephony.SPN_STRINGS_UPDATED";
        public static final String   EXTRA_PLMN = "plmn";
        public static final String  EXTRA_SHOW_PLMN = "showPlmn";
        public static final String  EXTRA_SHOW_SPN = "showSpn";
        public static final String  EXTRA_SPN = "spn";

        public static final String ACTION_SIGNAL_STRENGTH_CHANGED = "android.intent.action.SIG_STR";
        public static final String EXTRA_SIGNAL_STRENGTH = "GsmSignalStrength";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "### ----------------------------------------- ###");
        Log.d(TAG, "### Phone State Change To :  " + action);
     //   printExtras(intent.getExtras());
        Log.d(TAG, "### ----------------------------------------- ### \\n");

        if ( TelephonyIntents.ACTION_SIGNAL_STRENGTH_CHANGED.equals(action)) {
            // int mSignalStrength = SignalStrength.newFromBundle(intent.getExtras());
            Log.d(TAG, "### onReceive Type : ACTION_SIGNAL_STRENGTH_CHANGED " );
            // GsmSignalStrength = 10
            // isGsm = true
            int signal = intent.getIntExtra(TelephonyIntents.EXTRA_SIGNAL_STRENGTH, -1);
            if (signal>0) {
                Log.d(TAG, "### GsmSignalStrength > 0 : " + signal + " ==> Resent SMS" );
                resendSms(context);
            }
            //printExtras(intent.getExtras());
            Log.d(TAG, "### onReceive Type : ACTION_SIGNAL_STRENGTH_CHANGED (END)" );
        }
//        else if (ConnectivityManager.CONNECTIVITY_ACTION.equals( action)) {
//            boolean noConnectivity = intent.getBooleanExtra(  ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
//            if (!noConnectivity) {
//                Bundle extras = intent.getExtras();
//                NetworkInfo info = (NetworkInfo)extras.get(ConnectivityManager.EXTRA_NETWORK_INFO);
//                Log.d(TAG, "### NetworkInfo State :  " +  info.getState() +  " (connected "+ info.isConnected()+")");
//                if (ConnectivityManager.TYPE_MOBILE == info.getType()) {
//                    Log.d(TAG, "### NetworkInfo Type : TYPE_MOBILE " );
//
//                }
//            }
//        } else if ( TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
//            CharSequence  mTelephonyPlmn = getTelephonyPlmnFrom(intent);
//            CharSequence  mTelephonySpn = getTelephonySpnFrom(intent);
//            Log.d(TAG, "### onReceive Type : SPN_STRINGS_UPDATED_ACTION " );
//          //  printExtras(intent.getExtras());
//            Log.d(TAG, "### onReceive Type : SPN_STRINGS_UPDATED_ACTION (END)" );
//        } else  if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
//            // Log.d(TAG, "onReceiveIntent: ACTION_PHONE_STATE_CHANGED, state="   + intent.getStringExtra(Phone.STATE_KEY));
//            Log.d(TAG, "### onReceive Type : ACTION_PHONE_STATE_CHANGED " );
//           // printExtras(intent.getExtras());
//            Log.d(TAG, "### onReceive Type : ACTION_PHONE_STATE_CHANGED (END)" );
//        }
    }

    /**
     * @param intent The intent with action {@link TelephonyIntents#SPN_STRINGS_UPDATED_ACTION}
     * @return The string to use for the plmn, or null if it should not be shown.
     */
    private CharSequence getTelephonyPlmnFrom(  Intent intent) {
        if (intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false)) {
            final String plmn = intent.getStringExtra(TelephonyIntents.EXTRA_PLMN);
            return (plmn != null) ? plmn : getDefaultPlmn( );
        }
        return null;
    }

    /**
     * @return The default plmn (no service)
     */
    private CharSequence getDefaultPlmn() {
//        return context.getResources().getText(R.string.lockscreen_carrier_default);
        return "no service";
    }

    private CharSequence getTelephonySpnFrom(Intent intent) {
        if (intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, false)) {
            final String spn = intent.getStringExtra(TelephonyIntents.EXTRA_SPN);
            if (spn != null) {
                return spn;
            }
        }
        return null;
    }


    private void printExtras(Bundle extras) {
        Intents.printExtras(TAG, extras);
    }


    private void resendSms(Context context) {
        // Do the Resend
        Uri searchUri = SmsLogProvider.Constants.getContentUriTypeStatus(SmsLogTypeEnum.SEND_ERROR);
        String[] projection = null;
        String selection = null; // String.format("%s >= ?", SmsLogDatabase.SmsLogColumns.COL_TIME);
        String[] selectionArgs = null; //new String[] { String.valueOf( new Date(2014-1900,1,01).getTime())};
        int resendCount =  SmsSenderHelper.reSendSmsMessage(context, searchUri, selection, selectionArgs);
        Log.d(TAG, "### Resend done for : " + resendCount + " SMS Messages" );
        // Disable Services
        ExtraFeatureHelper.enabledSettingReSentSmsMessageReceiver(context, Boolean.FALSE);
        Log.d(TAG, "### ACTION_SMS_SENT ERROR ==>  Disable ReSend Service");
    }


}
