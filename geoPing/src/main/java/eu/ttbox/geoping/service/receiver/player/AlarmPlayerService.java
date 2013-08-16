package eu.ttbox.geoping.service.receiver.player;


import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.IOException;
import android.app.Notification;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;

/**
 *  com.example.android.musicplayer.MusicService
 */
public class AlarmPlayerService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener
//        ,MusicFocusable
{

    private static final String TAG = "AlarmPlayerService";
    private static final int NOTIFICATION_ID = 357;

    // Action
    public static final String ACTION_TOGGLE_PLAYBACK =   "eu.ttbox.geoping.musicplayer.ACTION_TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = "eu.ttbox.geoping.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "eu.ttbox.geoping.musicplayer.ACTION_PAUSE";
    public static final String ACTION_STOP = "eu.ttbox.geoping.musicplayer.ACTION_STOP";
    public static final String ACTION_SKIP = "eu.ttbox.geoping.musicplayer.ACTION_SKIP";
    public static final String ACTION_REWIND = "eu.ttbox.geoping.musicplayer.ACTION_REWIND";
    public static final String ACTION_URL = "eu.ttbox.geoping.musicplayer.ACTION_URL";

    // Service
    private AudioManager mAudioManager;
    private NotificationManager mNotificationManager;
    private Notification mNotification = null;
    private final IBinder binder = new LocalBinder();
    private MediaPlayer mPlayer = null;

    AudioFocusHelper mAudioFocusHelper = null;


    // Instance

    // indicates the state our service:
    enum State {
        Retrieving, // the MediaRetriever is retrieving music
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!). (but the media player may actually be
        // paused in this state if we don't have audio focus. But we stay in this state
        // so that we know we have to resume playback once we get focus back)
        Paused      // playback paused (media player ready!)
    };

    State mState = State.Retrieving;

    // if in Retrieving mode, this flag indicates whether we should start playing immediately
    // when we are ready or not.
    boolean mStartPlayingAfterRetrieve = false;
    // if mStartPlayingAfterRetrieve is true, this variable indicates the URL that we should
    // start playing when we are ready. If null, we should play a random song from the device
    Uri mWhatToPlayAfterRetrieve = null;
    // whether the song we are playing is streaming from the network
    boolean mIsStreaming = false;



    // Config
    String mSongTitle = "";
    private Uri playingItem = Settings.System.DEFAULT_ALARM_ALERT_URI;





    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public void onCreate() {
        Log.i(TAG, "debug: Creating service");

        // Service
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Create the retriever and start an asynchronous task that will prepare it.
//        mRetriever = new MusicRetriever(getContentResolver());
//        (new PrepareMusicRetrieverTask(mRetriever,this)).execute();

      //   mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
//
// else
//            mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

//        mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.dummy_album_art);

//        mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);

        onMusicRetrieverPrepared();
    }

    public void onMusicRetrieverPrepared() {
        // Done retrieving!
        mState = State.Stopped;

        // If the flag indicates we should start playing after retrieving, let's do that now.
        if (mStartPlayingAfterRetrieve) {
            tryToGetAudioFocus();
            playNextSong(mWhatToPlayAfterRetrieve == null ?
                    null : mWhatToPlayAfterRetrieve.toString());
        }
    }

    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing. If we don't do
            // that, the CPU might go to sleep while the song is playing, causing playback to stop.
            //
            // Remember that to use this, we have to declare the android.permission.WAKE_LOCK
            // permission in AndroidManifest.xml.
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // we want the media player to notify us when it's ready preparing, and when it's done
            // playing:
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        } else {
            mPlayer.reset();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Log.i(TAG, "debug: onStartCommand : action="+action);
        if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
        else if (action.equals(ACTION_PLAY)) processPlayRequest();
//        else if (action.equals(ACTION_PAUSE)) processPauseRequest();
//        else if (action.equals(ACTION_SKIP)) processSkipRequest();
        else if (action.equals(ACTION_STOP)) processStopRequest();
//        else if (action.equals(ACTION_REWIND)) processRewindRequest();
//        else if (action.equals(ACTION_URL)) processAddRequest(intent);

        return START_NOT_STICKY; // Means we started the service, but don't want it to
        // restart in case it's killed.
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        mState = State.Stopped;
        relaxResources(true);
        giveUpAudioFocus();
    }

    // ===========================================================
    // Media Player Command
    // ===========================================================


    void processPlayRequest() {
        Log.d(TAG, "processPlayRequest: State " +mState);

        if (mState == State.Retrieving) {
            // If we are still retrieving media, just set the flag to start playing when we're
            // ready
            mWhatToPlayAfterRetrieve = null; // play a random song
            mStartPlayingAfterRetrieve = true;
            return;
        }

        tryToGetAudioFocus();

        // actually play the song

        if (mState == State.Stopped) {
            // If we're stopped, just go ahead to the next song and start playing
            playNextSong(null);
        }
        else if (mState == State.Paused) {
            // If we're paused, just continue playback and restore the 'foreground service' state.
            mState = State.Playing;
            setUpAsForeground(mSongTitle + " (playing)");
            configAndStartMediaPlayer();
        }

        // Tell any remote controls that our playback state is 'playing'.
//        if (mRemoteControlClientCompat != null) {
//            mRemoteControlClientCompat.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
//        }
    }

    /**
     * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
     * from our Media Retriever (that is, it will be a random song in the user's device). If
     * manualUrl is non-null, then it specifies the URL or path to the song that will be played
     * next.
     */
    void playNextSong(String manualUrl) {
        mState = State.Stopped;
        relaxResources(false); // release everything except MediaPlayer

        try {

            createMediaPlayerIfNeeded();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(getApplicationContext(), playingItem );

            mSongTitle = "Alarm";

            mState = State.Preparing;
            setUpAsForeground(mSongTitle + " (loading)");


            mPlayer.prepareAsync();

//            if (mIsStreaming) mWifiLock.acquire();
//            else if (mWifiLock.isHeld()) mWifiLock.release();
        } catch (IOException ex) {
            Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    void processTogglePlaybackRequest() {
        if (mState == State.Paused || mState == State.Stopped) {
            processPlayRequest();
        } else {
            processPauseRequest();
        }
    }


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

    void processRewindRequest() {
        if (mState == State.Playing || mState == State.Paused)
            mPlayer.seekTo(0);
    }

    void processSkipRequest() {
        if (mState == State.Playing || mState == State.Paused) {
            tryToGetAudioFocus();
            playNextSong(null);
        }
    }

    void processStopRequest() {
        processStopRequest(false);
    }

    void processStopRequest(boolean force) {
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

            // service is no longer necessary. Will be started again if needed.
            stopSelf();
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
        updateNotification(mSongTitle + " (playing)");
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
    // Notification UI
    // ===========================================================

    /**
     * Configures service as a foreground service. A foreground service is a service that's doing
     * something the user is actively aware of (such as playing music), and must appear to the
     * user as a notification. That's why we create the notification here.
     */
    void setUpAsForeground(String text) {
        Intent stopIntent = new Intent(getApplicationContext(), AlarmPlayerService.class);
        stopIntent.setAction(ACTION_STOP);

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                stopIntent,   PendingIntent.FLAG_UPDATE_CURRENT);

    //    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
   //     builder.setTicker(text);
  //      mNotification =builder.build();

        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.ic_stat_notif_icon; //ic_stat_playing;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "RandomMusicPlayer",
                text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    void updateNotification(String text) {
        Intent stopIntent = new Intent(getApplicationContext(), AlarmPlayerService.class);
        stopIntent.setAction(ACTION_STOP);

        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.setLatestEventInfo(getApplicationContext(), "RandomMusicPlayer", text, pi);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    // ===========================================================
    // Binder
    // ===========================================================

    public class LocalBinder extends Binder {
        public AlarmPlayerService getService() {
            return AlarmPlayerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

}
