package eu.ttbox.geoping.service.accountsync;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;



public class ProfileActivity   extends ActionBarActivity {
    private static final String TAG = "ProfileActivity" ;


    // ===========================================================
    // Intent
    // ===========================================================

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        Log.d(TAG, "===========================================================");
        Log.d(TAG, "handleIntent for action : " + intent.getAction()  + " / extra " + intent);
        Log.d(TAG, "===========================================================");

    }

}
