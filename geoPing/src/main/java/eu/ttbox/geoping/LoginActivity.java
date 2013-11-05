package eu.ttbox.geoping;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import eu.ttbox.geoping.ui.lock.prefs.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import group.pals.android.lib.ui.lockpattern.prefs.SecurityPrefs;

public class LoginActivity extends ActionBarActivity { //

    private static final String TAG = "LoginActivity";

    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CommandsPrefsHelper.isPassword(this)) {
            // Open Login
            CommandsPrefsHelper.startActivityPatternCompare(this);
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

            /*
             * NOTE that there are 3 possible result codes!!!
             */
                switch (resultCode) {
                    case RESULT_OK:
                        // The user passed
                        msgId = android.R.string.ok;
                        startMainActivity();
                        break;
                    case RESULT_CANCELED:
                        // The user cancelled the task
                        msgId = android.R.string.cancel;
                        finish();
                        break;
                    case LockPatternActivity.RESULT_FAILED:
                        // The user failed to enter the pattern
                      //  msgId = R.string.failed;
                        finish();
                        break;
                    default:
                        finish();
                        return;
                }

            /*
             * In any case, there's always a key EXTRA_RETRY_COUNT, which holds
             * the number of tries that the user did.
             */
                String msg = String.format("%s (%,d tries)", getString(msgId),
                        data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 0));

                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                break;
            }// REQ_ENTER_PATTERN && REQ_VERIFY_CAPTCHA
        }
    }// onActivityResult()

    // ===========================================================
    // Handle Intent
    // ===========================================================

     public void startMainActivity() {
        Intent mainActivity = new Intent(this, MainActivity.class);
         startActivity(mainActivity);
         finish();
    }
}
