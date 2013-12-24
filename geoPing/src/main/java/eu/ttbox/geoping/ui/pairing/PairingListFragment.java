package eu.ttbox.geoping.ui.pairing;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.domain.pairing.PairingHelper;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.slave.GeoPingSlaveLocationService;

public class PairingListFragment extends Fragment {

	private static final String TAG = "PairingListFragment";

	private static final int PAIRING_LIST_LOADER = R.id.config_id_pairing_list_loader;

	// Constant
	private static final String PAIRING_SORT_DEFAULT = String.format("%s DESC, %s DESC", PairingColumns.COL_NAME, PairingColumns.COL_PHONE);

	private static final int EDIT_ENTITY = 0;

	// binding
	private ListView listView;
    private ImageButton lockPairingButton;
    private ImageButton lockPairingButtonHelp;
    private Button addEntityButton;
    private Button addEntityButtonHelp;
    // init
    private SharedPreferences sharedPreferences;
	private PairingListAdapter listAdapter;

	private final AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
			Log.w(TAG, "OnItemClickListener on Item at Position=" + position + " with id=" + id);
			Cursor cursor = (Cursor) parent.getItemAtPosition(position);
			PairingHelper helper = new PairingHelper().initWrapper(cursor);
			String entityId = helper.getPairingIdAsString(cursor);
			onEditEntityClick(entityId);
		}
	};

    private final OnClickListener mOnLockPairingClickListener =new OnClickListener() {
        @Override
        public void onClick(View v) {
            onLockPairingClick(v);
        }
    };
    // ===========================================================
    // Constructor
    // ===========================================================
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		View v = inflater.inflate(R.layout.pairing_list, container, false);
        // Service
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		// Bindings
		listView = (ListView) v.findViewById(android.R.id.list);
		listView.setEmptyView(v.findViewById(android.R.id.empty));
		addEntityButton = (Button) v.findViewById(R.id.add_pairing_button);
		addEntityButtonHelp = (Button) v.findViewById(R.id.add_pairing_button_help);
        lockPairingButton= (ImageButton)v.findViewById(R.id.lock_pairing_button);
        lockPairingButton.setOnClickListener(mOnLockPairingClickListener);
        lockPairingButtonHelp= (ImageButton)v.findViewById(R.id.lock_pairing_button_help);
        lockPairingButtonHelp.setOnClickListener(mOnLockPairingClickListener);
        // init
		listAdapter = new PairingListAdapter(getActivity(), null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		listView.setAdapter(listAdapter);
		listView.setOnItemClickListener(mOnClickListener);
		// Listener
		OnClickListener addPairingOnClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				onAddEntityClick(v);
			}
		};
		addEntityButton.setOnClickListener(addPairingOnClickListener);
		addEntityButtonHelp.setOnClickListener(addPairingOnClickListener);
		// Intents
        registerForContextMenu(listView);
		Log.d(TAG, "Binding end");

		return v;
	}



	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		Log.d(TAG, "onActivityCreated");
		// Load data
		getActivity().getSupportLoaderManager().initLoader(PAIRING_LIST_LOADER, null, pairingLoaderCallback);
        loadLockPairingData();
	}

    private void loadLockPairingData() {
        // Load Lock Config
        boolean isAuthNewPairing =  sharedPreferences.getBoolean(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING, true);
        initLockPairingButton(isAuthNewPairing);

    }
    // ===========================================================
    // LifeCycle
    // ===========================================================

    @Override
    public void onResume() {
        super.onResume();
        loadLockPairingData();
    }


    // ===========================================================
    // List Popup Menu
    // ===========================================================

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == android.R.id.list) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;

            Cursor cursor = (Cursor) lv.getItemAtPosition(acmi.position);
            PairingHelper helper = new PairingHelper().initWrapper(cursor);
            menu.add(Menu.NONE, R.id.menu_gcm_message, Menu.NONE, R.string.menu_geoping);
            menu.add(Menu.NONE, R.id.menu_edit, Menu.NONE, R.string.menu_edit);
            menu.add(Menu.NONE, R.id.menu_delete, Menu.NONE, R.string.menu_delete);

            //
            String titleMenu = helper.getDisplayName(cursor);
            if (titleMenu == null) {
                titleMenu = helper.getPairingPhone(cursor);
            }
            menu.setHeaderTitle(titleMenu);
            menu.setHeaderIcon(R.drawable.ic_device_access_secure);
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cursor = (Cursor) listView.getItemAtPosition(info.position);
        PairingHelper helper = new PairingHelper().initWrapper(cursor);
        //TODO implement menu
        switch (item.getItemId()) {
            case R.id.menu_gcm_message: {
                String phoneNumber = helper.getDisplayName(cursor);
                GeoPingSlaveLocationService.runFindLocationAndSendInService(getActivity(), MessageActionEnum.LOC_DECLARATION,
                        new String[]{phoneNumber}, null, null);
            }
            return true;

            case R.id.menu_edit: {
                String entityId = helper.getPairingIdAsString(cursor);
                onEditEntityClick(entityId);
            }
            return true;
            case R.id.menu_delete: {
                // TODO https://www.timroes.de/2013/09/23/enhancedlistview-swipe-to-dismiss-with-undo/
                // https://github.com/timroes/EnhancedListView/blob/master/EnhancedListView/src/main/res/layout/undo_popup.xml
                //
                // TODO https://code.google.com/p/romannurik-code/source/browse/misc/undobar/src/com/example/android/undobar/UndoBarController.java
                // TODO https://android.googlesource.com/platform/developers/samples/android/+/master/ui/actionbar/DoneBar/DoneBar/src/main/java/com/example/android/donebar/DoneBarActivity.java
                String entityId = helper.getPairingIdAsString(cursor);
                Uri entityUri = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI, entityId);
                int deleteCount = getActivity().getContentResolver().delete(entityUri, null, null);
            }
            return true;
            default:
                return super.onContextItemSelected(item);
        }

    }

    // ===========================================================
    // Click Listener
    // ===========================================================


	public void onAddEntityClick(View v) {
		Intent intent = Intents.editPairing(getActivity(), null);
		startActivityForResult(intent, EDIT_ENTITY);
	}

	public void onEditEntityClick(String entityId) {
		Intent intent = Intents.editPairing(getActivity(), entityId);
		startActivityForResult(intent, EDIT_ENTITY);
	}

	protected void handleIntent(Intent intent) {
		if (intent == null) {
			return;
		}
		if (Log.isLoggable(TAG, Log.DEBUG)) {
			Log.d(TAG, "handleIntent for action : " + intent.getAction());
		}
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (EDIT_ENTITY):
			if (resultCode == Activity.RESULT_OK) {
				getActivity().getSupportLoaderManager().restartLoader(PAIRING_LIST_LOADER, null, pairingLoaderCallback);
			}
		}
	}

    private void onLockPairingClick(View v) {
        boolean isAuthNewPairing = sharedPreferences.getBoolean(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING, true);
        isAuthNewPairing = !isAuthNewPairing;
        final SharedPreferences.Editor localEdit = sharedPreferences.edit();
        localEdit.putBoolean(AppConstants.PREFS_AUTHORIZE_GEOPING_PAIRING, isAuthNewPairing);
        localEdit.commit();
        initLockPairingButton(isAuthNewPairing);
    }

    private void initLockPairingButton(boolean isAuthNewPairing) {
        if (isAuthNewPairing) {
            lockPairingButton.setImageResource(R.drawable.ic_device_access_not_secure);
            lockPairingButtonHelp.setImageResource(R.drawable.ic_device_access_not_secure);
            addEntityButton.setEnabled(true);
            addEntityButtonHelp.setEnabled(true);
        } else {
            lockPairingButton.setImageResource(R.drawable.ic_device_access_secure);
            lockPairingButtonHelp.setImageResource(R.drawable.ic_device_access_secure);
            addEntityButton.setEnabled(false);
            addEntityButtonHelp.setEnabled(false);
        }
    }

	// ===========================================================
	// Loader
	// ===========================================================

	private final LoaderManager.LoaderCallbacks<Cursor> pairingLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			Log.d(TAG, "onCreateLoader");
			String sortOrder = PAIRING_SORT_DEFAULT;
			String selection = null;
			String[] selectionArgs = null;
			String queryString = null;
			// Loader
			CursorLoader cursorLoader = new CursorLoader(getActivity(), PairingProvider.Constants.CONTENT_URI, null, selection, selectionArgs, sortOrder);
			return cursorLoader;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

			// Display List
			listAdapter.changeCursor(cursor);
			cursor.setNotificationUri(getActivity().getContentResolver(), PairingProvider.Constants.CONTENT_URI);
			// Display Counter
			int count = 0;
			if (cursor != null) {
				count = cursor.getCount();
			}
			Log.d(TAG, "onLoadFinished with result count : " + count);

		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			listAdapter.changeCursor(null);
		}

	};

}
