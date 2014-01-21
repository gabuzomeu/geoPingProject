package eu.ttbox.geoping.ui.admob;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;

public class AdmobHelper {

    private static final String TAG = "AdmobHelper";

    // ===========================================================
    // AdView
    // ===========================================================


    public static AdView bindAdMobView(Activity context) {
        AdView adView = null;
        // Admob
        if (isAddBlocked(context)) {
            View admob = context.findViewById(R.id.adsContainer);
            if (admob!=null) {
                admob.setVisibility(View.GONE);
            }
        } else {
            adView = (AdView) context.findViewById(R.id.adView);
        }
        // Request Ad
        if (adView != null) {
            adView.setAdListener(new AdListener() {
                public void onAdOpened() {
                    Log.d(TAG, "### AdListener onAdOpened AdView"  );
                }
                public void onAdLoaded() {
                    Log.d(TAG, "### AdListener onAdLoaded AdView"  );
                }
                public void onAdFailedToLoad(int errorcode) {
                    Log.d(TAG, "### AdListener onAdFailedToLoad AdView : errorcode = " + errorcode);
                }
             });

           AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
            Log.d(TAG, "### Load adRequest AdView"  );
        } else {
            Log.e(TAG, "### Null  AdView"  );

        }
        return adView;
    }

    public static boolean isAddBlocked(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isAddBlocked = sharedPreferences != null ? sharedPreferences.getBoolean(AppConstants.PREFS_ADD_BLOCKED, false) : false;
        return isAddBlocked;
    }

    // ===========================================================
    // InterstitialAd
    // ===========================================================


    public static class AppAdListener extends AdListener {

        InterstitialAd interstitial;

        public AppAdListener() {
        }

        public AppAdListener(InterstitialAd interstitial) {
            this.interstitial = interstitial;
        }

        @Override
        public void onAdLoaded() {
            Log.i(TAG, "### AdListener : onAdLoaded");
            super.onAdLoaded();
            interstitial.show();
        }
    }

    public static InterstitialAd displayInterstitialAd(Context context) {
        return displayInterstitialAd(context, new AppAdListener());
    }

    public static InterstitialAd displayInterstitialAd(Context context, AppAdListener adListener) {
        final InterstitialAd interstitial = new InterstitialAd(context);
        interstitial.setAdUnitId(context.getString(R.string.admob_key));
        // Add Listener
        adListener.interstitial = interstitial;
        interstitial.setAdListener(adListener);

        // Create ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Begin loading your interstitial.
        interstitial.loadAd(adRequest);

        return interstitial;
    }

}
