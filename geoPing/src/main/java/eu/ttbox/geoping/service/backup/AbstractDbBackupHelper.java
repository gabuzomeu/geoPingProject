package eu.ttbox.geoping.service.backup;

import android.app.backup.BackupDataInputStream;
import android.app.backup.BackupDataOutput;
import android.app.backup.BackupHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import eu.ttbox.geoping.crypto.codec.Hex;
import eu.ttbox.geoping.domain.core.UpgradeDbHelper;
import eu.ttbox.geoping.domain.person.PersonDatabase.PersonColumns;

public abstract class AbstractDbBackupHelper implements BackupHelper {

    private static final String TAG = "AbstractDbBackupHelper";

    private static final int IS_BUFFER_SIZE = 1024;

    private final Context context;

    private final String backupKey;

    // ===========================================================
    // Constructor
    // ===========================================================

    public AbstractDbBackupHelper(Context ctx, String backupKey) {
        super();
        this.context = ctx;
        this.backupKey = backupKey;
    }

    // ===========================================================
    // States
    // ===========================================================

    @Override
    public void writeNewStateDescription(ParcelFileDescriptor newState) {
        Log.d(TAG, "--- --------------------------------- ---");
        Log.d(TAG, "--- writeNewStateDescription          ---");
        Log.d(TAG, "--- --------------------------------- ---");
        Log.d(TAG, "--- FileDescriptor : " +  newState.getFileDescriptor());
        Log.d(TAG, "--- FileDescriptor : " +  newState.toString());
       
    }

    // ===========================================================
    // Backup
    // ===========================================================

    @Override
    public void performBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "--- performBackup : key =  " + backupKey);
        // State
        // oldStateFd can be null
        FileDescriptor oldStateFd = oldState != null ? oldState.getFileDescriptor() : null;
        FileDescriptor newStateFd = newState.getFileDescriptor();
        if (newStateFd == null) {
            throw new NullPointerException();
        }

        // Doing copy
        ByteArrayOutputStream dataCopy = copyTableAsBytes();
        if (dataCopy != null) {
            try {
                byte[] dataBytes = dataCopy.toByteArray();
                data.writeEntityHeader(backupKey, dataBytes.length);
                data.writeEntityData(dataBytes, dataBytes.length);
                Log.i(TAG, "performBackup Entity '" + backupKey + "' size=" + dataBytes.length);
            } catch (IOException e) {
                Log.e(TAG, "Error during Backup Data : " + e.getMessage(), e);
            }
        }

        // Write State
        // FileOutputStream outstream = new
        // FileOutputStream(newState.getFileDescriptor());
        // DataOutputStream out = new DataOutputStream(outstream);
        // try {
        // long modified = System.currentTimeMillis(); //
        // mDataFile.lastModified();
        // out.writeLong(modified);
        // } catch (IOException e) {
        // Log.e(TAG, "Error Write State : " + e.getMessage(), e);
        // } finally {
        // try {
        // out.close();
        // outstream.close();
        // } catch (IOException e) {
        // Log.e(TAG, "Error Closing State : " + e.getMessage(), e);
        // }
        //
        // }

        Log.i(TAG, "----- performBackup End : key =  " + backupKey);
        Log.i(TAG, "-------------------------------------------------");
    }

    public abstract Cursor getBackupCursor();

    public ByteArrayOutputStream copyTableAsBytes() {
        ByteArrayOutputStream bufStream = null;
        Cursor cursor = null;
        try {
            cursor = getBackupCursor();
            bufStream = copyTableAsJsonByte(cursor);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bufStream;
    }

    public abstract ContentValues getCursorAsContentValues(Cursor cursor);

    private MessageDigest getMessageDigest() {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Could not create MessageDigest : " + e.getMessage(), e);
        }
        return md;
    }

    public ByteArrayOutputStream copyTableAsJsonByte(Cursor cursor) {
        ByteArrayOutputStream bufStream = null;
        // Do copy Table
        try {
            if (cursor.getCount() > 0) {
                // Config
                MessageDigest md = getMessageDigest();
                List<String> ignoreFields = Arrays.asList(new String[] { PersonColumns.COL_ID });
                // Init Is
                bufStream = new ByteArrayOutputStream(IS_BUFFER_SIZE);
                GZIPOutputStream gzipOut = new GZIPOutputStream(bufStream, IS_BUFFER_SIZE);
                // Prepare Disgester
                DigestOutputStream digesterIs = new DigestOutputStream(gzipOut, md);
                try {
                    // Json writer
                    JsonFactory f = new JsonFactory();
                    JsonGenerator g = f.createGenerator(digesterIs, JsonEncoding.UTF16_BE);
                    try {
                        g.writeStartArray();
                        while (cursor.moveToNext()) {
                            g.writeStartObject();
                            // Write Datas
                            ContentValues values = getCursorAsContentValues(cursor);
                            UpgradeDbHelper.writeLineToJson(g, values, ignoreFields);
                            // End Lines
                            g.writeEndObject();
                        }
                        g.writeEndArray();
                        // important: will force flushing of output, close
                        // underlying output stream
                    } finally {
                        g.close();
                    }
                } finally {
                    // Close Streams
                    digesterIs.close();
                    gzipOut.close();
                    bufStream.close();
                }
                // Digest
//                String digestString = new String(  md.digest(), Charset.forName("UTF-8"));
//                BigInteger digesterInt =  new BigInteger(md.digest());
//                String digestString =  digesterInt.toString(16);
                String digestString = new String ( Hex.encode(md.digest()));
                Log.d(TAG, "Backup Md5 : " + digestString);
                  
            }
        } catch (IOException e) {
            Log.e(TAG, "Backup IOException : " + e.getMessage(), e);
        }
        return bufStream;
    }

    // ===========================================================
    // Restore
    // ===========================================================

    @Override
    public void restoreEntity(BackupDataInputStream data) {
        String key = data.getKey();
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "--- restore Entity '" + key + "' size=" + data.size());

        if (backupKey.equals(key)) {
            readBackup(data);
        }
        Log.i(TAG, "----- restoreEntity End");
        Log.i(TAG, "-------------------------------------------------");
    }

    public void readBackup(InputStream data) {
        Log.d(TAG, "read Backup : " + data);
        try {
            JsonFactory f = new JsonFactory();
            GZIPInputStream gzipIs = new GZIPInputStream(data, IS_BUFFER_SIZE);
            ObjectMapper mapper = new ObjectMapper();
            JsonParser jp = f.createJsonParser(gzipIs);
            // advance stream to START_ARRAY first:
            jp.nextToken();
            // Read Objects
            TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            final List<String> allValidColumns = getValidColumns();
            while (jp.nextToken() == JsonToken.START_OBJECT) {
                HashMap<String, Object> jsonMap = mapper.readValue(jp, typeRef);
                ContentValues values = UpgradeDbHelper.convertJsonMapAsContentValues(jsonMap, allValidColumns);
                // Insert
                long entityId = insertEntity(values);
                Log.d(TAG, "Backup Pairing Line : new Inserting entity id " + entityId);
            }
            // Close
            jp.close();
            gzipIs.close();
        } catch (IOException e) {
            Log.e(TAG, "Backup IOException : " + e.getMessage(), e);
        }
    }

    public abstract List<String> getValidColumns();

    public abstract long insertEntity(ContentValues values);
}
