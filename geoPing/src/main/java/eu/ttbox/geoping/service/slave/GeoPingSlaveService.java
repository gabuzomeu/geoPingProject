package eu.ttbox.geoping.service.slave;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.domain.pairing.PairingHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.ContactVo;
import eu.ttbox.geoping.service.core.NotifPersonVo;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.service.receiver.LogReadHistoryService;
import eu.ttbox.geoping.service.receiver.player.AlarmPlayerService;
import eu.ttbox.geoping.service.slave.receiver.AuthorizePhoneTypeEnum;

// http://dhimitraq.wordpress.com/tag/android-intentservice/
// https://github.com/commonsguy/cwac-wakeful
public class GeoPingSlaveService extends IntentService implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "GeoPingSlaveService";

    private static final int SHOW_GEOPING_REQUEST_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_request_confirm;

    private static final int SHOW_PAIRING_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_pairing_request;

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
        this.displayGeopingRequestNotification = appPreferences.getBoolean(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION, false);
        this.authorizeNewPairing = appPreferences.getBoolean(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION)) {
            this.displayGeopingRequestNotification = appPreferences.getBoolean(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION, false);
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
            if (msgAction!=null) {
                String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
                Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
                Pairing pairing = null;
                if (phone!=null) {
                    pairing = gePairingByPhone(phone);

                }
                if (pairing==null) {
                  //  showNotificationNewPingRequestConfirm(pairing, params, GeopingNotifSlaveTypeEnum.PAIRING);
                  //  Intents.EXTRA_INTENT
                }
                switch (msgAction) {
                    case GEOPING_REQUEST: {
                        // GeoPing Request
                        Uri logUri = intent.getParcelableExtra(Intents.EXTRA_SMSLOG_URI);
                        manageGeopingRequest(phone, params, logUri);
                    }
                    break;
                    case ACTION_GEO_PAIRING: {
                        // GeoPing Pairing
                        managePairingRequest(phone, params);
                    }
                    break;
                    case COMMAND_OPEN_APP: {
                        // GeoPing Command : Open Application
                        manageCommandOpenApplication(phone, params);
                    }
                    break;
                    case COMMAND_RING: {
                        // GeoPing Command : Ring
                        manageCommandRing(phone, params);
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

    // ===========================================================
    // Commande
    // ===========================================================

    private void manageCommandOpenApplication(String phone, Bundle params) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(getApplicationContext(), MainActivity.class));
        stackBuilder.startActivities();
    }


    private void manageCommandRing(String phone, Bundle params) {
        Intent intent = new Intent(this ,AlarmPlayerService.class);
        intent.setAction(AlarmPlayerService.ACTION_PLAY);
        intent.putExtra(Intents.EXTRA_SMS_PHONE, phone);
        intent.putExtra(Intents.EXTRA_SMSLOG_SIDE_DBCODE, SmsLogSideEnum.SLAVE.getDbCode());
        startService(intent);
    }

    // ===========================================================
    // GeoPing Request
    // ===========================================================
    private void manageGeopingRequest(String phone, Bundle config, Uri logUri ) {
        // Request
        // registerGeoPingRequest(phone, params);
        Pairing pairing = getOrCreatePairingByPhone(phone);
        PairingAuthorizeTypeEnum authorizeType = pairing.authorizeType;
        boolean showNotification = pairing.showNotification;
        // Mark to Read
        if (showNotification && logUri!=null ) {
            LogReadHistoryService.markAsToReadLog(this, logUri, Boolean.TRUE);
        }
        // Manage Authorize
        switch (authorizeType) {
            case AUTHORIZE_NEVER:
                Log.i(TAG, "Ignore Geoping (Never Authorize) request from phone " + phone);
                // Show Blocking Notification
                if (showNotification) {
                    showGeopingRequestNotification(pairing, config, false);
                }
                break;
            case AUTHORIZE_ALWAYS:
                Log.i(TAG, "Accept Geoping (always Authorize) request from phone " + phone);
                GeoPingSlaveLocationService.runFindLocationAndSendInService(this , MessageActionEnum.LOC, new String[] { phone }, null, config);
                // Display Notification GeoPing
                if (showNotification) {
                    showGeopingRequestNotification(pairing, config, true);
                }
                break;
            case AUTHORIZE_REQUEST:
                GeopingNotifSlaveTypeEnum type = GeopingNotifSlaveTypeEnum.GEOPING_REQUEST_CONFIRM;
                if (AppConstants.UNSET_ID == pairing.id) {
                    type = GeopingNotifSlaveTypeEnum.GEOPING_REQUEST_CONFIRM_FIRST;
                }
                showNotificationNewPingRequestConfirm(pairing, config, type);
                break;
            default:
                break;
        }
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
        long personId = MessageEncoderHelper.readLong(params,  MessageParamEnum.PERSON_ID, -1l);
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
        long personId = MessageEncoderHelper.readLong(config, MessageParamEnum.PERSON_ID, -1l);
        GeopingNotifSlaveTypeEnum notifType = GeopingNotifSlaveTypeEnum.getByOrdinal(extras.getInt(Intents.EXTRA_NOTIFICATION_TYPE_ENUM_ORDINAL, -1));
        AuthorizePhoneTypeEnum type = AuthorizePhoneTypeEnum.getByOrdinal(extras.getInt(Intents.EXTRA_AUTHORIZE_PHONE_TYPE_ENUM_ORDINAL));
        Log.d(TAG, "******* AuthorizePhoneTypeEnum : " + type);
        String personNewName = extras.getString(Intents.EXTRA_PERSON_NAME);
        // Cancel Notification
        int notifId = extras.getInt(Intents.EXTRA_NOTIF_ID, -1);
        if (notifId != -1) {
            mNotificationManager.cancel(notifId);
        }
        // Read Pairing
        Pairing pairing = getOrCreatePairingByPhone(phone);
        if (TextUtils.isEmpty(pairing.displayName) && personNewName != null) {
            pairing.displayName = personNewName;
        }
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
        switch (notifType) {
        case GEOPING_REQUEST_CONFIRM:
            if (positifResponse) {
                GeoPingSlaveLocationService.runFindLocationAndSendInService(this, MessageActionEnum.LOC , new String[] {  phone } , null, config);
            }
            break;
        default:
            break;
        }
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
            params = MessageEncoderHelper.writeToBundle(null,  MessageParamEnum.PERSON_ID, personId);
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
            // TODO Read Prefs values
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
        ContactVo contactVo =  ContactHelper.searchContactForPhone(this, phoneNumber);
        if (contactVo!=null) {
            result.setContactId(String.valueOf(contactVo.id));
            result.setDisplayName(contactVo.displayName);
        }
        return result;
    }

    // ===========================================================
    // Notification
    // ===========================================================

    private void showGeopingRequestNotification(Pairing pairing, Bundle params, boolean authorizeIt) {
        NotificationSlaveHelper notif = new NotificationSlaveHelper(this);
        notif.showGeopingRequestNotification(pairing,   params,   authorizeIt);
    }

    private void showNotificationNewPingRequestConfirm(Pairing pairing, Bundle params, GeopingNotifSlaveTypeEnum onlyPairing) {
        NotificationSlavePairingHelper notif = new NotificationSlavePairingHelper(this);
        notif.showNotificationNewPingRequestConfirm(pairing, params, onlyPairing);
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
