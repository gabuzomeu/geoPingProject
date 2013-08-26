package eu.ttbox.geoping.service.receiver.player;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.service.core.ContactHelper;
import eu.ttbox.geoping.service.core.NotifPersonVo;

public class NotificationAlarmHelper {

    private static final String TAG = "NotificationAlarmHelper";

    // Constant
    private static final int SHOW_ON_NOTIFICATION_ID = AppConstants.PER_PERSON_ID_MULTIPLICATOR * R.id.show_notification_alarm;


    // Config
    private SmsLogSideEnum side;
    private String phone;

    private boolean showNotificationByPerson = true;


    // Service
    private AlarmPlayerService context;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    // Instance
    private int notifId;
    private Future taskRunning;

    // ===========================================================
    // Constructor
    // ===========================================================


    public NotificationAlarmHelper(AlarmPlayerService context, String phone, SmsLogSideEnum side) {
        this.context = context;
        this.mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Config
        this.side = side;
        this.phone = phone;
        // Instance

    }


    // ===========================================================
    // Notification Builder
    // ===========================================================

    private int getNotificationId(String phone) {
        int notifId = SHOW_ON_NOTIFICATION_ID;
        if (showNotificationByPerson) {
            notifId += phone.hashCode();
        }
        return notifId;
    }


    public void showNotificationAlarm() {
        // Contact Name
        NotifPersonVo person = null;
        if (side != null) {
            switch (side) {
                case SLAVE:
                    person = ContactHelper.getNotifPairingVo(context, phone);
                    break;
                case MASTER:
                    person = ContactHelper.getNotifPersonVo(context, phone);
                    break;
                default: {
                    Log.w(TAG, "Not manage showNotificationAlarm for Side : " + side);
                }
            }
        }

        // Create Action
        Intent stopIntent = new Intent(context.getApplicationContext(), AlarmPlayerService.class);
        stopIntent.setAction(AlarmPlayerService.ACTION_STOP);

        PendingIntent pi = PendingIntent.getService(context.getApplicationContext(), 0,
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Build Notification
        mBuilder = new NotificationCompat.Builder(context) //
                .setContentTitle("Alert") //
                .setSmallIcon(R.drawable.ic_stat_notif_icon) //
                .setContentIntent(pi);
        if (person != null) {
            mBuilder.setContentText(person.contactDisplayName); //
            if (person.photo != null) {
                mBuilder.setLargeIcon(person.photo);
            } else {
                Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_stat_notif_icon);
                mBuilder.setLargeIcon(icon);
            }
        }
        // Show Notification
        this.notifId = getNotificationId(phone);
        Notification mNotification = mBuilder.build();
        context.startForeground(notifId, mNotification);
    }

    public void cancelNotification() {
        if (taskRunning != null) {
            Log.d(TAG, "Cancel Notification Progressbar");

            taskRunning.cancel(true);
            taskRunning = null;
        }
    }

    public void updateNotification(String text) {
        // mBuilder.setContentText(text);
        // Notification mNotification = mBuilder.build();
        //  mNotificationManager.notify(notifId, mNotification);
        // Progress

     //   NotifProgessRunnable task = new NotifProgessRunnable(notifId);
     //   taskRunning = executor.submit(task);

    }

    private class NotifProgessCallable implements Callable<Void> {

        private int timeDurationInMs = 1000 * 60;

        private int progress = 0;

        @Override
        public Void call() throws Exception {
            progress += 5;
            return null;
        }
    }


    private class NotifProgessRunnable implements Runnable {
        int notifId;

        private NotifProgessRunnable(int notifId) {
            this.notifId = notifId;
        }
        public void interrupt() {
            Log.d(TAG, "--- ------------------------------------------------- ---");
            Log.d(TAG, "--- NotifProgessRunnable interrupt                    ---");
            Log.d(TAG, "--- ------------------------------------------------- ---");

        }
        @Override
        public void run() {
            int incr;
            // Do the "lengthy" operation 20 times
            for (incr = 0; incr <= 100; incr += 2) {

                if (taskRunning == null) {
                    break;
                }
                // Sets the progress indicator to a max value, the
                // current completion percentage, and "determinate"
                // state
                mBuilder.setProgress(100, incr, false);
                // Displays the progress bar for the first time.
                mNotificationManager.notify(notifId, mBuilder.build());
                // Sleeps the thread, simulating an operation
                // that takes time
                try {
                    // Sleep for 5 seconds
                    Thread.sleep(2 * 1000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "sleep failure");
                }
            }
            // End
            mBuilder.setContentText("Download complete") // Removes the progress bar
                    .setProgress(0, 0, false);
            mNotificationManager.notify(notifId, mBuilder.build());
            taskRunning = null;
            context.processStopRequest(true);

            //context.stopForeground(true);
        }
    }

    ;

}
