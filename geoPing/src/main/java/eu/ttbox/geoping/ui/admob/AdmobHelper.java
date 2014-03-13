package eu.ttbox.geoping.ui.admob;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;

public class AdmobHelper {

    private static final String TAG = "AdmobHelper";

    // ===========================================================
    // AdView : https://developers.google.com/mobile-ads-sdk/docs/admob/fundamentals
    // https://groups.google.com/forum/#!msg/google-admob-ads-sdk/8MCNsiVAc7A/pkRLcQ9zPtYJ
    // ===========================================================


    public static AdView bindAdMobView(Activity context) {
        AdView adView = null;
        // Admob
        if (isAddBlocked(context)) {
            View admob = context.findViewById(R.id.admob);
            Log.d(TAG, "### is Add Blocked adsContainer : " + admob);
            if (admob != null) {
                admob.setVisibility(View.GONE);
                Log.d(TAG, "### is Add Blocked adsContainer ==> GONE");
            }
        } else {
            // Container
            View admob = context.findViewById(R.id.admob);
            Log.d(TAG, "### is Add Not Blocked adsContainer : " + admob);
            if (admob != null) {
                admob.setVisibility(View.VISIBLE);
                Log.d(TAG, "### is Add Not Blocked adsContainer ==> VISIBLE");
            }
            // Find AdView
            adView = (AdView) context.findViewById(R.id.adView);
        }
        // Request Ad
        if (adView != null) {
            // http://stackoverflow.com/questions/11790376/animated-mopub-admob-native-ads-overlayed-on-a-game-black-out-screen
            //adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            // Listener
            adView.setAdListener(new AdListener() {
                public void onAdOpened() {
                    Log.d(TAG, "### AdListener onAdOpened AdView");
                }

                public void onAdLoaded() {
                    Log.d(TAG, "### AdListener onAdLoaded AdView");
                }

                public void onAdFailedToLoad(int errorcode) {
                    switch (errorcode) {
                        case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                            Log.d(TAG, "### AdListener onAdFailedToLoad AdView : errorcode = ERROR_CODE_INTERNAL_ERROR " );
                            break;
                        case AdRequest.ERROR_CODE_INVALID_REQUEST:
                            Log.d(TAG, "### AdListener onAdFailedToLoad AdView : errorcode = ERROR_CODE_INVALID_REQUEST " );
                            break;
                        case AdRequest.ERROR_CODE_NETWORK_ERROR:
                            Log.d(TAG, "### AdListener onAdFailedToLoad AdView : errorcode = ERROR_CODE_NETWORK_ERROR " );
                            break;
                        case AdRequest.ERROR_CODE_NO_FILL:
                            Log.d(TAG, "### AdListener onAdFailedToLoad AdView : errorcode = ERROR_CODE_NO_FILL " );
                            break;
                        default:
                            Log.d(TAG, "### AdListener onAdFailedToLoad AdView : errorcode = " + errorcode);
                    }

                }
            });
        //    adView.setAdUnitId(context.getString(R.string.admob_key));
        //    adView.setAdSize(AdSize.SMART_BANNER);

            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice("149D6C776DC12F380715698A396A64C4")
                    .build();
            adView.loadAd(adRequest);
            Log.d(TAG, "### Load adRequest AdView");
        } else {
            Log.e(TAG, "### Null  AdView");

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
