package eu.ttbox.geoping.utils.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class BatterySensorReplyFutur implements Future<Integer> {

    private static final String TAG = "LocationReceiverReplyFutur";


    private static enum State {WAITING, DONE, CANCELLED}


    // Instance
    private final BlockingQueue<Integer> reply = new ArrayBlockingQueue<Integer>(1);
    private volatile State state = State.WAITING;


    // Instance
    private Context context;

    // ===========================================================
    //   Constructor
    // ===========================================================


    public BatterySensorReplyFutur(Context context) {
        super();
        this.context = context;
        // Init
        init(context);
    }


    // ===========================================================
    //   LocationClient
    // ===========================================================


    public void init(Context context) {
        // Register receiver
        IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(singleUpdateReceiver, batteryIntentFilter);
    }


    private void cleanUp() {
        context.unregisterReceiver(singleUpdateReceiver);
    }


    protected BroadcastReceiver singleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = (rawlevel * 100) / scale;
            }
            Log.d(TAG, "Battery Level Remaining: " + level + "%");
            try {
                reply.put(Integer.valueOf(level));
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
    public Integer get() throws InterruptedException, ExecutionException {
        return this.reply.take();
    }

    @Override
    public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Integer replyOrNull = reply.poll(timeout, unit);
        if (replyOrNull == null) {
            //counterCall.getAndDecrement();
            throw new TimeoutException();
        }
        return replyOrNull;
    }

    public Integer getOrNull(long timeout, TimeUnit unit)  {
        Integer replyOrNull = null;
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