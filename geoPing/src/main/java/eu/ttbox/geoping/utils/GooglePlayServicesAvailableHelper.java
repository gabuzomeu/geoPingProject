package eu.ttbox.geoping.utils;


import android.app.Activity;
import android.content.DialogInterface;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class GooglePlayServicesAvailableHelper {

    public static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 10024632;

    public static boolean isGooglePlayServicesAvailable(Activity activity) {
        return isGooglePlayServicesAvailable(activity, true);
    }

    public static boolean isGooglePlayServicesAvailable(final Activity activity, boolean openDialog) {
        DialogInterface.OnCancelListener onCancelListener = new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                activity.finish();
            }
        };
        return isGooglePlayServicesAvailable(activity, onCancelListener, openDialog);
    }

    public static boolean isGooglePlayServicesAvailable(final Activity activity, DialogInterface.OnCancelListener onCancelListener, boolean openDialog) {
        int playResponseCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        boolean isValid = true;
       // playResponseCode = ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
        if (ConnectionResult.SUCCESS != playResponseCode) {
            isValid = false;
            if (openDialog) {
                GooglePlayServicesUtil.showErrorDialogFragment(playResponseCode, activity, REQUEST_CODE_RECOVER_PLAY_SERVICES,
                        onCancelListener
                );
            }
        }
        return isValid;
    }

}
