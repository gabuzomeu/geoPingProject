package eu.ttbox.geoping.ui.prefs;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.List;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.NotifToasts;
import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.ui.prefs.comp.version.AppVersionPreference;
import eu.ttbox.geoping.ui.prefs.lock.core.CommandsPrefsHelper;
import eu.ttbox.geoping.ui.prefs.lock.core.PreferenceHolder;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import group.pals.android.lib.ui.lockpattern.prefs.SecurityPrefs;

/**
 * http://www.blackmoonit.com/2012/07/all_api_prefsactivity/
 */
public class GeoPingPrefActivity extends PreferenceActivity //SlidingPreferenceActivity
        implements OnSharedPreferenceChangeListener, PreferenceHolder {

    private static final String TAG = "GeoPingPrefActivity";

    private SharedPreferences sharedPreferences;

    // Dev Listener
    private SharedPreferences developmentPreferences;
    SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

    // ===========================================================
    // Constructor
    // ===========================================================

    @Override
    public void onCreate(Bundle aSavedState) {
        // onBuildHeaders() will be called during super.onCreate()
        developmentPreferences = getSharedPreferences(AppVersionPreference.PREFS_DEV_MODE, Context.MODE_PRIVATE);
        super.onCreate(aSavedState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

// TODO       customizeSlidingMenu();
        // Compatibity
        if (!VersionUtils.isHc11) {
            final boolean showDev = developmentPreferences.getBoolean(AppVersionPreference.PREF_SHOW_DEVMODE, false);
            // addPreferencesFromResource(R.xml.prefs);
            addPreferencesFromResource(R.xml.geoping_prefs);
            addPreferencesFromResource(R.xml.pairing_prefs);
            addPreferencesFromResource(R.xml.map_prefs);
            // Lock Pattern
            addPreferencesFromResource(R.xml.lockpattern_prefs);
            new CommandsPrefsHelper(this, this).init();
            // Emergency Mode
            addPreferencesFromResource(R.xml.emergency_prefs);

            // Dev Mode
            if (showDev) {
                addPreferencesFromResource(R.xml.development_prefs);
            }
            addPreferencesFromResource(R.xml.info_prefs);
        } else {
            // Add selector
            setHcDisplayHomeAsUpEnabled();
        }
        // Init Summary
        initSummaries(this.getPreferenceScreen());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setHcDisplayHomeAsUpEnabled() {
        // Add selector
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Set the summaries of all preferences
     */
    private void initSummaries(PreferenceGroup pg) {
        if (pg == null) {
            return;
        }
        for (int i = 0; i < pg.getPreferenceCount(); ++i) {
            Preference p = pg.getPreference(i);
            // Init
            if (p instanceof PreferenceGroup) {
                this.initSummaries((PreferenceGroup) p); // recursion
            } else {
                setSummary(p);
            }
        }
    }

    /**
     * Set the summaries of the given preference
     */
    private static void setSummary(Preference pref) {
        // react on type or key
        if (pref instanceof EditTextPreference) {
            EditTextPreference editPref = (EditTextPreference) pref;
            String prefText = editPref.getText();
           int  editInputType =  editPref.getEditText().getInputType();
            if (prefText != null && prefText.length() > 0) {
                if((editInputType & InputType.TYPE_NUMBER_VARIATION_PASSWORD) > 0) {
                    pref.setSummary("XXXXX");
                } else {
                    pref.setSummary(prefText);
                }

            }
        } else if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());
        }
    }
    // ===========================================================
    // onActivityResult
    // ===========================================================


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CommandsPrefsHelper.REQ_CREATE_PATTERN: {
                if (resultCode == RESULT_OK) {
                    char[] pattern = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN);
                    SecurityPrefs.setPattern(this, pattern);
                } else {
                    // setTitle(R.string.app_name);
                }

                break;
            }// REQ_CREATE_PATTERN
        }
    }


    // ===========================================================
    // Tracking Event
    // ===========================================================

//    @Override
//    public void onStart() {
//        super.onStart();
//        // Tracker
//        EasyTracker.getInstance(this).activityStart(this);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        // Tracker
//        EasyTracker.getInstance(this).activityStop(this);
//    }

    @Override
    public void onResume() {
        super.onResume();
        // Register change listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        // Resume for Headeers
        if (VersionUtils.isHc11) {
            onResumeHc11();
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onResumeHc11() {
        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                // Recompute All Sumaries
                // TODO Recompute Only the changed Values  initSummaries(getPreferenceScreen());
                // Recompute All Headers
                invalidateHeaders();

            }
        };
        developmentPreferences.registerOnSharedPreferenceChangeListener(mDevelopmentPreferencesListener);
        invalidateHeaders();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Register change listener
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        // Resume for Headeers
        if (developmentPreferences != null && mDevelopmentPreferencesListener != null) {
            developmentPreferences.unregisterOnSharedPreferenceChangeListener(mDevelopmentPreferencesListener);
            mDevelopmentPreferencesListener = null;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
        if (VersionUtils.isHc11) {
            loadHeadersFromResource(R.xml.preference_headers, target);
            updateHeaderList(target);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void updateHeaderList(List<Header> target) {
        final boolean showDev = developmentPreferences.getBoolean(AppVersionPreference.PREF_SHOW_DEVMODE, false);
        int i = 0;
        while (i < target.size()) {
            Header header = target.get(i);
            // Ids are integers, so downcasting
            int id = (int) header.id;
            if (id == R.id.development_settings) {
                if (!showDev) {
                    target.remove(i);
                }
            }
            // Increment if the current one wasn't removed by the Utils code.
            // if (target.get(i) == header) {
            i++;
            // }
        }
    }

    // ===========================================================
    // Sliding Menu
    // ===========================================================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
//    
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public SlidingMenu customizeSlidingMenu() {
//        setBehindContentView(R.layout.slidingmenu_frame);
//        SlidingMenu slidingMenu = getSlidingMenu();
//        SlidingMenuHelper.customizeSlidingInstance(this, slidingMenu, SlidingMenu.TOUCHMODE_FULLSCREEN);
//        // Add selector
//        customizeSlidingMenuActionBar(); 
//        return slidingMenu;
//    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void customizeSlidingMenuActionBar() {
        if (VersionUtils.isHc11) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    // ===========================================================
    // Generic Fragment
    // ===========================================================

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle aSavedState) {
            super.onCreate(aSavedState);
            Context anAct = getActivity().getApplicationContext();
            String fragFileIdentifer = getArguments().getString("pref-resource");
            int thePrefRes = anAct.getResources().getIdentifier(fragFileIdentifer, "xml", anAct.getPackageName());
            Log.i(TAG, "Create PrefsFragment for file : " + fragFileIdentifer);
            addPreferencesFromResource(thePrefRes);
            // Init Summary
            initSummaries(this.getPreferenceScreen());
        }


        /**
         * Set the summaries of all preferences
         */
        private void initSummaries(PreferenceGroup pg) {
            if (pg == null) {
                return;
            }
            for (int i = 0; i < pg.getPreferenceCount(); ++i) {
                Preference p = pg.getPreference(i);
                // Init
                if (p instanceof PreferenceGroup) {
                    this.initSummaries((PreferenceGroup) p); // recursion
                } else {
                    setSummary(p);
                }
            }
        }
    }

    // ===========================================================
    // Lock Fragment
    // ===========================================================

    /**
     * This fragment shows the COMMANDS.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PrefsLockPatternFragment extends PreferenceFragment
            implements PreferenceHolder {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesForActivity();
        }// onCreate()


        private void addPreferencesForActivity() {
            addPreferencesFromResource(R.xml.lockpattern_prefs);
            new CommandsPrefsHelper(getActivity(), this).init();
            Log.d(TAG, "##################### int CommandsPrefsHelper #####################");
        }
    }// Fragment_Prefs_Commands

    // ===========================================================
    // Change Pref Listener
    // ===========================================================

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Ask Backup
        BackupManager.dataChanged(getPackageName());
        // Update Display
        // Preference pref = findPreference(key);
        // setSummary(pref);
        // Tracker
        // GeoPingApplication.getInstance().tracker().trackPageView("/Pref/" +
        // key);
        Tracker tracker =   tracker = GeoPingApplication.getGeoPingApplication(this).getTracker();
        tracker.send(new HitBuilders.EventBuilder()//
                .setCategory("ui_pref") // Category
                .setAction( "changed") // Action
                .setLabel(key) // Label
                .build());
    }

    // ===========================================================
    // Backup Restore
    // ===========================================================

    public void onBackupButtonClick(View v) {
        BackupManager.dataChanged(getPackageName());
    }

    /**
     * Click handler, designated in the layout, that runs a restore of the app's
     * most recent data when the button is pressed.
     */
    public void onRestoreButtonClick(View v) {
        Log.v(TAG, "Requesting restore of our most recent data");
        BackupManager mBackupManager = new BackupManager(this);
        mBackupManager.requestRestore(new RestoreObserver() {
            public void restoreFinished(int error) {
                /** Done with the restore! Now draw the new state of our data */
                if (error == 0) {
                    Log.v(TAG, "Restore finished, error = " + error);
                    NotifToasts.showBackupRestored(GeoPingPrefActivity.this);
                } else {
                    Log.e(TAG, "Restore finished with error = " + error);
                }
            }
        });
    }

    // ===========================================================
    // Other
    // ===========================================================

}
