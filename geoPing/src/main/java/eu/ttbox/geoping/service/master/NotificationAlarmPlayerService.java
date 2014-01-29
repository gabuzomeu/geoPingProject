package eu.ttbox.geoping.service.master;


import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.Future;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.service.receiver.player.AlarmPlayerService;

/**
 * com.example.android.musicplayer.MusicService
 */
public class NotificationAlarmPlayerService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener
//        ,MusicFocusable
{

    private static final String TAG = "NotificationAlarmPlayerService";
    private static final int NOTIFICATION_ID = 357;

    // Action
    public static final String ACTION_TOGGLE_PLAYBACK = "eu.ttbox.geoping.musicplayer.ACTION_TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "eu.ttbox.geoping.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "eu.ttbox.geoping.musicplayer.ACTION_PAUSE";
    public static final String ACTION_STOP = "eu.ttbox.geoping.musicplayer.ACTION_STOP";
    public static final String ACTION_SKIP = "eu.ttbox.geoping.musicplayer.ACTION_SKIP";
    public static final String ACTION_REWIND = "eu.ttbox.geoping.musicplayer.ACTION_REWIND";
    public static final String ACTION_URL = "eu.ttbox.geoping.musicplayer.ACTION_URL";

    // Extra
    public static final String EXTRA_NOTIFICATION = "EXTRA_NOTIFICATION";
    public static final String EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID";

    // Service
    private AudioManager mAudioManager;


    private final IBinder binder = new LocalBinder();

    // Instance
    private MediaPlayer mPlayer = null;

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    }


    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;
    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;


    // Config
    NotifHolder notifHolder;

    // ===========================================================
    // Static Helper
    // ===========================================================

    public static void startNotifAlarmService(Context context, int notificationId, Notification notification) {
        Intent intent = new Intent(context, NotificationAlarmPlayerService.class);
        intent.setAction(NotificationAlarmPlayerService.ACTION_PLAY);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        intent.putExtra(EXTRA_NOTIFICATION, notification);
        context.startService(intent);
    }

    public static Intent createStopAlarmWrappedIntent(Context context, Intent wrappedIntent) {
        Intent stopIntent = new Intent(context, NotificationAlarmPlayerService.class);
        stopIntent.setAction(AlarmPlayerService.ACTION_STOP);
        Intents.wrappedIntentAsActivity(stopIntent, wrappedIntent);
        return stopIntent;
    }
    // ===========================================================
    // Constructors
    // ===========================================================


    @Override
    public void onCreate() {
        Log.i(TAG, "### onCreate service");
        // Service
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        onMusicRetrieverPrepared();
    }


    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }


    // ===========================================================
    // Life cycle
    // ===========================================================

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "### Alarm Service onStartCommand :  "+ intent );
        handleIntent(intent);
        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind Intent : " + intent);
        handleIntent(intent);
        super.onRebind(intent);
    }

    private void handleIntent(Intent intent) {
        Log.d(TAG, "### Alarm Service handleIntent :  "+ intent );
        String action = intent.getAction();
        Log.i(TAG, "onStartCommand action : " + action);
        // Manage Wrapped Instance
        Intents.startWrappedIntent(getApplicationContext(), intent);
        // Manage Alarm Intent
        if (action.equals(ACTION_PLAY)) {
            int notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID);
            Notification notification = intent.getParcelableExtra(EXTRA_NOTIFICATION);
            // Holder
            NotifHolder notif = new NotifHolder(this, notifId, notification);
            processPlayRequest(notif);
        } else if (action.equals(ACTION_PAUSE)) processPauseRequest();
        else if (action.equals(ACTION_STOP)) processStopRequest();
        // else if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
    }


    // ===========================================================
    // Notification UI
    // ===========================================================

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */

    void setUpAsForeground(NotifHolder notif) {
        this.notifHolder = notif;
         startForeground(notif.notificationId, notif.notification);
    }

//    void updateNotification(String text) {
//        if (notification != null) {
//            notification.updateNotification(text);
//        }
//    }


    // ===========================================================
    // Business
    // ===========================================================


    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;

        // If the flag indicates we should start playing after retrieving, let's do that now.
        //if (mStartPlayingAfterRetrieve) {
        //    tryToGetAudioFocus();
        //    playNextSong(mWhatToPlayAfterRetrieve == null ?  null : mWhatToPlayAfterRetrieve.toString());
        //}
    }

    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            // we want the media player to notify us
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        } else {
            mPlayer.reset();
        }
    }


    // ===========================================================
    // Media Player Command
    // ===========================================================


    void processPlayRequest(NotifHolder notif) {
        Log.d(TAG, "processPlayRequest: State " + mState);
        if (mState == State.Retrieving) {
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }
        tryToGetAudioFocus();
        // actually play the song
        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(notif);
        } else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(notif);
            configAndStartMediaPlayer();
        }
    }


    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(NotifHolder notif ) {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {
            final int streamType = AudioManager.STREAM_ALARM;

            createMediaPlayerIfNeeded();
            mPlayer.setDataSource(getApplicationContext(), notif.playingItem);
            // Volume
            final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            int maxVolume = audioManager.getStreamMaxVolume(streamType);
            audioManager.setStreamVolume(streamType, maxVolume, 0);
            mPlayer.setAudioStreamType(streamType);

            // Title
            //     mSongTitle = "Alarm";

            mState = State.Preparing;
            setUpAsForeground(notif);


            mPlayer.prepareAsync();

//            if (mIsStreaming) mWifiLock.acquire();
//            else if (mWifiLock.isHeld()) mWifiLock.release();
        } catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage(), ex);
        }
    }

//    void processTogglePlaybackRequest() {
//        if (mState == State.Paused || mState == State.Stopped) {
//            processPlayRequest();
//        } else {
//            processPauseRequest();
//        }
//    }


    void processPauseRequest() {
        // if (mState == State.Retrieving) {
        //     // If we are still retrieving media, clear the flag that indicates we should start
        //     // playing when we're ready
        //     mStartPlayingAfterRetrieve = false;
        //     return;
        // }

        if (mState == State.Playing) {
            // Pause media player and cancel the 'foreground service' state.
            mState = State.Paused;
            mPlayer.pause();
            relaxResources(false); // while paused, we always retain the MediaPlayer
            // do not give up audio focus
        }

        // Tell any remote controls that our playback state is 'paused'.
//        if (mRemoteControlClientCompat != null) {
//            mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
//        }
    }

    public void processStopRequest() {
        processStopRequest(true);
    }

    public void processStopRequest(boolean force) {
        Log.d(TAG, "processStopRequest with force " + force + " in stattus " + mState);
        if (mState == State.Playing || mState == State.Paused || force) {
            mState = State.Stopped;

            // let go of all resources...
            relaxResources(true);
            giveUpAudioFocus();

            // Tell any remote controls that our playback state is 'paused'.
//            if (mRemoteControlClientCompat != null) {
//                mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
//            }

            if (notifHolder != null) {
                cancelNotification();
            }
            // service is no longer necessary. Will be started again if needed.
            stopSelf();
        }
    }

    private void cancelNotification() {
        if (notifHolder != null) {
            if (notifHolder.taskRunning != null) {
                Log.d(TAG, "Cancel Notification Progressbar");
                notifHolder.taskRunning.cancel(true);
                notifHolder.taskRunning = null;
            }
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    void relaxResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }


    }


    // ===========================================================
    // Audio Focuas
    // ===========================================================

    void giveUpAudioFocus() {
//        if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
//                && mAudioFocusHelper.abandonFocus())
//            mAudioFocus = AudioFocus.NoFocusNoDuck;
    }

    void tryToGetAudioFocus() {
//        if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
//                && mAudioFocusHelper.requestFocus())
//            mAudioFocus = AudioFocus.Focused;
    }

    void configAndStartMediaPlayer() {
//        if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
//            // If we don't have audio focus and can't duck, we have to pause, even if mState
//            // is State.Playing. But we stay in the Playing state so that we know we have to resume
//            // playback once we get the focus back.
//            if (mPlayer.isPlaying()) mPlayer.pause();
//            return;
//        }
//        else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
//            mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
//        else
        mPlayer.setVolume(1.0f, 1.0f); // we can be loud
        Log.d(TAG, "configAndStartMediaPlayer: mPlayer.isPlaying=" + mPlayer.isPlaying());
        if (!mPlayer.isPlaying()) mPlayer.start();
    }
    // ===========================================================
    // Media Player
    // ===========================================================

    /**
     * Called when MediaPlayer is ready
     */
    public void onPrepared(MediaPlayer player) {
        Log.d(TAG, "onPrepared: MediaPlayer");
        mState = State.Playing;
        // updateNotification(mSongTitle + " (playing)");
        configAndStartMediaPlayer();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // ... react appropriately ...
        // The MediaPlayer has moved to the Error state, must be reset!
        Log.e(TAG, "Media Play Error : " + what + " / extra : " + extra);
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // The media player finished playing the current song, so we go ahead and start the next.
        playNextSong(null);
    }


    // ===========================================================
    // Binder
    // ===========================================================

    public class LocalBinder extends Binder {
        public NotificationAlarmPlayerService getService() {
            return NotificationAlarmPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // ===========================================================
    // Notification Older
    // ===========================================================

    static class NotifHolder {

        Uri playingItem = Settings.System.DEFAULT_ALARM_ALERT_URI;

        int notificationId = -1;
        Notification notification;
        Future taskRunning;

        NotifHolder(Context context, int notificationId, Notification notification) {
            this.notificationId = notificationId;
            this.notification = notification;
            // Alarm
            playingItem = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.notif_alert_alien_siren);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NotifHolder)) return false;

            NotifHolder that = (NotifHolder) o;

            if (notificationId != that.notificationId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return notificationId;
        }

    }


}
