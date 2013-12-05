package eu.ttbox.geoping.ui.map;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.GeoPoint;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import eu.ttbox.geoping.BuildConfig;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.geotrack.GeoTrackDatabase;
import eu.ttbox.geoping.domain.model.Person;
import eu.ttbox.geoping.domain.person.PersonDatabase;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.ui.map.core.MapConstants;
import eu.ttbox.geoping.ui.map.geofence.GeofenceEditOverlay;
import eu.ttbox.geoping.ui.map.timeline.RangeTimelineValue;
import eu.ttbox.geoping.ui.map.timeline.RangeTimelineView;
import eu.ttbox.geoping.ui.map.track.GeoTrackOverlay;
import eu.ttbox.geoping.ui.map.track.dialog.SelectGeoTrackDialog;
import eu.ttbox.osm.ui.map.OsmMapFragment;

public class ShowMapFragmentV2 extends OsmMapFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ShowMapFragmentV2";

    private static final int GEOTRACK_PERSON_LOADER = R.id.config_id_map_geotrack_person_loader;

    // Constant
    /**
     * This number depend of previous menu
     */
    private int MENU_LAST_ID = 3;
    // Config
    private boolean geocodingAuto = true;
    private ConcurrentHashMap<String, GeoTrackOverlay> geoTrackOverlayByUser = new ConcurrentHashMap<String, GeoTrackOverlay>();

    private GeofenceEditOverlay geofenceListOverlay;

    // View
    private RangeTimelineView rangeTimelineBar;
    // Listener
    private StatusReceiver mStatusReceiver;
    // Service
    private SharedPreferences sharedPreferences;
    private SharedPreferences privateSharedPreferences;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    // Instance value
    private RangeTimelineValue rangeTimelineValue;

//    private handleCenter

    // ===========================================================
    // Message Handler
    // ===========================================================

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == GeofenceEditOverlay.MENU_CONTEXTUAL_EDIT) {
                Log.i(TAG, "GeofenceEditOverlay MENU CONTEXTUAL EDIT");
                ActionMode.Callback actionModeCallBack = geofenceListOverlay.getMenuActionCallback();
                ActionBarActivity activity = (ActionBarActivity) getActivity();
                ActionMode actionMode = activity.startSupportActionMode(actionModeCallBack);
            }
        }
    };


    // ===========================================================
    // Constructors
    // ===========================================================

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "### ### ### ### ### onCreateView call ### ### ### ### ###");
        View v = inflater.inflate(R.layout.map, container, false);

        // Services
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        privateSharedPreferences = getActivity().getSharedPreferences(MapConstants.PREFS_NAME, Context.MODE_PRIVATE);
        // Config
        geocodingAuto = sharedPreferences.getBoolean(AppConstants.PREFS_GEOPOINT_GEOCODING_AUTO, true);

        // Osm
        // ----------
        initMap();
        ViewGroup mapViewContainer = (ViewGroup) v.findViewById(R.id.mapViewContainer);
        mapViewContainer.addView((View) mapView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        // Overlay
        // ----------
        onRestoreSaveInstanceState(savedInstanceState);

        // Map Init Center
        // ----------
        // TODO ?    onResumeCenterOnLastPosition();

        // Service
        mStatusReceiver = new StatusReceiver();
        // Range Seek Bar
        // ---------------
        rangeTimelineBar = (RangeTimelineView) v.findViewById(R.id.map_timeline_bar);
        rangeTimelineValue = new RangeTimelineValue(rangeTimelineBar.getAbsoluteMinValue(), rangeTimelineBar.getAbsoluteMaxValue());
        rangeTimelineBar.setOnRangeTimelineChangeListener(onRangeTimelineValuesChangeListener);

        return v;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "### ### ### ### ### onActivityCreated call ### ### ### ### ###");
        super.onActivityCreated(savedInstanceState);
        loadDefaultDatas();
    }

    public void loadDefaultDatas() {
        // Query
        getActivity().getSupportLoaderManager().initLoader(GEOTRACK_PERSON_LOADER, null, geoTrackPersonLoaderCallback);
        // Handle Intents
        // Call onResume handleIntent(getActivity().getIntent());
    }


    // ===========================================================
    // Overwide
    // ===========================================================

    @Override
    public ITileSource getPreferenceMapViewTile() {
        return getPreferenceMapViewTileSource(privateSharedPreferences);
    }


    // ===========================================================
    // Life Cycle
    // ===========================================================
    public void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        Log.d(TAG, String.format("### Handle Intent for action %s : %s", action, intent));
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "### handleIntent : " + intent);
            Intents.printExtras(TAG, intent.getExtras());
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            String phone = intent.getStringExtra(Intents.EXTRA_SMS_PHONE);
            Bundle bundle = intent.getExtras();
            if (bundle.containsKey(GeoTrackDatabase.GeoTrackColumns.COL_LATITUDE_E6) && bundle.containsKey(GeoTrackDatabase.GeoTrackColumns.COL_LONGITUDE_E6)) {
                int latE6 = intent.getIntExtra(GeoTrackDatabase.GeoTrackColumns.COL_LATITUDE_E6, Integer.MIN_VALUE);
                int lngE6 = intent.getIntExtra(GeoTrackDatabase.GeoTrackColumns.COL_LONGITUDE_E6, Integer.MIN_VALUE);
                int accuracy = intent.getIntExtra(GeoTrackDatabase.GeoTrackColumns.COL_ACCURACY, -1);
                Log.d(TAG, String.format("### handleIntent on Map Phone [%s] for center (%s, %s) ", phone, latE6, lngE6));
                if (Integer.MIN_VALUE != latE6 && Integer.MIN_VALUE != lngE6) {
                    centerOnPersonPhone(phone, latE6, lngE6, accuracy);
                }
            } else {
                centerOnPersonPhone(phone);
            }
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "### ### ### ### ### onDestroy call ### ### ### ### ###");
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }


    @Override
    public void onResume() {
        Log.d(TAG, "### ### ### ### ### onResume call ### ### ### ### ###");

        super.onResume();
        // read preference
        restoreMapPreference(privateSharedPreferences);
        // Service
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_NEW_GEOTRACK_INSERTED);
        getActivity().registerReceiver(mStatusReceiver, filter);

        // Intent
        handleIntent(getActivity().getIntent());
    }


    @Override
    public void restoreMapPreference(SharedPreferences prefs) {
        super.restoreMapPreference(prefs);
        // long saveDateInMs = prefs.getLong(eu.ttbox.osm.ui.map.MapConstants.PREFS_SAVE_DATE_IN_MS, -1);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "### ### ### ### ### onPause call ### ### ### ### ###");
        // Service
        getActivity().unregisterReceiver(mStatusReceiver);
        // save Preference
        saveMapPreference(privateSharedPreferences);
        super.onPause();
        // timer.cancel();
    }


    // ===========================================================
    // On save
    // ===========================================================


    // ===========================================================
    // Range Listener
    // ===========================================================


    /**
     * http://android.cyrilmottier.com/?p=98
     * http://stackoverflow.com/questions/3654492
     * /android-can-height-of-slidingdrawer-be-set-with-wrap-content
     */
    public void swichRangeTimelineBarVisibility() {
        if (rangeTimelineBar != null) {
            Log.d(TAG, "swichRangeTimelineBarVisibility : " + rangeTimelineBar.getVisibility());
            switch (rangeTimelineBar.getVisibility()) {
                case View.VISIBLE:
                    Animation animationOut = AnimationUtils.loadAnimation(this.getActivity(), R.anim.slide_out_up);
                    rangeTimelineBar.clearAnimation();
                    rangeTimelineBar.startAnimation(animationOut);
                    rangeTimelineBar.setVisibility(View.INVISIBLE);
                    rangeTimelineBar.resetSelectedValues();
                    break;
                case View.INVISIBLE:
                case View.GONE:
                    Animation animation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.slide_in_up);
                    rangeTimelineBar.clearAnimation();
                    rangeTimelineBar.startAnimation(animation);
                    rangeTimelineBar.setVisibility(View.VISIBLE);
                    onRangeGeoTrackValuesChangeListener.computeRangeValues();
                    break;
                default:
                    break;
            }

        }
    }

    private RangeTimelineView.OnRangeTimelineValuesChangeListener onRangeTimelineValuesChangeListener = new RangeTimelineView.OnRangeTimelineValuesChangeListener() {

        @Override
        public void onRangeTimelineValuesChanged(int minValue, int maxValue, boolean isRangeDefine) {
            rangeTimelineValue.minValue = minValue;
            rangeTimelineValue.maxValue = maxValue;
            rangeTimelineValue.isRangeDefine = isRangeDefine;
            for (GeoTrackOverlay geotrack : geoTrackOverlayByUser.values()) {
                geotrack.onRangeTimelineValuesChanged(minValue, maxValue, isRangeDefine);
            }
        }
    };

    private GeoTrackValuesChangeListener onRangeGeoTrackValuesChangeListener = new GeoTrackValuesChangeListener();

    private class GeoTrackValuesChangeListener implements GeoTrackOverlay.OnRangeGeoTrackValuesChangeListener {

        private int geotrackRangeMin = Integer.MAX_VALUE;
        private int geotrackRangeMax = Integer.MIN_VALUE;

        public void onRangeGeoTrackValuesChange(int minValue, int maxValue) {
            onRangeGeoTrackValuesChange(minValue, maxValue, true);
        }

        public void computeRangeValues() {
            computeRangeValues(geoTrackOverlayByUser.values());
        }

        public boolean onRangeGeoTrackValuesChange(int minValue, int maxValue, boolean notify) {
            Log.d(TAG, "onRangeGeoTrackValuesChange  event values :  " + minValue + " to " + maxValue);
            Log.d(TAG, "onRangeGeoTrackValuesChange current range : " + geotrackRangeMin + " to " + geotrackRangeMax);
            Log.d(TAG, "onRangeGeoTrackValuesChange     is change : " + (minValue < geotrackRangeMin) + " to " + (maxValue > geotrackRangeMax));
            // Check if range bar is activated
            if (rangeTimelineBar == null) {
                Log.w(TAG, "onRangeGeoTrackValuesChange : Ignore for null rangeTimelineBar ");
                return false;
            }
            // Check Defaut Value
            if (minValue == Integer.MAX_VALUE && maxValue == Integer.MIN_VALUE) {
                Log.w(TAG, "onRangeGeoTrackValuesChange : Ignore default values " + minValue + " to " + maxValue);
                return false;
            }
            // TODO Add another maxRange Period in rangeTimelineBar

            // Check Range
            boolean isRangeUpdate = false;
            if (geotrackRangeMin == rangeTimelineBar.getRangeTimelineMin()) {
                geotrackRangeMin = Math.max(rangeTimelineBar.getRangeTimelineMin(), roundToHour(minValue, false));
                isRangeUpdate = true;
            } else if (minValue < geotrackRangeMin) {
                geotrackRangeMin = Math.min(geotrackRangeMin, roundToHour(minValue, false));
                isRangeUpdate = true;
            }
            // Max Range
            if (geotrackRangeMax == rangeTimelineBar.getRangeTimelineMax()) {
                geotrackRangeMax = Math.min(roundToHour(maxValue, true), rangeTimelineBar.getRangeTimelineMax());
                isRangeUpdate = true;
            } else if (maxValue > geotrackRangeMax) {
                geotrackRangeMax = Math.min(rangeTimelineBar.getRangeTimelineMax(), //
                        Math.max(geotrackRangeMax, roundToHour(maxValue, true))//
                );

                isRangeUpdate = true;
            }
            if (isRangeUpdate && notify) {
                Log.d(TAG, "onRangeGeoTrackValuesChange to set setAbsoluteValues " + geotrackRangeMin + " / " + geotrackRangeMax);
                rangeTimelineBar.setAbsoluteValues(geotrackRangeMin, geotrackRangeMax);
            }
            return isRangeUpdate;
        }

        private int roundToHour(int valueInS, boolean addOneHour) {
            int hours = valueInS / 3600;
            hours = hours * 3600;
            if (addOneHour) {
                hours += AppConstants.ONE_HOUR_IN_S;
            }
            return hours;
        }

        public void computeRangeValues(Collection<GeoTrackOverlay> geoTracks) {
            if (rangeTimelineBar == null) {
                Log.w(TAG, "onRangeGeoTrackValuesChange : Ignore for null rangeTimelineBar ");
                return;
            }

            // Reset Range
            geotrackRangeMin = rangeTimelineBar.getRangeTimelineMin();
            geotrackRangeMax = rangeTimelineBar.getRangeTimelineMax();

            boolean isSet = false;
            if (geoTracks != null && !geoTracks.isEmpty()) {
                Log.d(TAG, "computeRangeValues with  geoTracks Size :  " + geoTracks.size());
                for (GeoTrackOverlay geoTrack : geoTracks) {
                    int geoTrackMin = geoTrack.getGeoTrackRangeTimeValueMin();
                    int geoTrackMax = geoTrack.getGeoTrackRangeTimeValueMax();
                    isSet |= onRangeGeoTrackValuesChange(geoTrackMin, geoTrackMax, false);
                    // if (geoTrackMin < geoTrackMax) {
                    // isSet = true;
                    // min = Math.min(min, geoTrackMin);
                    // max = Math.max(max, geoTrackMax);
                    // }
                }
            } else {
                rangeTimelineBar.resetSelectedValues();
            }
            // Define Range
            // if (isSet) {
            // geotrackRangeMin = Math.max(
            // rangeTimelineBar.getAbsoluteMinValue(), roundToHour(min));
            // geotrackRangeMax = Math.min(AppConstants.ONE_DAY_IN_S,
            // roundToHour(max) + AppConstants.ONE_HOUR_IN_S);
            // } else {
            // geotrackRangeMin = 0;
            // geotrackRangeMax = AppConstants.ONE_DAY_IN_S;
            // }
            if (isSet) {
                Log.d(TAG, "computeRangeValues to set setAbsoluteValues " + geotrackRangeMin + " / " + geotrackRangeMax);
                rangeTimelineBar.setAbsoluteValues(geotrackRangeMin, geotrackRangeMax);
            }
        }
    }

    ;


    // ===========================================================
    // Map Action
    // ===========================================================


    public void centerOnPersonPhone(final String phone) {
        Log.d(TAG, "center OnPersonPhone : " + phone);
        if (myLocation != null) {
            myLocation.disableFollowLocation();
        }
        GeoTrackOverlay geoTrackOverlay = geoTrackOverlayGetOrAddForPhone(phone);
        Log.d(TAG, "### geoTrackOverlayGetOrAddForPhone " + phone + " ==> " + geoTrackOverlay);
        geoTrackOverlay.animateToLastKnowPosition(false);
    }

    public void centerOnPersonPhone(final String phone, final int latE6, final int lngE6, final int accuracy) {
        Log.d(TAG, "center OnPersonPhone : " + phone + " with GeoPoint(" + latE6 + ", " + lngE6 + ") +/- " + accuracy + "m.");
        if (myLocation != null) {
            myLocation.disableFollowLocation();
        }

        if (Integer.MIN_VALUE != latE6 && Integer.MIN_VALUE != lngE6) {
            GeoPoint geoPoint = new GeoPoint(latE6, lngE6);
            centerOnLocation(geoPoint, accuracy);
            mapController.setCenter(geoPoint);
        }

        // Display GeoPoints for person
        GeoTrackOverlay geoTrackOverlay = geoTrackOverlayGetOrAddForPhone(phone);
    }

    // ===========================================================
    // Select Person Dialog
    // ===========================================================

    public void showSelectPersonDialog() {
        LoaderManager loaderManager = getActivity().getSupportLoaderManager();
        SelectGeoTrackDialog personListDialod = new SelectGeoTrackDialog(getActivity(), loaderManager, onSelectPersonListener, geoTrackOverlayByUser);
        personListDialod.show();
    }

    private SelectGeoTrackDialog.OnSelectPersonListener onSelectPersonListener = new SelectGeoTrackDialog.OnSelectPersonListener() {

        @Override
        public void onDoRemovePerson(Person person) {
            Log.d(TAG, "onSelectPersonListener ask remove person : " + person);
            geoTrackOverlayRemovePerson(person);
        }

        @Override
        public void onDoAddPerson(Person person) {
            Log.d(TAG, "onSelectPersonListener ask add person : " + person);
            geoTrackOverlayAddPerson(person);
        }

        @Override
        public void onSelectPerson(Person person) {
            Log.d(TAG, "onSelectPersonListener ask Select person : " + person);
            geoTrackOverlayAnimateToLastKnowPosition(person.phone);
        }

        @Override
        public void onNoPerson(SelectGeoTrackDialog dialog) {
            // Open the Creation Person
            Intent intent = Intents.activityMain(getActivity());
            startActivity(intent);
        }
    };

    // ===========================================================
    // GeoTrack Overlay
    // ===========================================================

    private GeoTrackOverlay geoTrackOverlayGetOrAddForPhone(String phone) {
        GeoTrackOverlay geoTrackOverlay = geoTrackOverlayByUser.get(phone);
        Log.d(TAG, "geoTrackOverlay for phone  [" + phone + "] with center on it (" + ")  is found: " + (geoTrackOverlay != null));
        // Add person layer
        if (geoTrackOverlay == null) {
            Person person = null;
            Cursor cursor = getActivity().getContentResolver().query(PersonProvider.Constants.CONTENT_URI, null, PersonDatabase.PersonColumns.SELECT_BY_PHONE_NUMBER, new String[]{phone}, null);
            try {
                if (cursor.moveToFirst()) {
                    PersonHelper helper = new PersonHelper().initWrapper(cursor);
                    person = helper.getEntity(cursor);
                }
            } finally {
                cursor.close();
            }
            if (person != null) {
                geoTrackOverlay = geoTrackOverlayAddPerson(person);
            }
        }
        return geoTrackOverlay;
    }

    private synchronized GeoTrackOverlay geoTrackOverlayAddPerson(final Person person) {
        final String userId = person != null ? person.phone : null;
        if (TextUtils.isEmpty(userId)) {
            Log.e(TAG, String.format("Could not Add person %s with No Phone", person));
            return null;
        }
        final GeoTrackOverlay geoTrackOverlay;
        boolean isDone = false;
        if (!geoTrackOverlayByUser.containsKey(userId)) {
            Log.d(TAG, String.format("Need to add GeoTrackOverlay for person %s", person));
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            //
            // Last Position center
            // Log.d(TAG, "------------------------------------------");
            // Log.d(TAG, "------------------------------------------");
            // Log.d(TAG, "GeotrackLastAddedListener need to create " +
            // centerOnLastPos);
            // Log.d(TAG, "------------------------------------------");
            // Log.d(TAG, "------------------------------------------");
            geoTrackOverlay = new GeoTrackOverlay(getActivity(), this.mapView, loaderManager, person, System.currentTimeMillis(), null);
            geoTrackOverlay.setOnRangeGeoTrackValuesChangeListener(onRangeGeoTrackValuesChangeListener) //
                    .setGeocodingAuto(geocodingAuto);

            // Register this geoTrack
            geoTrackOverlayByUser.put(userId, geoTrackOverlay);
            onRangeGeoTrackValuesChangeListener.computeRangeValues();
            Log.d(TAG, String.format("Added GeoTrackOverlay for person %s", person));
            // register
            isDone = mapView.getOverlays().add(geoTrackOverlay);

            // Invalidate
            mapView.postInvalidate();
            Log.i(TAG, String.format("Add New GeoTrack Overlay (%s) for %s", isDone, person));
        } else {
            Log.e(TAG, String.format("Could not Add person %s in geoTrackOverlayByUser (It already in List)", person));
            geoTrackOverlay = geoTrackOverlayByUser.get(userId);
        }
        if (!isDone) {
            return null;
        }
        return geoTrackOverlay;
    }

    private boolean geoTrackOverlayRemovePerson(Person person) {
        boolean isDone = false;
        Log.d(TAG, String.format("Want to remove New GeoTrack Overlay for %s", person));
        String userId = person.phone;
        if (geoTrackOverlayByUser.containsKey(userId)) {
            GeoTrackOverlay geoTrackOverlay = geoTrackOverlayByUser.remove(userId);
            isDone = mapView.getOverlays().remove(geoTrackOverlay);
            geoTrackOverlay.onDetach(mapView);
            geoTrackOverlay.setOnRangeGeoTrackValuesChangeListener(null);
            onRangeGeoTrackValuesChangeListener.computeRangeValues();
            Log.i(TAG, String.format("Remove GeoTrack Overlay (%s) for %s", isDone, person));
        } else {
            Log.e(TAG, String.format("Could not remove person %s in geoTrackOverlayByUser", person));
        }
        return isDone;
    }

    private boolean geoTrackOverlayAnimateToLastKnowPosition(String userId) {
        boolean isDone = false;
        if (geoTrackOverlayByUser.containsKey(userId)) {
            GeoTrackOverlay geoTrackOverlay = geoTrackOverlayByUser.get(userId);
            geoTrackOverlay.animateToLastKnowPosition(false);
            if (myLocation != null) {
                myLocation.disableFollowLocation();
            }
            isDone = true;
        } else {
            Log.e(TAG, String.format("Could not Animate to last position of person %s in geoTrackOverlayByUser", userId));
            for (String key : geoTrackOverlayByUser.keySet()) {
                Log.e(TAG, String.format("geoTrackOverlayByUser contains Key : %s", key));
            }
        }
        Log.d(TAG, String.format("animateToLastKnowPosition for User : %s (is done %s)", userId, isDone));
        return isDone;
    }


    // ===========================================================
    // Geofence Overlay
    // ===========================================================

    public boolean isGeofenceOverlays() {
        return (geofenceListOverlay != null && mapView.getOverlays().contains(geofenceListOverlay));
    }

    public GeofenceEditOverlay showGeofenceOverlays() {
        Log.d(TAG, "show Geofence Overlay");
        if (geofenceListOverlay == null) {
            LoaderManager loaderManager = getActivity().getSupportLoaderManager();
            this.geofenceListOverlay = new GeofenceEditOverlay(getActivity(), mapView, loaderManager, handler);
            mapView.getOverlays().add(geofenceListOverlay);
            mapView.postInvalidate();
        } else if (!mapView.getOverlays().contains(geofenceListOverlay)) {
            mapView.getOverlays().add(geofenceListOverlay);
            mapView.postInvalidate();
        }
        return this.geofenceListOverlay;
    }

    public void hideGeofenceOverlays() {
        Log.d(TAG, "hide Geofence Overlay");
        if (geofenceListOverlay != null) {
            mapView.getOverlays().remove(geofenceListOverlay);
            this.geofenceListOverlay = null;
            mapView.postInvalidate();
        }
    }

    public void addGeofenceOverlayEditor() {
        Log.d(TAG, "add Geofence Overlay");
        showGeofenceOverlays();
        this.geofenceListOverlay.doAddCircleGeofence();
        //
        mapView.postInvalidate();
    }


    // ===========================================================
    // Loader
    // ===========================================================

    private final LoaderManager.LoaderCallbacks<Cursor> geoTrackPersonLoaderCallback = new LoaderManager.LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            Log.d(TAG, "onCreateLoader");
            String sortOrder = PersonDatabase.PersonColumns.ORDER_NAME_ASC;
            String selection = PersonDatabase.PersonColumns.SELECT_BYPHONE_NUMBER_NOT_NULL;// null;
            String[] selectionArgs = null;
            // Loader
            CursorLoader cursorLoader = new CursorLoader(getActivity(), PersonProvider.Constants.CONTENT_URI, null, selection, selectionArgs, sortOrder);
            return cursorLoader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            int resultCount = cursor.getCount();
            Log.d(TAG, String.format("onLoadFinished with %s results", resultCount));
            if (cursor.moveToFirst()) {
                PersonHelper helper = new PersonHelper().initWrapper(cursor);
                do {
                    Person pers = helper.getEntity(cursor);
                    Log.d(TAG, String.format("Add Person with phone : %s", pers));
                    geoTrackOverlayAddPerson(pers);
                } while (cursor.moveToNext());
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // for (Map.Entry<String, GeoTrackOverlay> entry :
            // geoTrackOverlayByUser.entrySet()) {
            // String key = entry.getKey();
            // removeGeoTrackOverlay(key);
            // }
        }

    };

    // ===========================================================
    // Listeners
    // ===========================================================

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(AppConstants.PREFS_GEOPOINT_GEOCODING_AUTO)) {
            geocodingAuto = sharedPreferences.getBoolean(AppConstants.PREFS_GEOPOINT_GEOCODING_AUTO, true);
        }
    }

    private class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(TAG, "########################################");
            Log.e(TAG, "ShowMap StatusReceiver onReceive  action : " + action);
            if (Intents.ACTION_NEW_GEOTRACK_INSERTED.equals(action)) {
            }
        }
    }



    // ===========================================================
    // Other
    // ===========================================================

}
