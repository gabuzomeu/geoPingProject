package eu.ttbox.geoping.ui.emergency;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.encoder.params.MessageParamField;
import eu.ttbox.geoping.ui.core.validator.Form;
import eu.ttbox.geoping.ui.core.validator.validate.ValidateTextView;
import eu.ttbox.geoping.ui.core.validator.validator.NotEmptyValidator;
import eu.ttbox.geoping.utils.contact.ContactHelper;
import eu.ttbox.geoping.utils.contact.ContactPickVo;
import eu.ttbox.geoping.utils.encoder.MessageEncoderHelper;


public class EmergencyModeFragment extends Fragment {


    private static final String TAG = "EmergencyModeFragment";

    // Constant
    private static final int PICK_CONTACT = 1;


    // Binding
    private EditText passwordEdit;
    private EditText phoneEditText;
    private Button geopingButton;
    private ImageButton selectContact;

    //Validator
    private Form formValidator;


    // ===========================================================
    // Constructor
    // ===========================================================

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.emergency_mode, container, false);
        // Binding
        passwordEdit = (EditText) v.findViewById(R.id.emergencyPasswordEdit);
        phoneEditText = (EditText) v.findViewById(R.id.person_phone);
        phoneEditText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return onSelectContactClick(v);
            }
        });
        selectContact = (ImageButton) v.findViewById(R.id.select_contact_button);
        selectContact.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectContactClick(v);
            }
        });
        // Sms Action
        geopingButton = (Button) v.findViewById(R.id.geoping_button_call);
        geopingButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickGeoPingButton(v);
            }
        });
        Log.d(TAG, "Binding end");
        // Form
        formValidator = createValidator(getActivity());

        return v;
    }

    // ===========================================================
    // Validator
    // ===========================================================


    public Form createValidator(Context context) {
        Form formValidator = new Form();
        // Name
        ValidateTextView nameTextField = new ValidateTextView(passwordEdit)//
                .addValidator(new NotEmptyValidator());
        formValidator.addValidates(nameTextField);

        // Phone
        ValidateTextView phoneTextField = new ValidateTextView(phoneEditText)//
                .addValidator(new NotEmptyValidator()) //  //
                ;
        formValidator.addValidates(phoneTextField);

        return formValidator;
    }


    // ===========================================================
    // Action
    // ===========================================================

    private void onClickGeoPingButton(View v) {
        // Validate
        if (!formValidator.validate()) {
            Toast.makeText(getActivity(), "C Mal Pas valide", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Context context = getActivity();
            // Read Param
            String password = passwordEdit.getText().toString();
            String phoneNumber = phoneEditText.getText().toString();
            Toast.makeText(getActivity(), "Ouuuuahhh tu as cliqué (password=" + password + ") vers tel : " + phoneNumber, Toast.LENGTH_SHORT).show();
            // Send  Geoping
            Long passwordValue = Long.valueOf(password);
            Bundle params = MessageEncoderHelper.writeToBundle(null, MessageParamField.EMERGENCY_PASSWORD, passwordValue);
            context.startService(Intents.sendSmsGeoPingRequest(context, phoneNumber, params));
        }

    }


    // ===========================================================
    // Contact Picker
    // ===========================================================

    public boolean onSelectContactClick(View v) {
        return ContactHelper.pickContactPhone(this, PICK_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        super.onActivityResult(requestCode, resultCode, result);

        switch (requestCode) {
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "PICK_CONTACT : " + result);
                    Uri contactData = result.getData();
                    saveContactData(contactData);
                    // finish();
                }
                break;
        }

    }


    /**
     * <a href="http://developer.android.com/guide/topics/providers/contacts-provider.html">contacts-provider</a>
     *
     * @param contactData
     */
    public void saveContactData(Uri contactData) {
        ContactPickVo contactPick = ContactHelper.loadContactPick(getActivity(), contactData);
        Log.d(TAG, "Select contact Uri : " + contactData + " ==> Contact Id : " + contactPick.contactId);
        // Check If exist in db
        phoneEditText.setText(contactPick.phone);

    }

    // ===========================================================
    // Other
    // ===========================================================

}
