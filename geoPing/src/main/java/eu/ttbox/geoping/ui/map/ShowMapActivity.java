package eu.ttbox.geoping.ui.map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.ArrayList;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.ui.GeoPingSlidingMenuFragmentActivity;

/**
 *  @see <a href="http://mobiforge.com/developing/story/using-google-maps-android">using-google-maps-android</a>
 * 
 */
public class ShowMapActivity extends GeoPingSlidingMenuFragmentActivity {

    private static final String TAG = "ShowMapActivity";

    private static final int PLAY_ERROR_REQUEST_CODE = 781498;
    // Constant
    /**
     * This number depend of previous menu
     */
    private int MENU_LAST_ID = 3;

    // private SlidingMenu slidingMenu;
    // Map
    private ShowMapFragment mapFragment;

    private ActionMode mActionMode;

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.map_activity);

        // Tracker
        EasyTracker.getInstance().activityStart(this);
    }

    public SlidingMenu customizeSlidingMenu() {
        SlidingMenu slidingMenu = super.customizeSlidingMenu();
        slidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        return slidingMenu;
    }

    @Override
    public void onStop() {
        super.onStop();
        // Tracker
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof ShowMapFragment) {
            mapFragment = (ShowMapFragment) fragment;
        }
    }

    // ===========================================================
    // Life cycle
    // ===========================================================

    @Override
    protected void onResume() {
        if (Log.isLoggable(TAG, Log.INFO)) {
            Log.i(TAG, "### ### ### ### ### onResume call ### ### ### ### ###");
        }
        super.onResume();
        handleIntent(getIntent());
    }

    // ===========================================================
    // Menu
    // ===========================================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // mapView.getOverlayManager().onPrepareOptionsMenu(menu, MENU_LAST_ID,
        // mapView);
        boolean prepare = super.onPrepareOptionsMenu(menu);

        // Current Tile Source
        ITileSource currentTileSrc = mapFragment.getMapViewTileSource();

        // Show My Location
        // ---------------
        MenuItem myLocationHideMenu = menu.findItem(R.id.menuMap_mypositon_hide );
        if (mapFragment.isMyLocationEnabled()) {
            myLocationHideMenu.setTitle(R.string.menu_map_mypositon_hide);
            myLocationHideMenu.setVisible(true);
//            myLocationHideMenu.setEnabled(true);
        } else {
            myLocationHideMenu.setTitle(R.string.menu_map_mypositon_show);
            myLocationHideMenu.setVisible(false);
//            myLocationHideMenu.setEnabled(false);
        }

        // Menu Geofence
        // ---------------
        MenuItem geofenceAddMenu = menu.findItem(R.id.menuMap_geofence_add);
        MenuItem geofenceMenu = menu.findItem(R.id.menuMap_geofence_list);
        int  statusGooglePlayServices = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == statusGooglePlayServices   ) {
            geofenceMenu.setEnabled(true);
            geofenceAddMenu.setEnabled(true);
        } else {
//            SERVICE_MISSING,  SERVICE_DISABLED, SERVICE_INVALID.
            geofenceMenu.setEnabled(false);
            geofenceAddMenu.setEnabled(false);
            // TODO Display Dialog
//            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(statusGooglePlayServices, this, PLAY_ERROR_REQUEST_CODE);
            // If Google Play services can provide an error dialog
//            if (errorDialog != null) {
//                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
//                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
//                errorFragment.show(  getSupportFragmentManager(),  "Geofence Detection");
//            }
        }
        if (mapFragment.isGeofenceOverlays()) {
            geofenceMenu.setTitle(R.string.menu_map_geofences_hide);
        } else {
            geofenceMenu.setTitle(R.string.menu_map_geofences_show);
        }

//        geofenceMenu.setTitle(R.string.menu_map_geofences_show);

        // Create Map Type
        // ---------------
        MenuItem mapTypeItem = menu.findItem(R.id.menuMap_mapmode);
        final SubMenu mapTypeMenu = mapTypeItem.getSubMenu();
        mapTypeMenu.clear();
        int MENU_MAP_GROUP = MENU_LAST_ID;
        // int MENU_TILE_SOURCE_STARTING_ID =
        // TilesOverlay.MENU_TILE_SOURCE_STARTING_ID;
        ArrayList<ITileSource> tiles = mapFragment.getMapViewTileSources();
        int tileSize = tiles.size();
        for (int a = 0; a < tileSize; a++) {
            final ITileSource tileSource = tiles.get(a);
            String tileName = mapFragment.getMapViewTileSourceName(tileSource);
            MenuItem tileMenuItem = mapTypeMenu.add(MENU_MAP_GROUP, TilesOverlay.MENU_TILE_SOURCE_STARTING_ID + MENU_MAP_GROUP + a, Menu.NONE, tileName);
            if (currentTileSrc != null && currentTileSrc.ordinal() == tileSource.ordinal()) {
                tileMenuItem.setChecked(true);
            }
        }
        mapTypeMenu.setGroupCheckable(MENU_MAP_GROUP, true, true);
        return prepare;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case R.id.menuMap_mypositoncenter: {
            mapFragment.centerOnMyPosition();
            return true;
        }
        case R.id.menuMap_mypositon_hide: {
            mapFragment.swichDisplayMyPosition();
            return true;
        }
        case R.id.menuMap_track_person: {
            mapFragment.showSelectPersonDialog();
            return true;
        }
        case R.id.menuMap_track_timeline: {
            mapFragment.swichRangeTimelineBarVisibility();
            return true;
        }
        case R.id.menuMap_geofence_list: {
            if (mapFragment.isGeofenceOverlays()) {
                mapFragment.hideGeofenceOverlays();
            } else {
                mapFragment.showGeofenceOverlays();
            } 
            return true;
        }
        case R.id.menuMap_geofence_add: {
            mapFragment.addGeofenceOverlayEditor();
            return true;
        }
        default: {
            // Map click
            final int menuId = item.getItemId() - MENU_LAST_ID;
            ArrayList<ITileSource> tiles = mapFragment.getMapViewTileSources();
            int tileSize = tiles.size();
            if ((menuId >= TilesOverlay.MENU_TILE_SOURCE_STARTING_ID) && (menuId < TilesOverlay.MENU_TILE_SOURCE_STARTING_ID + tileSize)) {
                mapFragment.setMapViewTileSource(tiles.get(menuId - TilesOverlay.MENU_TILE_SOURCE_STARTING_ID));
                // Compatibility
                if (VersionUtils.isHc11) {
                    isHc11InvalidateOptionsMenu();
                }
                return true;
            }
        }
        }
        return super.onOptionsItemSelected(item);
    }



    // ===========================================================
    // Compatibility
    // ===========================================================

    @SuppressLint("NewApi")
    private void isHc11InvalidateOptionsMenu() {
        if (VersionUtils.isHc11) {
            invalidateOptionsMenu();
        }
    }

    // ===========================================================
    // Handle Intent
    // ===========================================================

    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent Intent : " + intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (mapFragment != null) {
            mapFragment.handleIntent(intent);
        }
        // mapFragment.handleIntent(intent);
        //
        // String action = intent.getAction();
        // Log.d(TAG, String.format("Handle Intent for action %s : %s", action,
        // intent));
        // if (Intent.ACTION_VIEW.equals(action)) {
        // String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
        // Bundle bundle = intent.getExtras();
        // if ( bundle.containsKey(GeoTrackColumns.COL_LATITUDE_E6)
        // && bundle.containsKey(GeoTrackColumns.COL_LONGITUDE_E6) ) {
        // int latE6 = intent.getIntExtra(GeoTrackColumns.COL_LATITUDE_E6,
        // Integer.MIN_VALUE);
        // int lngE6 = intent.getIntExtra(GeoTrackColumns.COL_LONGITUDE_E6,
        // Integer.MIN_VALUE);
        // Log.w(TAG, String.format("Show on Map Phone [%s] (%s, %s) ", phone,
        // latE6, lngE6));
        // if (Integer.MIN_VALUE != latE6 && Integer.MIN_VALUE != lngE6) {
        // mapFragment.centerOnPersonPhone(phone, latE6, lngE6);
        // }
        // } else {
        // mapFragment.centerOnPersonPhone(phone);
        // }
        // }
    }

    // ===========================================================
    // Others
    // ===========================================================

}
