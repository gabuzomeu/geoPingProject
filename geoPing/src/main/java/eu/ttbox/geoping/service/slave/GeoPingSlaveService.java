package eu.ttbox.geoping.service.slave;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import eu.ttbox.geoping.LoginActivity;
import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.domain.pairing.PairingHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.encoder.params.MessageParamField;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.ContactVo;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.service.receiver.LogReadHistoryService;
import eu.ttbox.geoping.service.receiver.player.AlarmPlayerService;
import eu.ttbox.geoping.service.slave.receiver.AuthorizePhoneTypeEnum;

// http://dhimitraq.wordpress.com/tag/android-intentservice/
// https://github.com/commonsguy/cwac-wakeful
public class GeoPingSlaveService extends IntentService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "GeoPingSlaveService";

    //  private static final int SHOW_GEOPING_REQUEST_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_request_confirm;

    //  private static final int SHOW_PAIRING_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_pairing_request;

    private final IBinder binder = new LocalBinder();
    // Constant

    // Services
    private NotificationManager mNotificationManager;
    private SharedPreferences appPreferences;

    // Config
    boolean displayGeopingRequestNotification = false;
    boolean authorizeNewPairing = true;

    // Set<String> secuAuthorizeNeverPhoneSet;
    // Set<String> secuAuthorizeAlwaysPhoneSet;

    // ===========================================================
    // Constructors
    // ===========================================================

    public GeoPingSlaveService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // service
        this.appPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        loadPrefConfig();
        // register listener
        appPreferences.registerOnSharedPreferenceChangeListener(this);

        Log.d(TAG, "#################################");
        Log.d(TAG, "### GeoPing Service Started.");
        Log.d(TAG, "#################################");
    }

    private void loadPrefConfig() {
        this.displayGeopingRequestNotification = appPreferences.getBoolean(getString(R.string.pkey_shownotif_newparing_default), false);
        this.authorizeNewPairing = appPreferences.getBoolean(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String keyPrefsShowNotif = getString(R.string.pkey_shownotif_newparing_default);
        if (key.equals(keyPrefsShowNotif)) {
            this.displayGeopingRequestNotification = appPreferences.getBoolean(keyPrefsShowNotif, false);
        }
        if (key.equals(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING)) {
            this.authorizeNewPairing = appPreferences.getBoolean(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING, true);
        }
    }

    @Override
    public void onDestroy() {
        appPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
        Log.d(TAG, "#################################");
        Log.d(TAG, "### GeoPing Service Destroyed.");
        Log.d(TAG, "#################################");
    }

    // ===========================================================
    // Intent Handler
    // ===========================================================

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String action = intent.getAction();
            Log.d(TAG, "##################################");
            Log.d(TAG, String.format("### onHandleIntent for action %s : %s", action, intent));
            Log.d(TAG, "##################################");
            MessageActionEnum msgAction = MessageActionEnum.getByIntentName(action);
            if (msgAction != null) {
                String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
                Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
                // Check Action Pairing
                if (MessageActionEnum.ACTION_GEO_PAIRING.equals(msgAction)) {
                    // GeoPing Pairing
                    managePairingRequest(phone, params);
                }
                // --- Manage New Pairing
                // ------------------------
                // Check Security
                Pairing pairing = null;
                if (phone != null) {
                    pairing = gePairingByPhone(phone);
                    Log.d(TAG, "### Search Pairing for phone " + phone + " ==> " + pairing);
                    // --- Manage Urgency Mode
                    pairing = manageEmergencyMode(phone, params, pairing);
                }


                // --- Manage Security
                // ------------------------
                boolean isIntentAuthorize = isHandleIntentAuthorize(intent, msgAction, pairing);
                if (!isIntentAuthorize) {
                    Log.i(TAG, "Reject Intent request " + msgAction + " for pairing : " + pairing);
                    return;
                }

                // Manage Action
                switch (msgAction) {
                    case GEOPING_REQUEST: {
                        // GeoPing Request
                        Uri logUri = intent.getParcelableExtra(Intents.EXTRA_SMSLOG_URI);
                        manageGeopingRequest(pairing, msgAction, params, logUri);
                    }
                    break;
                    case COMMAND_OPEN_APP: {
                        // GeoPing Command : Open Application
                        manageCommandOpenApplication(pairing, phone, params);
                    }
                    break;
                    case COMMAND_RING: {
                        // GeoPing Command : Ring
                        manageCommandRing(pairing, params);
                    }
                    break;
                    default:
                        Log.w(TAG, "Not Manage Intent for Enum Action : " + msgAction);
                        break;
                }
            } else {
                if (Intents.ACTION_SLAVE_GEOPING_PHONE_AUTHORIZE.equals(action)) {
                    // GeoPing Pairing User ressponse
                    manageNotificationAuthorizeIntent(intent.getExtras());
                } else {
                    Log.w(TAG, "Not Manage Intent Action (not an MessageActionEnum) : " + action);
                }
            }


        } finally {
            // synchronized (LOCK) {
            // sWakeLock.release();
            // }
        }
    }

    private boolean isHandleIntentAuthorize(Intent intent, MessageActionEnum msgAction, Pairing pairing) {
        String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
        Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
        // --- Display Notification
        // ------------------------

        if (pairing == null) {
            showNotificationForNewPairing(phone, intent, msgAction, GeopingNotifSlaveTypeEnum.PAIRING);
            return false;
        }

        // --- Manage Security
        // ------------------------
        boolean isAuthorize = false;
        boolean showNotification = pairing.showNotification;
        switch (pairing.authorizeType) {
            case AUTHORIZE_ALWAYS: {
                Log.i(TAG, "Accept Geoping request " + msgAction + " (always Authorize) from phone " + pairing);
                isAuthorize = true;
            }
            break;
            case AUTHORIZE_NEVER: {
                Log.i(TAG, "Reject Geoping request " + msgAction + " (Never Authorize) from phone " + pairing);
                // Show Blocking Notification
                if (showNotification) {
                    showGeopingRequestNotification(pairing, msgAction, params, false);
                }
                isAuthorize = false;
            }
            case AUTHORIZE_REQUEST: {
                Log.i(TAG, "Request Confirm Geoping request " + msgAction + " (Request Authorize) from phone " + pairing);
                Log.i(TAG, "Request Confirm Geoping request " + msgAction + " ------------------- ");
                Intents.printExtras(TAG, intent);
                AuthorizePhoneTypeEnum authorizeType = AuthorizePhoneTypeEnum.getByOrdinal(intent.getIntExtra(Intents.EXTRA_AUTHORIZE_PHONE_TYPE_ENUM_ORDINAL, -1));
                if (authorizeType == null) {
                    GeopingNotifSlaveTypeEnum type = GeopingNotifSlaveTypeEnum.GEOPING_REQUEST_CONFIRM;
                    Bundle config = intent.getExtras();
                    showNotificationForNewPairing(phone, intent, msgAction, GeopingNotifSlaveTypeEnum.GEOPING_REQUEST_CONFIRM);
                    //showNotificationNewPingRequestConfirm(pairing, config, type);
                    isAuthorize = false;
                } else {
                    switch (authorizeType) {
                        case ALWAYS:
                        case YES:
                            Log.i(TAG, "Request Confirm Geoping request " + msgAction + " (User Accept) from phone " + pairing);
                            isAuthorize = true;
                            break;
                        case NEVER:
                        case NO:
                            Log.i(TAG, "Request Confirm Geoping request " + msgAction + " (User Block) from phone " + pairing);
                            isAuthorize = false;
                            break;
                    }
                }

            }
            break;
            default:
                Log.w(TAG, "Not manage Pairing authorizeType : " + pairing.authorizeType);
                throw new RuntimeException("Not manage Pairing authorizeType : " + pairing.authorizeType);
        }
        return isAuthorize;
    }

    // ===========================================================
    // Commande
    // ===========================================================

    private void manageCommandOpenApplication(Pairing pairing, String phone, Bundle params) {
        Intent loginIntent = new Intent(getApplicationContext(), LoginActivity.class);
        loginIntent.putExtra(Intents.EXTRA_SMS_PHONE, phone);
        // Create Stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(loginIntent);
        stackBuilder.startActivities();
    }


    private void manageCommandRing(Pairing pairing, Bundle params) {
        String phone = pairing.phone;
        Intent intent = new Intent(this, AlarmPlayerService.class);
        intent.setAction(AlarmPlayerService.ACTION_PLAY);
        intent.putExtra(Intents.EXTRA_SMS_PHONE, phone);
        intent.putExtra(Intents.EXTRA_SMSLOG_SIDE_DBCODE, SmsLogSideEnum.SLAVE.getDbCode());
        startService(intent);
    }

    // ===========================================================
    // GeoPing Request
    // ===========================================================
    private void manageGeopingRequest(Pairing pairing,  MessageActionEnum msgAction, Bundle config, Uri logUri) {

        // Request
        // registerGeoPingRequest(phone, params);

        PairingAuthorizeTypeEnum authorizeType = pairing.authorizeType;
        boolean showNotification = pairing.showNotification;
        // Mark to Read
        if (showNotification && logUri != null) {
            LogReadHistoryService.markAsToReadLog(this, logUri, Boolean.TRUE);
        }
        // Manage Authorize
//        switch (authorizeType) {
//            case AUTHORIZE_NEVER:{
//                Log.i(TAG, "Ignore Geoping (Never Authorize) request from phone " + pairing);
//                // Show Blocking Notification
//                if (showNotification) {
//                    showGeopingRequestNotification(pairing, config, false);
//                }
//            }
//                break;
//            case AUTHORIZE_ALWAYS: {
        String phone = pairing.phone;
        Log.i(TAG, "Accept Geoping (always Authorize) request from phone " + pairing);
        GeoPingSlaveLocationService.runFindLocationAndSendInService(this, MessageActionEnum.LOC, new String[]{phone}, null, config);
        // Display Notification GeoPing
        if (showNotification) {
            showGeopingRequestNotification(pairing, msgAction, config, true);
        }
//            }
//                break;
//            case AUTHORIZE_REQUEST: {
//                GeopingNotifSlaveTypeEnum type = GeopingNotifSlaveTypeEnum.GEOPING_REQUEST_CONFIRM;
//                if (AppConstants.UNSET_ID == pairing.id) {
//                    type = GeopingNotifSlaveTypeEnum.GEOPING_REQUEST_CONFIRM_FIRST;
//                }
//                showNotificationNewPingRequestConfirm(pairing, config, type);
//            }
//                break;
//            default:
//                break;
//        }
    }

    // ===========================================================
    // Pairing
    // ===========================================================

    private void managePairingRequest(String phone, Bundle params) {
        PairingAuthorizeTypeEnum authorizeType = PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST;
        Log.i(TAG, "########## pairing request : " + authorizeType);
        Pairing pairing = getOrCreatePairingByPhone(phone);
        if (pairing != null && pairing.authorizeType != null) {
            authorizeType = pairing.authorizeType;
        }
        long personId = MessageEncoderHelper.readLong(params, MessageParamEnum.PERSON_ID, -1l);
        switch (authorizeType) {
            case AUTHORIZE_ALWAYS:// Already pairing, resent the response
                doPairingPhone(pairing, PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS, personId);
                break;
            case AUTHORIZE_NEVER: // No Auhtorize it !!
                doPairingPhone(pairing, PairingAuthorizeTypeEnum.AUTHORIZE_NEVER, personId);
                break;
            case AUTHORIZE_REQUEST:
                // Open the Notification For asking Yes or fuck
                showNotificationNewPingRequestConfirm(pairing, params, GeopingNotifSlaveTypeEnum.PAIRING);
                break;
            default:
                break;
        }
    }

    private void manageNotificationAuthorizeIntent(Bundle extras) {
        // Read Intent
        String phone = extras.getString(Intents.EXTRA_SMS_PHONE);
        Bundle config = extras.getBundle(Intents.EXTRA_SMS_PARAMS);
        Intent enventIntent = extras.getParcelable(Intents.EXTRA_INTENT_ACTIVITY);
        long personId = MessageEncoderHelper.readLong(config, MessageParamEnum.PERSON_ID, -1l);
        GeopingNotifSlaveTypeEnum notifType = GeopingNotifSlaveTypeEnum.getByOrdinal(extras.getInt(Intents.EXTRA_NOTIFICATION_TYPE_ENUM_ORDINAL, -1));
        AuthorizePhoneTypeEnum type = AuthorizePhoneTypeEnum.getByOrdinal(extras.getInt(Intents.EXTRA_AUTHORIZE_PHONE_TYPE_ENUM_ORDINAL));
        Log.d(TAG, "******* AuthorizePhoneTypeEnum : " + type);
//        String personNewName = extras.getString(Intents.EXTRA_PERSON_NAME);
        // Cancel Notification
        int notifId = extras.getInt(Intents.EXTRA_NOTIF_ID, -1);
        if (notifId != -1) {
            mNotificationManager.cancel(notifId);
        }
        // Read Pairing
        Pairing pairing = getOrCreatePairingByPhone(phone);
        // ### Manage Pairing Type
        // #############################
        Log.d(TAG, String.format("manageAuthorizeIntent for phone %s with User security choice %s", phone, type));
        boolean positifResponse = false;
        switch (type) {
            case NEVER:
                doPairingPhone(pairing, PairingAuthorizeTypeEnum.AUTHORIZE_NEVER, personId);
                break;
            case ALWAYS:
                doPairingPhone(pairing, PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS, personId);
                positifResponse = true;
                break;
            case YES:
                positifResponse = true;
                if (AppConstants.UNSET_ID == pairing.id) {
                    doPairingPhone(pairing, PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST, personId);
                }
                break;
            default:
                Log.w(TAG, "Not manage PhoneAuthorizeTypeEnum for " + type);
                positifResponse = false;
                break;
        }

        // ### Manage Notification Type
        // #############################
        if (enventIntent != null) {
            Intent cloneIntent = new Intent(enventIntent);
            if (type != null) {
                cloneIntent.putExtra(Intents.EXTRA_AUTHORIZE_PHONE_TYPE_ENUM_ORDINAL, type.ordinal());
            }
            onHandleIntent(cloneIntent);
        }
//        switch (notifType) {
//        case GEOPING_REQUEST_CONFIRM:
//            if (positifResponse) {
//                GeoPingSlaveLocationService.runFindLocationAndSendInService(this, MessageActionEnum.LOC , new String[] {  phone } , null, config);
//            }
//            break;
//        default:
//            break;
//        }
    }

    private void doPairingPhone(Pairing pairing, PairingAuthorizeTypeEnum authorizeType, long personId) {
        // ### Persist Pairing
        // #############################
        if (pairing.id > -1l) {
            if (pairing.authorizeType == null || !pairing.authorizeType.equals(authorizeType)) {
                // update
                ContentValues values = authorizeType.writeTo(null);
                values.put(PairingColumns.COL_PAIRING_TIME, System.currentTimeMillis());
                Uri uri = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI, String.valueOf(pairing.id));
                int affectedRow = getContentResolver().update(uri, values, null, null);
                Log.w(TAG, String.format("Change %s Pairing %s : to new  %s", affectedRow, uri, authorizeType));
            } else {
                Log.d(TAG, String.format("Ignore Change Pairing type %s to %s", pairing.authorizeType, authorizeType));
            }
        } else {
            // Create
            pairing.setPairingTime(System.currentTimeMillis());
            ContentValues values = PairingHelper.getContentValues(pairing);
            authorizeType.writeTo(values);
            Uri pairingUri = getContentResolver().insert(PairingProvider.Constants.CONTENT_URI, values);
            if (pairingUri != null) {
                String entityId = pairingUri.getLastPathSegment();
                pairing.setId(Long.valueOf(entityId));
            }
            Log.w(TAG, String.format("Insert new Pairing %s : to new  %s", pairingUri, pairing));
        }
        // ### Send Pairing response
        // #############################
        switch (authorizeType) {
            case AUTHORIZE_NEVER:
                // TODO Check Last Send
                break;
            case AUTHORIZE_ALWAYS:
                sendPairingResponse(pairing.phone, personId, authorizeType);
                break;
            default:
                break;
        }

    }

    private void sendPairingResponse(String phone, long personId, PairingAuthorizeTypeEnum authorizeType) {
        Bundle params = null;
        if (personId != -1l) {
            params = MessageEncoderHelper.writeToBundle(null, MessageParamEnum.PERSON_ID, personId);
        }
        SmsSenderHelper.sendSmsAndLogIt(this, SmsLogSideEnum.SLAVE, phone, MessageActionEnum.ACTION_GEO_PAIRING_RESPONSE, params);
    }

    // ===========================================================
    // GeoPing Security
    // ===========================================================

    private Pairing getOrCreatePairingByPhone(String phoneNumber) {
        // Search
        Pairing result = gePairingByPhone(phoneNumber);
        Log.d(TAG, String.format("Search Painring for Phone [%s] : Found %s", phoneNumber, result));
        // Create It
        if (result == null) {
            result = createPairingByPhone(phoneNumber);
        }
        return result;
    }

    private Pairing gePairingByPhone(String phoneNumber) {
        Pairing result = null;
        // Search
        // Log.d(TAG, String.format("Search Painring for Phone [%s]",  phoneNumber));
        Uri uri = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI_PHONE_FILTER, Uri.encode(phoneNumber));
        Cursor cur = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                PairingHelper helper = new PairingHelper().initWrapper(cur);
                result = helper.getEntity(cur);
            }
        } finally {
            cur.close();
        }
        return result;
    }

    private Pairing manageEmergencyMode(String phoneNumber, Bundle params, Pairing pairing) {
        Pairing result = pairing;
        // Check Flag Emergency
        if (MessageEncoderHelper.isToBundle(params, MessageParamField.EMERGENCY_PASSWORD)) {
            Log.d(TAG, "### Manage Emergency Mode for pairing : " + pairing);
            long emergyPassword = MessageEncoderHelper.readLong(params, MessageParamField.EMERGENCY_PASSWORD, Long.MAX_VALUE);
            // Control Password
            String validPasswordString = appPreferences.getString(getString(R.string.pkey_emergency_password), null);
            if (validPasswordString != null && (emergyPassword == Long.parseLong(validPasswordString))) {
                // Create Granted Access
                if (result == null) {
                    result = new Pairing();
                    result.setDisplayName(createEmergencyLabel(phoneNumber));
                    result.setPhone(phoneNumber);
                } else {
                    result.setDisplayName(createEmergencyLabel(result.displayName));
                }
                // Controle Granted
                result.setAuthorizeType(PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS);
                Log.w(TAG, "### Emergercy Mode : Create Pairing ");
            } else {
                // TODO Display Bad Try of Emergercy Mode
                Log.w(TAG, "### Emergercy Mode : Bad Password");
            }
            Log.d(TAG, "### Return Emergency Mode for pairing : " + result);
        }
        return result;
    }

    private String createEmergencyLabel(String name) {
        return "Emergency for " + name;
    }

    private Pairing createPairingByPhone(String phoneNumber) {
        Pairing result = new Pairing();
        result.setPhone(phoneNumber);
        result.setShowNotification(displayGeopingRequestNotification);
        if (authorizeNewPairing) {
            result.setAuthorizeType(PairingAuthorizeTypeEnum.AUTHORIZE_REQUEST);
        } else {
            result.setAuthorizeType(PairingAuthorizeTypeEnum.AUTHORIZE_NEVER);
        }
        // Search Contact Data
        ContactVo contactVo = ContactHelper.searchContactForPhone(this, phoneNumber);
        if (contactVo != null) {
            result.setContactId(String.valueOf(contactVo.id));
            result.setDisplayName(contactVo.displayName);
        }
        return result;
    }

    // ===========================================================
    // Notification
    // ===========================================================

    @Deprecated
    private void showNotificationNewPingRequestConfirm(Pairing pairing, Bundle params, GeopingNotifSlaveTypeEnum onlyPairing) {
        NotificationSlavePairingHelper notif = new NotificationSlavePairingHelper(this);
        notif.showNotificationNewPingRequestConfirm(pairing, params, onlyPairing);
    }


    private void showGeopingRequestNotification(Pairing pairing, MessageActionEnum msgAction, Bundle eventParam, boolean authorizeIt) {
        NotificationSlave2Helper notif = new NotificationSlave2Helper(this);
        notif.showGeopingRequestNotification(pairing, msgAction, eventParam, authorizeIt);
    }


    private void showNotificationForNewPairing(String phone, Intent eventIntent, MessageActionEnum msgAction, GeopingNotifSlaveTypeEnum onlyPairing) {
        Pairing pairing = createPairingByPhone(phone);
        showNotificationForNewPairing(pairing, eventIntent, msgAction, onlyPairing);
    }

    private void showNotificationForNewPairing(Pairing pairing, Intent eventIntent, MessageActionEnum msgAction, GeopingNotifSlaveTypeEnum onlyPairing) {
        NotificationSlavePairing2Helper notif = new NotificationSlavePairing2Helper(this);
        notif.showNotificationNewPingRequestConfirm(pairing, eventIntent, msgAction, onlyPairing);
    }
    // ===========================================================
    // Binder
    // ===========================================================

    public class LocalBinder extends Binder {
        public GeoPingSlaveService getService() {
            return GeoPingSlaveService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

}
