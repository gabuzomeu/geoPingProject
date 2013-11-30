package eu.ttbox.geoping;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.ui.admob.AdmobHelper;
import eu.ttbox.geoping.ui.prefs.lock.core.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;

public class LoginActivity extends ActionBarActivity { //

    private static final String TAG = "LoginActivity";

    private static final long LOCK_BASE_TIME_IN_MS = 30 * 1000;
    private static final String DEFAULT_USER = "Local_";
    // Service
    private SharedPreferences loginPrefs;
    private CountDownTimer countDownTimer;

    // AddView
    private AdView adView;

    // Binding
    private ImageView mSignboardImageView;
    private TextView mSignboardTextView;

    // Config instance
    private String user = DEFAULT_USER;
    private Intent destIntent = null;

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginPrefs = getSharedPreferences(AppConstants.PREFS_FILE_LOGIN, MODE_PRIVATE);

        // Read Parameter
        handleIntent(getIntent());
        // Load Screen
        if (CommandsPrefsHelper.isPassword(this)) {
            // Init Binding
            initBinding();
        } else {
            startMainActivity();
        }
    }

    private void initBinding() {
        setContentView(R.layout.login_activity);
        // Bind
        mSignboardImageView = (ImageView) findViewById(R.id.keyguard_signboard);
        if (!AdmobHelper.isAddBlocked(this)) {
            mSignboardImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "### onClick : Display Interstitial Ad");
                    displayInterstitial();
                }
            });
        }
        mSignboardTextView = (TextView) findViewById(R.id.keyguard_signboard_textview);
        // Manage Visibility
        setSignboardVisibility(false);

        // Ad View
        adView = AdmobHelper.bindAdMobView(this);
    }


    // ===========================================================
    // Handle Intent
    // ===========================================================

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String phone = null;

        if (intent != null) {
            phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            destIntent = intent.getParcelableExtra(Intents.EXTRA_INTENT_ACTIVITY);
            Log.d(TAG, "### Handle intent : " + intent);
            Intents.printExtras(TAG, intent.getExtras());
            Log.d(TAG, "### Define wanted intent  : " + destIntent);
        }
        // Define User Id
        if (phone != null) {
            user = phone + "_";
        } else {
            user = DEFAULT_USER;
        }
        //
        if (destIntent == null) {
            destIntent = new Intent(this, MainActivity.class);
        }
    }


    // ===========================================================
    // Save and Restore
    // ===========================================================


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Intents.EXTRA_PERSON_ID, user);
        if (destIntent != null) {
            outState.putParcelable(Intents.EXTRA_INTENT_ACTIVITY, destIntent);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        user = savedInstanceState.getString(Intents.EXTRA_PERSON_ID);
        Intent savIntent = savedInstanceState.getParcelable(Intents.EXTRA_INTENT_ACTIVITY);
        if (savIntent != null) {
            destIntent = savIntent;
        }
    }

    // ===========================================================
    // Life Cycle
    // ===========================================================

    @Override
    public void onStart() {
        super.onStart();
        // Tracker
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Tracker
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adView != null) {
            adView.pause();
        }
        cancelCountDown();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        if (!lockScreen()) {
            Log.d(TAG, "### onResume : openPromptPassword");
            openPromptPassword();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adView != null) {
            adView.destroy();
        }
        cancelCountDown();

    }

    // ===========================================================
    // AdView
    // ===========================================================


    private InterstitialAd interstitial;

    private InterstitialAd displayInterstitial() {
        if (interstitial == null) {
            AdmobHelper.AppAdListener adListener = new AdmobHelper.AppAdListener() {
                @Override
                public void onAdClosed() {
                    super.onAdClosed();
                    interstitial = null;
                }
            };
            // Create the interstitial.
//            final InterstitialAd
            interstitial = AdmobHelper.displayInterstitialAd(this, adListener);
        }
        return interstitial;
    }


    // ===========================================================
    // Action
    // ===========================================================

    private long getExpectedEnlapseLockTime(int failedCount) {
        // exponential Algo
        long failFactor = Math.round(Math.exp(Math.max(0, failedCount - 1)));
//       failFactor = 0;
        long enlapseExpectedLockTime = failFactor * LOCK_BASE_TIME_IN_MS;
        Log.d(TAG, "### Fail count " + failedCount + " ==> Factor : " + failFactor + " ==> " + enlapseExpectedLockTime + "ms");
        return enlapseExpectedLockTime;
    }


    private long getRestLockTimeInMs() {
        int failedCount = readPrefInt(R.string.pkey_login_failed_count, 0);
        return getRestLockTimeInMs(failedCount);
    }

    private long getRestLockTimeInMs(int failedCount) {
        long restLockTimeInMs = 0;
        if (failedCount > 0) {
            long now = System.currentTimeMillis();
            long failedDate = readPrefLong(R.string.pkey_login_failed_date, 0);
            // Lock Time
            long enlapseExpectedLockTime = getExpectedEnlapseLockTime(failedCount);
            // Compute Time
            long enlapseRealTime = now - failedDate;
            if (enlapseRealTime >= enlapseExpectedLockTime) {
                // The lock Time is past
                return 0;
            } else if (enlapseRealTime < 0) {
                // The system date as change, hack in progress ?
                Log.e(TAG, "The system date as change, hack in progress ?");
                enlapseExpectedLockTime = getExpectedEnlapseLockTime(failedCount * 4);
            } else {
                // Compute Resting time
                restLockTimeInMs = enlapseExpectedLockTime - enlapseRealTime;
            }
        }
        return restLockTimeInMs;
    }

    private boolean lockScreen() {
        long restLockTimeInMs = getRestLockTimeInMs();
        boolean isLockNeed = restLockTimeInMs > 0;
        if (isLockNeed) {
            startCountDownInSeconds(restLockTimeInMs);
        }
        return isLockNeed;
    }


    // ===========================================================
    // Scheduler
    // ===========================================================


    private void setDisplayText(long millisUntilFinished) {
        final long secondsRemaining = Math.round(millisUntilFinished / 1000);
        String msgDisplay = null;
        if (secondsRemaining >= 60l) {
            long s = secondsRemaining % 60;
            long min = (secondsRemaining / 60) % 60;
            if (secondsRemaining >= 60 * 60) {
                long hour = (secondsRemaining / (60 * 60)) % 24;
//                msgDisplay = "" + hour + "h " + min + "min " + s + "s";
                msgDisplay = getString(R.string.kg_too_many_failed_attempts_countdown_hours, hour, min, s);
                Log.d(TAG, "### Time to finish : " + hour + "h " + min + "min " + s + "s");
            } else {
//                msgDisplay = ""  + min + "min " + s + "s";
                msgDisplay = getString(R.string.kg_too_many_failed_attempts_countdown_minutes, min, s);
                Log.d(TAG, "### Time to finish : " + min + "min " + s + "s");
            }

        } else {
            //  msgDisplay = "" + secondsRemaining + "s";
            msgDisplay = getString(R.string.kg_too_many_failed_attempts_countdown_seconds, secondsRemaining);
            Log.d(TAG, "### Time to finish : " + secondsRemaining + "s");
        }
        if (secondsRemaining % 60 == 0) {
            if (!AdmobHelper.isAddBlocked(this)) {
                displayInterstitial();
            }
        }

        mSignboardTextView.setText(msgDisplay);
    }

    private synchronized void startCountDownInSeconds(long lockTimeInMs) {
        if (countDownTimer != null) {
            Log.w(TAG, "### Cancel previous CountDown");
            cancelCountDown();
        }
        countDownTimer = new CountDownTimer(lockTimeInMs, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                setDisplayText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                String displayText = "";
                mSignboardTextView.setText(displayText);
                //    if (countDownTimer != null) {
                countDownTimer = null;
                Log.d(TAG, "### CountDown onFinish : openPromptPassword");
                openPromptPassword();

                //     }
            }
        };
        Log.i(TAG, "### Start CountDown for : " + lockTimeInMs + " ms");
        setDisplayText(lockTimeInMs);
        setSignboardVisibility(true);
        countDownTimer.start();
    }

    private void setSignboardVisibility(boolean isVisible) {
        int visible = isVisible ? View.VISIBLE : View.GONE;
        mSignboardImageView.setVisibility(visible);
        mSignboardTextView.setVisibility(visible);

    }

    // ===========================================================
    // Prefs Accessors
    // ===========================================================

    private int incrementKey(SharedPreferences.Editor prefEditor, int pkeyId, int incCount) {
        return incrementKey(prefEditor, pkeyId, incCount, true);
    }

    private int incrementKey(SharedPreferences.Editor prefEditor, int pkeyId, int incCount, boolean perUser) {
        int afterIncVal = -1;
        if (incCount != 0) {
            int previousCount = readPrefInt(pkeyId, 0, perUser);
            int incVal = incCount + previousCount;
            writePrefInt(prefEditor, pkeyId, incVal, perUser);
            Log.d(TAG, "### Increment " + getString(pkeyId, perUser) + " : " + previousCount + " ===> " + incVal);
            afterIncVal = incVal;
        }
        return afterIncVal;
    }

    // Pref Long
    private SharedPreferences.Editor writePrefLong(SharedPreferences.Editor prefEditor, int pkeyId, long val) {
        return writePrefLong(prefEditor, pkeyId, val, true);
    }

    private SharedPreferences.Editor writePrefLong(SharedPreferences.Editor prefEditor, int pkeyId, long val, boolean perUser) {
        String key = getPrefKey(pkeyId, perUser);
        return prefEditor.putLong(key, val);
    }

    private long readPrefLong(int pkeyId, long defaultVal) {
        return readPrefLong(pkeyId, defaultVal, true);
    }

    private long readPrefLong(int pkeyId, long defaultVal, boolean perUser) {
        String key = getPrefKey(pkeyId, perUser);
        return loginPrefs.getLong(key, defaultVal);
    }

    // Pref Int
    private SharedPreferences.Editor writePrefInt(SharedPreferences.Editor prefEditor, int pkeyId, int val) {
        return writePrefInt(prefEditor, pkeyId, val, true);
    }

    private SharedPreferences.Editor writePrefInt(SharedPreferences.Editor prefEditor, int pkeyId, int val, boolean perUser) {
        String key = getPrefKey(pkeyId, perUser);
        return prefEditor.putInt(key, val);
    }

    private int readPrefInt(int pkeyId, int defaultVal) {
        return readPrefInt(pkeyId, defaultVal, true);
    }

    private int readPrefInt(int pkeyId, int defaultVal, boolean perUser) {
        String key = getPrefKey(pkeyId, perUser);
        return loginPrefs.getInt(key, defaultVal);
    }

    // Pref Key
    private String getPrefKey(int pkeyId, boolean perUser) {
        String key = getString(pkeyId);
        if (perUser) {
            key = user + key;
        }
        return key;
    }


    // ===========================================================
    // Logging Event
    // ===========================================================


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean needToLock = false;
        switch (requestCode) {
            case CommandsPrefsHelper.REQ_ENTER_PATTERN:
            case CommandsPrefsHelper.REQ_VERIFY_CAPTCHA: {
                int retryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 0);
                SharedPreferences.Editor prefEditor = loginPrefs.edit();
                long now = System.currentTimeMillis();
                try {
                    switch (resultCode) {
                        case RESULT_OK: {
                            setSignboardVisibility(false);
                            // Mark Success
                            writePrefLong(prefEditor, R.string.pkey_login_success_date, now);
                            incrementKey(prefEditor, R.string.pkey_login_success_count, 1);
                            // Reset retry
                            writePrefInt(prefEditor, R.string.pkey_login_retry_count, 0);
                            // Reset Failed
                            writePrefLong(prefEditor, R.string.pkey_login_failed_date, Long.MIN_VALUE);
                            writePrefInt(prefEditor, R.string.pkey_login_failed_count, 0);
                            // The user passed
                            startMainActivity();
                        }
                        break;
                        case RESULT_CANCELED: {
                            // The user cancelled the task
                            incrementKey(prefEditor, R.string.pkey_login_retry_count, retryCount);
                            finish();
                        }
                        break;
                        case LockPatternActivity.RESULT_FAILED: {
                            // The user failed to enter the pattern
                            // Mark Failed
                            writePrefLong(prefEditor, R.string.pkey_login_failed_date, now);
                            // Reset retry
                            writePrefInt(prefEditor, R.string.pkey_login_retry_count, 0);
                            // Increment Failed Count
                            incrementKey(prefEditor, R.string.pkey_login_failed_count, 1);
                            // Define Timer
                            needToLock = true;
                        }
                        break;
                        case LockPatternActivity.RESULT_FORGOT_PATTERN: {
                            // The user forgot the pattern and invoked your recovery Activity.
                            break;
                        }
                        default:
                            Log.d(TAG, "### Login Unknown Status for  ResultCode : " + resultCode);
                            finish();
                            return;
                    }
                } finally {
                    prefEditor.commit();
                }
                // lock
                if (needToLock) {
                    Log.d(TAG, "### onActivityResult : lockScreen");
                    lockScreen();
                }
            }
        }
    }

    // ===========================================================
    // Handle Intent
    // ===========================================================

    private void cancelCountDown() {
        CountDownTimer countDown = countDownTimer;
        if (countDown != null) {
            Log.d(TAG, "### Cancel CountDownTimer");
            countDown.cancel();
            countDownTimer = null;
            // Display Text
            mSignboardTextView.setText(null);
        }
    }

    private void openPromptPassword() {
        int previousRetryCount = readPrefInt(R.string.pkey_login_retry_count, 0);
        Log.d(TAG, "### Open Prompt Password : " + previousRetryCount + " retry");
        setSignboardVisibility(false);
        CommandsPrefsHelper.startActivityPatternCompare(this, previousRetryCount);
    }


    public void startMainActivity() {
        Intent mainActivity = null;
        if (destIntent == null) {
            mainActivity = new Intent(this, MainActivity.class);
        } else {
            mainActivity = destIntent;
        }
        Log.d(TAG, "### Redirect to Intent : " + mainActivity);
        // Increment Log
        int counterInc = markPrefLoginStatus();
        startActivity(mainActivity);
        if (!AdmobHelper.isAddBlocked(this)) {
            if (counterInc%20 == 0) {
                AdmobHelper.displayInterstitialAd(this);
            }
        }
        finish();
    }

    private int markPrefLoginStatus() {
        SharedPreferences.Editor prefEditor = loginPrefs.edit();
        long now = System.currentTimeMillis();
        writePrefLong(prefEditor, R.string.pkey_login_success_date, now, false);
        int incCounter = incrementKey(prefEditor, R.string.pkey_login_success_count, 1, false);
        prefEditor.commit();
        return incCounter;
    }

}
