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

import com.google.android.gms.internal.v;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.ui.lock.prefs.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import group.pals.android.lib.ui.lockpattern.util.Sys;

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
    private TextView loginText;

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
        loginText = (TextView) findViewById(R.id.login_status_textView);
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
        startCountDownInMinutes(3);
//        startCountDownInSeconds(30);
    }


    // ===========================================================
    // Scheduler
    // ===========================================================

    private void startCountDownInMinutes(int minutes) {
        int countMin = minutes;
//        if (minutes<)

        countDownTimer = new CountDownTimer(countMin * 60 * 1000, 60 *1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int counterInMin = (int)( millisUntilFinished / (60 * 1000));
                String displayText = "Try again in " + counterInMin + " minutes";
                loginText.setText(displayText);
            }

            @Override
            public void onFinish() {
                String displayText = "Try again in 1 minutes";
                startCountDownInSeconds(59);
            }
        };

    }

    private void startCountDownInSeconds(int seconds) {
        countDownTimer = new CountDownTimer(seconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int counterInS = (int)( millisUntilFinished / 1000);
                String displayText = "Try again in " + counterInS + " seconds";
                loginText.setText(displayText);

            }

            @Override
            public void onFinish() {
                String displayText = "";
                loginText.setText(displayText);
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
