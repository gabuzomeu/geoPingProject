package eu.ttbox.geoping.ui.billing;


import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import eu.ttbox.geoping.LaucherMainActivity;
import eu.ttbox.geoping.service.geofence.ReceiveTransitionsIntentService;
import eu.ttbox.geoping.ui.widget.PersonWidgetProvider;
import eu.ttbox.geoping.ui.widget.pairing.PairingWidgetProvider;

public class ExtraFeatureHelper {

    private static final String TAG = "ExtraFeatureHelper";


    // ===========================================================
    // Extra Feature accessors
    // ===========================================================

    public static ComponentName getComponentNameLaucherIcon(Context context ) {
        ComponentName componentName = new ComponentName(context, LaucherMainActivity.class);
        return componentName;
    }
    public static ComponentName getComponentNamePersonListWidget(Context context ) {
        ComponentName componentName = new ComponentName(context, PersonWidgetProvider.class);
        return componentName;
    }
    public static ComponentName getComponentNamePairingListWidget(Context context ) {
        ComponentName componentName = new ComponentName(context, PairingWidgetProvider.class);
        return componentName;
    }


    public static boolean switchEnabledSettingLaucherIcon(Context context) {
        ComponentName componentName = new ComponentName(context, LaucherMainActivity.class);
        boolean  wantedState = isComponentEnabledSetting(context, componentName);
        return enabledComponentEnabledSetting(context, componentName, !wantedState);
    }

    public static boolean isEnabledSettingLaucherIcon(Context context, Boolean wantedState) {
        ComponentName componentName = getComponentNameLaucherIcon(context);
        return isComponentEnabledSetting(context, componentName);
    }

    public static boolean enabledSettingLaucherIcon(Context context, Boolean wantedState) {
        ComponentName componentName = getComponentNameLaucherIcon(context);
        return enabledComponentEnabledSetting(context, componentName, wantedState);
    }

    public static boolean enabledSettingPesonListIcon(Context context, Boolean wantedState) {
        ComponentName componentName = getComponentNamePersonListWidget(context);
        return enabledComponentEnabledSetting(context, componentName, wantedState);
    }

    public static boolean enabledSettingPairingListIcon(Context context, Boolean wantedState) {
        ComponentName componentName = getComponentNamePairingListWidget(context);
        return enabledComponentEnabledSetting(context, componentName, wantedState);
    }

    public static boolean enabledGeofence(Context context, Boolean wantedState) {
        ComponentName componentName = new ComponentName(context, ReceiveTransitionsIntentService.class);
        return enabledComponentEnabledSetting(context, componentName, wantedState);
    }


    // ===========================================================
    // Generic Fonctions
    // ===========================================================


    public static boolean isComponentEnabledSetting(Context context,  ComponentName componentName  ) {
        boolean result  = false;
        PackageManager pm = context.getPackageManager();
        int setting = pm.getComponentEnabledSetting(componentName);
        switch (setting) {
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                result = true;
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
            default:
                result = false;
                Log.w(TAG, "Not manage component setting : " + setting);
                break;
        }
        return result;
    }

    public static boolean enabledComponentEnabledSetting(Context context, ComponentName componentName , Boolean wantedState) {
        boolean newStatus = false;
        // Conrol Visibility of LaucherMainActivity
        PackageManager pm = context.getPackageManager();
        int setting = pm.getComponentEnabledSetting(componentName);
        Log.i(TAG, "enabled Component Enabled Setting " +componentName +   " : " + setting);
        switch (setting) {
            case PackageManager.COMPONENT_ENABLED_STATE_DEFAULT:
            case PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                newStatus = true;
                if (wantedState==null || !wantedState.booleanValue()) {
                    Log.i(TAG, "Disable component " + componentName);
                    pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    newStatus = false;
                }
                break;
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER:
            case PackageManager.COMPONENT_ENABLED_STATE_DISABLED:
                newStatus = false;
                if (wantedState==null ||  wantedState.booleanValue()) {
                    Log.i(TAG, "Enable component " + componentName);
                    pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    newStatus = true;
                }
                break;
            default:
                Log.w(TAG, "Not manage component " +componentName +
                        " setting : " + setting);
                break;
        }
        return newStatus;
    }

}
