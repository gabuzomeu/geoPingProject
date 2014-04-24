package eu.ttbox.geoping.ui.prefs.sms;

import android.content.ComponentName;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;


public class SmsMessageReSendCheckBoxPreference extends CheckBoxPreference {

    public SmsMessageReSendCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SmsMessageReSendCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmsMessageReSendCheckBoxPreference(Context context) {
        super(context);
    }


    public ComponentName getComponentNameBind() {
        return ExtraFeatureHelper.getComponentNameReSentSmsMessageReceiver(getContext());
    }


    @Override
    protected boolean persistBoolean(boolean value) {
        boolean result = super.persistBoolean(value);
        if (!value) {
            ExtraFeatureHelper.enabledComponentEnabledSetting(getContext(), getComponentNameBind(), value);
        }
        return result;
    }
}
