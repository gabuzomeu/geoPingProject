package eu.ttbox.geoping.ui.prefs.lock;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import eu.ttbox.geoping.R;
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
        ViewHolder holder = getViewHolder(v);

//        char[] pattern = SecurityPrefs.getPattern(getContext());
//        if (pattern==null || pattern.length<1) {
//            holder.actionView.setVisibility(View.GONE);
//        } else {
//            holder.actionView.setVisibility(View.VISIBLE);
//        }
     }

    private ViewHolder getViewHolder(View v) {
        ViewHolder holder = (ViewHolder)v.getTag();
        if (holder ==null) {
            holder = new ViewHolder();
            holder.actionView = (ImageButton) v.findViewById( R.id.action_button);
            v.setTag(holder);
            // Add Listener
            if (holder.actionView!=null) {
                holder.actionView.setOnClickListener(mPasswordResetListener);
            }
            // FIXME : Why we need to do that just because you setWidgetLayoutResource
            v.setOnClickListener(mPasswordCreateClickListener);
        }
        return holder;
    }

    static class ViewHolder {
        ImageButton actionView;
    }

}
