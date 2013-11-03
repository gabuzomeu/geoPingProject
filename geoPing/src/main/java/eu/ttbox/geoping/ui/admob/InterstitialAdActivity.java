package eu.ttbox.geoping.ui.admob;


import android.app.Activity;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import eu.ttbox.geoping.R;


public class InterstitialAdActivity extends Activity {

    private InterstitialAd interstitial;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create the interstitial.
        interstitial = new InterstitialAd(this);
        String adUnitId = getString(R.string.admob_key);
        interstitial.setAdUnitId(adUnitId);

        // Create ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Begin loading your interstitial.
        interstitial.loadAd(adRequest);

        // Show Add
        displayInterstitial();
    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }
}
