package eu.ttbox.geoping.ui.pairing;

import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.actionbarsherlock.app.SherlockFragment;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.VersionUtils.AndroidPermissions;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.domain.pairing.PairingHelper;

public class PairingNotificationFragment extends SherlockFragment {

    private static final String TAG = "PairingNotificationFragment";

    // Constant
    private static final int PAIRING_NOTIF_LOADER = R.id.config_id_pairing_notif_loader;

    // Bindings
    private CompoundButton notifShutdown;
    private CompoundButton notifBatteryLow;
    private CompoundButton notifSimChange;
    private CompoundButton notifPhoneCall;

    // Alls
    private CompoundButton[] notifViews;

    // Instance
    private Uri entityUri;

    // ===========================================================
    // Constructors
    // ===========================================================

    private String getDatabaseColomnForView(CompoundButton v) {
        String result = null;
        switch (v.getId()) {
        case R.id.pairing_notification_shutdown:
            result = PairingColumns.COL_NOTIF_SHUTDOWN;
            break;
        case R.id.pairing_notification_battery_low:
            result = PairingColumns.COL_NOTIF_BATTERY_LOW;
            break;
        case R.id.pairing_notification_sim_change:
            result = PairingColumns.COL_NOTIF_SIM_CHANGE;
            break;
        case R.id.pairing_notification_phone_call:
            result = PairingColumns.COL_NOTIF_PHONE_CALL;
            break;
        default:
            Log.w(TAG, "No DB Column mapping for View : " + v);
            break;
        }
        return result;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.pairing_notification, container, false);

        // Bindings
        notifShutdown = (CompoundButton) v.findViewById(R.id.pairing_notification_shutdown);
        notifBatteryLow = (CompoundButton) v.findViewById(R.id.pairing_notification_battery_low);
        notifSimChange = (CompoundButton) v.findViewById(R.id.pairing_notification_sim_change);
        notifPhoneCall = (CompoundButton) v.findViewById(R.id.pairing_notification_phone_call);

        // Listeners
        notifViews = new CompoundButton[] { notifShutdown, notifBatteryLow, notifSimChange, notifPhoneCall };
        OnClickListener notifOnClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                CompoundButton check = (CompoundButton) v;
                String dbcol = getDatabaseColomnForView(check);
                if (dbcol != null) {
                    boolean isChecked = check.isChecked();
                    saveNotif(dbcol, isChecked);
                }
            }
        };
        for (CompoundButton view : notifViews) {
            view.setOnClickListener(notifOnClickListener);
        }
        // Security
        checkPermissionsForButtonVisibility();

        // Load Data
        loadEntity(getArguments());
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // ===========================================================
    // Check Permission
    // ===========================================================

    private void checkPermissionsForButtonVisibility() {
        // Sim Change
        if (!AndroidPermissions.isPermissionGranted(getActivity(), AndroidPermissions.READ_PHONE_STATE)) {
            notifSimChange.setVisibility(View.GONE);
        } else {
            notifSimChange.setVisibility(View.VISIBLE);
        }
        // Phone Call
        if (!AndroidPermissions.isPermissionGranted(getActivity(), AndroidPermissions.PROCESS_OUTGOING_CALLS)) {
            notifPhoneCall.setVisibility(View.GONE);
        } else {
            notifPhoneCall.setVisibility(View.VISIBLE);
        }

    }

    // ===========================================================
    // Bussiness
    // ===========================================================

    private void saveNotif(String column, boolean value) {
        SpyNotifUpdaterAsyncTask updater = new SpyNotifUpdaterAsyncTask(value);
        updater.doInBackground(column);
    }

    // ===========================================================
    // Accessor
    // ===========================================================
    public class SpyNotifUpdaterAsyncTask extends AsyncTask<String, Void, Integer> {
        Boolean value;

        public SpyNotifUpdaterAsyncTask(Boolean value) {
            super();
            this.value = value;
        }

        @Override
        protected Integer doInBackground(String... params) {
            String columnName = params[0];
            ContentValues values = new ContentValues();
            values.put(columnName, value);
            int rowCount = getActivity().getContentResolver().update(entityUri, values, null, null);
            Log.d(TAG, "EventSpy Update " + entityUri + " : Column " + columnName + " = " + value);
            return Integer.valueOf(rowCount);
        }
    }

    // ===========================================================
    // LoaderManager
    // ===========================================================

    private void loadEntity(Bundle args) {
        if (args != null && args.containsKey(Intents.EXTRA_DATA_URI)) {
            this.entityUri = Uri.parse(args.getString(Intents.EXTRA_DATA_URI));
            getActivity().getSupportLoaderManager().initLoader(PAIRING_NOTIF_LOADER, args, pairingNotifLoaderCallback);
        }
    }

    private final LoaderManager.LoaderCallbacks<Cursor> pairingNotifLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader");
            String entityId = args.getString(Intents.EXTRA_DATA_URI);
            Uri entityUri = Uri.parse(entityId);
            // Loader
            CursorLoader cursorLoader = new CursorLoader(getActivity(), entityUri, PairingColumns.NOTIFS_COLS, null, null, null);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.d(TAG, "onLoadFinished with cursor result count : " + cursor.getCount());
            // Display List
            if (cursor.moveToFirst()) {
                // Data
                PairingHelper helper = new PairingHelper().initWrapper(cursor);
                helper.setCompoundButtonWithIdx(notifShutdown, cursor, helper.notifShutdown);
                helper.setCompoundButtonWithIdx(notifBatteryLow, cursor, helper.notifBatteryLow);
                helper.setCompoundButtonWithIdx(notifSimChange, cursor, helper.notifSimChange);
                helper.setCompoundButtonWithIdx(notifPhoneCall, cursor, helper.notifPhoneCall);
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            for (CompoundButton view : notifViews) {
                view.setChecked(false);
            }
        }

    };

    class PairingNotificationContentObserver extends ContentObserver {

        PairingNotificationContentObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "*** PairingNotificationContentObserver onChange Observer");
            // TODO Valeus
        }
    }
}
