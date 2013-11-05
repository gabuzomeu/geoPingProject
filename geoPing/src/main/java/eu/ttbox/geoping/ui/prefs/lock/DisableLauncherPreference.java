package eu.ttbox.geoping.ui.prefs.lock;


import android.content.Context;
import android.preference.CheckBoxPreference;
import android.util.AttributeSet;

public class DisableLauncherPreference extends CheckBoxPreference {


    public DisableLauncherPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DisableLauncherPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DisableLauncherPreference(Context context) {
        super(context); 
    }


}
