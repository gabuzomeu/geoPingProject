package eu.ttbox.geoping.ui.emergency;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
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
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.ui.core.validator.Form;
import eu.ttbox.geoping.ui.core.validator.validate.ValidateTextView;
import eu.ttbox.geoping.ui.core.validator.validator.NotEmptyValidator;


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

    private View.OnClickListener selectContactClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onClickGeoPingButton(v);
        }
    };
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
        geopingButton = (Button) v.findViewById(R.id.geoping_button_call);
        geopingButton.setOnClickListener(selectContactClickListener);
        selectContact = (ImageButton)v.findViewById(R.id.select_contact_button);
        selectContact.setOnClickListener(selectContactClickListener);
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        // Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        startActivityForResult(intent, PICK_CONTACT);
        return true;
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
        String selection = null;
        String[] selectionArgs = null;
        Log.d(TAG, "Select contact Uri : " + contactData);
        ContentResolver cr = getActivity().getContentResolver();
        Cursor c = getActivity().getContentResolver().query(contactData, new String[]{ //
                // BaseColumns._ID , //
                ContactsContract.Data.CONTACT_ID, //
                ContactsContract.CommonDataKinds.Identity.DISPLAY_NAME, //
                ContactsContract.CommonDataKinds.Phone.NUMBER, //
                ContactsContract.Contacts.LOOKUP_KEY, //
                ContactsContract.CommonDataKinds.Phone.TYPE}, selection, selectionArgs, null);
        // Uri contactLookupUri = ContactsContract.Data.getContactLookupUri(cr,
        // contactData);

        try {
            // Read value
            if (c != null && c.moveToFirst()) {
                String contactId = c.getString(0);
                String name = c.getString(1);
                String phone = c.getString(2);
                // String lookupKey = c.getString(3);
                // Uri lookupUri =  ContactsContract.Contacts.getLookupUri(Long.valueOf(contactId), lookupKey);
                // int type = c.getInt(4);
                Log.d(TAG, "Select contact Uri : " + contactData + " ==> Contact Id : " + contactId);
                // Check If exist in db
                phoneEditText.setText(phone);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    // ===========================================================
    // Other
    // ===========================================================

}
