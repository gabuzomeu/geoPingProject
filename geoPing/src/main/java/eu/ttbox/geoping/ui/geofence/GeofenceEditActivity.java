package eu.ttbox.geoping.ui.geofence;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.model.CircleGeofence;
import eu.ttbox.geoping.domain.pairing.GeoFenceDatabase;
import eu.ttbox.geoping.ui.GeoPingActionBarActivity;
import eu.ttbox.geoping.ui.smslog.SmsLogListFragment;
import eu.ttbox.geoping.utils.GooglePlayServicesAvailableHelper;

public class GeofenceEditActivity extends GeoPingActionBarActivity {

    private static final String TAG = "GeofenceEditActivity";

    // Status
    private static final String SAVE_KEY_VIEWPAGER_PAGE_COUNT = "viewPagerPageCount";

    // Binding
    private GeofenceEditFragment editFragment;
    private GeofenceEditMapFragment mapFragment;
    private SmsLogListFragment smsLogFragment;


    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    // Instance
    private static final int VIEW_PAGER_LOADPERS_PAGE_COUNT = 3;
    private int viewPagerPageCount = 2;

    private Uri geofenceUri;
    private String geofenceRequestId;

    // ===========================================================
    // Listener
    // ===========================================================

    private GeofenceEditFragment.OnGeofenceSelectListener onGeofenceSelectListener = new GeofenceEditFragment.OnGeofenceSelectListener() {

        @Override
        public void onGeofenceSelect(Uri id, CircleGeofence fence) {
            Log.d(TAG,"onGeofenceSelectListener : " + fence );
            // Check Update Fence
            if (fence!=null && !TextUtils.isEmpty(fence.requestId)  ) {
                if (smsLogFragment != null ) { //&& !fence.requestId.equals(geofenceRequestId)
                    Bundle args = new Bundle();
                    args.putString(SmsLogListFragment.Intents.EXTRA_GEOFENCE_REQUEST_ID, fence.requestId);
//                    args.putInt(SmsLogListFragment.Intents.EXTRA_SIDE_DBCODE, SmsLogSideEnum.SLAVE.getDbCode());
                    smsLogFragment.refreshLoader(args);
                }
                geofenceRequestId=fence.requestId;
            }
            geofenceUri = id;
            // Map Geofence
            mapFragment.onGeofenceSelect(   id,   fence);
            // Update Ui Tabs
            if (viewPagerPageCount != VIEW_PAGER_LOADPERS_PAGE_COUNT) {
                viewPagerPageCount = VIEW_PAGER_LOADPERS_PAGE_COUNT;
                mSectionsPagerAdapter.notifyDataSetChanged();
            }
        }

        public void onGeofencePrepareInsert(CircleGeofence fence) {
            mapFragment.onGeofencePrepareInsert(  fence);
        }

        @Override
        public void onGeofenceRequestFocus(int requestFocusId) {
            switch (requestFocusId) {
                case GeofenceEditFragment.OnGeofenceSelectListener.GEOFENCE_EDIT :
                    mViewPager.setCurrentItem(SectionsPagerAdapter.GEOFENCE);
                    break;
                case GeofenceEditFragment.OnGeofenceSelectListener.GEOFENCE_MAP :
                    mViewPager.setCurrentItem(SectionsPagerAdapter.GEOFENCE_MAP);
                    break;
                default:
                    throw new IllegalArgumentException("Not implemented onGeofenceRequestFocus Id : " + requestFocusId);
            }
        }


    };

    public void setCurrentItem(int sectionsPagerAdapterId) {
        mViewPager.setCurrentItem(sectionsPagerAdapterId);
    }

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geofence_edit_activity);
        // Add selector
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Pagers
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        // Fragment
        editFragment = new GeofenceEditFragment();
        editFragment.setOnGeofenceSelectListener( onGeofenceSelectListener);
        // Fragment Map
        mapFragment = new GeofenceEditMapFragment();

        // Analytic
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // Intents
        handleIntent(getIntent());

    }


    // ===========================================================
    // LifeCycle
    // ===========================================================

    @Override
    public void onResume() {
        super.onResume();
        if (GooglePlayServicesAvailableHelper.isGooglePlayServicesAvailable(this)) {
            Log.i(TAG, "GooglePlayServices Available");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GooglePlayServicesAvailableHelper.REQUEST_CODE_RECOVER_PLAY_SERVICES) {
            if (resultCode == Activity.RESULT_OK) {
                Log.e(TAG, "Google Play Service Available");
            } else {
                Log.e(TAG, "Google Play Service Not Available");
                finish();
            }
        }
    }

// ===========================================================
    // Menu
    // ===========================================================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_edit:
                mViewPager.setCurrentItem(SectionsPagerAdapter.GEOFENCE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }





    // ===========================================================
    // Tracking Event
    // ===========================================================

//    @Override
//    public void onStart() {
//        super.onStart();
//        // Tracker
//        EasyTracker.getInstance(this).activityStart(this);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        // Tracker
//        EasyTracker.getInstance(this).activityStop(this);
//    }

    // ===========================================================
    // Life Cycle
    // ===========================================================


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_KEY_VIEWPAGER_PAGE_COUNT, viewPagerPageCount);
        if (geofenceUri != null) {
            outState.putString(GeoFenceDatabase.GeoFenceColumns.COL_ID, geofenceUri.toString());
            outState.putString(GeoFenceDatabase.GeoFenceColumns.COL_REQUEST_ID, geofenceRequestId);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String pariringUriString = savedInstanceState.getString(GeoFenceDatabase.GeoFenceColumns.COL_ID);
        setViewPagerPageCount(savedInstanceState.getInt(SAVE_KEY_VIEWPAGER_PAGE_COUNT));
        if (pariringUriString != null) {
            geofenceUri = Uri.parse(pariringUriString);
            geofenceRequestId = savedInstanceState.getString(GeoFenceDatabase.GeoFenceColumns.COL_REQUEST_ID);
        }
    }

    private void setViewPagerPageCount(int count) {
        viewPagerPageCount = count;
        mSectionsPagerAdapter.notifyDataSetChanged();
    }


    // ===========================================================
    // Intent Handler
    // ===========================================================

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    protected void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, "handleIntent for action : " + action);

        if (Intent.ACTION_EDIT.equals(action) || Intent.ACTION_DELETE.equals(action)) {
            mViewPager.setCurrentItem(SectionsPagerAdapter.GEOFENCE);
            // Prepare Edit
            Uri entityUri = intent.getData();
            // Set Fragment
            Bundle fragArgs = new Bundle();
            fragArgs.putString(Intents.EXTRA_PERSON_ID, entityUri.toString());
            editFragment.setArguments(fragArgs);

        } else if (Intent.ACTION_INSERT.equals(action)) {
            mViewPager.setCurrentItem(SectionsPagerAdapter.GEOFENCE_MAP);
        }

    }


    public void onSaveClick() {
        editFragment.onSaveClick();
    }

    // ===========================================================
    // Pages Adapter
    // ===========================================================

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the primary sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        static final int GEOFENCE = 0;
        static final int GEOFENCE_MAP = 1;
         static final int GEOFENCE_LOG = 2;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
            case GEOFENCE:
                fragment = editFragment;
                break;
            case GEOFENCE_MAP:
                fragment = mapFragment;
                break;

            case GEOFENCE_LOG:
                if (smsLogFragment == null) {
                    Bundle args = new Bundle();
                    args.putString(SmsLogListFragment.Intents.EXTRA_GEOFENCE_REQUEST_ID, geofenceRequestId);
//                    args.putInt(SmsLogListFragment.Intents.EXTRA_SIDE_DBCODE, SmsLogSideEnum.SLAVE.getDbCode());
                    smsLogFragment = new SmsLogListFragment();
                    smsLogFragment.setArguments(args);
                    Log.d(TAG,"new smsLogFragment with args : " + args );
                }
                fragment = smsLogFragment;
                break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return viewPagerPageCount;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
            case GEOFENCE:
                return getString(R.string.menu_geofence);//.toUpperCase(Locale.getDefault());
            case GEOFENCE_MAP:
                return getString(R.string.menu_map);//.toUpperCase(Locale.getDefault());
            case GEOFENCE_LOG:
                return getString(R.string.menu_smslog);//.toUpperCase(Locale.getDefault());
            }
            return null;
        }
    }


    // ===========================================================
    // Other
    // ===========================================================





}
