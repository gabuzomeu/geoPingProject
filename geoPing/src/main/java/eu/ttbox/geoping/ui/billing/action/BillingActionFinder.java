package eu.ttbox.geoping.ui.billing.action;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class BillingActionFinder  {

    List<BillingAction> billingActions;

    public BillingActionFinder() {
        super();
        init();
    }

    public void init() {
        List<BillingAction> actions = new ArrayList<BillingAction>();
        actions.add(new NoAdsBillingAction());
        this.billingActions = actions;
    }


    private BillingAction getBillingAction(String skuTested) {
        BillingAction actionMatch = null;
        for (BillingAction action : billingActions) {
            boolean isApply = action.isApply(skuTested);
            if (isApply) {
            //    actionMatch =  action;
                return action;
            }
        }
        return actionMatch;
    }


    public boolean isApply(String skuTested) {
        BillingAction action = getBillingAction(skuTested);
        return action!=null;
    }


    public boolean isActif(Context context, String skuTested) {
        BillingAction action = getBillingAction(skuTested);
        if (action!=null) {
            return  action.isActif(context);
        }
        return false;
    }


    public boolean activate(Context context, String skuTested, boolean wantedActif) {
        BillingAction action = getBillingAction(skuTested);
        if (action!=null) {
            return  action.activate(context, wantedActif);
        }
        return false;
    }
}
