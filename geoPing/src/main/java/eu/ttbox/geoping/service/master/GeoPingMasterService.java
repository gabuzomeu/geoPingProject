package eu.ttbox.geoping.service.master;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

import java.util.Locale;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase.GeoTrackColumns;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.Person;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.person.PersonDatabase.PersonColumns;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.ContactVo;
import eu.ttbox.geoping.service.encoder.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.service.encoder.MessageParamEnumLabelHelper;
import eu.ttbox.geoping.ui.person.PhotoThumbmailCache;

public class GeoPingMasterService extends IntentService {

    private static final String TAG = "GeoPingMasterService";
    private static final int SHOW_ON_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_response;
    private static final String ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ = "eu.ttbox.geoping.ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ";
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
    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UI_MSG_TOAST:
                    String msgText = (String) msg.obj;
                    Toast.makeText(getApplicationContext(), msgText, Toast.LENGTH_SHORT).show();
                    break;

                default:
                    break;
            }
        }
    };


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
        this.notifyGeoPingResponse = appPreferences.getBoolean(AppConstants.PREFS_SHOW_GEOPING_NOTIFICATION, false);
        // Google Analytics
        EasyTracker.getInstance().setContext(this);
        tracker = EasyTracker.getTracker();

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

            tracker.sendEvent("MasterService", // Category
                    "HandleIntent", // Action
                    "SMS_GEOPING_REQUEST", // Label
                    0l); // Value
        } else if (Intents.ACTION_SMS_PAIRING_RESQUEST.equals(action)) {
            String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            long userId = intent.getLongExtra(Intents.EXTRA_SMS_USER_ID, -1);
            sendSmsPairingRequest(phone, userId);
            // Tracker
            // tracker.trackPageView("/action/SMS_PAIRING_RESQUEST");
            tracker.sendEvent("MasterService", // Category
                    "HandleIntent", // Action
                    "SMS_PAIRING_RESQUEST", // Label
                    0l); // Value
        } else if (Intents.ACTION_SMS_PAIRING_RESPONSE.equals(action)) {
            String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
            long userId =   MessageEncoderHelper.readLong(params, MessageParamEnum.PERSON_ID, -1);
            consumeSmsPairingResponse(phone, userId);
            // Tracker
            // tracker.trackPageView("/action/SMS_PAIRING_RESPONSE");
            tracker.sendEvent("MasterService", // Category
                    "HandleIntent", // Action
                    "SMS_PAIRING_RESPONSE", // Label
                    0l); // Value
        } else if (ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ.equals(action)) {
            Log.i(TAG, "****************************************************");
            Log.i(TAG, "****************************************************");
            Log.i(TAG, "***** ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ ******");
            Log.i(TAG, "****************************************************");
            Log.i(TAG, "****************************************************");
        } else {
            MessageActionEnum actionEnum = MessageActionEnum.getByIntentName(action);
            if (actionEnum != null) {
                Log.d(TAG, String.format("###  onHandleIntent for actionEnum %s : %s", actionEnum, intent));
                switch (actionEnum) {
                    case COMMAND_OPEN_APP: {
                        String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
                        Bundle params = intent.getBundleExtra(Intents.EXTRA_SMS_PARAMS);
                        sendSmsCommand(actionEnum, phone, params);
                    }
                    break;
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
                // Tracker
                tracker.sendEvent("MasterService", // Category
                        "HandleIntent", // Action
                        actionEnum.name(), // Label
                        0l); // Value
            }
        }

    }


    // ===========================================================
    // Intent Handler
    // ===========================================================

    private void printExtras(Bundle extras) {
        if (extras != null && !extras.isEmpty()) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, "--- Intent extras : " + key + " = " + value);
            }
        } else {
            Log.d(TAG, "--- Intent extras : NONE");
        }
    }

    private Person getPersonByPhone(String phoneNumber) {
        Person result = null;
        // Search
        // Log.d(TAG, String.format("Search Painring for Phone [%s]",
        // phoneNumber));

        Uri uri = PersonProvider.Constants.getUriPhoneFilter(phoneNumber);
        Cursor cur = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                PersonHelper helper = new PersonHelper().initWrapper(cur);
                result = helper.getEntity(cur);
            }
        } finally {
            cur.close();
        }
        Log.d(TAG, String.format("Search Person for Phone [%s] : Found %s", phoneNumber, result));
        return result;
    }

    // ===========================================================
    // Search Person
    // ===========================================================

    private void consumeSmsPairingResponse(String phone, long userId) {
        if (userId != -1l) {
            // Search Phone
            String personPhone = null;
            Uri uri = Uri.withAppendedPath(PersonProvider.Constants.CONTENT_URI, String.valueOf(userId));
            String[] cols = new String[]{PersonColumns.COL_PHONE};
            Cursor cur = getContentResolver().query(uri, cols, null, null, null);
            try {
                if (cur != null && cur.moveToFirst()) {
                    personPhone = cur.getString(1);
                }
            } finally {
                cur.close();
            }
            Log.d(TAG, String.format("Paring response for person Id : %s with phone [%s] =?= Sms [%s]", userId, personPhone, phone));
            // Update
            if (personPhone == null || !personPhone.equals(phone)) {
                ContentValues values = new ContentValues(1);
                values.put(PersonColumns.COL_PHONE, phone);
                values.put(PersonColumns.COL_PAIRING_TIME, System.currentTimeMillis());
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
        Person person = getPersonByPhone(phone);
        if (person == null || TextUtils.isEmpty(person.encryptionPubKey)) {
            ContentValues values = new ContentValues();
            // Generated encryption Key
            Uri entityUri = Uri.withAppendedPath(PersonProvider.Constants.CONTENT_URI, String.valueOf(person.id));
            // getContentResolver().update(entityUri, values, null, null);
        }
        Bundle params =  MessageEncoderHelper.writeToBundle(null,  MessageParamEnum.PERSON_ID,userId);
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
            return false;
        }

        try {
            Uri logUri = SmsSenderHelper.sendSmsAndLogIt(this, SmsLogSideEnum.MASTER, phone, action, params);
            isSend = (logUri != null);
        } catch (IllegalArgumentException e) {
            Message msg = uiHandler.obtainMessage(UI_MSG_TOAST, getResources().getString(R.string.toast_notif_sended_geoping_smsError, phone + " : " + e.getMessage()));
            uiHandler.sendMessage(msg);
        } catch (NullPointerException e) {
            Message msg = uiHandler.obtainMessage(UI_MSG_TOAST, getResources().getString(R.string.toast_notif_sended_geoping_smsError, phone + " : " + e.getMessage()));
            uiHandler.sendMessage(msg);
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
                showNotificationGeoPing(actionEnum, uri, values, geoTrack, params);
            }
            isConsume = true;
        }
        return isConsume;
    }

    // ===========================================================
    // Consume Localisation
    // ===========================================================

    private Person searchPersonForPhone(String phoneNumber) {
        Person person = null;
        Log.d(TAG, String.format("Search Contact Name for Phone : [%s]", phoneNumber));
        Uri uri = PersonProvider.Constants.getUriPhoneFilter(phoneNumber);
        Cursor cur = getContentResolver().query(uri, null, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                PersonHelper helper = new PersonHelper().initWrapper(cur);
                person = helper.getEntity(cur);
            } else {
                Log.w(TAG, "Person not found for phone : [" + phoneNumber + "]");
            }
        } finally {
            cur.close();
        }
        return person;
    }

    private Intent createMarkAsReadLogIntent(String phone) {
        Intent intent = new Intent(this, GeoPingMasterService.class);
        intent.setAction(ACTION_MASTER_GEOPING_PHONE_MARK_AS_READ);
        intent.putExtra(Intents.EXTRA_SMS_PHONE, phone);
        return intent;
    }

    // ===========================================================
    // Notification
    // ===========================================================

    @SuppressLint("NewApi")
    private void showNotificationGeoPing(MessageActionEnum actionEnum, Uri geoTrackData, ContentValues values, GeoTrack geoTrack,  Bundle params) {
        String phone = values.getAsString(GeoTrackColumns.COL_PHONE);

        // Contact Name
        Person person = searchPersonForPhone(phone);
        String contactDisplayName = phone;
        Bitmap photo = null;
        if (person != null) {
            if (person.displayName != null && person.displayName.length() > 0) {
                contactDisplayName = person.displayName;
            }
            PhotoThumbmailCache photoCache = ((GeoPingApplication) getApplication()).getPhotoThumbmailCache();
            photo = ContactHelper.openPhotoBitmap(this, photoCache, person.contactId, phone);
        } else {
            // Search photo in contact database
            ContactVo contactVo = ContactHelper.searchContactForPhone(this, phone);
            if (contactVo != null) {
                contactDisplayName = contactVo.displayName;
                PhotoThumbmailCache photoCache = ((GeoPingApplication) getApplication()).getPhotoThumbmailCache();
                photo = ContactHelper.openPhotoBitmap(this, photoCache, String.valueOf(contactVo.id), phone);
            }
        }
        // --- Create Notif Intent response ---7
        // --- ---------------------------- ---
        // --- Create Parent
        // Create an explicit content Intent that starts the main Activity
        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(getApplicationContext(), MainActivity.class));
        stackBuilder.addNextIntent(Intents.showOnMap(getApplicationContext(), geoTrackData, values));
//        stackBuilder.addNextIntent(createMarkAsReadLogIntent(phone));

        // Get a PendingIntent containing the entire back stack
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

//        PendingIntent pendingIntent = null;
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            pendingIntent = PendingIntent.getActivities(this, 0, //
//                    new Intent[] { new Intent(this, MainActivity.class), Intents.showOnMap(this, geoTrackData, values) }, //
//                    PendingIntent.FLAG_CANCEL_CURRENT);
//        } else {
//            pendingIntent = PendingIntent.getActivity(this, 0, //
//                    Intents.showOnMap(this, geoTrackData, values), //
//                    PendingIntent.FLAG_CANCEL_CURRENT);
//        }
        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Create Notifiation
        Log.d(TAG, "----- contentTitle with Bundle : " + params);
        String contentTitle = MessageActionEnumLabelHelper.getString(this, actionEnum);
        if (MessageEncoderHelper.isToBundle(params, MessageParamEnum.GEOFENCE_NAME)) {
            String geofenceName = MessageEncoderHelper.readString(params, MessageParamEnum.GEOFENCE_NAME);
            if (MessageActionEnum.GEOFENCE_ENTER.equals(actionEnum)) {
                contentTitle =  getString(R.string.sms_action_geofence_transition_enter_with_name, geofenceName);
            } else if (MessageActionEnum.GEOFENCE_EXIT.equals(actionEnum)) {
                contentTitle =  getString(R.string.sms_action_geofence_transition_exit_with_name, geofenceName);
            }
        }
        builder //
                .setDefaults(Notification.DEFAULT_ALL) //
                .setSmallIcon(R.drawable.ic_stat_notif_icon) //
                .setWhen(System.currentTimeMillis()) //
                .setAutoCancel(true) //
                .setContentIntent(pendingIntent)//
                .setContentTitle(contentTitle) //
                        // TODO .setContentTitle(getString(R.string.notif_geoping)) //
                .setContentText(contactDisplayName); //
        if (photo != null) {
            builder.setLargeIcon(photo);
        } else {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_stat_notif_icon);
            builder.setLargeIcon(icon);
        }
//         builder.setNumber(6);
        // Details
        String coordString = getLatLngAsString(geoTrack);
        if (coordString != null) {
            NotificationCompat.InboxStyle inBoxStyle = new NotificationCompat.InboxStyle();
            inBoxStyle.setBigContentTitle(contentTitle);
            inBoxStyle.addLine(contactDisplayName);
            inBoxStyle.addLine(coordString);
            if (geoTrack.batteryLevelInPercent > -1) {
                String batteryLabel = MessageParamEnumLabelHelper.getString(this, MessageParamEnum.BATTERY, geoTrack.batteryLevelInPercent);
                inBoxStyle.addLine(batteryLabel);
            }
            if (geoTrack.hasEventTime()) {
                String smsTypeTime = MessageParamEnumLabelHelper.getLabelHolder(this, MessageParamEnum.EVT_DATE).getString(this, geoTrack.eventTime);
                inBoxStyle.addLine(smsTypeTime);
            }

            builder.setStyle(inBoxStyle);
        }

        // Show
        int notifId = SHOW_ON_NOTIFICATION_ID + phone.hashCode();
        Log.d(TAG, String.format("GeoPing Notification Id : %s for phone %s", notifId, phone));
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        mNotificationManager.notify(notifId, notification);

    }

    private String getLatLngAsString(GeoTrack geoTrack) {
        String coordString = null;

        if (geoTrack.hasLatLng()) {
            coordString = String.format(Locale.US, "(%.6f, %.6f) +/- %s m", geoTrack.getLatitude(), geoTrack.getLongitude(), geoTrack.getAccuracy());
        }
        return coordString;
    }

    public class LocalBinder extends Binder {
        public GeoPingMasterService getService() {
            return GeoPingMasterService.this;
        }
    }

    // ===========================================================
    // Other
    // ===========================================================



}
