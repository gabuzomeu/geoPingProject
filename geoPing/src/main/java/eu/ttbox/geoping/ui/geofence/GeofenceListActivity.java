package eu.ttbox.geoping.ui.geofence;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.ui.GeoPingSlidingMenuFragmentActivity;

public class GeofenceListActivity extends GeoPingSlidingMenuFragmentActivity {

    private static final String TAG = "GeofenceListActivity";

    // binding
    private GeofenceListFragment listFragment;

    // ===========================================================
    // Constructor
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geofence_list_activity);
        // SlidingMenu
//        final SlidingMenu slidingMenu = SlidingMenuHelper.newInstance(this);
//        slidingMenu.attachToActivity(this, SlidingMenu.SLIDING_WINDOW);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Intents
        handleIntent(getIntent());
    }


    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof GeofenceListFragment) {
            listFragment = (GeofenceListFragment) fragment;
        }
    }

    // ===========================================================
    // Menu
    // ===========================================================

    public void onAddEntityClick(View v) {
        listFragment.onAddEntityClick(v);
    }

    private void onCancelClick() {
        finish();
    }

//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getSupportMenuInflater();
//        inflater.inflate(R.menu.menu_geofence_list, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add:
            onAddEntityClick(null);
            return true;
        case R.id.menu_cancel:
            onCancelClick();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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
        Log.d(TAG, "handleIntent for action : " + intent.getAction());

    }

    // ===========================================================
    // Other
    // ===========================================================

}
