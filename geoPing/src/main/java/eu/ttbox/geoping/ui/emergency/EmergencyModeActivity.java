package eu.ttbox.geoping.ui.emergency;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.ui.GeoPingSlidingMenuFragmentActivity;


public class EmergencyModeActivity extends GeoPingSlidingMenuFragmentActivity {

    private static final String TAG = "EmergencyModeActivity";


    private EmergencyModeFragment emergencyModeFragment;
    // ===========================================================
    // Constructor
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emergency_mode_activity);

        // Intents
        //handleIntent(getIntent());
    }


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof EmergencyModeFragment) {
            emergencyModeFragment = (EmergencyModeFragment) fragment;
        }
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================


}
