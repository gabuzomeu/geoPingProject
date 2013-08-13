package eu.ttbox.geoping.service.master;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.Locale;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.GeoTrackerProvider;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.geotrack.GeoTrackHelper;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.Person;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.ContactVo;
import eu.ttbox.geoping.service.encoder.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.service.encoder.MessageParamEnumLabelHelper;
import eu.ttbox.geoping.ui.person.PhotoThumbmailCache;

public class NotificationHelperV2 {

    private static final String TAG = "NotificationHelperV2";

    private static final int SHOW_ON_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_response;

    private Context context;

    // Service
    private NotificationManager mNotificationManager;
    private PhotoThumbmailCache photoCache;

    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationHelperV2(Context context) {
        this.context = context;
        this.photoCache = ((GeoPingApplication) context.getApplicationContext()).getPhotoThumbmailCache();
    }


    // ===========================================================
    // Notification Builder
    // ===========================================================


    private TaskStackBuilder createShowOnMapActivity( Uri geoTrackData, String phone, int latE6, int lngE6, int accuracy) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(context.getApplicationContext(), MainActivity.class));
        stackBuilder.addNextIntent(Intents.showOnMap(context, geoTrackData,   phone,   latE6,   lngE6,   accuracy));
        return stackBuilder;
    }


    private TaskStackBuilder clearReadHistoryShowOnMapActivity( Uri geoTrackData, String phone, int latE6, int lngE6, int accuracy) {
        Intent serviceIntent = new Intent(context, NotificationReadHistoryService.class);
        // Create Stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent( serviceIntent);
        return stackBuilder;
    }


    @SuppressLint("NewApi")
    public void showNotificationGeoPing(MessageActionEnum actionEnum, Uri geoTrackData, ContentValues values
            , GeoTrack geoTrack,  Bundle params) {
        String phone = values.getAsString(GeoTrackDatabase.GeoTrackColumns.COL_PHONE);
        // Contact Name
        NotifPersonVo person = getNotifPersonVo(phone);


        // --- Create Notif Intent response ---7
        // --- ---------------------------- ---
        // --- Create Parent
        // Create an explicit content Intent that starts the main Activity
        Intent mapAction = Intents.showOnMap(context.getApplicationContext(), geoTrackData, values);
        // Intent
        Intent readAction = new Intent(context, NotificationReadHistoryService.class);
        readAction.putExtra(Intents.EXTRA_INTENT, mapAction);

        // Construct a task stack
        // TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        //stackBuilder.addParentStack(MainActivity.class);
       // stackBuilder.addNextIntent(new Intent(context.getApplicationContext(), MainActivity.class));
     //   stackBuilder.addNextIntent(Intents.showOnMap(context.getApplicationContext(), geoTrackData, values));
        // stackBuilder.addNextIntent(readAction);

        // Get a PendingIntent containing the entire back stack
       // PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, readAction,  PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Create Notifiation
        Log.d(TAG, "----- contentTitle with Bundle : " + params);
        String contentTitle = MessageActionEnumLabelHelper.getString(context, actionEnum);
        if (MessageEncoderHelper.isToBundle(params, MessageParamEnum.GEOFENCE_NAME)) {
            String geofenceName = MessageEncoderHelper.readString(params, MessageParamEnum.GEOFENCE_NAME);
            if (MessageActionEnum.GEOFENCE_ENTER.equals(actionEnum)) {
                contentTitle =  context.getString(R.string.sms_action_geofence_transition_enter_with_name, geofenceName);
            } else if (MessageActionEnum.GEOFENCE_EXIT.equals(actionEnum)) {
                contentTitle =  context.getString(R.string.sms_action_geofence_transition_exit_with_name, geofenceName);
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
                .setContentText(person.contactDisplayName); //
        if (person.photo != null) {
            builder.setLargeIcon(person.photo);
        } else {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notif_icon);
            builder.setLargeIcon(icon);
        }

        int msgUnreadCount = createNotificationContent(phone);
         builder.setNumber(msgUnreadCount);
        // Details
        String coordString = getLatLngAsString(geoTrack);
        if (coordString != null) {
            NotificationCompat.InboxStyle inBoxStyle = new NotificationCompat.InboxStyle();
            inBoxStyle.setBigContentTitle(contentTitle);
            inBoxStyle.addLine(person.contactDisplayName);
            inBoxStyle.addLine(coordString);
            if (geoTrack.batteryLevelInPercent > -1) {
                String batteryLabel = MessageParamEnumLabelHelper.getString(context, MessageParamEnum.BATTERY, geoTrack.batteryLevelInPercent);
                inBoxStyle.addLine(batteryLabel);
            }
            if (geoTrack.hasEventTime()) {
                String smsTypeTime = MessageParamEnumLabelHelper.getLabelHolder(context, MessageParamEnum.EVT_DATE).getString(context, geoTrack.eventTime);
                inBoxStyle.addLine(smsTypeTime);
            }
            builder.setStyle(inBoxStyle);
        }

        // Show
        int notifId = SHOW_ON_NOTIFICATION_ID + phone.hashCode();
        Log.d(TAG, String.format("GeoPing Notification Id : %s for phone %s", notifId, phone));
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = builder.build();
        mNotificationManager.notify(notifId, notification);
    }

    // ===========================================================
    // Message Read History
    // ===========================================================


    private int createNotificationContent( String phone) {
        int count = NotificationReadHistoryService.getReadLogHistory(context, phone);
        return count;
    }


    // ===========================================================
    // Person Detail
    // ===========================================================


    private NotifPersonVo getNotifPersonVo(String phone) {
        // Contact Name
        Person person = searchPersonForPhone(phone);
        String contactDisplayName = phone;
        Bitmap photo = null;
        if (person != null) {
            if (person.displayName != null && person.displayName.length() > 0) {
                contactDisplayName = person.displayName;
            }
            photo = ContactHelper.openPhotoBitmap(context, photoCache, person.contactId, phone);
        } else {
            // Search photo in contact database
            ContactVo contactVo = ContactHelper.searchContactForPhone(context, phone);
            if (contactVo != null) {
                contactDisplayName = contactVo.displayName;
                photo = ContactHelper.openPhotoBitmap(context, photoCache, String.valueOf(contactVo.id), phone);
            }
        }
        return new NotifPersonVo(phone, contactDisplayName, photo);
    }

    private Person searchPersonForPhone(String phoneNumber) {
        Person person = null;
        Log.d(TAG, String.format("Search Contact Name for Phone : [%s]", phoneNumber));
        Uri uri = PersonProvider.Constants.getUriPhoneFilter(phoneNumber);
        Cursor cur = context.getContentResolver().query(uri, null, null, null, null);
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

    // ===========================================================
    // Other
    // ===========================================================

    private String getLatLngAsString(GeoTrack geoTrack) {
        String coordString = null;

        if (geoTrack.hasLatLng()) {
            coordString = String.format(Locale.US, "(%.6f, %.6f) +/- %s m", geoTrack.getLatitude(), geoTrack.getLongitude(), geoTrack.getAccuracy());
        }
        return coordString;
    }
}
