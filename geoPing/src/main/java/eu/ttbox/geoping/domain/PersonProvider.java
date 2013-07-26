package eu.ttbox.geoping.domain;

import android.app.SearchManager;
import android.app.backup.BackupManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import eu.ttbox.geoping.domain.person.PersonDatabase;
import eu.ttbox.geoping.domain.person.PersonDatabase.PersonColumns;

public class PersonProvider extends ContentProvider {

    private static final String TAG = "PersonProvider";

    // Constante

    // MIME types used for searching words or looking up a single definition
    public static final String PERSONS_LIST_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/ttbox.geoping.person";
    public static final String PERSON_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/ttbox.geoping.person";

    public static class Constants {
        public static String AUTHORITY = "eu.ttbox.geoping.PersonProvider";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/person");

        public static final Uri CONTENT_URI_PHONE_FILTER = Uri.withAppendedPath(CONTENT_URI, "phone_lookup");
        
        public static final Uri getUriPhoneFilter(String phoneNumber) {
        	Uri uri = Uri.withAppendedPath( CONTENT_URI_PHONE_FILTER, Uri.encode(phoneNumber));
        	Log.d(TAG, "PersonProvider phone filter Uri : " + uri);
        	return uri;
        }
        public static final Uri getUriEntityId(long entityId) {
            return getUriEntityId(String.valueOf(entityId));
        }
        public static final Uri getUriEntityId(String entityId) {
            Uri uri = Uri.withAppendedPath( CONTENT_URI, entityId);
            Log.d(TAG, "PersonProvider Entity Uri : " + uri);
            return uri;
        }

    }

    private PersonDatabase personDatabase;

    // UriMatcher stuff
    private static final int PERSONS = 0;
    private static final int PERSON_ID = 1;
    private static final int PHONE_FILTER = 2;
    private static final int SEARCH_SUGGEST = 3;
    private static final int REFRESH_SHORTCUT = 4;

    private static final UriMatcher sURIMatcher = buildUriMatcher();

    /**
     * Builds up a UriMatcher for search suggestion and shortcut refresh
     * queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        // to get definitions...
        matcher.addURI(Constants.AUTHORITY, "person", PERSONS);
        matcher.addURI(Constants.AUTHORITY, "person/#", PERSON_ID);
        /*
         * <pre> Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_URI,
         * Uri.encode(phoneNumber)); </pre>
         */
        matcher.addURI(Constants.AUTHORITY, "person/phone_lookup/*", PHONE_FILTER);
        // to get suggestions...
        matcher.addURI(Constants.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(Constants.AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);

        /*
         * The following are unused in this implementation, but if we include
         * {@link SearchManager#SUGGEST_COLUMN_SHORTCUT_ID} as a column in our
         * suggestions table, we could expect to receive refresh queries when a
         * shortcutted suggestion is displayed in Quick Search Box, in which
         * case, the following Uris would be provided and we would return a
         * cursor with a single item representing the refreshed suggestion data.
         */
        matcher.addURI(Constants.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, REFRESH_SHORTCUT);
        matcher.addURI(Constants.AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", REFRESH_SHORTCUT);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        personDatabase = new PersonDatabase(getContext());
        return true;
    }

    /**
     * Handles all the dictionary searches and suggestion queries from the
     * Search Manager. When requesting a specific word, the uri alone is
     * required. When searching all of the dictionary for matches, the
     * selectionArgs argument must carry the search query as the first element.
     * All other arguments are ignored.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.d(TAG, "query for uri : " + uri);
        // Use the UriMatcher to see what kind of query we have and format the
        // db query accordingly
        switch (sURIMatcher.match(uri)) {
        case SEARCH_SUGGEST:
            if (selectionArgs == null) {
                throw new IllegalArgumentException("selectionArgs must be provided for the Uri: " + uri);
            }
            return getSuggestions(selectionArgs[0]);
        case PERSONS:
            return search(projection, selection, selectionArgs, sortOrder);
            // if (selectionArgs == null) {
            // throw new
            // IllegalArgumentException("selectionArgs must be provided for the Uri: "
            // + uri);
            // }
            // return search(selectionArgs[0]);
        case PERSON_ID:
            return getPerson(uri);
        case PHONE_FILTER:
            String phone = uri.getLastPathSegment();
            String phoneDecoder = Uri.decode(phone);
            return personDatabase.searchForPhoneNumber(phoneDecoder, projection, selection,  selectionArgs, sortOrder);
        case REFRESH_SHORTCUT:
            return refreshShortcut(uri);
        default:
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }
     
    private Cursor getSuggestions(String query) {
        query = query.toLowerCase();
        String[] columns = new String[] { PersonDatabase.PersonColumns.COL_ID, //
                PersonDatabase.PersonColumns.COL_NAME, PersonDatabase.PersonColumns.COL_PHONE, //
                SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, //
                /*
                 * SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, (only if you want
                 * to refresh shortcuts)
                 */
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID };

        return personDatabase.getEntityMatches(columns, query, null);
    }
 

    private Cursor search(String[] _projection, String _selection, String[] _selectionArgs, String _sortOrder) {
        String[] projection = _projection == null ? PersonColumns.ALL_COLS : _projection;
        String selection = _selection;
        String[] selectionArgs = _selectionArgs;
        String sortOrder = _sortOrder;
        return personDatabase.queryEntities(projection, selection, selectionArgs, sortOrder);
    }

    private Cursor getPerson(Uri uri) {
        String rowId = uri.getLastPathSegment();
        String[] columns = PersonDatabase.PersonColumns.ALL_COLS;
        return personDatabase.getEntityById(rowId, columns);
    }

    private Cursor refreshShortcut(Uri uri) {
        Log.i(TAG, "refreshShortcut uri " + uri);
        String rowId = uri.getLastPathSegment();
        String[] columns = new String[] { PersonDatabase.PersonColumns.COL_ID //
                , BaseColumns._ID //
                // , PersonDatabase.PersonColumns.KEY_LASTNAME,
                // PersonDatabase.PersonColumns.KEY_FIRSTNAME //
                , SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2 //
        // , SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
        // SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
        };

        return personDatabase.getEntityById(rowId, columns);
    }

    /**
     * This method is required in order to query the supported types. It's also
     * useful in our own query() method to determine the type of Uri received.
     */
    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri)) {
        case PERSONS:
            return PERSONS_LIST_MIME_TYPE;
        case PERSON_ID:
            return PERSON_MIME_TYPE;
        case PHONE_FILTER:
       	 return PERSON_MIME_TYPE;
        case SEARCH_SUGGEST:
            return SearchManager.SUGGEST_MIME_TYPE;
        case REFRESH_SHORTCUT:
            return SearchManager.SHORTCUT_MIME_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    // Other required implementations...

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        switch (sURIMatcher.match(uri)) {
        case PERSONS:
            long personId = personDatabase.insertEntity(values);
            Uri personUri = null;
            if (personId > -1) {
                personUri = Uri.withAppendedPath(Constants.CONTENT_URI, String.valueOf(personId));
                getContext().getContentResolver().notifyChange(uri, null);
                // Backup
                BackupManager.dataChanged(getContext().getPackageName());
            }
            return personUri;
        default:
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        switch (sURIMatcher.match(uri)) {
        case PERSON_ID:
            String entityId = uri.getLastPathSegment();
            String[] args = new String[] { entityId };
            count = personDatabase.deleteEntity(PersonColumns.SELECT_BY_ENTITY_ID, args);
            break;
        case PERSONS:
            count = personDatabase.deleteEntity(selection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            // Backup
           BackupManager.dataChanged(getContext().getPackageName());
         }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        switch (sURIMatcher.match(uri)) {
        case PERSON_ID:
            String entityId = uri.getLastPathSegment();
            String[] args = new String[] { entityId };
            count = personDatabase.updateEntity(values, PersonColumns.SELECT_BY_ENTITY_ID, args);
            break;
        case PERSONS:
            count = personDatabase.updateEntity(values, selection, selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            // Backup
            BackupManager.dataChanged(getContext().getPackageName());
        }
        return count;
    }

}
