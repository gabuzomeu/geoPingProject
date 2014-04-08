package eu.ttbox.geoping.service.master;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.model.GeoTrack;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.utils.contact.ContactHelper;
import eu.ttbox.geoping.utils.contact.NotifPersonVo;
import eu.ttbox.geoping.utils.encoder.MessageEncoderHelper;
import eu.ttbox.geoping.utils.encoder.MessageParamEnumLabelHelper;
import eu.ttbox.geoping.service.receiver.LogReadHistoryService;

public class NotificationMasterHelper {

    private static final String TAG = "NotificationMasterHelper";

    // Constant
    private static final int SHOW_ON_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_response;

    // Config
    private final SmsLogSideEnum side = SmsLogSideEnum.MASTER;
    private boolean showNotificationByPerson = true;

    // Service
    private Context context;
    private NotificationManager mNotificationManager;
    private SharedPreferences prefs;

    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationMasterHelper(Context context) {
        this.context = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    // ===========================================================
    // Notification Builder
    // ===========================================================


    private TaskStackBuilder createShowOnMapActivity(Uri geoTrackData, String phone, int latE6, int lngE6, int accuracy) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(new Intent(context.getApplicationContext(), MainActivity.class));
        stackBuilder.addNextIntent(Intents.showOnMap(context, geoTrackData, phone, latE6, lngE6, accuracy));
        return stackBuilder;
    }


    private TaskStackBuilder clearReadHistoryShowOnMapActivity(Uri geoTrackData, String phone, int latE6, int lngE6, int accuracy) {
        Intent serviceIntent = new Intent(context, LogReadHistoryService.class);
        // Create Stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(serviceIntent);
        return stackBuilder;
    }


    @SuppressLint("NewApi")
    public NotifPersonVo showNotificationGeoPing(MessageActionEnum actionEnum, Uri geoTrackData, ContentValues values
            , GeoTrack geoTrack, Bundle params) {
        String phone = values.getAsString(GeoTrackDatabase.GeoTrackColumns.COL_PHONE);
        // Contact Name
        NotifPersonVo person = ContactHelper.getNotifPersonVo(context, phone);

        // --- Create Notif Intent response ---
        // --- ---------------------------- ---
        // --- Create Parent
        // Create an explicit content Intent that starts the main Activity
        Intent mapAction = Intents.showOnMap(context.getApplicationContext(), geoTrackData, values);
        //TODO
        mapAction = Intents.showLoginPrompt(context.getApplicationContext(), mapAction);
        // Success Intent
        PendingIntent pendingIntent = LogReadHistoryService.createClearLogPendingIntent(context, side, phone, mapAction);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Create Notifiation
        Log.d(TAG, "----- contentTitle with Bundle : " + params);
        // Action Label
        String[] actionLabelParams = null;
        if (MessageActionEnum.GEOFENCE_ENTER.equals(actionEnum) || MessageActionEnum.GEOFENCE_EXIT.equals(actionEnum)) {
            if (MessageEncoderHelper.isToBundle(params, MessageParamEnum.GEOFENCE_NAME)) {
                String geofenceName = MessageEncoderHelper.readString(params, MessageParamEnum.GEOFENCE_NAME);
                actionLabelParams = new String[]{geofenceName};
            }
        }
        String contentTitle = MessageActionEnumLabelHelper.getString(context, actionEnum, actionLabelParams);
        // Notification
        builder
                .setSmallIcon(R.drawable.ic_stat_notif_icon) //
                .setWhen(System.currentTimeMillis()) //
                .setAutoCancel(true) //
                .setContentIntent(pendingIntent)//
                .setDeleteIntent(LogReadHistoryService.createClearLogPendingIntent(context, side, phone, null))
                .setContentTitle(contentTitle) //
                        // TODO .setContentTitle(getString(R.string.notif_geoping)) //
                .setContentText(person.contactDisplayName); //
        // Photo
        if (person.photo != null) {
            builder.setLargeIcon(person.photo);
        } else {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notif_icon);
            builder.setLargeIcon(icon);
        }
        // Alram Type


        // Read Count
        int msgUnreadCount = LogReadHistoryService.getReadLogHistory(context, phone, side);
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
            // Geofence
            if (msgUnreadCount > 1) {
                // TODO Geofence Count / No Requestid
                ArrayList<String> geofences = null;// LogReadHistoryService.getReadLogHistoryGeofenceViolation(  context,   phone,   side);
                if (geofences != null && !geofences.isEmpty()) {
                    for (String geofence : geofences) {
                        inBoxStyle.addLine(geofence);
                    }
                }
            }
            //
            inBoxStyle.setSummaryText(person.contactDisplayName);
            builder.setStyle(inBoxStyle);
        }
        // Sound
        defineSound(context, builder);


        // Show
        int notifId = getNotificationId(phone);
        Log.d(TAG, String.format("GeoPing Notification Id : %s for phone %s", notifId, phone));
        Notification notification = builder.build();

        boolean isAlarm = false;
       // isAlarm = true;
        if (isAlarm) {
            Intent stopIntent = NotificationAlarmPlayerService.createStopAlarmWrappedIntent(context,  mapAction);
          //  stopIntent.setAction(AlarmPlayerService.ACTION_STOP);
          //  Log.d(TAG, "### Start notif service");
            NotificationAlarmPlayerService.startNotifAlarmService(context, notifId, notification);
        } else {
            mNotificationManager.notify(notifId, notification);
        }

        // return person
        return person;
    }


    private void defineSound(Context context, NotificationCompat.Builder builder) {

        if (false) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            builder.setDefaults(Notification.DEFAULT_LIGHTS);
            //   builder.setLights(0xFF0000FF, 100, 3000);

            Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notif_alert_alien_siren);
            // Uri playingItem = Settings.System.DEFAULT_ALARM_ALERT_URI;
            builder.setSound(sound, AudioManager.STREAM_ALARM); //RingtoneManager.TYPE_ALARM
            //  builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        } else {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            builder.setDefaults(Notification.DEFAULT_LIGHTS);
            // Sound
            String soundUri = prefs.getString(context.getString(R.string.pkey_geoping_notif_sound), "content://settings/system/notification_sound");
            Log.d(TAG, "### Notif Geoping Sound Uri : " + soundUri);
            Uri sound = Uri.parse(soundUri);
            builder.setSound(sound, AudioManager.STREAM_NOTIFICATION);
        }

    }

    private int getNotificationId(String phone) {
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
