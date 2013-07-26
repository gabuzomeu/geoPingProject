package eu.ttbox.geoping.ui.slidingmenu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.cloud.backend.android.sample.guestbook.GuestbookActivity;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivityBase;

import eu.ttbox.geoping.MainActivity;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.core.VersionUtils;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.person.PersonDatabase.PersonColumns;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.ui.billing.ExtraFeaturesActivity;
import eu.ttbox.geoping.ui.geofence.GeofenceListActivity;
import eu.ttbox.geoping.ui.map.ShowMapActivity;
import eu.ttbox.geoping.ui.pairing.PairingListActivity;
import eu.ttbox.geoping.ui.person.PersonListActivity;
import eu.ttbox.geoping.ui.prefs.GeoPingPrefActivity;
import eu.ttbox.geoping.ui.slidingmenu.SlidingPersonListAdapter.SlidingMenuPersonListItemListener;
import eu.ttbox.geoping.ui.smslog.SmsLogListActivity;


public class GeopingSlidingItemMenuFragment extends Fragment {

    private static final String TAG = "GeopingSlidingItemMenuFragment";

    private static final int SLIDINGMENU_PERSON_LIST_LOADER = R.id.config_id_slidingmenu_person_list_loader;
    private static final String PERSON_SORT_DEFAULT = String.format("%s DESC, %s DESC", PersonColumns.COL_NAME, PersonColumns.COL_PHONE);

    private ScrollView slidingmenuContainer;
    private TextView personListTitleTextView;
    private ListView personListView;
    private SlidingPersonListAdapter personAdpater;

    private SparseArray<SlindingMenuItemView> menuItems;
    private static int[] menuIds = new int[] { //
      R.id.menuMap, R.id.menu_track_person, R.id.menu_pairing,R.id.menu_geofence , R.id.menu_smslog//
      , R.id.menu_settings, R.id.menu_extra_feature //
      , R.id.menu_gcm_message //
     };

    // ===========================================================
    // Constructors
    // ===========================================================

    public GeopingSlidingItemMenuFragment() {
        super();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.slidingmenu_item_list, null);
        slidingmenuContainer = (ScrollView) v.findViewById(R.id.slidingmenu_container);
        personListTitleTextView = (TextView) v.findViewById(R.id.slidingmenu_person_list_title);
        personListView = (ListView) v.findViewById(R.id.slidingmenu_person_list);
        // Register Menu Item
        OnClickListener menuItemOnClickListener = new OnClickListener() {

            @Override
            public void onClick(View v) {
                int itemId = v.getId();
                onSlidingMenuSelectItem(itemId);
            }

        };
        // Register listener
        SparseArray<SlindingMenuItemView> menuItems = new SparseArray<SlindingMenuItemView>();
        for (int menuId : menuIds) {
            SlindingMenuItemView menuItem = (SlindingMenuItemView) v.findViewById(menuId);
            menuItems.put(menuId, menuItem);
            if (menuItem != null) {
                menuItem.setOnClickListener(menuItemOnClickListener);
            }
            // Clear All Selector, just in case
            if (menuItem.isSlidingMenuSelectedVisible()) {
                menuItem.setSlidingMenuSelectedVisible(false);
            }
        }
        this.menuItems = menuItems;

        return v;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Binding Menu
        FragmentActivity activity = getActivity();
        Class<? extends Activity> activityClass = (Class<? extends Activity>) activity.getClass();
        for (int menuId : menuIds) {
            Class<? extends Activity> menuItemClass = getActivityClassByItemId(menuId);
            if (menuItemClass != null && activityClass.isAssignableFrom(menuItemClass)) {
                SlindingMenuItemView menuItem = menuItems.get(menuId);
                menuItem.setSlidingMenuSelectedVisible(true);
            }
        }

        // Binding Person
        personAdpater = new SlidingPersonListAdapter(getActivity(), null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        personListView.setAdapter(personAdpater);
        // Send Sms Listener
        personAdpater.setPersonListItemListener(new SlidingMenuPersonListItemListener() {

            @Override
            public void onClickPing(View v, long personId, String phoneNumber) {
                Context context = getActivity();
                context.startService(Intents.sendSmsGeoPingRequest(context, phoneNumber));
            }
        });
        // person Item Click
        AdapterView.OnItemClickListener personListViewOnItemClickListener = new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Log.w(TAG, "OnItemClickListener on Item at Position=" + position + " with id=" + id);
                Cursor cursor = (Cursor) parent.getItemAtPosition(position);
                PersonHelper helper = new PersonHelper().initWrapper(cursor);
                long personId = helper.getPersonId(cursor);
                String phoneNumber = helper.getPersonPhone(cursor);
                // Close Menu
                switchFragment();
                // Start Activity
                Intents.startActivityShowOnMapPerson(v, getActivity(), personId, phoneNumber);
                // onEditEntityClick(entityId);
            }
        };
        personListView.setOnItemClickListener(personListViewOnItemClickListener);

        // Load Data
        activity.getSupportLoaderManager().initLoader(SLIDINGMENU_PERSON_LIST_LOADER, null, slidingmenuPersonLoaderCallback);
    }

    // ===========================================================
    // Scroll View
    // ===========================================================

    private void showScrollViewOnTop() {
        // Ugly way to display always the top
        slidingmenuContainer.post(new Runnable() {
            public void run() {
                // slidingmenuContainer.scrollTo(0, 0);
                slidingmenuContainer.fullScroll(ScrollView.FOCUS_UP);
            }
        });
    }

    // ===========================================================
    // Sliding Menu Select Item
    // ===========================================================

    @SuppressLint("InlinedApi")
    private void clearActivityHistoryStack(Intent intentOption, boolean isRootActivity) {
        if (isRootActivity) {
            if (VersionUtils.isHc11) {
                intentOption.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Log.d(TAG, "Clear ActivityHistoryStack for Intent : " + intentOption);
            }
        }
    }

    private boolean onSlidingMenuSelectItem(int itemId) {
        Context context = getActivity();
        boolean isRootActivity = false;
        switch (itemId) {
        case R.id.menu_settings:
        case R.id.menu_pairing:
        case R.id.menu_geofence:
        case R.id.menu_track_person:
        case R.id.menu_smslog:
            isRootActivity = true;
        case R.id.menu_extra_feature:
        case R.id.menu_gcm_message:
        case R.id.menuMap:
            Class<? extends Activity> intentClass = getActivityClassByItemId(itemId);
            if (intentClass != null) {
                switchFragment();
                // Activity
                Class activityClass = getActivity().getClass();
                if (!activityClass.isAssignableFrom(intentClass)) {
                    Intent intentOption = new Intent(context, intentClass);
                    clearActivityHistoryStack(intentOption, isRootActivity);
                    // Create Stacks
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
                    stackBuilder.addParentStack(MainActivity.class);
                    stackBuilder.addNextIntent(new Intent(getActivity(), MainActivity.class));
                    stackBuilder.addNextIntent(intentOption);
                    stackBuilder.startActivities();
                }
                return true;
            }
            return false;

        case R.id.menuAppComment:
            switchFragment();
            Intents.startActivityAppMarket(context);
            return true;

            // case R.id.menuAppShare:
            // Intent intentAppShare = new Intent(Intent.ACTION_SEND);
            // intentAppShare.putExtra(Intent.EXTRA_TEXT,
            // "market://details?id=eu.ttbox.geoping");
            // return true;

        }
        return false;
    }

    private Class<? extends Activity> getActivityClassByItemId(int itemId) {
        switch (itemId) {
        case R.id.menu_settings:
            return GeoPingPrefActivity.class;
        case R.id.menuMap:
            return ShowMapActivity.class;
        case R.id.menu_pairing:
            return PairingListActivity.class;
        case R.id.menu_geofence:
            return GeofenceListActivity.class;
        case R.id.menu_track_person:
            return PersonListActivity.class;
        case R.id.menu_smslog:
            return SmsLogListActivity.class;
        case R.id.menu_extra_feature:
            return ExtraFeaturesActivity.class;
        case R.id.menu_gcm_message:
             return GuestbookActivity.class;
        default:
            return null;
        }
    }

    // the meat of switching the above fragment
    private void switchFragment() {
        if (getActivity() == null)
            return;

        if (getActivity() instanceof SlidingActivityBase) {
            SlidingActivityBase fca = (SlidingActivityBase) getActivity();
            fca.showContent();
        }
    }

    // ===========================================================
    // Slinding Menu Person Item
    // ===========================================================

    // ===========================================================
    // Loader
    // ===========================================================

    private final LoaderManager.LoaderCallbacks<Cursor> slidingmenuPersonLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

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
            // Display Counter
            int count = 0;
            if (cursor != null) {
                count = cursor.getCount();
            }
            Log.d(TAG, "onLoadFinished with result count : " + count);
            if (count > 0) {
                if (View.VISIBLE != personListTitleTextView.getVisibility()) {
                    personListTitleTextView.setVisibility(View.VISIBLE);
                }
                // Scale it
                ViewGroup.LayoutParams personlayoutParams = personListView.getLayoutParams();
                float scale = getResources().getDisplayMetrics().density;
                Log.d(TAG, "personListView LayoutParams Before: " + personlayoutParams.height);
                final int personItemHeight = 70;
                personlayoutParams.height = (int) (count * scale * personItemHeight);
                personListView.setLayoutParams(personlayoutParams);
                Log.d(TAG, "personListView LayoutParams Before: " + personlayoutParams.height);
            } else { 
                if (View.GONE != personListTitleTextView.getVisibility()) {
                    personListTitleTextView.setVisibility(View.GONE);
                }

            }

            // Display List
            personAdpater.changeCursor(cursor);
            cursor.setNotificationUri(getActivity().getContentResolver(), PersonProvider.Constants.CONTENT_URI);
            // Ugly way to display always the top
            showScrollViewOnTop();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            personAdpater.changeCursor(null);
        }

    };

}
