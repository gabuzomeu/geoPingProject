package eu.ttbox.geoping;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.ui.lock.prefs.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;

public class LoginActivity extends ActionBarActivity { //

    private static final String TAG = "LoginActivity";

    private SharedPreferences loginPrefs;

    private static final String PREF_RETRY_COUNT = "retry_count";
    private static final String PREF_LOGIN_SUCCESS_DATE = "login_success_date";

    private static final String PREF_LOGIN_FAILED_DATE = "login_failed_date";
    private static final String PREF_LOGIN_FAILED_COUNT = "login_failed_count";

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CommandsPrefsHelper.isPassword(this)) {
            loginPrefs = getSharedPreferences(AppConstants.PREFS_FILE_LOGIN, MODE_PRIVATE);
            // Open Login
            int previousRetryCount = loginPrefs.getInt(PREF_RETRY_COUNT, 0);
            CommandsPrefsHelper.startActivityPatternCompare(this, previousRetryCount);
        } else {
            startMainActivity();
        }
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
                            prefEditor.putLong(PREF_LOGIN_SUCCESS_DATE, now);
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
                            Log.d(TAG, "### Login Cancel after retryCount : " + retryCount );
                            int previousRetryCount = loginPrefs.getInt(PREF_RETRY_COUNT, 0);
                            Log.d(TAG, "### Login Cancel Previous retryCount : " + previousRetryCount );
                            Log.d(TAG, "### Login Cancel Store Previous retryCount : " + (retryCount+previousRetryCount) );
                            prefEditor.putInt(PREF_RETRY_COUNT, retryCount+previousRetryCount);
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
                            int failedCount = loginPrefs.getInt(PREF_LOGIN_FAILED_COUNT, 0);
                            prefEditor.putInt(PREF_LOGIN_FAILED_COUNT, failedCount+1);
                            // finish();
                        }
                        break;
                        case LockPatternActivity.RESULT_FORGOT_PATTERN: {
                            // The user forgot the pattern and invoked your recovery Activity.
                            break;
                        }
                        default:
                            Log.d(TAG, "### Login Unknown Status for  ResultCode : " + resultCode );
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
