package eu.ttbox.geoping.test.crypto;

import android.test.AndroidTestCase;
import android.util.Log;

import eu.ttbox.geoping.crypto.CryptoUtils;

public class CryptoUtilsTest extends AndroidTestCase {

    public static final String TAG = "CryptoUtilsTest";

    
    public void testGenerateUniqueId() throws Exception {
        for (int i=0 ;i <100; i++) {
            String id = CryptoUtils.generateUniqueId();
            int idSize = id.length();
//            assertEquals(23, idSize);
            Log.i(TAG, "Generate Unique Id " + i + " : " + id + " (size " +idSize + " chars)");
        }
    }
}
