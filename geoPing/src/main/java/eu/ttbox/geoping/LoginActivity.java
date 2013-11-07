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
import eu.ttbox.geoping.ui.lock.KeyguardMessageArea;
import eu.ttbox.geoping.ui.lock.SecurityMessageDisplay;
import eu.ttbox.geoping.ui.lock.prefs.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;

public class LoginActivity extends ActionBarActivity { //

    private static final String TAG = "LoginActivity";


    private static final String PREF_RETRY_COUNT = "retry_count";

    private static final String PREF_LOGIN_SUCCESS_DATE = "login_success_date";
    private static final String PREF_LOGIN_SUCCESS_COUNT = "login_success_count";

    private static final String PREF_LOGIN_FAILED_DATE = "login_failed_date";
    private static final String PREF_LOGIN_FAILED_COUNT = "login_failed_count";

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
        int previousRetryCount = loginPrefs.getInt(PREF_RETRY_COUNT, 0);
        CommandsPrefsHelper.startActivityPatternCompare(this, previousRetryCount);
    }


    private void lockScreen() {
        // Read Param
       long failedDate = loginPrefs.getLong(PREF_LOGIN_FAILED_DATE, 0);
       int failedCount = loginPrefs.getInt(PREF_RETRY_COUNT, 0);
        // Compute Lock Time
        long now = System.currentTimeMillis();
//        long lockPass =
        // Apply Lock
  //      startCountDownInMinutes(3);
        startCountDownInSeconds(30*3);
    }


    // ===========================================================
    // Scheduler
    // ===========================================================

    private static final String HANDLER_DISPLAY_TEXT = "TXT";
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            String displayText = msg.getData().getString(HANDLER_DISPLAY_TEXT);
            mSecurityMessageDisplay.setText( displayText );
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
                mSecurityMessageDisplay.setText(displayText );
                //
                countDownTimer = null;
                openPromptPassword();
            }
        };
        countDownTimer.start();
    }

    // ===========================================================
    // Handle Intent
    // ===========================================================


    private void incrementKey(SharedPreferences.Editor prefEditor, String pkey, int incCount) {
        if (incCount != 0) {
            int previousCount = loginPrefs.getInt(pkey, 0);
            int incVal = incCount + previousCount;
            prefEditor.putInt(pkey, incVal);
            Log.d(TAG, "### Increment " + pkey + " : " + previousCount + " ===> " + incVal);
         }
    }

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
                            prefEditor.putLong(PREF_LOGIN_SUCCESS_DATE, now);
                            incrementKey(prefEditor, PREF_LOGIN_SUCCESS_COUNT, 1);
                            // Reset retry
                            prefEditor.putInt(PREF_RETRY_COUNT, 0);
                            // Reset Failed
                            prefEditor.putLong(PREF_LOGIN_FAILED_DATE, Long.MIN_VALUE);
                            prefEditor.putInt(PREF_LOGIN_FAILED_COUNT, 0);
                        }
                        break;
                        case RESULT_CANCELED: {
                            // The user cancelled the task
                            msgId = android.R.string.cancel;
                            incrementKey(prefEditor, PREF_RETRY_COUNT, retryCount);
                            finish();
                        }
                        break;
                        case LockPatternActivity.RESULT_FAILED: {
                            // The user failed to enter the pattern
                            // Mark Failed
                            prefEditor.putLong(PREF_LOGIN_FAILED_DATE, now);
                            // Reset retry
                            prefEditor.putInt(PREF_RETRY_COUNT, 0);
                            // Increment Failed Count
                            incrementKey(prefEditor, PREF_LOGIN_FAILED_COUNT, 1);
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
