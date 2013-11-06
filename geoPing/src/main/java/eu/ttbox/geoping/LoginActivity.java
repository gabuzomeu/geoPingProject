package eu.ttbox.geoping;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import eu.ttbox.geoping.core.AppConstants;
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
    private ScheduledExecutorService scheduler;
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
            int previousRetryCount = loginPrefs.getInt(PREF_RETRY_COUNT, 0);
            CommandsPrefsHelper.startActivityPatternCompare(this, previousRetryCount);
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
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    // ===========================================================
    // Scheduler
    // ===========================================================

    private static final String HANDLER_MESSAGE = "MSG";

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String displayText = bundle.getString(HANDLER_MESSAGE);
            loginText.setText(displayText);
        }
    };

    private void schedulerTimer() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1);
        }
        final Runnable beeper = new Runnable() {
            private int counterInS = 30;

            public void run() {
                // Dec Counter
                counterInS = counterInS - 1;
                // Define Text
                String displayText = "Try again in " + counterInS + " seconds";
                Log.d(TAG, displayText);
                // Send Message
                Message myMessage = handler.obtainMessage();
                myMessage.getData().putString(HANDLER_MESSAGE, displayText);
                handler.sendMessage(myMessage);
            }
        };
        // Schedule Tasks
        ScheduledFuture<?> beeperHandle =  scheduler.scheduleAtFixedRate(beeper, 0, 1, TimeUnit.SECONDS);
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
            //   return incVal;
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
                            schedulerTimer();
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
