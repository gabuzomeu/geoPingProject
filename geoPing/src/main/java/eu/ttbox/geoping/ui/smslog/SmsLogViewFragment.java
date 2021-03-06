package eu.ttbox.geoping.ui.smslog;


import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.MessageActionEnumLabelHelper;
import eu.ttbox.geoping.domain.model.SmsLogSideEnum;
import eu.ttbox.geoping.domain.model.SmsLogTypeEnum;
import eu.ttbox.geoping.domain.smslog.SmsLogDatabase;
import eu.ttbox.geoping.domain.smslog.SmsLogHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.encoder.params.ParamEncoderHelper;
import eu.ttbox.geoping.service.SmsSenderHelper;
import eu.ttbox.geoping.ui.person.PhotoHeaderBinderHelper;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;
import eu.ttbox.geoping.utils.encoder.MessageParamEnumLabelHelper;
import eu.ttbox.geoping.utils.encoder.adpater.BundleEncoderAdapter;


public class SmsLogViewFragment extends Fragment {

    private static final String TAG = "SmsLogViewFragment";
    // Binding
    private PhotoHeaderBinderHelper photoHeader;
    private TextView messageTextView;
    private TextView messageDetailTextView;

    private ImageView smsTypeImageView;
    private TextView smsTypeTimeTextView;
    private LinearLayout paramListView;


    // Binding Header
    private ImageView headerMainIcon;
    private ImageView headerSubIcon;

    // Binding Resend
    private View errorContainer;
    private ImageView errorIconTwo;
    private ImageView errorIconThree;
    private TextView errorText;

    // Cache
    private PhotoThumbmailCache photoCache;
    private SmsLogResources mResources;
    // Instance
    private Uri entityUri;
    // Context
    private Context mContext;
    private SmsLogHelper helper = new SmsLogHelper();
    private Handler handler = new Handler();
    private PersonNameFinderHelper cacheNameFinder;

    private ContentObserver observer = new ContentObserver(handler) {
        @Override
        public void onChange(boolean selfChange) {
            loadEntity(entityUri);
            super.onChange(selfChange);
        }
    };

    // ===========================================================
    // Constructors
    // ===========================================================


    public SmsLogViewFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.smslog_view, container, false);
        mContext = getActivity();
        // Menu on Fragment
        setHasOptionsMenu(true);
        // Cache
        this.photoCache = ((GeoPingApplication) mContext.getApplicationContext()).getPhotoThumbmailCache();
        this.mResources = new SmsLogResources(getActivity());
        this.cacheNameFinder = new PersonNameFinderHelper(mContext, false);

        // Bindings
        photoHeader = new PhotoHeaderBinderHelper(v);

        this.messageTextView = (TextView) v.findViewById(R.id.smslog_message);
        this.messageDetailTextView = (TextView) v.findViewById(R.id.smslog_message_detail);
        this.paramListView = (LinearLayout) v.findViewById(R.id.smslog_message_param_list);

        this.smsTypeImageView = (ImageView) v.findViewById(R.id.smslog_list_item_smsType_imgs);
        this.smsTypeTimeTextView = (TextView) v.findViewById(R.id.smslog_list_item_time_ago);

        this.messageTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClickSmsMessage();
                return false;
            }
        });

        // Header Bindings
        headerMainIcon = (ImageView) v.findViewById(R.id.header_photo_main_action);
        headerSubIcon = (ImageView) v.findViewById(R.id.header_photo_subelt_icon);

        // Binding Resend
        errorContainer = v.findViewById(R.id.smslog_list_error_container);
        errorIconTwo = (ImageView) v.findViewById(R.id.smslog_list_error_img_two);
        errorIconThree = (ImageView) v.findViewById(R.id.smslog_list_error_img_three);
        errorText = (TextView) v.findViewById(R.id.smslog_list_error_text);

        return v;
    }


    // ===========================================================
    // Display Screen
    // ===========================================================

    private void dispayReSendCount(int count) {
        int errorVisibility = count > 0 ? View.VISIBLE : View.GONE;
        int iconTwo = count > 1 && count < 3 ? View.VISIBLE : View.GONE;
        int iconThree = count == 3 ? View.VISIBLE : View.GONE;
        int textVisibility = count > 3 ? View.VISIBLE : View.GONE;
        // Manage Text
        errorText.setText(count + "x");
        // Visibility
        errorContainer.setVisibility(errorVisibility);
        errorIconTwo.setVisibility(iconTwo);
        errorIconThree.setVisibility(iconThree);
        errorText.setVisibility(textVisibility);
    }


    // ===========================================================
    // Resend Sms Message
    // ===========================================================

    public void resendMessage() {
        headerSubIcon.setOnClickListener(null);
        if (entityUri != null) {
            SmsSenderHelper.reSendSmsMessage(getActivity(), entityUri, null, null);
        }
        Toast.makeText(getActivity(), "Resend", Toast.LENGTH_SHORT).show();
    }

    // ===========================================================
    // Menu
    // ===========================================================

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Uri loadUri = null;
        if (savedInstanceState != null) {
            Log.d(TAG, "Restore onCreate savedInstanceState: " + savedInstanceState);
            loadUri = savedInstanceState.getParcelable(Intents.EXTRA_DATA_URI);
        }
        Log.d(TAG, "onActivityCreated");
        // Load Data
        if (loadUri != null) {
            loadEntity(loadUri);
        } else {
            loadEntity(getActivity().getIntent());
        }
    }

    // ===========================================================
    // Lyfe Cycle
    // ===========================================================

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Intents.EXTRA_DATA_URI, entityUri.toString());
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Save onSaveInstanceState : " + outState);
    }

    private void registerContentObserver(Uri entityUri) {
        if (entityUri != null) {
            ContentResolver cr = getActivity().getContentResolver();
            cr.registerContentObserver(entityUri, false, observer);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        registerContentObserver(entityUri);
    }

    @Override
    public void onPause() {
        super.onPause();
        ContentResolver cr = getActivity().getContentResolver();
        cr.unregisterContentObserver(observer);
    }
    // ===========================================================
    // User Action
    // ===========================================================

    private void onLongClickSmsMessage() {
        if (View.VISIBLE != messageDetailTextView.getVisibility()) {
            messageDetailTextView.setVisibility(View.VISIBLE);
        }
    }

    // ===========================================================
    // Load Data
    // ===========================================================

    private void loadEntity(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            loadEntity(uri);
        } else {
            Log.w(TAG, "Could not load entity without Uri : " + intent);
        }
    }

    public void loadEntity(Uri entityUri) {
        Log.d(TAG, "Load entity Uri : " + entityUri);
        this.entityUri = entityUri;
        ContentResolver cr = mContext.getContentResolver();
        String selection = null;
        String[] selectionArgs = null;
        Cursor cursor = cr.query(entityUri, SmsLogDatabase.SmsLogColumns.ALL_COLS, selection, selectionArgs, null);
        Log.d(TAG, "loadEntity : " + Arrays.toString(SmsLogDatabase.SmsLogColumns.ALL_COLS));
        try {
            if (cursor.moveToFirst()) {
                loadEntity(cursor);
            }
        } finally {
            cursor.close();
        }
        // Register Cursor Observer
        registerContentObserver(entityUri);
    }

    public void loadEntity(Cursor cursor) {
        if (helper.isNotInit) {
            helper.initWrapper(cursor);
        }
        // Phone
        String phone = helper.getSmsLogPhone(cursor);
        photoHeader.subEltPhoneTextView.setText(phone);

        // Action
        MessageActionEnum action = helper.getSmsMessageActionEnum(cursor);
        String actionLabel = MessageActionEnumLabelHelper.getString(mContext, action);
        photoHeader.subEltNameTextView.setText(actionLabel);

        // Messages Sizes    helper.getM
        String smsMessage = helper.getMessage(cursor);
        int msgSize = smsMessage == null ? 0 : smsMessage.length();
        String message = getString(R.string.smslog_message_size, msgSize);
        messageTextView.setText(message);

        messageDetailTextView.setText(smsMessage);
        // Bind Value
        SmsLogTypeEnum smLogType = helper.getSmsLogType(cursor);
        Drawable iconType = mResources.getCallTypeDrawable(smLogType);
        smsTypeImageView.setImageDrawable(iconType);
        // Time SmsType
        long smsLogTime = helper.getSmsLogTime(cursor);
        switch (smLogType) {
            case SEND_ACK:
                smsLogTime = helper.getAckSendTimeInMs(cursor);
                break;
            case SEND_DELIVERY_ACK:
                smsLogTime = helper.getAckDeliveryTimeInMs(cursor);
                break;
            case SEND_ERROR:
                headerSubIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        resendMessage();
                    }
                });
                break;
        }
        // Time
        String smsTypeTime = DateUtils.formatDateRange(mContext, smsLogTime, smsLogTime,
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR
        );
        smsTypeTimeTextView.setText(smsTypeTime);
        // Acknowledge
        Log.d(TAG, "getAckReSendMsgCount Idx : " + helper.ackReSendMsgCountIdx );
        int resendCount = helper.getAckReSendMsgCount(cursor);
        dispayReSendCount(resendCount);

        // Params
        Bundle msgParams = helper.getMessageParamsAsBundle(cursor);
        bindMessageParams(msgParams);

        //TODO Name Person
        SmsLogSideEnum smsLogSide = helper.getSmsLogSideEnum(cursor);
        cacheNameFinder.setTextViewPersonNameByPhone(photoHeader.mainActionNameTextView, phone, smsLogSide);

        // Photo
        photoCache.loadPhoto(getActivity(), photoHeader.photoImageView, null, phone);
    }


    private void bindMessageParams(String msgParams) {
        if (msgParams != null && msgParams.length() > 0) {
            // Decode Params
            BundleEncoderAdapter dest = new BundleEncoderAdapter();
            ParamEncoderHelper.decodeMessageAsMap(dest, msgParams);
            Bundle bundle = dest.getMap();
            // Bind to Screen
            bindMessageParams(bundle);
        }
    }

    private void bindMessageParams(Bundle bundle) {
        Log.d(TAG, "Read Json Params : " + bundle);
        if (bundle != null && bundle.size() > 0) {
            // Clean of previous Parent
            paramListView.removeAllViewsInLayout();
            // Insert Param In View
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            for (String key : bundle.keySet()) {
                MessageParamEnum param = MessageParamEnum.getByDbFieldName(key);
                // MessageParamField.
                Log.d(TAG, "Read Json Params : " + key + " = " + bundle.get(key));
                if (param == null) {
                    // No ref of this param
                    String val = String.valueOf(bundle.get(key));
                    // Not necessary to display because it should be in the multifield
                    Log.d(TAG, "Ignore display key " + key + " = " + val);
                    //   addParamTextLabel(layoutInflater, key, val);
                } else {
                    String label = MessageParamEnumLabelHelper.getString(getActivity(), param, bundle);
                    if (label != null) {
                        addParamTextLabel(layoutInflater, key, label);
                    } else {
                        // No ref of this param
                        String val = String.valueOf(bundle.get(key));
                        addParamTextLabel(layoutInflater, key, val);
                    }
                }
            }
        }
    }

    private void addParamTextLabel(LayoutInflater layoutInflater, String key, String val) {
        // Create Layout
        Log.d(TAG, "JSONObject key : " + key + " = " + val);
        View convertView = layoutInflater.inflate(R.layout.smslog_view_list_param_item, null);
        TextView keyTextView = (TextView) convertView.findViewById(R.id.smslog_list_item_param_key);
        TextView valueTextView = (TextView) convertView.findViewById(R.id.smslog_list_item_param_value);
        // Define View Values
        keyTextView.setText(key);
        valueTextView.setText(val);
        // Add To Parent View
        paramListView.addView(convertView);

    }


    // ===========================================================
    // Others
    // ===========================================================

}
