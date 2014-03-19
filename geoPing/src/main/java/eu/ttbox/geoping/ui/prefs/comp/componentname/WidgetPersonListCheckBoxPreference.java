package eu.ttbox.geoping.ui.prefs.comp.componentname;


import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;

import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;

public class WidgetPersonListCheckBoxPreference extends AbstractComponentNameCheckBoxPreference {

    public WidgetPersonListCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WidgetPersonListCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetPersonListCheckBoxPreference(Context context) {
        super(context);
    }


    @Override
    public ComponentName getComponentNameBind() {
        return ExtraFeatureHelper.getComponentNamePersonListWidget(getContext());
    }

    @Override
    protected boolean isChangeValueValid(boolean newValueBoolean) {
        boolean isChangeValid = VersionUtils.isHc11;
        return isChangeValid;
    }

    
}
