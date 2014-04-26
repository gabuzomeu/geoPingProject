package eu.ttbox.geoping.domain.pairing;


import android.content.ComponentName;
import android.content.Context;

import eu.ttbox.geoping.service.slave.eventspy.BootCompleteReceiver;
import eu.ttbox.geoping.service.slave.eventspy.LowBatteryReceiver;
import eu.ttbox.geoping.service.slave.eventspy.PhoneCallReceiver;
import eu.ttbox.geoping.service.slave.eventspy.ShutdownReceiver;
import eu.ttbox.geoping.service.slave.eventspy.SimChangeReceiver;

public class SpyServiceComponentName {


    public static ComponentName[] getComponentSpyBootShutdownReceiver(Context context) {
        ComponentName shutdown = new ComponentName(context, ShutdownReceiver.class);
        ComponentName bootComplete = new ComponentName(context, BootCompleteReceiver.class);
        return new ComponentName[]{shutdown, bootComplete};
    }

    public static ComponentName[] getComponentSpyLowBatteryReceiver(Context context) {
        ComponentName componentName = new ComponentName(context, LowBatteryReceiver.class);
        return new ComponentName[]{componentName};
    }

    public static ComponentName[] getComponentSimChangeReceiver(Context context) {
        ComponentName componentName = new ComponentName(context, SimChangeReceiver.class);
        return new ComponentName[]{componentName};
    }

    public static ComponentName[] getComponentPhoneCallReceiver(Context context) {
        ComponentName componentName = new ComponentName(context, PhoneCallReceiver.class);
        return new ComponentName[]{componentName};
    }


}
