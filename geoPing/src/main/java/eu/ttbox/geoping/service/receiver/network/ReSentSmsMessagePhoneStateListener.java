package eu.ttbox.geoping.service.receiver.network;


import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.util.Log;

@Deprecated
public class ReSentSmsMessagePhoneStateListener extends PhoneStateListener {

    private static final String TAG = "ReSentSmsMessagePhoneStateListener";

    private Context context;

    public ReSentSmsMessagePhoneStateListener(Context context) {
        super();
        this.context = context;
    }

    public void onServiceStateChanged(ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        int state = serviceState.getState();
         if (ServiceState.STATE_IN_SERVICE == state) {
            Log.d(TAG, "--- ----------------------------------------- ---");
            Log.d(TAG, "--- Phone State Change To :  STATE_IN_SERVICE ---");
            Log.d(TAG, "--- ----------------------------------------- ---");
        }
    }

}
