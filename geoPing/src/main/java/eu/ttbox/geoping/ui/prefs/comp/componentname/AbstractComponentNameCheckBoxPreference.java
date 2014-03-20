package eu.ttbox.geoping.ui.prefs.comp.componentname;


import android.content.ComponentName;
import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;
import android.util.Log;

import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;

public abstract class AbstractComponentNameCheckBoxPreference extends CheckBoxPreference {

    private static final String TAG = "AbstractComponentNameCheckBoxPreference";


    public AbstractComponentNameCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public AbstractComponentNameCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractComponentNameCheckBoxPreference(Context context) {
        super(context);
    }


    public abstract ComponentName getComponentNameBind();

    protected boolean isChangeValueValid(boolean newValueBoolean) {
        return true;
    }

    @Override
    protected boolean callChangeListener(Object newValue) {
        boolean isToChange = true;
        if (isToChange) {
            if (newValue instanceof Boolean) {
                boolean newValueBoolean = (Boolean) newValue;
                isToChange = isChangeValueValid(newValueBoolean);
            }
        }
        // Propage change is valid
        if (isToChange) {
            isToChange = super.callChangeListener(newValue);
        } else {
            Log.d(TAG, "### Cancel callChangeListener for newValue : " + newValue);
        }
        return isToChange;
    }


    @Override
    protected boolean getPersistedBoolean(boolean defaultReturnValue) {
        // Read real States
        boolean readRealState = ExtraFeatureHelper.isComponentEnabledSetting(getContext(), getComponentNameBind());
        return readRealState;
        //  return super.getPersistedBoolean(defaultReturnValue);
    }

    @Override
    protected boolean persistBoolean(boolean value) {
        boolean result = super.persistBoolean(value);
        result = ExtraFeatureHelper.enabledComponentEnabledSetting(getContext(), getComponentNameBind(), value);
        return result;
    }


}
