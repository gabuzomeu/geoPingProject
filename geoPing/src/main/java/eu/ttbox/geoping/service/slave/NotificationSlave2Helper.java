package eu.ttbox.geoping.service.slave;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.util.Log;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.NotifPersonVo;
import eu.ttbox.geoping.service.receiver.LogReadHistoryService;

public class NotificationSlave2Helper {

    private static final String TAG = "NotificationSlaveHelper";

    // Constant
    private static final int SHOW_GEOPING_REQUEST_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_request_confirm;

    // Config
    private  boolean showNotificationByPerson = true;
    //
    private final SmsLogSideEnum side = SmsLogSideEnum.SLAVE;

    // Service
    private Context context;
    private NotificationManager mNotificationManager;
    private SharedPreferences prefs;

    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationSlave2Helper(Context context) {
        this.context = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }


    // ===========================================================
    // GeoPing Request Notification Builder
    // ===========================================================

    public void showGeopingRequestNotification(Pairing pairing, MessageActionEnum msgAction, Bundle eventParam, boolean authorizeIt) {
        String phone = pairing.phone;
        // Contact Name
        NotifPersonVo person = ContactHelper.getNotifPairingVo(context, pairing);


        // Create Notifiation
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context) //
                .setDefaults(Notification.DEFAULT_ALL) //
                .setSmallIcon(R.drawable.ic_stat_notif_icon) //
                .setWhen(System.currentTimeMillis()) //
                .setAutoCancel(true) //
                .setContentText(person.contactDisplayName); //

        if (authorizeIt) {
            builder.setContentTitle(context.getString(R.string.notif_geoping_request)); //
        } else {
            String actionLabel = MessageActionEnumLabelHelper.getString(context,  msgAction );
            builder.setContentTitle(context.getString(R.string.notif_actionMessage_blocked, actionLabel)); //
        }
        if (person.photo != null) {
            builder.setLargeIcon(person.photo);
        } else {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notif_icon);
            builder.setLargeIcon(icon);
        }

        // Notification Count
        PendingIntent contentIntent = null;
        PendingIntent deleteIntent = null;
        if (pairing.showNotification) {
            String searchPhone = showNotificationByPerson ? phone : null;
            int msgUnreadCount =  LogReadHistoryService.getReadLogHistory(context, searchPhone, side);
            if (msgUnreadCount > 1) {
                // Mark as read  Action
                contentIntent =LogReadHistoryService.createClearLogPendingIntent(context, side, searchPhone, null);
                builder.setContentIntent(contentIntent);
                deleteIntent= LogReadHistoryService.createClearLogPendingIntent(context, side, searchPhone, null, 10);
                builder.setDeleteIntent(deleteIntent);
                // Ser counter
                builder.setNumber(msgUnreadCount);
            }
        }
        // Sound
        defineSound(context, builder);

        // Display Notif
        int notifId = getGeopingRequestNotificationId(phone);
        Notification notification = builder.build();
        // It is a bug ??
        if (contentIntent!=null) {
            notification.contentIntent = contentIntent;
        }
        if (deleteIntent!=null) {
            notification.deleteIntent = deleteIntent;
        }
        // Display Notification
        mNotificationManager.notify(notifId, notification);
    }


    private void defineSound(Context context, NotificationCompat.Builder builder) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            builder.setDefaults(Notification.DEFAULT_LIGHTS);
            // Sound
            String soundUri = prefs.getString(context.getString(R.string.pkey_pairing_notif_sound), "content://settings/system/notification_sound");
            Log.d(TAG, "### Notif Geoping Sound Uri : " + soundUri);
            Uri sound = Uri.parse(soundUri);
            builder.setSound(sound, AudioManager.STREAM_NOTIFICATION);
    }

    private int getGeopingRequestNotificationId(String  phone) {
        int notifId = SHOW_GEOPING_REQUEST_NOTIFICATION_ID;
        if (showNotificationByPerson) {
            notifId += phone.hashCode();
            Log.d(TAG, String.format("GeoPing Notification Id : %s for phone %s", notifId, phone));
        }
        return notifId;
    }



    // ===========================================================
    // Pairing Request Notification Builder
    // ===========================================================

}
