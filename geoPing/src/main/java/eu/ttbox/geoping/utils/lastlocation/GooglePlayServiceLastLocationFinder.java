package eu.ttbox.geoping.utils.lastlocation;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.util.concurrent.Future;

import eu.ttbox.geoping.utils.lastlocation.retryfutur.LocationClientReplyFutur;


public class GooglePlayServiceLastLocationFinder implements ILastLocationFinder {

    private static String TAG = "GingerbreadLastLocationFinder";

    private Context context;
    private LocationClientConnectionCallbacks connectionCallbacks = new LocationClientConnectionCallbacks();
    private LocationClientReplyFutur locationReplyFutur = null;
    // Service
    private LocationClient mLocationClient;

    // ===========================================================
    //   Constructor
    // ===========================================================


    public GooglePlayServiceLastLocationFinder(Context context) {
        this.context = context;
        mLocationClient = new LocationClient(context, this.connectionCallbacks, this.connectionCallbacks);
        this.locationReplyFutur = new LocationClientReplyFutur(mLocationClient);
    }

    @Override
    public void onStop() {
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
    }

    // ===========================================================
    //   ConnectionCallbacks  OnConnectionFailedListener
    // ===========================================================

    public class LocationClientConnectionCallbacks implements
            GooglePlayServicesClient.ConnectionCallbacks,
            GooglePlayServicesClient.OnConnectionFailedListener {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }

        @Override
        public void onConnected(Bundle bundle) {


        }

        @Override
        public void onDisconnected() {

        }
    }
    // ===========================================================
    //   Business
    // ===========================================================


    @Override
    public Future<Location> getLastBestLocation(int minDistance, long minTime) {
        return locationReplyFutur;
    }

    @Override
    public Future<Location> getLastLocation() {
        return locationReplyFutur;
    }
}
