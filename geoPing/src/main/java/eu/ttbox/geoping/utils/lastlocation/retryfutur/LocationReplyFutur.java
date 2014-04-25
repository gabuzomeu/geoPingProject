package eu.ttbox.geoping.utils.lastlocation.retryfutur;

import android.location.Location;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class LocationReplyFutur implements Future<Location> {

    private Location mLocation;


    // ===========================================================
    //   Constructor
    // ===========================================================


    public LocationReplyFutur(Location mLocation) {
        this.mLocation = mLocation;
    }


    // ===========================================================
    //   Futur
    // ===========================================================


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return mLocation !=null;
    }

    @Override
    public Location get() throws InterruptedException, ExecutionException {
        return mLocation;
    }

    @Override
    public Location get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mLocation;
    }
}
