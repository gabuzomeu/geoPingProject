package eu.ttbox.geoping.ui.person;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ListView;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.person.PersonDatabase.PersonColumns;
import eu.ttbox.geoping.domain.person.PersonHelper;

public class PersonListFragment extends Fragment {

    private static final String TAG = "PersonListFragment";

    private static final int PERSON_LIST_LOADER = R.id.config_id_person_list_loader;

    // Constant
    private static final String PERSON_SORT_DEFAULT = String.format("%s DESC, %s DESC", PersonColumns.COL_NAME, PersonColumns.COL_PHONE);

    private static final int EDIT_ENTITY = 0;

    // binding
    private ListView listView;

    // init
    private PersonListAdapter listAdapter;

    private final AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            Log.w(TAG, "OnItemClickListener on Item at Position=" + position + " with id=" + id);
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            PersonHelper helper = new PersonHelper().initWrapper(cursor);
//			String entityId = helper.getPersonIdAsString(cursor);
            long personId = helper.getPersonId(cursor);
            String phoneNumber = helper.getPersonPhone(cursor);
            Intents.startActivityShowOnMapPerson(v, getActivity(), personId, phoneNumber);
        }
    };

    private final PersonListAdapter.PersonListItemListener mListAdapterListListener = new PersonListAdapter.PersonListItemListener() {

        @Override
        public void onClickPing(View v, long personId, String phoneNumber) {
            Context context = getActivity();
            context.startService(Intents.sendSmsGeoPingRequest(context, phoneNumber));
        }

        @Override
        public void onClickEditPerson(View v, long personId, String phoneNumber) {
            String entityId = String.valueOf(personId);
            onEditEntityClick(entityId);
        }
    };

    // ===========================================================
    // Constructors
    // ===========================================================

    public PersonListFragment() {
        super();
//		Log.i(TAG, "---------- Constructor PersonListFragment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.track_person_list, container, false);
        Log.d(TAG, "onCreateView");
//		Log.i(TAG, "---------- onCreateView PersonListFragment");

        // Bindings
        listView = (ListView) v.findViewById(R.id.personlist_list);
        listView.setEmptyView(v.findViewById(R.id.personlist_empty));
        // Button
        Button addPersonButton = (Button) v.findViewById(R.id.add_track_person_button);
        Button addPersonButtonHelp = (Button) v.findViewById(R.id.add_track_person_button_help);
        OnClickListener addPersonOnClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddEntityClick(v);
            }
        };
        addPersonButton.setOnClickListener(addPersonOnClickListener);
        addPersonButtonHelp.setOnClickListener(addPersonOnClickListener);
        // init
        listAdapter = new PersonListAdapter(getActivity(), null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(mOnClickListener);
        listAdapter.setPersonListItemListener(mListAdapterListListener);
        // Empty List
        // emptyListView = (TextView) v.findViewById(android.R.id.empty);
        // listView.setEmptyView(emptyListView);
        Log.d(TAG, "Binding end");
        // Intents
        registerForContextMenu(listView);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        getActivity().getSupportLoaderManager().initLoader(PERSON_LIST_LOADER, null, personLoaderCallback);

    }

    // ===========================================================
    // Click Event
    // ===========================================================

    public void onAddEntityClick(View v) {
        Intent intent = Intents.editPerson(getActivity(), null);
        startActivityForResult(intent, EDIT_ENTITY);
    }

    public void onEditEntityClick(String entityId) {
        Intent intent = Intents.editPerson(getActivity(), entityId);
        startActivityForResult(intent, EDIT_ENTITY);
    }


    // ===========================================================
    // List Popup Menu
    // ===========================================================

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.personlist_list) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;

            Cursor cursor = (Cursor) lv.getItemAtPosition(acmi.position);
            PersonHelper helper = new PersonHelper().initWrapper(cursor);
            menu.add(Menu.NONE, R.id.menu_context_person_geoping_send, Menu.NONE, R.string.menu_geoping);
            menu.add(Menu.NONE, R.id.menuMap, Menu.NONE, R.string.menu_map);
            menu.add(Menu.NONE, R.id.menu_context_person_edit, Menu.NONE, R.string.menu_edit);
            menu.add(Menu.NONE, R.id.menu_context_person_delete, Menu.NONE, R.string.menu_delete);
            //
            String titleMenu = helper.getPersonDisplayName(cursor);
            if (titleMenu == null) {
                titleMenu = helper.getPersonPhone(cursor);
            }
            menu.setHeaderTitle(titleMenu);
            menu.setHeaderIcon(R.drawable.ic_action_user);

        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        Cursor cursor = (Cursor) listView.getItemAtPosition(info.position);
        PersonHelper helper = new PersonHelper().initWrapper(cursor);
        //TODO implement menu
        Log.d(TAG, "### onContextItemSelected : " + item);
        switch (item.getItemId()) {
            case R.id.menu_context_person_geoping_send: {
                String phoneNumber = helper.getPersonPhone(cursor);
                getActivity().startService(Intents.sendSmsGeoPingRequest(getActivity(), phoneNumber));
            }
            return true;
            case R.id.menuMap: {
                long personId = helper.getPersonId(cursor);
                String phoneNumber = helper.getPersonPhone(cursor);
                Intents.startActivityShowOnMapPerson(listView, getActivity(), personId, phoneNumber);
            }
            return true;
            case R.id.menu_context_person_edit: {
                String entityId = helper.getPersonIdAsString(cursor);
                onEditEntityClick(entityId);
            }
            return true;
            case R.id.menu_context_person_delete: {
                // TODO https://www.timroes.de/2013/09/23/enhancedlistview-swipe-to-dismiss-with-undo/
                // https://github.com/timroes/EnhancedListView/blob/master/EnhancedListView/src/main/res/layout/undo_popup.xml
                //
                // TODO https://code.google.com/p/romannurik-code/source/browse/misc/undobar/src/com/example/android/undobar/UndoBarController.java
                // TODO https://android.googlesource.com/platform/developers/samples/android/+/master/ui/actionbar/DoneBar/DoneBar/src/main/java/com/example/android/donebar/DoneBarActivity.java
                String entityId = helper.getPersonIdAsString(cursor);
                Uri entityUri = Uri.withAppendedPath(PersonProvider.Constants.CONTENT_URI, entityId);
                int deleteCount = getActivity().getContentResolver().delete(entityUri, null, null);
            }
            return true;
            default:
                return super.onContextItemSelected(item);
        }

    }
    // ===========================================================
    // Accessors
    // ===========================================================

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (EDIT_ENTITY):
                if (resultCode == Activity.RESULT_OK) {
                    reloadDataList();
                }
        }
    }

    public void reloadDataList() {
        getActivity().getSupportLoaderManager().restartLoader(PERSON_LIST_LOADER, null, personLoaderCallback);
    }

    // ===========================================================
    // Loader
    // ===========================================================

    private final LoaderManager.LoaderCallbacks<Cursor> personLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader");
            String sortOrder = PERSON_SORT_DEFAULT;
            String selection = null;
            String[] selectionArgs = null;
            String queryString = null;
            // Loader
            CursorLoader cursorLoader = new CursorLoader(getActivity(), PersonProvider.Constants.CONTENT_URI, null, selection, selectionArgs, sortOrder);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

            // Display List
            listAdapter.changeCursor(cursor);
            cursor.setNotificationUri(getActivity().getContentResolver(), PersonProvider.Constants.CONTENT_URI);
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
