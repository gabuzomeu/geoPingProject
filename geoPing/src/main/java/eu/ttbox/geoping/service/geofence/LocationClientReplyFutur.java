package eu.ttbox.geoping.service.geofence;


import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * http://www.nurkiewicz.com/2013/02/implementing-custom-future.html
 * http://www.javacodegeeks.com/2013/02/implementing-custom-future.html
 */
public class LocationClientReplyFutur implements Future<Location>,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = "LocationClientReplyFutur";


    private static enum State {WAITING, DONE, CANCELLED}

    // Service
    private LocationClient mLocationClient;

    // Instance
    private final BlockingQueue<Location> reply = new ArrayBlockingQueue<Location>(1);
    private volatile State state = State.WAITING;

    public LocationClientReplyFutur(LocationClient locationClient) {
        this.mLocationClient = locationClient;
        mLocationClient.registerConnectionCallbacks(this);
        mLocationClient.registerConnectionFailedListener(this);
    }


    // ===========================================================
    //   ConnectionCallbacks  OnConnectionFailedListener
    // ===========================================================

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        this.state = State.CANCELLED;
    }

    // ===========================================================
    //   ConnectionCallbacks  ConnectionCallbacks
    // ===========================================================


    @Override
    public void onConnected(Bundle bundle) {
        Location lastLoc = mLocationClient.getLastLocation();
        try {
            reply.put(lastLoc);
            // Register
            state = State.DONE;
            cleanUp();
        } catch (InterruptedException e) {
            Log.e(TAG, "InterruptedException : " + e.getMessage(), e);
        }

    }

    @Override
    public void onDisconnected() {

    }


    // ===========================================================
    //   LocationClient
    // ===========================================================
    private boolean isLocationClientAvailable() {
        return mLocationClient != null && (mLocationClient.isConnected() || mLocationClient.isConnecting());
    }


    private void cleanUp() {
        if (mLocationClient!=null) {
            mLocationClient.unregisterConnectionCallbacks(this);
            mLocationClient.unregisterConnectionFailedListener(this);
        }
    }

    // ===========================================================
    //   Furtur
    // ===========================================================


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        state = State.CANCELLED;
        cleanUp();
        return true;

    }

    @Override
    public boolean isCancelled() {
        return state == State.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state == State.DONE;
    }

    @Override
    public Location get() throws InterruptedException, ExecutionException {
        if (isLocationClientAvailable()) {
            return this.reply.take();
        }
        return null;
    }

    @Override
    public Location get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Location replyOrNull = null;
        if (isLocationClientAvailable()) {
            replyOrNull = reply.poll(timeout, unit);
//        if (replyOrNull == null) {
//            throw new TimeoutException();
//        }
        }
        return replyOrNull;

    }
}
