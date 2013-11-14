package eu.ttbox.geoping.ui.starting;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.ui.prefs.lock.core.CommandsPrefsHelper;

public class SecurityChildrenFragment extends Fragment {

    private static final String TAG = "SecurityChildrenFragment";

    private static final int ADD_PAIRING_REQUEST = 0;
    private static final int ADD_LOCKMODEL_REQUEST = 1;

    TextView textView;

    int[] buttonIds = new int[]{
            R.id.wizard_new_pairing_button, //
            R.id.wizard_lockpattern_button, //
            R.id.wizard_furtifmode_button, //

    };

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onCLickButton(v);
        }
    };


    // ===========================================================
    // Constructors
    // ===========================================================


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View v = inflater.inflate(
                R.layout.starting_wizard_children, container, false);
        Bundle args = getArguments();
        textView = (TextView) v.findViewById(android.R.id.text1);

        // Button
        for (int buttonId : buttonIds) {
            Button button = (Button) v.findViewById(buttonId);
            button.setOnClickListener(clickListener);
        }
        return v;
    }


    // ===========================================================
    // Actions
    // ===========================================================

    private void onCLickButton(View v) {
        int buttonId = v.getId();
        switch (buttonId) {
            case R.id.wizard_new_pairing_button: {
                Intent intent = Intents.editPairing(getActivity(), null);
                startActivityForResult(intent, ADD_PAIRING_REQUEST);
            }
            break;
            case R.id.wizard_lockpattern_button: {
                Intent intent = CommandsPrefsHelper.getIntentLockPatternCreate(getActivity());
                startActivityForResult(intent, ADD_LOCKMODEL_REQUEST);
            }
            break;
            case R.id.wizard_furtifmode_button: {

            }
            break;

        }
    }


    // ===========================================================
    // Action Result
    // ===========================================================


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_PAIRING_REQUEST : {

            }
            break;
            case ADD_LOCKMODEL_REQUEST :  {

            }
            break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
