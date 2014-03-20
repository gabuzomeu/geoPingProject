package eu.ttbox.geoping.ui.prefs.lock;


import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;

import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;
import eu.ttbox.geoping.ui.prefs.comp.componentname.AbstractComponentNameCheckBoxPreference;

public class WidgetPairingListCheckBoxPreference extends AbstractComponentNameCheckBoxPreference {

    public WidgetPairingListCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WidgetPairingListCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetPairingListCheckBoxPreference(Context context) {
        super(context);
    }


    @Override
    public ComponentName getComponentNameBind() {
        return ExtraFeatureHelper.getComponentNamePairingListWidget(getContext());
    }

    @Override
    protected boolean isChangeValueValid(boolean newValueBoolean) {
        boolean isChangeValid = VersionUtils.isHc11;
        return isChangeValid;
    }

}
