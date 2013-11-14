package eu.ttbox.geoping.ui.starting;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;

public class StartingWizardHelper {

    private static final String TAG = "StartingWizardActivity";


    public static void setSecurityMode(Context context, SecurityModeEnum securityMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        switch (securityMode) {
            case CHILDREN: {
                setBoolean(context, editor, R.string.pkey_shownotif_newparing_default, false);
                setBoolean(context, editor, R.string.pkey_launcher_icon, false);
                setBoolean(context, editor, R.string.pkey_widget_pairing_list, false);
                setBoolean(context, editor, R.string.pkey_widget_person_list, false);
            }
            case PARENT: {

            }

        }

        editor.commit();
    }


    private static void setBoolean(Context context, SharedPreferences.Editor editor, int keyId, boolean value) {
        String key = context.getString(keyId);
        editor.putBoolean(key, value);
    }

    private static void applyPrefsConfig(Context context, int keyId, boolean value) {
        // Appply Config
        switch (keyId) {
            case R.string.pkey_launcher_icon :
                ExtraFeatureHelper.enabledSettingLaucherIcon(context, value);
                break;
            case R.string.pkey_widget_pairing_list :
                ExtraFeatureHelper.enabledSettingPairingListIcon(context, value);
                break;
            case R.string.pkey_widget_person_list :
                ExtraFeatureHelper.enabledSettingPesonListIcon(context, value);
                break;

        }
    }
}
