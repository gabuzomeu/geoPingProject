package eu.ttbox.geoping.ui.person;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.master.GeoPingMasterService;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;


public class PersonRemoteControlFragment extends Fragment {

    private static final String TAG = "PersonRemoteControlFragment";
    // Constant
    private static final int[] buttonIds = new int[]{ //
            R.id.track_person_remote_control_pairingButton //
            , R.id.track_person_remote_control_openButton //
            , R.id.track_person_remote_control_ringButton //
    };
    // Instance
    private Uri entityUri;
    private String entityPhoneNumber;
    // Bindings
    private SparseArray<Button> buttonsMap;
    private PhotoHeaderBinderHelper photoHeader;
    private TextView remoteSummmary;

    // Cache
    private PhotoThumbmailCache photoCache;
    // ===========================================================
    // OnClick Listener
    // ===========================================================
    private View.OnClickListener buttonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onButtonClick(v);
        }
    };

    // ===========================================================
    // Constructors
    // ===========================================================
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.track_person_remote_control, container, false);

        // Binding
        remoteSummmary = (TextView)v.findViewById(R.id.header_photo_main_name);
        buttonsMap = new SparseArray<Button>(buttonIds.length);
        for (int buttonId : buttonIds) {
            Button localButton = (Button) v.findViewById(buttonId);
            localButton.setOnClickListener(buttonOnClickListener);
            buttonsMap.put(buttonId, localButton);
        }
        photoHeader = new PhotoHeaderBinderHelper(v);
        photoHeader.setBlockSubElementVisible(false);
        // Cache
        photoCache = GeoPingApplication.getInstance().getPhotoThumbmailCache();

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Load Data
        loadEntity(getArguments());
    }


    // ===========================================================
    // Menu
    // ===========================================================


    // ===========================================================
    // Accessor
    // ===========================================================

    public void loadEntity(Bundle agrs) {
        if (agrs != null && agrs.containsKey(Intents.EXTRA_DATA_URI)) {
//            String entityId = agrs.getString(Intents.EXTRA_PERSON_ID);
            String phone = agrs.getString(Intents.EXTRA_SMS_PHONE);
            Uri entiyUrl = Uri.parse(agrs.getString(Intents.EXTRA_DATA_URI));
            setEntity(entiyUrl, phone);
        }
    }

    public void setEntity(Uri entityUri, String phoneNumber) {
        this.entityUri = entityUri;
        this.entityPhoneNumber = phoneNumber;
        this.remoteSummmary.setText(getString(R.string.menu_person_remote_control_summary));
        if (!TextUtils.isEmpty(phoneNumber)) {
            setButtonsVisibility(true);
            // Photo
            if (photoCache != null) {
                photoCache.loadPhoto(getActivity(), photoHeader.photoImageView, null, entityPhoneNumber);
            }
        } else {
            setButtonsVisibility(false);
        }

    }

    private void setButtonsVisibility(boolean isEnable) {
        if (buttonsMap != null) {
            for (int key : buttonIds) {
                Button localButton = buttonsMap.get(key);
                if (localButton != null) {
                    localButton.setEnabled(isEnable);
                }
            }
        }
    }

    // ===========================================================
    // Action
    // ===========================================================
    public void onButtonClick(View v) {
        Button localButton = buttonsMap.get(v.getId());
        switch (v.getId()) {
            case R.id.track_person_remote_control_pairingButton:
                // Toast.makeText(getActivity(), "Pairing button click", Toast.LENGTH_SHORT).show();
                onPairingClick(v);
                break;
            case R.id.track_person_remote_control_openButton: {
                onOpenApplicationClick(v);
                Toast.makeText(getActivity(), "Open App button click", Toast.LENGTH_SHORT).show();
            }
            break;
            case R.id.track_person_remote_control_ringButton:
                onTestPlaySoundClick(v);
                Toast.makeText(getActivity(), "Send Test Long SMS click", Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new IllegalArgumentException("Not Implemented action for Id : " + v.getId());

        }
    }

    // ===========================================================
    // Command
    // ===========================================================
    public void onOpenApplicationClick(View v) {
        String entityId = entityUri.getLastPathSegment();
        Intent intent = Intents.commandOpenApplication(getActivity(), entityPhoneNumber, entityId);
        getActivity().startService(intent);

    }

    public void onPairingClick(View v) {
        String entityId = entityUri.getLastPathSegment();
        Intent intent = Intents.pairingRequest(getActivity(), entityPhoneNumber, entityId);
        getActivity().startService(intent);
    }



    private MediaPlayer.OnErrorListener mediOnErrorListener = new MediaPlayer.OnErrorListener() {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "Media Play Error : " + what + " / extra : " + extra);
            return false;
        }
    };


    private MediaPlayer mMediaPlayer;

    private void playSound(Context context, Uri alert) {
        Log.i(TAG, "--- -------------------------------------");
        Log.i(TAG, "--- Request Playing Media : " + alert);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(context, alert);
            final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 10, 0);
                 mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
              //  mMediaPlayer.setOnErrorListener(mediOnErrorListener);
              //  mMediaPlayer.setWakeMode(getActivity().getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
         //   }
        } catch (IOException e) {
            System.out.println("OOPS");
        }
        Log.i(TAG, "--- -------------------------------------");
    }


    public void onTestPlaySoundClick(View v) {
      //   Uri alert =  Settings.System.DEFAULT_ALARM_ALERT_URI;
       //playSound(getActivity(), alert);
        Log.d(TAG, "------------------ onTestPlaySoundClick");
//        Intent intent = new Intent(getActivity() ,AlarmPlayerService.class);
        Intent intent = new Intent(getActivity() , GeoPingMasterService.class);
        intent.setAction(MessageActionEnum.COMMAND_RING.intentAction);
        intent.putExtra(Intents.EXTRA_SMS_PHONE, entityPhoneNumber);
     //   intent.putExtra(Intents.EXTRA_SMSLOG_SIDE_DBCODE, SmsLogSideEnum.MASTER.getDbCode());

        getActivity().startService(intent);
       // Intent checkIntent = new Intent();
       // checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        //startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
    }

    private int MY_DATA_CHECK_CODE = 332;
    private TextToSpeech mTts;

    TextToSpeech.OnInitListener textToSpeechInitListener = new TextToSpeech.OnInitListener()  {

        @Override
        public void onInit(int status) {
            mTts.speak("Il aimait à la voir, avec ses jupes blanches,", TextToSpeech.QUEUE_FLUSH, null);
            mTts.speak("Courir tout au travers du feuillage et des branches,", TextToSpeech.QUEUE_ADD, null);
            mTts.speak("Gauche et pleine de grâce, alors qu’elle cachait", TextToSpeech.QUEUE_ADD, null);
            mTts.speak("Sa jambe, si la robe aux buissons s’accrochait.", TextToSpeech.QUEUE_ADD, null);
        //    mTts.speak("", TextToSpeech.QUEUE_ADD, null);
        }
    };

    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(getActivity(), textToSpeechInitListener);
                mTts.setLanguage(Locale.FRANCE);




            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }


}
