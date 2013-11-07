package eu.ttbox.geoping.ui.lock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import eu.ttbox.geoping.R;


/***
 * Manages a number of views inside of the given layout. See below for a list of widgets.
 */
public class KeyguardMessageArea extends TextView {
    static final int CHARGING_ICON = 0; //R.drawable.ic_lock_idle_charging;
    static final int BATTERY_LOW_ICON = 0; //R.drawable.ic_lock_idle_low_battery;

    static final int SECURITY_MESSAGE_DURATION = 5000;
    protected static final int FADE_DURATION = 750;

    // are we showing battery information?
    boolean mShowingBatteryInfo = false;

    // is the bouncer up?
    boolean mShowingBouncer = false;

    // last known plugged in state
    boolean mPluggedIn = false;

    // last known battery level
    int mBatteryLevel = 100;


    // Timeout before we reset the message to show charging/owner info
    long mTimeout = SECURITY_MESSAGE_DURATION;

    // Shadowed text values
    protected boolean mBatteryCharged;
    protected boolean mBatteryIsLow;

    private Handler mHandler;

    CharSequence mMessage;
    boolean mShowingMessage;
    Runnable mClearMessageRunnable = new Runnable() {
        @Override
        public void run() {
            mMessage = null;
            mShowingMessage = false;
            if (mShowingBouncer) {
                hideMessage(FADE_DURATION, true);
            } else {
                update();
            }
        }
    };

    public static class Helper implements SecurityMessageDisplay {
        KeyguardMessageArea mMessageArea;
        Helper(View v) {
            mMessageArea = (KeyguardMessageArea) v.findViewById(R.id.keyguard_message_area);
            if (mMessageArea == null) {
                throw new RuntimeException("Can't find keyguard_message_area in " + v.getClass());
            }
        }

        public void setMessage(CharSequence msg, boolean important) {
            if (!TextUtils.isEmpty(msg) && important) {
                mMessageArea.mMessage = msg;
                mMessageArea.securityMessageChanged();
            }
        }

        public void setMessage(int resId, boolean important) {
            if (resId != 0 && important) {
                mMessageArea.mMessage = mMessageArea.getContext().getResources().getText(resId);
                mMessageArea.securityMessageChanged();
            }
        }

        public void setMessage(int resId, boolean important, Object... formatArgs) {
            if (resId != 0 && important) {
                mMessageArea.mMessage = mMessageArea.getContext().getString(resId, formatArgs);
                mMessageArea.securityMessageChanged();
            }
        }

        @Override
        public void showBouncer(int duration) {
            mMessageArea.hideMessage(duration, false);
            mMessageArea.mShowingBouncer = true;
        }

        @Override
        public void hideBouncer(int duration) {
            mMessageArea.showMessage(duration);
            mMessageArea.mShowingBouncer = false;
        }

        @Override
        public void setTimeout(int timeoutMs) {
            mMessageArea.mTimeout = timeoutMs;
        }
    }



   // private CharSequence mSeparator;

    public KeyguardMessageArea(Context context) {
        this(context, null);
    }

    public KeyguardMessageArea(Context context, AttributeSet attrs) {
        super(context, attrs);

        // This is required to ensure marquee works
        setSelected(true);

        // Registering this callback immediately updates the battery state, among other things.

        mHandler = new Handler(Looper.myLooper());

     //   mSeparator = getResources().getString(R.string.kg_text_message_separator);

        update();
    }

    public void securityMessageChanged() {
        setAlpha(1f);
        mShowingMessage = true;
        update();
        mHandler.removeCallbacks(mClearMessageRunnable);
        if (mTimeout > 0) {
            mHandler.postDelayed(mClearMessageRunnable, mTimeout);
        }
        announceForAccessibility(getText());
    }

    /**
     * Update the status lines based on these rules:
     * AlarmStatus: Alarm state always gets it's own line.
     * Status1 is shared between help, battery status and generic unlock instructions,
     * prioritized in that order.
     */
    void update() {
        CharSequence status =  getCurrentMessage();
        setText(status);
    }

//    private CharSequence concat(CharSequence... args) {
//        StringBuilder b = new StringBuilder();
//        if (!TextUtils.isEmpty(args[0])) {
//            b.append(args[0]);
//        }
//        for (int i = 1; i < args.length; i++) {
//            CharSequence text = args[i];
//            if (!TextUtils.isEmpty(text)) {
//                if (b.length() > 0) {
//                    b.append(mSeparator);
//                }
//                b.append(text);
//            }
//        }
//        return b.toString();
//    }

    CharSequence getCurrentMessage() {
        return mShowingMessage ? mMessage : null;
    }




    private void hideMessage(int duration, boolean thenUpdate) {
        if (duration > 0) {
            Animator anim = ObjectAnimator.ofFloat(this, "alpha", 0f);
            anim.setDuration(duration);
            if (thenUpdate) {
                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        update();
                    }
                });
            }
            anim.start();
        } else {
            setAlpha(0f);
            if (thenUpdate) {
                update();
            }
        }
    }

    private void showMessage(int duration) {
        if (duration > 0) {
            Animator anim = ObjectAnimator.ofFloat(this, "alpha", 1f);
            anim.setDuration(duration);
            anim.start();
        } else {
            setAlpha(1f);
        }
    }
}
