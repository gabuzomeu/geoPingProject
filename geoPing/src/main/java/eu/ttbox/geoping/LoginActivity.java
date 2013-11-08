package eu.ttbox.geoping;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.ui.lock.prefs.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;

public class LoginActivity extends ActionBarActivity { //

    private static final String TAG = "LoginActivity";

    private final long LOCK_BASE_TIME_IN_MS = 30 * 1000;

    // Service
    private SharedPreferences loginPrefs;
    private CountDownTimer countDownTimer;

    // Binding
    private TextView mSecurityMessageDisplay;

    // Config instance
    private String user = "Local_";

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CommandsPrefsHelper.isPassword(this)) {
            loginPrefs = getSharedPreferences(AppConstants.PREFS_FILE_LOGIN, MODE_PRIVATE);
            // Init Binding
            initBinding();
        } else {
            startMainActivity();
        }
    }

    private void initBinding() {
        setContentView(R.layout.login_activity);
        mSecurityMessageDisplay = (TextView) findViewById(R.id.keyguard_message_area);
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            Log.d(TAG, "### onPause : countDownTimer.cancel()");
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!lockScreen()) {
            Log.d(TAG, "### onResume : openPromptPassword");
            openPromptPassword();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            Log.d(TAG, "### onDestroy : countDownTimer.cancel()");
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    // ===========================================================
    // Action
    // ===========================================================

    private long getExpectedEnlapseLockTime(int failedCount) {
        // exponential Algo
        long failFactor = Math.round(Math.exp(Math.max(0, failedCount - 1)));
//        failFactor = 0;
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

    private static final String HANDLER_DISPLAY_TEXT = "TXT";
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String displayText = msg.getData().getString(HANDLER_DISPLAY_TEXT);
            mSecurityMessageDisplay.setText(displayText);
        }

    };

    private void setDisplayText(long millisUntilFinished) {
        final long secondsRemaining =  Math.round(millisUntilFinished / 1000);
        String msgDisplay = getString(R.string.kg_too_many_failed_attempts_countdown, secondsRemaining);
        if (secondsRemaining >= 60l) {
            long s = secondsRemaining % 60;
            long min = (secondsRemaining / 60) % 60;
            long hour = (secondsRemaining / (60 * 60)) % 24;
            Log.d(TAG, "### Time to finish : " + hour + "h " + min + "min " + s + "s");
        } else {
            Log.d(TAG, "### Time to finish : " + secondsRemaining + "s");
        }
        mSecurityMessageDisplay.setText(msgDisplay);
    }

    private synchronized void startCountDownInSeconds(long lockTimeInMs) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            Log.w(TAG, "### Cancel previous CountDown");
        }
        countDownTimer = new CountDownTimer(lockTimeInMs, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                setDisplayText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                String displayText = "";
                mSecurityMessageDisplay.setText(displayText);
                //    if (countDownTimer != null) {
                countDownTimer = null;
                Log.d(TAG, "### CountDown onFinish : openPromptPassword");
                openPromptPassword();
                //     }
            }
        };
        Log.i(TAG, "### Start CountDown for : " + lockTimeInMs + " ms");
        setDisplayText(lockTimeInMs);
        countDownTimer.start();
    }


    // ===========================================================
    // Prefs Accessors
    // ===========================================================


    private void incrementKey(SharedPreferences.Editor prefEditor, int pkeyId, int incCount) {
        if (incCount != 0) {
            int previousCount = readPrefInt(pkeyId, 0);
            int incVal = incCount + previousCount;
            writePrefInt(prefEditor, pkeyId, incVal);
            Log.d(TAG, "### Increment " + getString(pkeyId) + " : " + previousCount + " ===> " + incVal);
        }
    }

    private void writePrefLong(SharedPreferences.Editor prefEditor, int pkeyId, long val) {
        String key = getPrefKey(pkeyId);
        prefEditor.putLong(key, val);
    }

    private long readPrefLong(int pkeyId, long defaultVal) {
        String key = getPrefKey(pkeyId);
        return loginPrefs.getLong(key, defaultVal);
    }

    private void writePrefInt(SharedPreferences.Editor prefEditor, int pkeyId, int val) {
        String key = getPrefKey(pkeyId);
        prefEditor.putInt(key, val);
    }

    private int readPrefInt(int pkeyId, int defaultVal) {
        String key = getPrefKey(pkeyId);
        return loginPrefs.getInt(key, defaultVal);
    }

    private String getPrefKey(int pkeyId) {
        String key = user + getString(pkeyId);
        return key;
    }


    // ===========================================================
    // Handle Intent
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

    private void openPromptPassword() {
        int previousRetryCount = readPrefInt(R.string.pkey_login_retry_count, 0);
        Log.d(TAG, "### Open Prompt Password : " + previousRetryCount + " retry");
        CommandsPrefsHelper.startActivityPatternCompare(this, previousRetryCount);
    }

    public void startMainActivity() {
        finish();
        Intent mainActivity = new Intent(this, MainActivity.class);
        startActivity(mainActivity);
    }


}
