package eu.ttbox.geoping.ui.billing;


import java.util.ArrayList;

import eu.ttbox.geoping.service.billing.util.IabHelper;
import eu.ttbox.geoping.service.billing.util.SkuDetails;

public class SkuProductEnum {

    // Free Feature
    public static SkuDetails SECU_HIDE_LAUNCHER = new SkuDetails("secu_hide_launcher" //
            , IabHelper.ITEM_TYPE_INAPP, "No icon app launcher", "Free" //
            , "Hide the GeoPing Application in the Phone");

    // Add
    public static SkuDetails SKU_NO_AD_PER_YEAR = new SkuDetails("no_ad_per_year" //
            , IabHelper.ITEM_TYPE_INAPP, "No add in app", "$1.99" //
            , "Suppress all adds during one year");

    // Bear
    public static SkuDetails BEAR_PINT = new SkuDetails("bear.pint" //
            , IabHelper.ITEM_TYPE_INAPP, "A Pint of Guissness", "5 €" //
            , "Pay me Bear : A good and french pint of Guissness in an Pub.");

    public static SkuDetails BEAR_HALFPINT = new SkuDetails("bear.halfpint"//
            , IabHelper.ITEM_TYPE_INAPP, "A Half Pint in a Pub", "2.5 €" //
            , "Pay me Bear : A Half pint of Bear in an Pub.");

    public static SkuDetails BEAR_SUPERMARKET = new SkuDetails("bear.supermarket" //
            , IabHelper.ITEM_TYPE_INAPP, "A Supermarket Kro", "1 €" //
            , "Pay me Bear : A supermarket's Kro.");


    public static ArrayList<SkuDetails> createListItems() {
        ArrayList<SkuDetails> adapter = new ArrayList<SkuDetails>();
        // Feature
        adapter.add(SECU_HIDE_LAUNCHER);
        adapter.add(SKU_NO_AD_PER_YEAR);

        // Bear
        adapter.add(BEAR_PINT);
        adapter.add(BEAR_HALFPINT);
        adapter.add(BEAR_SUPERMARKET);

        // Test Product
        adapter.add(new SkuDetails("android.test.purchased", IabHelper.ITEM_TYPE_INAPP, "Android Test Purchased", "$0.99" //
                , "you can test your signature verification implementation using these responses."));


        return adapter;

    }
}
