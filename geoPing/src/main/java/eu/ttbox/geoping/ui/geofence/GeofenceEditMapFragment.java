package eu.ttbox.geoping.ui.geofence;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.util.BoundingBoxE6;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.domain.model.CircleGeofence;
import eu.ttbox.geoping.ui.map.ShowMapFragmentV2;
import eu.ttbox.geoping.ui.map.geofence.GeofenceEditOverlay;

public class GeofenceEditMapFragment extends ShowMapFragmentV2 {

    private static final String TAG = "GeofenceEditMapFragment";

    private CircleGeofence editGeofence;



    // ===========================================================
    // Constructors
    // ===========================================================


    public GeofenceEditMapFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        // Menu on Fragment
        setHasOptionsMenu(true);

        return v;
    }

    // ===========================================================
    // Menu
    // ===========================================================

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.geofence_edit_map_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                onSaveClick();
                return true;
            case R.id.menuMap_mypositoncenter: {
                super.centerOnMyLocationFix();
                return true;
            }
//            case R.id.menuMap_mypositon_hide: {
//                 swichDisplayMyPosition();
//                return true;
//            }

        }
        return super.onOptionsItemSelected(item);
    }

    private void onSaveClick() {
        GeofenceEditActivity activity = (GeofenceEditActivity) getActivity();
        if (activity != null) {
            activity.onSaveClick();
        }
    }


    // ===========================================================
    // Load data
    // ===========================================================

    @Override
    public void loadDefaultDatas() {
        // Activate
        if (editGeofence != null) {
            displayGeofence(editGeofence);
        }
    }

    private void displayGeofence(CircleGeofence editGeofence) {
        if (mapController != null) {
            // Prepare Insert
            if (editGeofence.id == -1) {
                // Compute the default fence Size
                BoundingBoxE6 boundyBox = mapView.getBoundingBox();
                IGeoPoint center = boundyBox.getCenter();
                int radiusInMeters = boundyBox.getDiagonalLengthInMeters() / 8;
                radiusInMeters = Math.max(50, radiusInMeters);
                // Define to default Point
                editGeofence.setCenter(center);
                editGeofence.setRadiusInMeters(radiusInMeters);
                Log.d(TAG, "Prepare Insert for : " + editGeofence);
            }
            //Define Center
            myLocationFollow(false);
            mapController.setCenter(editGeofence.getCenterAsGeoPoint());
            // Do Edit
            GeofenceEditOverlay mapOverlay = super.showGeofenceOverlays();
            if (mapOverlay!=null) {
                mapOverlay.doEditCircleGeofenceWithoutMenu(editGeofence);
            }
        }
    }

    // ===========================================================
    // Life Cycle
    // ===========================================================
    public void handleIntent(Intent intent) {

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // ===========================================================
    // Load Data
    // ===========================================================
    public void onGeofencePrepareInsert(CircleGeofence fence) {
        Log.d(TAG, "onGeofencePrepareInsert");
        // Do Edit
        this.editGeofence = fence;
        displayGeofence(fence);
    }


    public void onGeofenceSelect(Uri id, CircleGeofence fence) {
        this.editGeofence = fence;
        displayGeofence(fence);
    }

    // ===========================================================
    // Play Service Check
    // ===========================================================


    // ===========================================================
    // Other
    // ===========================================================


}
