package eu.ttbox.geoping.ui.prefs.comp.componentname;


import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;

public class StarterIconCheckBoxPreference extends AbstractComponentNameCheckBoxPreference {

    private static final String TAG = "StarterIconCheckBoxPreference";


    public StarterIconCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public StarterIconCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StarterIconCheckBoxPreference(Context context) {
        super(context);
    }


    @Override
    public ComponentName getComponentNameBind() {
        return ExtraFeatureHelper.getComponentNameLaucherIcon(getContext());
    }

    @Override
    protected boolean isChangeValueValid(boolean newValueBoolean) {
        boolean isChangeValid = true;
        if (!newValueBoolean) {
            // TODO Check Valid condition for Hide Application
            Log.d(TAG, "### Check callChangeListener for newValueBoolean : " + newValueBoolean);
            isChangeValid = true;
        }
        return isChangeValid;
    }


}
