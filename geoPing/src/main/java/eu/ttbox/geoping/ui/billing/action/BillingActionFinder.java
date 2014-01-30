package eu.ttbox.geoping.ui.billing.action;


import android.content.Context;

import java.util.ArrayList;


public class BillingActionFinder  {

    private ArrayList<BillingAction> billingActions;

    private Context context;

    public BillingActionFinder( Context context) {
        super();
        this.context = context;
        init();
    }

    public void init() {
        ArrayList<BillingAction> actions = new ArrayList<BillingAction>();
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


    public boolean isActif( String skuTested) {
        BillingAction action = getBillingAction(skuTested);
        if (action!=null) {
            return  action.isActif(context);
        }
        return false;
    }


    public boolean activate( String skuTested, boolean wantedActif) {
        BillingAction action = getBillingAction(skuTested);
        if (action!=null) {
            return  action.activate(context, wantedActif);
        }
        return false;
    }
}
