package eu.ttbox.geoping.domain.pairing;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;

public class PairingHelper {

    private static final String TAG = "PairingHelper";

    boolean isNotInit = true;
    public int idIdx = -1;
    public int nameIdx = -1;
    public int personUuidIdx = -1;
    public int emailIdx = -1;
    // Phone
    public int phoneIdx = -1;
    public int contactIdIdx = -1;
    public int phoneNormalizedIdx = -1;
    public int authorizeTypeIdx = -1;
    public int showNotificationIdx = -1;
    public int pairingTimeIdx = -1;
    // App Version
    public int appVersionIdx = -1;
    public int appVersionTimeIdx = -1;
    // Spy Notif
    public int notifShutdown = -1;
    public int notifBatteryLow = -1;
    public int notifSimChange = -1;
    public int notifPhoneCall = -1;
    // Encryption
    public int encryptionPubKeyIdx = -1;
    public int encryptionPrivKeyIdx = -1;
    public int encryptionRemotePubKeyIdx = -1;
    public int encryptionRemoteTimeIdx = -1;
    public int encryptionRemoteWayIdx = -1;

    public PairingHelper() {
        super();
    }

    public PairingHelper(Cursor cursor) {
        this();
        initWrapper(cursor);
    }


    public PairingHelper initWrapper(Cursor cursor) {
        idIdx = cursor.getColumnIndex(PairingColumns.COL_ID);
        nameIdx = cursor.getColumnIndex(PairingColumns.COL_NAME);
        personUuidIdx = cursor.getColumnIndex(PairingColumns.COL_PERSON_UUID);
        emailIdx = cursor.getColumnIndex(PairingColumns.COL_EMAIL);
        // Phone
        phoneIdx = cursor.getColumnIndex(PairingColumns.COL_PHONE);
        contactIdIdx = cursor.getColumnIndex(PairingColumns.COL_CONTACT_ID);
        phoneNormalizedIdx = cursor.getColumnIndex(PairingColumns.COL_PHONE_NORMALIZED);
        authorizeTypeIdx = cursor.getColumnIndex(PairingColumns.COL_AUTHORIZE_TYPE);
        showNotificationIdx = cursor.getColumnIndex(PairingColumns.COL_SHOW_NOTIF);
        pairingTimeIdx = cursor.getColumnIndex(PairingColumns.COL_PAIRING_TIME);
        // Notification
        notifShutdown = cursor.getColumnIndex(PairingColumns.COL_NOTIF_SHUTDOWN);
        notifBatteryLow = cursor.getColumnIndex(PairingColumns.COL_NOTIF_BATTERY_LOW);
        notifSimChange = cursor.getColumnIndex(PairingColumns.COL_NOTIF_SIM_CHANGE);
        notifPhoneCall = cursor.getColumnIndex(PairingColumns.COL_NOTIF_PHONE_CALL);
        // App Version
        appVersionIdx = cursor.getColumnIndex(PairingColumns.COL_APP_VERSION);
        appVersionTimeIdx = cursor.getColumnIndex(PairingColumns.COL_APP_VERSION);
        // Encryption
        encryptionPubKeyIdx = cursor.getColumnIndex(PairingColumns.COL_ENCRYPTION_PUBKEY);
        encryptionPrivKeyIdx = cursor.getColumnIndex(PairingColumns.COL_ENCRYPTION_PRIVKEY);
        encryptionRemotePubKeyIdx = cursor.getColumnIndex(PairingColumns.COL_ENCRYPTION_REMOTE_PUBKEY);
        encryptionRemoteTimeIdx = cursor.getColumnIndex(PairingColumns.COL_ENCRYPTION_REMOTE_TIME);
        encryptionRemoteWayIdx = cursor.getColumnIndex(PairingColumns.COL_ENCRYPTION_REMOTE_WAY);

        isNotInit = false;
        return this;
    }

    public Pairing getEntity(Cursor cursor) {
        if (isNotInit) {
            initWrapper(cursor);
        }
        Pairing user = new Pairing();
        user.setId(idIdx > -1 ? cursor.getLong(idIdx) : AppConstants.UNSET_ID);
        user.setDisplayName(nameIdx > -1 ? cursor.getString(nameIdx) : null);
        user.setPersonUuid(personUuidIdx > -1 ? cursor.getString(personUuidIdx) : null);
        user.setEmail(emailIdx > -1 ? cursor.getString(emailIdx) : null);
        // Phone
        user.setPhone(phoneIdx > -1 ? cursor.getString(phoneIdx) : null);
        user.setContactId(contactIdIdx > -1 ? cursor.getString(contactIdIdx) : null);
        user.setAuthorizeType(authorizeTypeIdx > -1 ? getPairingAuthorizeTypeEnum(cursor) : null);
        user.setShowNotification(showNotificationIdx > -1 ? cursor.getInt(showNotificationIdx) == 1 ? true : false : false);
        user.setPairingTime(pairingTimeIdx > -1 ? cursor.getLong(pairingTimeIdx) : AppConstants.UNSET_TIME);
        // App Version
        user.setAppVersion(appVersionIdx > -1 ? cursor.getInt(appVersionIdx) : -1);
        user.setAppVersionTime(appVersionTimeIdx > -1 ? cursor.getLong(appVersionTimeIdx) : AppConstants.UNSET_TIME);
        // Encryption
        user.encryptionPrivKey = encryptionPrivKeyIdx > -1 ? cursor.getString(encryptionPrivKeyIdx) : null;
        user.encryptionPubKey = encryptionPubKeyIdx > -1 ? cursor.getString(encryptionPubKeyIdx) : null;
        user.encryptionRemotePubKey = encryptionRemotePubKeyIdx > -1 ? cursor.getString(encryptionRemotePubKeyIdx) : null;
        user.setEncryptionRemoteTime(encryptionRemoteTimeIdx > -1 ? cursor.getLong(encryptionRemoteTimeIdx) : AppConstants.UNSET_TIME);
        user.encryptionRemoteWay = encryptionRemoteWayIdx > -1 ? cursor.getString(encryptionRemoteWayIdx) : null;

        return user;
    }

    private PairingHelper setTextWithIdx(TextView view, Cursor cursor, int idx) {
        view.setText(cursor.getString(idx));
        return this;
    }

    public PairingHelper setCompoundButtonWithIdx(CompoundButton view, Cursor cursor, int idx) {
        boolean value = cursor.getInt(idx) == 1;
        view.setChecked(value);
        return this;
    }


    public PairingHelper setTextPairingId(TextView view, Cursor cursor) {
        return setTextWithIdx(view, cursor, idIdx);
    }

    public String getPairingIdAsString(Cursor cursor) {
        return cursor.getString(idIdx);
    }

    public long getPairingId(Cursor cursor) {
        return cursor.getLong(idIdx);
    }

    public int getPairingColor(Cursor cursor) {
        return cursor.getInt(authorizeTypeIdx);
    }

    public PairingHelper setTextPairingName(TextView view, Cursor cursor) {
        return setTextWithIdx(view, cursor, nameIdx);
    }

    public PairingHelper setTextPairingPhone(TextView view, Cursor cursor) {
        return setTextWithIdx(view, cursor, phoneIdx);
    }

    public int getAppVersion(Cursor cursor) {
        return cursor.getInt(appVersionIdx);
    }
    public long getAppVersionTime(Cursor cursor) {
        return cursor.getLong(appVersionTimeIdx);
    }


    public PairingHelper setTextPairingAuthorizeType(TextView view, Cursor cursor) {
        PairingAuthorizeTypeEnum authType = getPairingAuthorizeTypeEnum(cursor);
        switch (authType) {
            case AUTHORIZE_REQUEST:
                view.setText(eu.ttbox.geoping.R.string.pairing_authorize_type_ask);
                break;
            case AUTHORIZE_NEVER:
                view.setText(eu.ttbox.geoping.R.string.pairing_authorize_type_never);
                break;
            case AUTHORIZE_ALWAYS:
                view.setText(eu.ttbox.geoping.R.string.pairing_authorize_type_always);
                break;
            default:
                view.setText(authType.name());
                Log.w(TAG, "No traduction for enum PairingAuthorizeTypeEnum : " + authType.name());
                break;
        }
        return this;
    }

    public PairingHelper setCheckBoxPairingShowNotif(CheckBox view, Cursor cursor) {
        boolean showNotif = cursor.getInt(showNotificationIdx) == 1;
        view.setChecked(showNotif);
        return this;
    }

    public PairingAuthorizeTypeEnum getPairingAuthorizeTypeEnum(Cursor cursor) {
        return PairingAuthorizeTypeEnum.getByCode(cursor.getInt(authorizeTypeIdx));
    }

    public String getPairingPhone(Cursor cursor) {
        return cursor.getString(phoneIdx);
    }

    public String getContactId(Cursor cursor) {
        return cursor.getString(contactIdIdx);
    }


    public String getDisplayName(Cursor cursor) {
        return cursor.getString(nameIdx);
    }

    public static ContentValues getContentValues(Pairing entity) {
        ContentValues initialValues = new ContentValues(PairingColumns.ALL_COLS.length);
        if (entity.id > -1) {
            initialValues.put(PairingColumns.COL_ID, Long.valueOf(entity.id));
        }
        initialValues.put(PairingColumns.COL_NAME, entity.displayName);
        initialValues.put(PairingColumns.COL_PERSON_UUID, entity.personUuid);
        initialValues.put(PairingColumns.COL_EMAIL, entity.email);
        // Phone
        initialValues.put(PairingColumns.COL_PHONE, entity.phone);
        initialValues.put(PairingColumns.COL_CONTACT_ID, entity.contactId);
        initialValues.put(PairingColumns.COL_SHOW_NOTIF, entity.showNotification);
        initialValues.put(PairingColumns.COL_PAIRING_TIME, entity.pairingTime);
        // secu
        PairingAuthorizeTypeEnum authorizeType = entity.authorizeType != null ? entity.authorizeType : PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST;
        initialValues.put(PairingColumns.COL_AUTHORIZE_TYPE, authorizeType.getCode());
        // App Version
        initialValues.put(PairingColumns.COL_APP_VERSION, entity.appVersion);
        initialValues.put(PairingColumns.COL_APP_VERSION_TIME, entity.appVersionTime);
        // Encryption
        initialValues.put(PairingColumns.COL_ENCRYPTION_PRIVKEY, entity.encryptionPrivKey);
        initialValues.put(PairingColumns.COL_ENCRYPTION_PUBKEY, entity.encryptionPubKey);
        initialValues.put(PairingColumns.COL_ENCRYPTION_REMOTE_PUBKEY, entity.encryptionRemotePubKey);
        initialValues.put(PairingColumns.COL_ENCRYPTION_REMOTE_TIME, entity.encryptionRemoteTime);
        initialValues.put(PairingColumns.COL_ENCRYPTION_REMOTE_WAY, entity.encryptionRemoteWay);

        return initialValues;
    }

}
