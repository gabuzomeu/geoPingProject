package eu.ttbox.geoping.domain.smslog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import eu.ttbox.geoping.domain.smslog.SmsLogDatabase.SmsLogColumns;
/**
 * <a href="http://www.ragtag.info/2011/feb/1/database-pitfalls/">SQLLiteOpenHelper n’aide pas vraiment</a>
 * <ul>
 * <li>Db version 7  : Geoping 0.1.5 (37)</li>
 * <li>Db version 8  : Geoping 0.1.6 (39)</li>
 * <li>Db version 9  : Geoping 0.2.0 (??) : Add COL_TO_READ & COL_REQUEST_ID</li>
 * <li>Db version 10 : Geoping 0.2.2 (??)</li>
 * <li>Db version 11 : Geoping 0.2.3 (??) : Rename COL_TO_READ to COL_TO_READ and ad INDEX</li>
 * <li>Db version 12 : Geoping 0.3.2 (??) : Add COL_MSG_ACK_RESEND_TIME_MS to COL_MSG_ACK_RESEND_MSG_COUNT</li>
 * </ul>
 *  
 *
 */
public class SmsLogOpenHelper extends SQLiteOpenHelper {

    private static final String TAG = "SmsLogOpenHelper";

    public static final String DATABASE_NAME = "smsLog.db";
    public static final int DATABASE_VERSION = 11;

    // ===========================================================
    // Table
    // ===========================================================

    /*
     * Note that FTS3 does not support column constraints and thus, you cannot
     * declare a primary key. However, "rowid" is automatically used as a unique
     * identifier, so when making requests, we will use "_id" as an alias for
     * "rowid"
     */
    private static final String FTS_TABLE_CREATE = "CREATE TABLE " + SmsLogDatabase.TABLE_SMSLOG   //
            + "( " + SmsLogColumns.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT"//
            + ", " + SmsLogColumns.COL_TIME + " INTEGER NOT NULL"//
            + ", " + SmsLogColumns.COL_SMSLOG_TYPE + " INTEGER"  //@see SmsLogTypeEnum
            + ", " + SmsLogColumns.COL_ACTION+ " TEXT"// // @see SmsMessageActionEnum
            + ", " + SmsLogColumns.COL_PHONE + " TEXT"// 
            + ", " + SmsLogColumns.COL_PHONE_MIN_MATCH + " TEXT"// 
            + ", " + SmsLogColumns.COL_MESSAGE + " TEXT"//
            + ", " + SmsLogColumns.COL_MESSAGE_PARAMS + " TEXT"//
            + ", " + SmsLogColumns.COL_PARENT_ID  + " INTEGER"// 
            + ", " + SmsLogColumns.COL_SMS_SIDE  + " INTEGER"//
            + ", " + SmsLogColumns.COL_MSG_COUNT + " INTEGER"//
            // Acknowledge
            + ", " + SmsLogColumns.COL_MSG_ACK_SEND_MSG_COUNT + " INTEGER"//
            + ", " + SmsLogColumns.COL_MSG_ACK_DELIVERY_MSG_COUNT + " INTEGER"//
            + ", " + SmsLogColumns.COL_MSG_ACK_SEND_TIME_MS + " INTEGER"//
            + ", " + SmsLogColumns.COL_MSG_ACK_DELIVERY_TIME_MS + " INTEGER"//
            + ", " + SmsLogColumns.COL_MSG_ACK_SEND_RESULT_MSG + " TEXT"//
            + ", " + SmsLogColumns.COL_MSG_ACK_DELIVERY_RESULT_MSG + " TEXT"//
            // Notif
            + ", " + SmsLogColumns.COL_TO_READ + " INTEGER"//
            // Geofence
            + ", " + SmsLogColumns.COL_REQUEST_ID  + " TEXT"//
            + ");";

    // ===========================================================
    // Index
    // ===========================================================
    // Time


    // Phone Min match
    private static final String INDEX_PHONE_MINMATCH_AK = "IDX_SMSLOG_PHONEMINMATCH_AK";
    private static final String CREATE_INDEX_PHONE_MINMATCH_AK = "CREATE INDEX IF NOT EXISTS " + INDEX_PHONE_MINMATCH_AK + " on " + SmsLogDatabase.TABLE_SMSLOG + "(" //
            +  SmsLogColumns.COL_PHONE_MIN_MATCH + ");";

    // To Read
    private static final String INDEX_TOREAD_AK = "IDX_SMSLOG_TOREAD_AK";
    private static final String CREATE_INDEX_TOREAD_AK = "CREATE INDEX IF NOT EXISTS " + INDEX_TOREAD_AK + " on " + SmsLogDatabase.TABLE_SMSLOG + "(" //
            +  SmsLogColumns.COL_TO_READ + "," + SmsLogColumns.COL_SMS_SIDE  + ");";


    // ===========================================================
    // Constructors
    // ===========================================================

    private SQLiteDatabase mDatabase;

    SmsLogOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        mDatabase = db;
        mDatabase.execSQL(FTS_TABLE_CREATE);
        // Index
        db.execSQL(CREATE_INDEX_PHONE_MINMATCH_AK);
        db.execSQL(CREATE_INDEX_TOREAD_AK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        // Drop Index
        db.execSQL("DROP INDEX IF EXISTS " + INDEX_PHONE_MINMATCH_AK);
        db.execSQL("DROP INDEX IF EXISTS " + INDEX_TOREAD_AK);
        // Drop Table
        db.execSQL("DROP TABLE IF EXISTS " + SmsLogDatabase.TABLE_SMSLOG);
        // Create
        onCreate(db);
    }

}
