package eu.ttbox.geoping.ui.billing.action;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BooleanPrefsBillingAction implements BillingAction {

    private final String sku;
    private final String prefKey;
    private final boolean prefDefaultValue;

    public BooleanPrefsBillingAction(String sku, String prefKey, boolean prefDefaultValue) {
        this.sku = sku;
        this.prefKey = prefKey;
        this.prefDefaultValue = prefDefaultValue;
    }

    public String getSku() {
        return sku;
    }

    public boolean isApply(String skuTested) {
        return sku.equals(skuTested);
    }

    public SharedPreferences getSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences;
    }

    public boolean isActif(Context context) {
        SharedPreferences sharedPreferences =getSharedPreferences(context);
        boolean isAddBlocked = sharedPreferences != null ? sharedPreferences.getBoolean(prefKey, prefDefaultValue) : prefDefaultValue;
        return convertValue(isAddBlocked);
    }

    public boolean activate(Context context, boolean wantedActif) {
        SharedPreferences sharedPreferences =getSharedPreferences(context);
        SharedPreferences.Editor editor =  sharedPreferences.edit();
        editor.putBoolean(prefKey, convertValue(wantedActif));
        return editor.commit();
    }

    protected boolean convertValue(boolean val) {
        return val;
    }
}
