package eu.ttbox.geoping.service.receiver.network;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

public class ReSentSmsMessageReceiver extends BroadcastReceiver {

    private static final String TAG = "ReSentSmsMessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        ;
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
        }
    }


    private void printExtras(Bundle extras) {
        if (extras!=null) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, "### Phone State extras : " + key + " = " + value);
            }
        }
    }
}
