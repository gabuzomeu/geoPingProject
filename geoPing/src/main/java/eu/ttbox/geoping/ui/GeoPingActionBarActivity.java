package eu.ttbox.geoping.ui;

import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.ads.AdView;

import eu.ttbox.geoping.ui.admob.AdmobHelper;


public class GeoPingActionBarActivity extends ActionBarActivity {


    private AdView adView;

    // ===========================================================
    // Adview event
    // ===========================================================

    @Override
    public void onStart() {
        super.onStart();
        adView = AdmobHelper.bindAdMobView(this);
    }


    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }


}


