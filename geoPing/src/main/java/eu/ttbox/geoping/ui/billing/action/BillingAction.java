package eu.ttbox.geoping.ui.billing.action;

import android.content.Context;

public interface BillingAction {

    boolean isApply(String skuTested);

    boolean isActif(Context context);

    boolean activate(Context context, boolean wantedActif);

}
