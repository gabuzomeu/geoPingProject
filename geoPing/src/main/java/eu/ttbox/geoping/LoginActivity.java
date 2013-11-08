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

    // Service
    private SharedPreferences loginPrefs;
    private CountDownTimer countDownTimer;
    // Binding
    private TextView mSecurityMessageDisplay;

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CommandsPrefsHelper.isPassword(this)) {
            loginPrefs = getSharedPreferences(AppConstants.PREFS_FILE_LOGIN, MODE_PRIVATE);
            // Open Login
            openPromptPassword();
            // Init Binding
            initBinding();
        } else {
            startMainActivity();
        }
    }

    private void initBinding() {
        setContentView(R.layout.login_activity);
        mSecurityMessageDisplay =(TextView)findViewById(R.id.keyguard_message_area);
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================

    @Override
    public void onPause() {
        super.onPause();
//        if (countDownTimer != null) {
//            countDownTimer.cancel();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    // ===========================================================
    // Action
    // ===========================================================
    private void openPromptPassword(){
        int previousRetryCount = readPrefInt(R.string.pkey_login_retry_count, 0);
        CommandsPrefsHelper.startActivityPatternCompare(this, previousRetryCount);
    }

    private final  long LOCK_BASE_TIME_IN_MS = 30* 1000;

    private void long isLockApp() {
        long lockTime = 0;
        int failedCount = readPrefInt(R.string.pkey_login_failed_count, 0);
        if (failedCount>0) {
            long now = System.currentTimeMillis();
            long failedDate = readPrefLong(R.string.pkey_login_failed_date, 0);
            // Compute Time
            long enlapseRealTime = now - failedDate;
            long enlapseExpectedLockTime = failedCount * LOCK_BASE_TIME_IN_MS; // TODO exponential Algo
            long enlapseExpectedTime = now - enlapseExpectedLockTime; // TODO exponential Algo
            long restTimeInMs = enlapseExpectedLockTime - enlapseRealTime;
        }
        return lockTime;
    }

    private void lockScreen() {
        // Read Param
       long failedDate = readPrefLong(R.string.pkey_login_failed_date, 0);
       int failedCount = readPrefInt(R.string.pkey_login_failed_count, 1);
        // Compute Lock Time
        long now = System.currentTimeMillis();
//        long lockPass =
        // Apply Lock
  //      startCountDownInMinutes(3);
        startCountDownInSeconds(30*failedCount);
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

    /**
     * ./platform_frameworks_base/policy/src/com/android/internal/policy/impl/keyguard/KeyguardPatternView.java
     * @param minutes
     */
    private void startCountDownInMinutes(int minutes) {
        int countMin = minutes;
//        if (minutes<)

        countDownTimer = new CountDownTimer(countMin * 60 * 1000, 60 *1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsRemaining = (int) (millisUntilFinished / 1000);
                String msgDisplay = getString( R.string.kg_too_many_failed_attempts_countdown, secondsRemaining);
                mSecurityMessageDisplay.setText(msgDisplay);

                int counterInMin = (int)( millisUntilFinished / (60 * 1000));

                String displayText = "Try again in " + counterInMin + " minutes";
                // Display Text
                Message msg  = handler.obtainMessage();
                msg.getData().putString(HANDLER_DISPLAY_TEXT, displayText);
                handler.dispatchMessage(msg);
            }

            @Override
            public void onFinish() {
                String displayText = "Try again in 1 minutes";
                startCountDownInSeconds(59);
            }
        };

        countDownTimer.start();
    }

    private void startCountDownInSeconds(int seconds) {
        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsRemaining = (int) (millisUntilFinished / 1000);
                String msgDisplay = getString( R.string.kg_too_many_failed_attempts_countdown, secondsRemaining);
                mSecurityMessageDisplay.setText(msgDisplay);
            }

            @Override
            public void onFinish() {
                String displayText = "";
                mSecurityMessageDisplay.setText(displayText);
                //
                countDownTimer = null;
                openPromptPassword();
            }
        };
        countDownTimer.start();
    }


    // ===========================================================
    // Prefs Accessors
    // ===========================================================


    private void incrementKey(SharedPreferences.Editor prefEditor, int pkeyId, int incCount) {
        if (incCount != 0) {
            int previousCount = readPrefInt(pkeyId, 0);
            int incVal = incCount + previousCount;
            writePrefInt( prefEditor, pkeyId, incVal);
            Log.d(TAG, "### Increment " + getString(pkeyId) + " : " + previousCount + " ===> " + incVal);
        }
    }

    private void writePrefLong(SharedPreferences.Editor prefEditor, int pkeyId, long val) {
        String key = getString(pkeyId);
        prefEditor.putLong(key, val);
    }

    private long readPrefLong(int pkeyId, long defaultVal) {
        String key = getString(pkeyId);
        return loginPrefs.getLong(key, defaultVal);
    }

    private void writePrefInt(SharedPreferences.Editor prefEditor, int pkeyId, int val) {
        String key = getString(pkeyId);
        prefEditor.putInt(key, val);
    }

    private int readPrefInt(int pkeyId, int defaultVal) {
        String key = getString(pkeyId);
        return loginPrefs.getInt(key, defaultVal);
    }


    // ===========================================================
    // Handle Intent
    // ===========================================================



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CommandsPrefsHelper.REQ_ENTER_PATTERN:
            case CommandsPrefsHelper.REQ_VERIFY_CAPTCHA: {
                int msgId = 0;
                int retryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 0);
                SharedPreferences.Editor prefEditor = loginPrefs.edit();
                long now = System.currentTimeMillis();
                try {
                    switch (resultCode) {
                        case RESULT_OK: {
                            // The user passed
                            msgId = android.R.string.ok;
                            startMainActivity();
                            // Mark Success
                            writePrefLong(prefEditor, R.string.pkey_login_success_date  , now);
                            incrementKey(prefEditor, R.string.pkey_login_success_count, 1);
                            // Reset retry
                            writePrefInt(prefEditor, R.string.pkey_login_retry_count, 0);
                            // Reset Failed
                            writePrefLong(prefEditor, R.string.pkey_login_failed_date, Long.MIN_VALUE);
                            writePrefInt(prefEditor, R.string.pkey_login_failed_count, 0);
                        }
                        break;
                        case RESULT_CANCELED: {
                            // The user cancelled the task
                            msgId = android.R.string.cancel;
                            incrementKey(prefEditor, R.string.pkey_login_retry_count, retryCount);
                            finish();
                        }
                        break;
                        case LockPatternActivity.RESULT_FAILED: {
                            // The user failed to enter the pattern
                            // Mark Failed
                            writePrefLong(prefEditor, R.string.pkey_login_failed_date, now);
                            // Reset retry
                            writePrefInt(prefEditor,  R.string.pkey_login_retry_count, 0);
                            // Increment Failed Count
                            incrementKey(prefEditor, R.string.pkey_login_failed_count, 1);
                            // Define Timer
                            lockScreen();
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

            }
        }
    }

    // ===========================================================
    // Handle Intent
    // ===========================================================

    public void startMainActivity() {
        Intent mainActivity = new Intent(this, MainActivity.class);
        startActivity(mainActivity);
        finish();
    }


}
