package eu.ttbox.geoping.service.sensor;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatterySensorStaticAccessor {

    private static final String TAG = "BatterySensorStaticAccessor";


    public static interface BatteryLevelCallback {
        void setBatteryLevel(int batteryLevelInPercent);
    }

    public static class BatteryLevelReceiver extends BroadcastReceiver {

        // Callback
        private BatteryLevelCallback callback;

        // Value
        private long time = -1;
        private int batteryLevelInPercent = -1;


        public BatteryLevelReceiver(BatteryLevelCallback batteryLevelCallback) {
            super();
            this.callback = batteryLevelCallback;
        }

        public void onReceive(Context context, Intent intent) {
            context.unregisterReceiver(this);
            int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int level = -1;
            if (rawlevel >= 0 && scale > 0) {
                level = (rawlevel * 100) / scale;
            }
            Log.d(TAG, "Battery Level Remaining: " + level + "%");
            time = System.currentTimeMillis();
            batteryLevelInPercent = level;
            // Callback
            if (callback != null) {
                callback.setBatteryLevel(batteryLevelInPercent);
            }
        }

        public boolean isDone() {
            return batteryLevelInPercent > -1;
        }

        public int getBatteryLevel() {
            return batteryLevelInPercent;
        }

        public long getTime() {
            return time;
        }
    }

    public static BatteryLevelReceiver batteryLevel(Context context) {
        return batteryLevel(context, null);
    }
    
    public static BatteryLevelReceiver batteryLevel(Context context, BatteryLevelCallback batteryLevelCallback) {
        BatteryLevelReceiver batteryLevelReceiver = new BatteryLevelReceiver(batteryLevelCallback);
        // register
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        context.registerReceiver(batteryLevelReceiver, batteryLevelFilter);
        return batteryLevelReceiver;
    }

}
