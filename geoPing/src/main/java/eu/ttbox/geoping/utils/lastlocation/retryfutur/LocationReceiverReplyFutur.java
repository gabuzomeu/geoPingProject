package eu.ttbox.geoping.utils.lastlocation.retryfutur;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class LocationReceiverReplyFutur implements Future<Location> {

    private static final String TAG = "LocationReceiverReplyFutur";

    private static final String SINGLE_LOCATION_UPDATE_ACTION = "eu.ttbox.osm.utils.lastlocation.SINGLE_LOCATION_UPDATE_ACTION";

    private static enum State {WAITING, DONE, CANCELLED}


    // Instance
    private final BlockingQueue<Location> reply = new ArrayBlockingQueue<Location>(1);
    private volatile State state = State.WAITING;


    // Instance
    private Context context;
    private PendingIntent singleUpatePI;

    // Service
    private LocationManager locationManager;

    // ===========================================================
    //   Constructor
    // ===========================================================


    public LocationReceiverReplyFutur(Context context) {
        super();
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Construct the Pending Intent that will be broadcast by the oneshot
        // location update.
        Intent updateIntent = new Intent(SINGLE_LOCATION_UPDATE_ACTION);
        singleUpatePI = PendingIntent.getBroadcast(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Init
        init(context);
    }


    // ===========================================================
    //   LocationClient
    // ===========================================================


    public void init(Context context) {
        // Criteria
        // Coarse accuracy is specified here to get the fastest possible result.
        // The calling Activity will likely (or have already) request ongoing
        // updates using the Fine location provider.
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_LOW);
        // Register receiver
        IntentFilter locIntentFilter = new IntentFilter(SINGLE_LOCATION_UPDATE_ACTION);
        context.registerReceiver(singleUpdateReceiver, locIntentFilter);
        locationManager.requestSingleUpdate(criteria, singleUpatePI);
    }


    private void cleanUp() {
        context.unregisterReceiver(singleUpdateReceiver);
        locationManager.removeUpdates(singleUpatePI);
    }


    protected BroadcastReceiver singleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String key = LocationManager.KEY_LOCATION_CHANGED;
            Location location = (Location) intent.getExtras().get(key);
            try {
                reply.put(location);
                // Register
                state = State.DONE;
                cleanUp();
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException : " + e.getMessage(), e);
            }

        }
    };

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
        return this.reply.take();
    }

    @Override
    public Location get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Location replyOrNull = reply.poll(timeout, unit);
        if (replyOrNull == null) {
            //counterCall.getAndDecrement();
            throw new TimeoutException();
        }
        return replyOrNull;
    }

}
