package eu.ttbox.geoping.ui.lock.prefs;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.Log;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;
import group.pals.android.lib.ui.lockpattern.LockPatternActivity;
import group.pals.android.lib.ui.lockpattern.prefs.Prefs;
import group.pals.android.lib.ui.lockpattern.prefs.SecurityPrefs;

public class CommandsPrefsHelper {

    private static final String TAG = "CommandsPrefsHelper";

    public static final int REQ_CREATE_PATTERN = 0;
    public static final int REQ_ENTER_PATTERN = 1;
    public static final int REQ_VERIFY_CAPTCHA = 2;

    private final Activity mActivity;
    private final PreferenceHolder mPreferenceHolder;


    // ===========================================================
    // Listeners
    // ===========================================================


//    private final Preference.OnPreferenceClickListener mCmdDisableLaucherListener = new Preference.OnPreferenceClickListener() {
//
//        @Override
//        public boolean onPreferenceClick(Preference preference) {
//            Log.d(TAG,  "### Ask DisableLaucherListener");
//            ExtraFeatureHelper.switchEnabledSettingLaucherIcon(mActivity);
//            return true;
//        }// onPreferenceClick()
//    };// mCmdCreatePatternListener

    private final Preference.OnPreferenceChangeListener  mLauncherDisableChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Boolean value =(Boolean) newValue;
            ExtraFeatureHelper.enabledSettingLaucherIcon(mActivity, value);
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener mCmdCreatePatternListener = new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            return startActivityPatternCreate(mActivity);
        }// onPreferenceClick()
    };// mCmdCreatePatternListener


    private final Preference.OnPreferenceClickListener mCmdEnterPatternListener = new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            return startActivityPatternCompare(mActivity);
        }// onPreferenceClick()
    };// mCmdEnterPatternListener





    // ===========================================================
    // Constructors
    // ===========================================================


    /**
     * Creates new instance.
     *
     * @param activity the activity, which will be used to start
     *                 {@link LockPatternActivity}.
     * @param holder   the preference holder.
     */
    public CommandsPrefsHelper(Activity activity, PreferenceHolder holder) {
        mActivity = activity;
        mPreferenceHolder = holder;
    }// CommandsPrefsHelper()

    /**
     * Initializes handler for commands.
     */
    public void init() {
        Preference lockPref = mPreferenceHolder.findPreference(mActivity.getString(R.string.pkey_create_pattern));
        lockPref.setOnPreferenceClickListener(mCmdCreatePatternListener);

        Preference disableLauncherPref = mPreferenceHolder.findPreference(mActivity.getString(R.string.pkey_launcher_icon));
        disableLauncherPref.setOnPreferenceChangeListener(mLauncherDisableChangeListener);

//        mPreferenceHolder.findPreference(
//                mActivity.getString(R.string.pkey_enter_pattern))
//                .setOnPreferenceClickListener(mCmdEnterPatternListener);
//        mPreferenceHolder.findPreference(
//                mActivity.getString(R.string.pkey_verify_captcha))
//                .setOnPreferenceClickListener(mCmdVerifyCaptchaListener);
    }// init()


    // ===========================================================
    // Start Activity
    // ===========================================================

    public static boolean startActivityPatternCreate(Activity context) {
        Intent intentActivity = new Intent(
                LockPatternActivity.ACTION_CREATE_PATTERN, null, context,
                LockPatternActivity.class);
        intentActivity.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity(context));
        context
                .startActivityForResult(intentActivity, REQ_CREATE_PATTERN);

        return true;
    }

    public static boolean startActivityPatternCompare(Activity context) {
        Intent intentActivity = getIntentLockPatternCompare(context);
        context.startActivityForResult(intentActivity, REQ_ENTER_PATTERN);
        return true;
    }

    public static Intent getIntentLockPatternCompare(Context context) {
        Intent intentActivity = new Intent(LockPatternActivity.ACTION_COMPARE_PATTERN, null,
                context, LockPatternActivity.class);
        intentActivity.putExtra(LockPatternActivity.EXTRA_THEME, getThemeForLockPatternActivity(context));
        return intentActivity;
    }

    private final Preference.OnPreferenceClickListener mCmdVerifyCaptchaListener = new Preference.OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intentActivity = new Intent(
                    LockPatternActivity.ACTION_VERIFY_CAPTCHA, null, mActivity,
                    LockPatternActivity.class);
            intentActivity.putExtra(LockPatternActivity.EXTRA_THEME,
                    getThemeForLockPatternActivity(mActivity));
            mActivity.startActivityForResult(intentActivity, REQ_VERIFY_CAPTCHA);

            return true;
        }// onPreferenceClick()
    };// mCmdVerifyCaptchaListener


    // ===========================================================
    // Utilities
    // ===========================================================

    /**
     * Gets the theme that the user chose to apply to
     * {@link LockPatternActivity}.
     *
     * @param context the context.
     * @return the theme for {@link LockPatternActivity}.
     */
    @SuppressLint("InlinedApi")
    public static int getThemeForLockPatternActivity(Context context) {
        SharedPreferences p = context.getSharedPreferences(Prefs.genPreferenceFilename(), Context.MODE_MULTI_PROCESS);

        //return useDialogTheme ? R.style.Alp_Theme_Dialog_Dark  : R.style.Alp_Theme_Dark;
        return R.style.Alp_Theme_Dark;
    }// getThemeForLockPatternActivity()

    public static boolean isPassword(Context context) {
        char[] password = SecurityPrefs.getPattern(context);
        return (password != null && password.length > 0);
    }
}
