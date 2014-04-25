package eu.ttbox.geoping.utils.lastlocation.retryfutur;


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
import java.util.concurrent.atomic.AtomicInteger;

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
    private final AtomicInteger counterCall = new AtomicInteger();
    private final BlockingQueue<Location> reply = new ArrayBlockingQueue<Location>(1);
    private volatile State state = State.WAITING;


    // ===========================================================
    //   Constructor
    // ===========================================================


    public LocationClientReplyFutur(LocationClient mLocationClient) {
        this.mLocationClient = mLocationClient;
        init();
    }


    private void init() {
        this.mLocationClient.registerConnectionCallbacks(this);
        this.mLocationClient.registerConnectionFailedListener(this);
    }


    private void cleanUp() {
        this.mLocationClient.unregisterConnectionCallbacks(this);
        this.mLocationClient.unregisterConnectionFailedListener(this);
    }

    // ===========================================================
    //   ConnectionCallbacks  OnConnectionFailedListener
    // ===========================================================

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        this.state = State.CANCELLED;
        cleanUp();
    }

    // ===========================================================
    //   ConnectionCallbacks  ConnectionCallbacks
    // ===========================================================


    @Override
    public void onConnected(Bundle bundle) {
        Location lastLoc = mLocationClient.getLastLocation();
        try {
            while (counterCall.getAndDecrement()>0) {
                reply.put(lastLoc);
            }
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
        if (mLocationClient.isConnected()) {
            return mLocationClient.getLastLocation();
        } else {
            counterCall.incrementAndGet();
            return this.reply.take();
        }
    }

    @Override
    public Location get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Location replyOrNull = null;
        if (mLocationClient.isConnected()) {
            replyOrNull =  mLocationClient.getLastLocation();
        } else {
            counterCall.incrementAndGet();
            replyOrNull = reply.poll(timeout, unit);
            if (replyOrNull == null) {
                counterCall.getAndDecrement();
                throw new TimeoutException();
            }
        }
        return replyOrNull;
    }

    public Location getOrNull(long timeout, TimeUnit unit)  {
        Location replyOrNull = null;
        try {
            replyOrNull = get(timeout, unit);
        } catch (InterruptedException e) {
            Log.d(TAG, "Ignore InterruptedException : " + e.getMessage());
        } catch (ExecutionException e) {
            Log.d(TAG, "ExecutionException : " + e.getMessage());
        } catch (TimeoutException e) {
            Log.d(TAG, "TimeoutException : " + e.getMessage());
        }
        return replyOrNull;
    }
}
