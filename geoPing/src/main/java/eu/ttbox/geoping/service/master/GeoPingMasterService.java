package eu.ttbox.geoping.service.master;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.lang.ref.WeakReference;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.Person;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.person.PersonDatabase.PersonColumns;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.utils.contact.ContactHelper;
import eu.ttbox.geoping.utils.contact.NotifPersonVo;
import eu.ttbox.geoping.utils.encoder.MessageEncoderHelper;

public class GeoPingMasterService extends IntentService {

    private static final String TAG = "GeoPingMasterService";
    private static final int UI_MSG_TOAST = 0;
    private final IBinder binder = new LocalBinder();
    // config
    boolean notifyGeoPingResponse = false;
    // Service
    private SharedPreferences appPreferences;

    // ===========================================================
    // UI Handler
    // ===========================================================
    private Tracker tracker;

    private Handler uiHandler = new MyInnerHandler(this);

    static class MyInnerHandler extends Handler {
        WeakReference<GeoPingMasterService> mFrag;

        MyInnerHandler(GeoPingMasterService aFragment) {
            mFrag = new WeakReference<GeoPingMasterService>(aFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            GeoPingMasterService theFrag = mFrag.get();
            switch (msg.what) {
                case UI_MSG_TOAST:
                    String msgText = (String) msg.obj;
                    Toast.makeText(theFrag.getApplicationContext(), msgText, Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
        }
    }
    // ===========================================================
    // Constructors
    // ===========================================================

    public GeoPingMasterService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // service
        this.appPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.notifyGeoPingResponse = appPreferences.getBoolean(getString(R.string.pkey_shownotif_newparing_default), false);
        // Google Analytics
        tracker = GeoPingApplication.getGeoPingApplication(this).getTracker();

        Log.d(TAG, "#################################");
        Log.d(TAG, "### GeoPingMasterService Service Started.");
        Log.d(TAG, "#################################");
    }

    // ===========================================================
    // Binder
    // ===========================================================

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, String.format("###  onHandleIntent for action %s : %s", action, intent));
        if (Intents.ACTION_SMS_GEOPING_REQUEST_SENDER.equals(action)) {
            String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
            sendSmsGeoPingRequest(phone, params);
            // Tracker
            // tracker.trackPageView("/action/SMS_GEOPING_REQUEST");

            tracker.send(new HitBuilders.EventBuilder() //
                    .setCategory("MasterService") // Category
                    .setAction("HandleIntent") // Action
                    .setLabel("SMS_GEOPING_REQUEST") // Label
                    .build()); // Value
        } else if (Intents.ACTION_SMS_PAIRING_RESQUEST.equals(action)) {
            String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            long userId = intent.getLongExtra(Intents.EXTRA_SMS_USER_ID, -1);
            sendSmsPairingRequest(phone, userId);
            // Tracker
            // tracker.trackPageView("/action/SMS_PAIRING_RESQUEST");
            tracker.send(new HitBuilders.EventBuilder() //
                    .setCategory("MasterService") // Category
                    .setAction("HandleIntent") // Action
                    .setLabel("SMS_PAIRING_RESQUEST") // Label
                    .build()); // Value
//        } else if (Intents.ACTION_SMS_PAIRING_RESPONSE.equals(action)) {
//            String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
//            Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
//            long userId =   MessageEncoderHelper.readLong(params, MessageParamEnum.PERSON_ID, -1);
//            consumeSmsPairingResponse(phone, userId);
//            // Tracker
//            // tracker.trackPageView("/action/SMS_PAIRING_RESPONSE");
//            tracker.sendEvent("MasterService", // Category
//                    "HandleIntent", // Action
//                    "SMS_PAIRING_RESPONSE", // Label
//                    0l); // Value
        } else {
            MessageActionEnum actionEnum = MessageActionEnum.getByIntentName(action);
            if (actionEnum != null) {
                Log.d(TAG, String.format("###  onHandleIntent for actionEnum %s : %s", actionEnum, intent));
                switch (actionEnum) {
                    // Send Sms
                    case COMMAND_RING:
                    case COMMAND_OPEN_APP: {
                        String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
                        Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
                        Log.d(TAG, "### Request Sending Sms for " + actionEnum + " and Phone : " + phone);
                        sendSmsCommand(actionEnum, phone, params);
                    }
                    break;
                    // Read Sms
                    case ACTION_GEO_PAIRING_RESPONSE: {
                        String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
                        Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
                        consumeSmsPairingResponse(phone, params);
                        // Tracker
                        tracker.send(new HitBuilders.EventBuilder() //
                                .setCategory("MasterService") // Category
                                .setAction("HandleIntent") // Action
                                .setLabel("SMS_PAIRING_RESPONSE") // Label
                                .build()); // Value
                    }
                    break;
                    // Read Sms
                    case SPY_SHUTDOWN:
                        // TODO Display Notification
                        break;
                    case SPY_SIM_CHANGE:
                        // TODO Add Spy Person in DB for register next Data
                        // TODO consumeSimChange(bundle);
                        if (true) {
                            Bundle bundle = intent.getExtras();
                            String newPhone = bundle.getString(Intents.EXTRA_SMS_PHONE);
                            Bundle params = bundle.getBundle(Intents.EXTRA_SMS_PARAMS);
                            String originalPhone = MessageEncoderHelper.readString(params, MessageParamEnum.PHONE_NUMBER);
                            Log.i(TAG, "Sim Change [" + originalPhone + "] to new Phone [" + newPhone + "]");
                        }
                    case SPY_BOOT:
                    case SPY_LOW_BATTERY:
                    case SPY_PHONE_CALL:
                    case LOC_DECLARATION:
                    case GEOFENCE_Unknown_transition:
                    case GEOFENCE_ENTER:
                    case GEOFENCE_EXIT:
                    case LOC:
                        consumeGeoPingResponse(actionEnum, intent.getExtras());
                        break;
                    default:
                        Log.w(TAG, "--- ------------------------------------ ---");
                        Log.w(TAG, "--- Not managed EventSpy response : " + action + " ---");
                        Log.w(TAG, "--- ------------------------------------ ---");
                        printExtras(intent.getExtras());
                        Log.w(TAG, "--- ------------------------------------ ---");
                        break;
                }
                tracker.send(new HitBuilders.EventBuilder() //
                        .setCategory("MasterService") // Category
                        .setAction("HandleIntent") // Action
                        .setLabel(actionEnum.name()) // Label
                        .build()); // Value
            }
        }

    }


    // ===========================================================
    // Intent Handler
    // ===========================================================

    private void printExtras(Bundle extras) {
        Intents.printExtras(TAG, extras);
    }



    // ===========================================================
    // Search Person
    // ===========================================================

    private void consumeSmsPairingResponse(String phone,  Bundle params) {
        long userId = MessageEncoderHelper.readLong(params, MessageParamEnum.PERSON_ID, -1);
        if (userId != -1l) {
            Person person = ContactHelper.searchPersonForPhone(this, phone);
            if (person != null && person.id == userId) {
                ContentValues values = new ContentValues(2);
                values.put(PersonColumns.COL_PHONE, phone);
                values.put(PersonColumns.COL_PAIRING_TIME, System.currentTimeMillis());
                // Update
                Uri uri = person.getPersonUri();
                getContentResolver().update(uri, values, null, null);
            }
        } else {
            Log.w(TAG, "Paring response Canceled for no userId");
        }

    }

    // ===========================================================
    // Send GeoPing Commande
    // ===========================================================
    private void sendSmsCommand(MessageActionEnum actionEnum, String phone, Bundle params) {
        boolean isSend = sendSms(phone, actionEnum, params);
    }


    // ===========================================================
    // Send GeoPing Request
    // ===========================================================

    private void sendSmsPairingRequest(String phone, long userId) {
        Person person =  ContactHelper.searchPersonForPhone(this, phone);
        if (person == null || TextUtils.isEmpty(person.encryptionPubKey)) {
            ContentValues values = new ContentValues();
            // TODO  Generated encryption Key
            Uri entityUri = Uri.withAppendedPath(PersonProvider.Constants.CONTENT_URI, String.valueOf(person.id));
            // getContentResolver().update(entityUri, values, null, null);
        }
        Bundle params = MessageEncoderHelper.writeToBundle(null, MessageParamEnum.PERSON_ID, userId);
        boolean isSend = sendSms(phone, MessageActionEnum.ACTION_GEO_PAIRING, params);
        if (isSend) {
            Message msg = uiHandler.obtainMessage(UI_MSG_TOAST, getResources().getString(R.string.toast_notif_sended_geoping_pairing, phone));
            uiHandler.sendMessage(msg);
        }
    }

    // ===========================================================
    // Sender Sms message
    // ===========================================================

    private void sendSmsGeoPingRequest(String phone, Bundle params) {
        Bundle geopingRequest = SmsSenderHelper.completeRequestTimeOutFromPrefs(appPreferences, params);
        boolean isSend = sendSms(phone, MessageActionEnum.GEOPING_REQUEST, geopingRequest);
        Log.d(TAG, String.format("Send SMS GeoPing %s : %s", phone, geopingRequest));
        // Display Notif
        if (isSend) {
            Message msg = uiHandler.obtainMessage(UI_MSG_TOAST, getResources().getString(R.string.toast_notif_sended_geoping_request, phone));
            uiHandler.sendMessage(msg);
        }
        // final String formatStr =
        // getResources().getString(R.string.toast_notif_sended_geoping_request,
        // phone);
        // Toast.makeText(getApplicationContext(),formatStr,
        // Toast.LENGTH_SHORT).show();
    }

    private boolean sendSms(String phone, MessageActionEnum action, Bundle params) {
        boolean isSend = false;

        if (phone == null || phone.length() < 1) {
            Log.d(TAG, "### Not Sending Sms for Not Phone define");
            return false;
        }

        try {
            Uri logUri = SmsSenderHelper.sendSmsAndLogIt(this, SmsLogSideEnum.MASTER, phone, action, params);
            isSend = (logUri != null);
            Log.d(TAG, "### Sending Sms : " + isSend + " for lorUri : " + logUri);

        } catch (IllegalArgumentException e) {
            Message msg = uiHandler.obtainMessage(UI_MSG_TOAST, getResources().getString(R.string.toast_notif_sended_geoping_smsError, phone + " : " + e.getMessage()));
            uiHandler.sendMessage(msg);
            Log.e(TAG, "Error sending Sms : " + e.getMessage(), e);
        } catch (NullPointerException e) {
            Message msg = uiHandler.obtainMessage(UI_MSG_TOAST, getResources().getString(R.string.toast_notif_sended_geoping_smsError, phone + " : " + e.getMessage()));
            uiHandler.sendMessage(msg);
            Log.e(TAG, "Error sending Sms : " + e.getMessage(), e);
        }

        return isSend;
    }

    // ===========================================================
    // Consume Alert Change Sim
    // ===========================================================

    private boolean consumeSimChange(Bundle bundle) {
        boolean isConsume = false;
        String phone = bundle.getString(Intents.EXTRA_SMS_PHONE);

        return isConsume;
    }

    private boolean consumeGeoPingResponse(MessageActionEnum actionEnum, Bundle bundle) {
        boolean isConsume = false;
        String phone = bundle.getString(Intents.EXTRA_SMS_PHONE);
        Bundle params = bundle.getBundle(Intents.EXTRA_SMS_PARAMS);
        GeoTrack geoTrack = GeoTrackHelper.getEntityFromBundle(params);
        geoTrack.setPhone(phone);
        if (!MessageActionEnum.LOC.equals(actionEnum)) {
            geoTrack.eventType = actionEnum.name();
        }
        if (geoTrack != null) {
            ContentValues values = GeoTrackHelper.getContentValues(geoTrack);
            Uri uri = null;
            if (geoTrack.isValid()) {
                uri = getContentResolver().insert(GeoTrackerProvider.Constants.CONTENT_URI, values);
            } else {
                Log.w(TAG, "Not valid Geotrack for action : " + actionEnum);
            }
            if (uri != null) {
                Log.d(TAG, String.format("Send Broadcast Notification for New GeoTrack %s ", uri));
                // BroadCast Response
                Intent intent = Intents.newGeoTrackInserted(uri, values);
                sendBroadcast(intent);
                // Display Notification
                NotifPersonVo personVo =showNotificationGeoPing(actionEnum, uri, values, geoTrack, params);
                if (personVo!=null) {
                    checkAppVersion(personVo.person, params);
                }
            }
            isConsume = true;
        }
        return isConsume;
    }

    // ===========================================================
    // Check App Version
    // ===========================================================

    private void checkAppVersion(Person person, Bundle bundle) {
        if (person==null) {
            return;
        }
        if (MessageEncoderHelper.isToBundle(bundle,MessageParamEnum.APP_VERSION)) {
            int remoteVersion = MessageEncoderHelper.readInt(bundle, MessageParamEnum.APP_VERSION, Integer.MIN_VALUE);
            if (person.appVersion != remoteVersion) {
                long now = System.currentTimeMillis();
                // Save app version
                if (person.appVersionTime < now) {
                    Uri personUri = person.getPersonUri();
                    if (personUri != null) {
                        // Update to DB
                        ContentResolver cr = getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(PersonColumns.COL_APP_VERSION, remoteVersion);
                        values.put(PersonColumns.COL_APP_VERSION_TIME, now);
                        int updateCount = cr.update(personUri, values, null, null);
                    }
                }
            }
        }
    }

    // ===========================================================
    // Consume Localisation
    // ===========================================================




    // ===========================================================
    // Notification
    // ===========================================================


    private NotifPersonVo showNotificationGeoPing(MessageActionEnum actionEnum, Uri geoTrackData, ContentValues values, GeoTrack geoTrack, Bundle params) {
        NotificationMasterHelper helper = new NotificationMasterHelper(this);
        NotifPersonVo personVo = helper.showNotificationGeoPing(actionEnum, geoTrackData, values, geoTrack, params);
        return personVo;
    }


    public class LocalBinder extends Binder {
        public GeoPingMasterService getService() {
            return GeoPingMasterService.this;
        }
    }


    // ===========================================================
    // Clear Log History
    // ===========================================================


    // ===========================================================
    // Other
    // ===========================================================


}
