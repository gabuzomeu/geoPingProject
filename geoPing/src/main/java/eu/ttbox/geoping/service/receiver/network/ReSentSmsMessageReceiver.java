package eu.ttbox.geoping.service.receiver.network;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import eu.ttbox.geoping.core.Intents;

/**
 * platform_frameworks_base : com.android.internal.policy.impl.keyguard.KeyguardUpdateMonitor
 */
public class ReSentSmsMessageReceiver extends BroadcastReceiver {

    private static final String TAG = "ReSentSmsMessageReceiver";

    static class TelephonyIntents {
        public static final String SPN_STRINGS_UPDATED_ACTION = "android.provider.Telephony.SPN_STRINGS_UPDATED";
        public static final String EXTRA_PLMN = "plmn";
        public static final String  EXTRA_SHOW_PLMN = "showPlmn";
        public static final String  EXTRA_SHOW_SPN = "showSpn";
        public static final String  EXTRA_SPN = "spn";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.d(TAG, "### ----------------------------------------- ###");
        Log.d(TAG, "### Phone State Change To :  " + action);
        printExtras(intent.getExtras());
        Log.d(TAG, "### ----------------------------------------- ###");
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals( action)) {
            boolean noConnectivity = intent.getBooleanExtra(  ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            if (!noConnectivity) {
                Bundle extras = intent.getExtras();
                NetworkInfo info = (NetworkInfo)extras.get(ConnectivityManager.EXTRA_NETWORK_INFO);
                Log.d(TAG, "### NetworkInfo State :  " +  info.getState() +  " (connected "+ info.isConnected()+")");
                if (ConnectivityManager.TYPE_MOBILE == info.getType()) {
                    Log.d(TAG, "### NetworkInfo Type : TYPE_MOBILE " );

                }
            }
        } else if ( TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)) {
            CharSequence  mTelephonyPlmn = getTelephonyPlmnFrom(intent);
            CharSequence  mTelephonySpn = getTelephonySpnFrom(intent);
        }
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
}
