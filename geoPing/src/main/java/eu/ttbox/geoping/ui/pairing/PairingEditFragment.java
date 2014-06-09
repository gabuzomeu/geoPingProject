package eu.ttbox.geoping.ui.pairing;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Paint;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.PhoneNumberUtils;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.domain.pairing.PairingHelper;
import eu.ttbox.geoping.ui.core.BindingHelper;
import eu.ttbox.geoping.ui.core.validator.Form;
import eu.ttbox.geoping.ui.core.validator.validate.ValidateTextView;
import eu.ttbox.geoping.ui.core.validator.validator.NotEmptyValidator;
import eu.ttbox.geoping.ui.pairing.validator.ExistPairingPhoneValidator;
import eu.ttbox.geoping.ui.person.PhotoEditorView;
import eu.ttbox.geoping.utils.contact.ContactHelper;
import eu.ttbox.geoping.utils.contact.ContactPickVo;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;

public class PairingEditFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "PairingEditFragment";

    // Constant
    private static final int PAIRING_EDIT_LOADER = R.id.config_id_pairing_edit_loader;

    public static final int PICK_CONTACT = 0;
    public static final int PICK_RINGTONE = 1;

    // Service
    private SharedPreferences sharedPreferences;

    // Config
    private static final boolean DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION = false;
    private static final boolean DEFAULT_PREFS_GEOFENCE_NOTIFICATION = false;

    private boolean showNotifDefault = DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION;
    private boolean geofenceNotifDefault = DEFAULT_PREFS_GEOFENCE_NOTIFICATION;

    // Paint
    Paint mPaint = new Paint();

    // Bindings
    private EditText nameEditText;
    private EditText phoneEditText;
    private TextView authorizeTypeTextView;

    private RadioGroup authorizeTypeRadioGroup;

    private RadioButton authorizeTypeAskRadioButton;
    private RadioButton authorizeTypeNeverRadioButton;
    private RadioButton authorizeTypeAlwaysRadioButton;

 //   private View selectNotificationSoundView;
    private TextView selectNotificationSoundSummary;

    // Compbound
    CompoundButton[] notifViews;
    private CompoundButton showNotificationCheckBox;
    private CompoundButton geofenceNotificationCheckBox;

    //Validator
    private Form formValidator;
    private ExistPairingPhoneValidator existValidator;

    // Image
    private PhotoEditorView photoImageView;

    private ImageButton selectContactClickButton;

    // Listener
    private OnPairingSelectListener onPairingSelectListener;

    // Cache
    private PhotoThumbmailCache photoCache;

    // Instance
    // private String entityId;
    private Uri entityUri;
    private String contactId;

    // ===========================================================
    // Interface
    // ===========================================================

    public interface OnPairingSelectListener {

        void onPersonSelect(Uri id, String phone);

    }

    // ===========================================================
    // Constructors
    // ===========================================================


    public PairingEditFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "****************** onCreateView");
        View v = inflater.inflate(R.layout.pairing_edit, container, false);
        // Menu on Fragment
        setHasOptionsMenu(true);

        // Cache
        photoCache = ((GeoPingApplication) getActivity().getApplicationContext()).getPhotoThumbmailCache();

        // Prefs
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Config
        String keyPrefsShowNotif = getString(R.string.pkey_shownotif_newparing_default);
        showNotifDefault = sharedPreferences.getBoolean(keyPrefsShowNotif, DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION);

        // binding
        photoImageView = (PhotoEditorView) v.findViewById(R.id.pairing_photo_imageView);
        nameEditText = (EditText) v.findViewById(R.id.pairing_name);
        phoneEditText = (EditText) v.findViewById(R.id.pairing_phone);
        showNotificationCheckBox = (CheckBox) v.findViewById(R.id.paring_show_notification);
        geofenceNotificationCheckBox = (CheckBox) v.findViewById(R.id.paring_geofence_notification);
        authorizeTypeTextView = (TextView) v.findViewById(R.id.pairing_authorize_type);

        authorizeTypeRadioGroup = (RadioGroup) v.findViewById(R.id.pairing_authorize_type_radioGroup);
        authorizeTypeAskRadioButton = (RadioButton) v.findViewById(R.id.pairing_authorize_type_radio_ask);
        authorizeTypeNeverRadioButton = (RadioButton) v.findViewById(R.id.pairing_authorize_type_radio_never);
        authorizeTypeAlwaysRadioButton = (RadioButton) v.findViewById(R.id.pairing_authorize_type_radio_always);

        selectContactClickButton = (ImageButton) v.findViewById(R.id.select_contact_button);

   //     selectNotificationSoundView = v.findViewById(R.id.select_notification_sound);
        selectNotificationSoundSummary = (TextView) v.findViewById(R.id.select_notification_sound_summary);

        // Radio Auth Listener
        OnClickListener radioAuthListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                onRadioAuthorizeTypeButtonClicked(v);

            }
        };
        authorizeTypeAskRadioButton.setOnClickListener(radioAuthListener);
        authorizeTypeNeverRadioButton.setOnClickListener(radioAuthListener);
        authorizeTypeAlwaysRadioButton.setOnClickListener(radioAuthListener);


        // Compbound
          notifViews = new CompoundButton[]{showNotificationCheckBox, geofenceNotificationCheckBox};

        CompoundButton.OnCheckedChangeListener notifOnClickListener = new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int buttonId = buttonView.getId();
                switch (buttonId) {
                    case R.id.paring_show_notification: {
                        onShowNotificationClick(buttonView);
                    }
                    break;
                    case R.id.paring_geofence_notification: {
                        onGeofenceNotificationClick(buttonView);
                    }
                    default: {
                        Log.w(TAG, "Not Manage CompoundButton Id : " + buttonId);
                        throw  new RuntimeException( "Not Manage CompoundButton Id : " + buttonId)  ;
                    }
                }
            }

        };
        for (CompoundButton compoundButton : notifViews) {
            compoundButton.setOnCheckedChangeListener(notifOnClickListener);
        }
        // Show Notification
        showNotificationCheckBox.setChecked(showNotifDefault);

        // Geofence
        geofenceNotificationCheckBox.setChecked(geofenceNotifDefault);

        // Select contact
        selectContactClickButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onSelectContactClick(v);

            }
        });
//        selectNotificationSoundView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onSelectRingtonePickerClick();
//            }
//        });

        // Form
        formValidator = createValidator(getActivity());

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        // Load Data
        loadEntity(getArguments());
    }


    @Override
    public void onDestroy() {
        if (notifViews != null && notifViews.length > 0) {
            // Unregister the listenr on Switch
            for (CompoundButton view : notifViews) {
                view.setOnCheckedChangeListener(null);
            }
        }
        // Prefs
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }


    // ===========================================================
    // Validator
    // ===========================================================

    public Form createValidator(Context context) {
        Form formValidator = new Form();
        // Name
        ValidateTextView nameTextField = new ValidateTextView(nameEditText)//
                .addValidator(new NotEmptyValidator());
        formValidator.addValidates(nameTextField);

        // Phone
        String entityId = entityUri == null ? null : entityUri.getLastPathSegment();
        existValidator = new ExistPairingPhoneValidator(getActivity(), entityId);
        ValidateTextView phoneTextField = new ValidateTextView(phoneEditText)//
                .addValidator(new NotEmptyValidator()) //
                .addValidator(existValidator);
        formValidator.addValidates(phoneTextField);


        return formValidator;
    }

    // ===========================================================
    // Menu
    // ===========================================================

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_pairing_edit, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                onSaveClick();
                return true;
            case R.id.menu_delete:
                onDeleteClick();
                return true;
            case R.id.menu_select_contact:
                onSelectContactClick(null);
                return true;
            case R.id.menu_cancel:
                onCancelClick();
                return true;
        }
        return false;
    }

    // ===========================================================
    // Life Cycle
    // ===========================================================



    // ===========================================================
    // Preferences
    // ===========================================================

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String keyPrefsShowNotif = getString(R.string.pkey_shownotif_newparing_default);
        if (key.equals(keyPrefsShowNotif)) {
            showNotifDefault = sharedPreferences.getBoolean(keyPrefsShowNotif, DEFAULT_PREFS_SHOW_GEOPING_NOTIFICATION);
        }
    }

    // ===========================================================
    // Accessor
    // ===========================================================

    public void setOnPersonSelectListener(OnPairingSelectListener onPersonSelectListener) {
        this.onPairingSelectListener = onPersonSelectListener;
    }

    private void loadEntity(Bundle agrs) {
        if (agrs != null && agrs.containsKey(Intents.EXTRA_PERSON_ID)) {
            Uri entityId = Uri.parse(agrs.getString(Intents.EXTRA_PERSON_ID));
            loadEntity(entityId);
        } else {
            // prepare for insert
            prepareInsert();
        }
    }

    private void loadEntity(Uri entityUri) { // String entityId
        Log.d(TAG, "loadEntity : " + entityUri);
        // this.entityUri =
        // Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI,
        // entityId);
        this.entityUri = entityUri;
        String entityId = entityUri == null ? null : entityUri.getLastPathSegment();
        existValidator.setEntityId(entityId);
        Bundle bundle = new Bundle();
        bundle.putString(Intents.EXTRA_DATA_URI, entityUri.toString());
        getActivity().getSupportLoaderManager().initLoader(PAIRING_EDIT_LOADER, bundle, pairingLoaderCallback);
    }

    private void prepareInsert() {
        this.entityUri = null;
        existValidator.setEntityId(null);
        showNotificationCheckBox.setChecked(showNotifDefault);
        // Open Selection contact Diallog
        onSelectContactClick(null);
        // Defautl value
        authorizeTypeAlwaysRadioButton.setChecked(true);
    }

    public void onDeleteClick() {
        int deleteCount = getActivity().getContentResolver().delete(entityUri, null, null);
        Log.d(TAG, "Delete %s entity successuf");
        if (deleteCount > 0) {
            getActivity().setResult(Activity.RESULT_OK);
        }
        getActivity().finish();
    }

    public void onSaveClick() {
        String name = nameEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        // TODO Select authorizeType
        PairingAuthorizeTypeEnum authType = null;
        if (authorizeTypeAlwaysRadioButton.isChecked()) {
            authType = PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS;
        } else if (authorizeTypeNeverRadioButton.isChecked()) {
            authType = PairingAuthorizeTypeEnum.AUTHORIZE_NEVER;
        } else {
            authType = PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST;
        }

        // Do Save
        Uri uri = doSavePairing(name, phone, authType, contactId);
        if (uri != null) {
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
        }
    }

    public void onCancelClick() {
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }

    /**
     * {link http://www.higherpass.com/Android/Tutorials/Working-With-Android-
     * Contacts/}
     *
     * @param v
     */
    public void onSelectContactClick(View v) {
        ContactHelper.pickContactPhone(this, PICK_CONTACT);
    }

    public void onPairingClick(View v) {
        String entityId = entityUri.getLastPathSegment();
        Intent intent = Intents.pairingRequest(getActivity(), phoneEditText.getText().toString(), entityId);
        getActivity().startService(intent);
    }

    public void onShowNotificationClick(View v) {
        if (entityUri != null) {
            boolean isCheck = showNotificationCheckBox.isChecked();
            ContentValues values = new ContentValues();
            values.put(PairingColumns.COL_SHOW_NOTIF, isCheck);
            int count = getActivity().getContentResolver().update(entityUri, values, null, null);
        }
    }

    public void onGeofenceNotificationClick(View v) {
        if (entityUri != null) {
            boolean isCheck = geofenceNotificationCheckBox.isChecked();
            ContentValues values = new ContentValues();
            values.put(PairingColumns.COL_GEOFENCE_NOTIF, isCheck);
            int count = getActivity().getContentResolver().update(entityUri, values, null, null);
        }
    }


    // ===========================================================
    // Listener
    // ===========================================================

    // ===========================================================
    // Contact Picker
    // ===========================================================

    public void saveContactData(Uri contactData) {
        ContactPickVo contactPick = ContactHelper.loadContactPick(getActivity(), contactData);
        ContentResolver cr = getActivity().getContentResolver();
        // Check If exist in db
        String checkExistId = checkExistEntityId(cr, contactPick.phone);
        // Save The select person
        if (checkExistId == null) {
            Uri uri = doSavePairing(contactPick.name, contactPick.phone, null, contactId);
        } else {
            Log.i(TAG, "Found existing Entity [" + checkExistId + "] for Phone : " + contactPick.phone);
            Uri checkExistUri = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI, checkExistId);
            loadEntity(checkExistUri);
        }
        // showSelectedNumber(type, number);
    }


    private String checkExistEntityId(ContentResolver cr, String phone) {
        Uri checkExistUri = PairingProvider.Constants.getUriPhoneFilter(phone);
        String[] checkExistProjections = new String[]{PairingColumns.COL_ID};
        Cursor checkExistCursor = cr.query(checkExistUri, checkExistProjections, null, null, null);
        String checkExistId = null;
        try {
            if (checkExistCursor.moveToNext()) {
                int checkExistColumnIndex = checkExistCursor.getColumnIndex(checkExistProjections[0]);
                checkExistId = checkExistCursor.getString(checkExistColumnIndex);
            }
        } finally {
            checkExistCursor.close();
        }
        return checkExistId;
    }

    public void onRadioAuthorizeTypeButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        PairingAuthorizeTypeEnum authType = null;
        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.pairing_authorize_type_radio_ask:
                if (checked)
                    authType = PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST;
                showNotificationCheckBox.setVisibility(View.GONE);
                break;
            case R.id.pairing_authorize_type_radio_always:
                if (checked)
                    authType = PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS;
                showNotificationCheckBox.setVisibility(View.VISIBLE);
                break;
            case R.id.pairing_authorize_type_radio_never:
                if (checked)
                    authType = PairingAuthorizeTypeEnum.AUTHORIZE_NEVER;
                showNotificationCheckBox.setVisibility(View.VISIBLE);
                break;
        }
        if (authType != null && entityUri != null) {
            ContentValues values = authType.writeTo(null);
            getActivity().getContentResolver().update(entityUri, values, null, null);
        }
    }

    // ===========================================================
    // Data Model Management
    // ===========================================================

    private String cleanPhone(String phone) {
        String cleanPhone = phone;
        if (cleanPhone != null) {
            cleanPhone = PhoneNumberUtils.normalizeNumber(phone);
        }
        if (cleanPhone != null) {
            cleanPhone = cleanPhone.trim();
            if (cleanPhone.length() < 1) {
                cleanPhone = null;
            }
        }
        return cleanPhone;
    }


    private Uri doSavePairing(String nameDirty, String phoneDirty, PairingAuthorizeTypeEnum authorizeType, String contactId) {
        String phone = cleanPhone(phoneDirty);
        String name = BindingHelper.trimToNull(nameDirty);
        setPairing(name, phone, contactId);
        // Validate
        if (!formValidator.validate()) {
            return null;
        }
        // Prepare db insert
        ContentValues values = new ContentValues();
        values.put(PairingColumns.COL_NAME, name);
        values.put(PairingColumns.COL_PHONE, phone);
        if (contactId != null) {
            values.put(PairingColumns.COL_CONTACT_ID, contactId);
        }
        if (authorizeType != null) {
            authorizeType.writeTo(values);
        }

        Log.d(TAG, "Save Pairing with Contact Id : " + contactId);
        // Content
        Uri uri;
        ContentResolver cr = getActivity().getContentResolver();
        if (entityUri == null) {
            // Show Notification checked
            boolean isCheck = showNotificationCheckBox.isChecked();
            values.put(PairingColumns.COL_SHOW_NOTIF, isCheck);
            // Geofence
            boolean isGeofenceNotif = geofenceNotificationCheckBox.isChecked();
            values.put(PairingColumns.COL_GEOFENCE_NOTIF, isGeofenceNotif);
            // Do Insert
            uri = cr.insert(PairingProvider.Constants.CONTENT_URI, values);
            this.entityUri = uri;
            String entityId = entityUri == null ? null : entityUri.getLastPathSegment();
            existValidator.setEntityId(entityId);
            getActivity().setResult(Activity.RESULT_OK);
        } else {
            uri = entityUri;
            int count = cr.update(uri, values, null, null);
            if (count != 1) {
                Log.e(TAG, String.format("Error, %s entities was updates for Expected One", count));
            }
        }
        // Notifify listener
        if (onPairingSelectListener != null) {
            onPairingSelectListener.onPersonSelect(entityUri, phone);
        }
        return uri;
    }

    private void setPairing(String name, String phone, String contactId) {
        nameEditText.setText(name);
        phoneEditText.setText(phone);
        this.contactId = contactId;
        photoCache.loadPhoto(getActivity(), photoImageView, contactId, phone);
    }

    // ===========================================================
    // RingTone picker
    // ===========================================================

    protected void onSelectRingtonePickerClick() {
        // Launch the ringtone picker
        Intent intent = onPrepareRingtonePickerIntent();
        startActivityForResult(intent, PICK_RINGTONE);
    }

    /**
     * Prepares the intent to launch the ringtone picker. This can be modified
     * to adjust the parameters of the ringtone picker.
     */
    protected Intent onPrepareRingtonePickerIntent() {
        Intent ringtonePickerIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        int mRingtoneType = RingtoneManager.TYPE_NOTIFICATION;
        boolean mShowDefault = true;
        boolean mShowSilent = false;

        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getPairingRingtone());
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, mShowDefault);
        if (mShowDefault) {
            Uri defaultUri = getDefaultRingtone(mRingtoneType);
            ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri);
            //ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,  RingtoneManager.getDefaultUri(mRingtoneType));

        }
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, mShowSilent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, mRingtoneType);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.prefs_notification_sound_dialog_title));
        return ringtonePickerIntent;
    }

    private Uri getDefaultRingtone(int mRingtoneType) {
        Uri defUri = RingtoneManager.getDefaultUri(mRingtoneType);
        String defUriString = defUri != null ? defUri.toString() : null;
        String prefPairingUriString = sharedPreferences.getString(getString(R.string.pkey_pairing_notif_sound), defUriString);
        Uri prefPairingUri = null;
        if (!TextUtils.isEmpty(prefPairingUriString)) {
            prefPairingUri = Uri.parse(prefPairingUriString);
        }
        Log.d(TAG, "### getDefaultRingtone : " + prefPairingUri);
        // prefPairingUri = null; //RingtoneManager.getDefaultUri(mRingtoneType);
        return prefPairingUri;
    }

    private Uri getPairingRingtone() {
        // TODO
        // return getDefaultRingtone( RingtoneManager.TYPE_NOTIFICATION);
        return null;
    }

    private void onSaveRingtone(Uri ringtoneUri) {
        Log.d(TAG, "### onSaveRingtone : " + ringtoneUri);
        // Display Ringtone name
        Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
        String name = ringtone.getTitle(getActivity());
        selectNotificationSoundSummary.setText(name);
        // TODO Save In DB


    }

    // ===========================================================
    // Activity Result handler
    // ===========================================================

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        Log.d(TAG, "### onActivityResult reqCode " + reqCode + " : " + data);
        switch (reqCode) {
            case (PICK_CONTACT): {
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    saveContactData(contactData);
                    // finish();
                }
            }
            break;
            case (PICK_RINGTONE): {
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "### onActivityResult extras : " + data.getExtras());
                    Intents.printExtras(TAG, data.getExtras());
                    if (data != null) {
                        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        onSaveRingtone(uri);
                    }
                }
            }
            break;

            default:
                break;
        }
    }

    // ===========================================================
    // LoaderManager
    // ===========================================================

    private final LoaderManager.LoaderCallbacks<Cursor> pairingLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader");
            String entityId = args.getString(Intents.EXTRA_DATA_URI);
            Uri entityUri = Uri.parse(entityId);
            // Loader
            CursorLoader cursorLoader = new CursorLoader(getActivity(), entityUri, null, null, null, null);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d(TAG, "onLoadFinished with cursor result count : " + cursor.getCount());
            // Display List
            if (cursor.moveToFirst()) {
                // Data
                PairingHelper helper = new PairingHelper().initWrapper(cursor);
                // Data
                contactId = helper.getContactId(cursor);
                String pairingPhone = helper.getPairingPhone(cursor);
                // Binding
                phoneEditText.setText(pairingPhone);
                helper.setTextPairingName(nameEditText, cursor)//
                        .setCheckBoxPairingGeofenceNotif(geofenceNotificationCheckBox, cursor)
                        .setCheckBoxPairingShowNotif(showNotificationCheckBox, cursor);
                // Pairing
                PairingAuthorizeTypeEnum authType = helper.getPairingAuthorizeTypeEnum(cursor);
                switch (authType) {
                    case AUTHORIZE_REQUEST:
                        authorizeTypeAskRadioButton.setChecked(true);
                        break;
                    case AUTHORIZE_NEVER:
                        authorizeTypeNeverRadioButton.setChecked(true);
                        break;
                    case AUTHORIZE_ALWAYS:
                        authorizeTypeAlwaysRadioButton.setChecked(true);
                        break;

                    default:
                        break;
                }
                // Notif
                if (PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST.equals(authType)) {
                    showNotificationCheckBox.setVisibility(View.GONE);
                }
                // Notify listener
                if (onPairingSelectListener != null) {
                    Uri pairingUri = entityUri;
                    onPairingSelectListener.onPersonSelect(pairingUri, pairingPhone);
                }
                // Photo
                photoCache.loadPhoto(getActivity(), photoImageView, contactId, pairingPhone);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            setPairing(null, null, null);
        }

    };


    // ===========================================================
    // Others
    // ===========================================================

}
