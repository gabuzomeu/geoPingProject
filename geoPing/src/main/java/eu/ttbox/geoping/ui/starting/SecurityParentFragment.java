package eu.ttbox.geoping.ui.starting;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import eu.ttbox.geoping.R;

public class SecurityParentFragment extends Fragment {


    private TextView textView;

    public SecurityParentFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View v = inflater.inflate(
                R.layout.starting_wizard_parent, container, false);
        Bundle args = getArguments();
        textView = (TextView) v.findViewById(android.R.id.text1);

        return v;
    }
}
