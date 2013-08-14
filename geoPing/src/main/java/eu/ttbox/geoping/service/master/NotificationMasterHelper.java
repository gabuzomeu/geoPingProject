package eu.ttbox.geoping.service.master;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.Locale;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.NotifPersonVo;
import eu.ttbox.geoping.service.encoder.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.service.encoder.MessageParamEnumLabelHelper;
import eu.ttbox.geoping.service.receiver.LogReadHistoryService;

public class NotificationMasterHelper {

    private static final String TAG = "NotificationMasterHelper";

    // Constant
    private static final int SHOW_ON_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_response;

    // Config
    private final SmsLogSideEnum side = SmsLogSideEnum.MASTER;
    private  boolean showNotificationByPerson = true;

    // Service
    private Context context;
    private NotificationManager mNotificationManager;

    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationMasterHelper(Context context) {
        this.context = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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
        Intent serviceIntent = new Intent(context, LogReadHistoryService.class);
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
        NotifPersonVo person = ContactHelper.getNotifPersonVo(context, phone);

        // --- Create Notif Intent response ---7
        // --- ---------------------------- ---
        // --- Create Parent
        // Create an explicit content Intent that starts the main Activity
        Intent mapAction = Intents.showOnMap(context.getApplicationContext(), geoTrackData, values);
        // Intent

        PendingIntent pendingIntent = LogReadHistoryService.createClearLogPendingIntent(context, side, phone, mapAction);

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
                .setDeleteIntent(LogReadHistoryService.createClearLogPendingIntent(context, side, phone, null))
                .setContentTitle(contentTitle) //
                        // TODO .setContentTitle(getString(R.string.notif_geoping)) //
                .setContentText(person.contactDisplayName); //
        if (person.photo != null) {
            builder.setLargeIcon(person.photo);
        } else {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notif_icon);
            builder.setLargeIcon(icon);
        }

        int msgUnreadCount =  LogReadHistoryService.getReadLogHistory(context, phone, side);
        if (msgUnreadCount > 1) {
         builder.setNumber(msgUnreadCount);
        }
        // Details
        String coordString = getLatLngAsString(geoTrack);
        if (coordString != null) {
            NotificationCompat.InboxStyle inBoxStyle = new NotificationCompat.InboxStyle();
            inBoxStyle.setBigContentTitle(contentTitle);
            //inBoxStyle.addLine(person.contactDisplayName);
            inBoxStyle.addLine(coordString);
            if (geoTrack.batteryLevelInPercent > -1) {
                String batteryLabel = MessageParamEnumLabelHelper.getString(context, MessageParamEnum.BATTERY, geoTrack.batteryLevelInPercent);
                inBoxStyle.addLine(batteryLabel);
            }
            if (geoTrack.hasEventTime()) {
                String smsTypeTime = MessageParamEnumLabelHelper.getLabelHolder(context, MessageParamEnum.EVT_DATE).getString(context, geoTrack.eventTime);
                inBoxStyle.addLine(smsTypeTime);
            }
            inBoxStyle.setSummaryText(person.contactDisplayName);
            builder.setStyle(inBoxStyle);
        }

        // Show
        int notifId = getNotificationId(phone);
        Log.d(TAG, String.format("GeoPing Notification Id : %s for phone %s", notifId, phone));

        Notification notification = builder.build();
        mNotificationManager.notify(notifId, notification);
    }


    private int getNotificationId(String  phone) {
        int notifId = SHOW_ON_NOTIFICATION_ID;
        if (showNotificationByPerson) {
            notifId += phone.hashCode();
        }
        return notifId;
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
