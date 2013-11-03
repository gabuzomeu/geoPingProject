package eu.ttbox.geoping.ui.prefs.lock;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.ui.lock.prefs.CommandsPrefsHelper;
import group.pals.android.lib.ui.lockpattern.prefs.SecurityPrefs;


public class LockAppPreference extends Preference {

    private static final String TAG = "LockAppPreference";

    private View.OnClickListener mPasswordResetListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            SecurityPrefs.setPattern(getContext(), null);
            Toast.makeText(getContext(), "Clear Password", Toast.LENGTH_SHORT).show();
        }
    };

    private View.OnClickListener mPasswordCreateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            getOnPreferenceClickListener().onPreferenceClick(LockAppPreference.this);
        }
    };

    public LockAppPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public LockAppPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LockAppPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.prefs_lockpattern);
    }

    @Override
    protected void onBindView(View v) {
        super.onBindView(v);
        final Button actionView = (Button) v.findViewById( R.id.action_button);
        if (actionView!=null) {
            actionView.setOnClickListener(mPasswordResetListener);
        }
        // FIXME : Why we need to do that just because you setWidgetLayoutResource
        v.setOnClickListener(mPasswordCreateClickListener);
     }
 
}
