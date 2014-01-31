package eu.ttbox.geoping.ui.billing.action;


import eu.ttbox.geoping.core.AppConstants;

public class NoAdsBillingAction extends BooleanPrefsBillingAction {

    private static String SKU_NO_AD_PER_YEAR = "no_ad_per_year";

    public NoAdsBillingAction( ) {
        super(SKU_NO_AD_PER_YEAR, AppConstants.PREFS_ADD_BLOCKED, false);
    }
}
