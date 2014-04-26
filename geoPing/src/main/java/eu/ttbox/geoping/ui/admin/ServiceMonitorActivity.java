package eu.ttbox.geoping.ui.admin;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.ui.GeoPingSlidingMenuFragmentActivity;

public class ServiceMonitorActivity extends GeoPingSlidingMenuFragmentActivity {

    private static final String TAG = "ServiceMonitorActivity";

    private ServiceMonitorFragment serviceMonitorFragment;

    // ===========================================================
    // Constructor
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.service_monitor_activity);

        // Intents
        //handleIntent(getIntent());
    }


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ServiceMonitorFragment) {
            serviceMonitorFragment = (ServiceMonitorFragment) fragment;
        }
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================


}
