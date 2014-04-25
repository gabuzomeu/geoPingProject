package eu.ttbox.geoping.service.slave;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.slave.receiver.AuthorizePhoneTypeEnum;
import eu.ttbox.geoping.utils.contact.ContactHelper;
import eu.ttbox.geoping.utils.contact.NotifPairingVo;

public class NotificationSlavePairing2Helper {

    private static final String TAG = "NotificationSlavePairingHelper";

    private static final int SHOW_GEOPING_REQUEST_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_new_geoping_request_confirm;
    private static final int SHOW_PAIRING_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_pairing_request;

    // Config
    private  boolean showNotificationByPerson = true;
    //
    private final SmsLogSideEnum side = SmsLogSideEnum.SLAVE;

    // Service
    private Context context;
    private NotificationManager mNotificationManager;

    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationSlavePairing2Helper(Context context) {
        this.context = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }


    // ===========================================================
    // GeoPing Request Notification Builder
    // ===========================================================

    private String getTitle(MessageActionEnum msgAction, GeopingNotifSlaveTypeEnum onlyPairing ) {
        String actionLabel = MessageActionEnumLabelHelper.getString(context, msgAction);
        return actionLabel;
    }

    public NotifPairingVo showNotificationNewPingRequestConfirm(Pairing pairing, Intent eventIntent, MessageActionEnum msgAction, GeopingNotifSlaveTypeEnum onlyPairing) {
        // Log.d(TAG,"******************************************************");
        // Log.d(TAG,"*****  showNotificationNewPingRequestConfirm  ****");
        // Log.d(TAG,"******************************************************");
        String phone = pairing.phone;

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notif_geoping_request_register);
        // Contact Name
        NotifPairingVo person = ContactHelper.getNotifPairingVo(context, pairing);

        // Generate Notification ID per Person
        int notifId = SHOW_GEOPING_REQUEST_NOTIFICATION_ID + phone.hashCode();
        Log.d(TAG, String.format("GeoPing Notification Id : %s for phone %s", notifId, phone));

        // Content Intent In android 2.3 no Custun View displayble
        // TODO Propose a choice
        PendingIntent contentIntent = null;

        // Service
        Resources r = context.getResources();
        // Title
        String title;
        String contentText = person.contactDisplayName + r.getString(R.string.notif_click_to_accept);
        switch (onlyPairing) {
            case PAIRING:
                notifId = SHOW_PAIRING_NOTIFICATION_ID + phone.hashCode();
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_yes, View.VISIBLE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_no, View.GONE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_never, View.VISIBLE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_always, View.VISIBLE);
                contentView.setTextViewText(R.id.notif_geoping_confirm_button_yes, r.getText(R.string.notif_confirm_request_eachtime));
                title = context.getString(R.string.notif_pairing);
                contentIntent = PendingIntent.getService(context, 0, //
                        Intents.authorizePhone(context, phone, eventIntent, AuthorizePhoneTypeEnum.ALWAYS, notifId, onlyPairing),//
                        PendingIntent.FLAG_UPDATE_CURRENT);
                break;
            case GEOPING_REQUEST_CONFIRM:
                title = getTitle(msgAction, onlyPairing);//context.getString(R.string.notif_geoping_request);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_yes, View.VISIBLE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_no, View.VISIBLE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_never, View.GONE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_always, View.GONE);
                break;
            case GEOPING_REQUEST_CONFIRM_FIRST:
                title =  getTitle(msgAction, onlyPairing);//context.getString(R.string.notif_geoping_request);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_yes, View.VISIBLE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_no, View.GONE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_always, View.VISIBLE);
                contentView.setViewVisibility(R.id.notif_geoping_confirm_button_never, View.VISIBLE);
                contentView.setTextViewText(R.id.notif_geoping_confirm_button_yes, r.getText(R.string.notif_confirm_request_eachtime));
                break;
            default:
                title = context.getString(R.string.app_name);
                break;
        }

        // View
        contentView.setTextViewText(R.id.notif_geoping_title, title);
        contentView.setTextViewText(R.id.notif_geoping_phone, person.contactDisplayName);
        // Pending Intent
        PendingIntent secuNo = PendingIntent.getService(context, 0, //
                Intents.authorizePhone(context, phone,  eventIntent, AuthorizePhoneTypeEnum.NO, notifId, onlyPairing),//
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent secuNever = PendingIntent.getService(context, 1, //
                Intents.authorizePhone(context, phone, eventIntent, AuthorizePhoneTypeEnum.NEVER, notifId, onlyPairing),//
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent secuYes = PendingIntent.getService(context, 2, //
                Intents.authorizePhone(context, phone,  eventIntent, AuthorizePhoneTypeEnum.YES, notifId, onlyPairing),//
                PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent secuAlways = PendingIntent.getService(context, 3, //
                Intents.authorizePhone(context, phone, eventIntent, AuthorizePhoneTypeEnum.ALWAYS, notifId, onlyPairing),//
                PendingIntent.FLAG_UPDATE_CURRENT);
        // Manage Button Confirmation
        contentView.setOnClickPendingIntent(R.id.notif_geoping_confirm_button_no, secuNo);
        contentView.setOnClickPendingIntent(R.id.notif_geoping_confirm_button_never, secuNever);
        contentView.setOnClickPendingIntent(R.id.notif_geoping_confirm_button_yes, secuYes);
        contentView.setOnClickPendingIntent(R.id.notif_geoping_confirm_button_always, secuAlways);

        // Content Intent
        if (contentIntent == null) {
            contentIntent = PendingIntent.getService(context, 0, //
                    Intents.authorizePhone(context, phone,  eventIntent, AuthorizePhoneTypeEnum.YES, notifId, onlyPairing),//
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Create Notifiation
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context) //
                .setDefaults(Notification.DEFAULT_ALL) //
                .setSmallIcon(R.drawable.ic_stat_notif_icon) //
                .setWhen(System.currentTimeMillis()) //
                .setAutoCancel(true) //

                .setOngoing(true) //
                .setContentTitle(title) //
                .setContentText(contentText) //
                .setTicker(title) //
                .setContentIntent(contentIntent); //

        // .setNumber(5) //
        // Content Value
        if (VersionUtils.isJb16) {
            // Jb
            NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(notificationBuilder);
            //style.addLine(contactDisplayName) //
            style.setSummaryText(person.contactDisplayName)//
            ;
            notificationBuilder.setStyle(style);
            // Add Action
            notificationBuilder.setDeleteIntent(secuNo);

            switch (onlyPairing) {
                case PAIRING:
                    notificationBuilder.addAction(R.drawable.ic_cadenas_ferme_rouge, r.getString(R.string.notif_pairing_never), secuNever);
                    notificationBuilder.addAction(R.drawable.ic_cadenas_entrouvert_jaune, r.getString(android.R.string.yes), secuYes);
                    notificationBuilder.addAction(R.drawable.ic_cadenas_ouvert_vert, r.getString(R.string.notif_pairing_always), secuAlways);
                    break;
                case GEOPING_REQUEST_CONFIRM:
                    notificationBuilder.addAction(R.drawable.ic_menu_nav_accept, r.getString(android.R.string.yes), secuYes);
                    notificationBuilder.addAction(R.drawable.ic_menu_nav_cancel, r.getString(android.R.string.no), secuNo);
                    break;
                case GEOPING_REQUEST_CONFIRM_FIRST:
                    notificationBuilder.addAction(R.drawable.ic_cadenas_ferme_rouge, r.getString(R.string.notif_pairing_never), secuNever);
                    notificationBuilder.addAction(R.drawable.ic_menu_nav_accept, r.getString(android.R.string.yes), secuYes);
                    notificationBuilder.addAction(R.drawable.ic_cadenas_ouvert_vert, r.getString(R.string.notif_pairing_always), secuAlways);
                    break;
            }
            // notificationBuilder.addAction(R.drawable.ic_cadenas_ferme_rouge,
            // r.getString(R.string.notif_pairing_never), secuNever);
            // notificationBuilder.addAction(R.drawable.ic_menu_nav_cancel,
            // r.getString(android.R.string.no), secuNo);
            // notificationBuilder.addAction(R.drawable.ic_menu_nav_accept,
            // r.getString(android.R.string.yes), secuYes);
            // notificationBuilder.addAction(R.drawable.ic_cadenas_ouvert_vert,
            // r.getString(R.string.notif_pairing_always), secuAlways);
            // Tocker

        } else {
            // Ics, Hb, ginger
            notificationBuilder.setContent(contentView);
        }

        if (person.photo != null) {
            notificationBuilder.setLargeIcon(person.photo);
        } else {
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notif_icon);
            notificationBuilder.setLargeIcon(icon);
        }
        Notification notification = notificationBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        // notification.contentIntent = contentIntent;
        // notification.contentView = contentView;
        // notification.flags = Notification.FLAG_ONGOING_EVENT |
        // Notification.FLAG_ONLY_ALERT_ONCE;
        // notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        // Show
        mNotificationManager.notify(notifId, notification);


        return person;
    }

}
