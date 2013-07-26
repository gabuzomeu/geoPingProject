package eu.ttbox.geoping.test.domain;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.pairing.PairingHelper;

public class PairingProviderTest extends ProviderTestCase2<PairingProvider> {

    private static final Uri CONTENT_URI = PairingProvider.Constants.CONTENT_URI;

    static final Uri[] validUris = new Uri[] { CONTENT_URI };

    public PairingProviderTest() {
        super(PairingProvider.class, PairingProvider.Constants.AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testQuery() {
        ContentProvider provider = getProvider();
        for (Uri uri : validUris) {
            Cursor cursor = provider.query(uri, null, null, null, null);
            assertNotNull(cursor);
        }
    }

    public void testInsertAndRead() {
        ContentProvider provider = getProvider();
        // Data
        Pairing vo = new Pairing()//
                .setPhone("0601020304") //
                .setShowNotification(true)//
                .setPairingTime(System.currentTimeMillis())
                .setAuthorizeType(PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS);
        ContentValues values = PairingHelper.getContentValues(vo);
        // Insert
        Uri uri = provider.insert(CONTENT_URI, values);
        // Select
        Cursor cursor = provider.query(uri, null, null, null, null);
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            PairingHelper helper = new PairingHelper().initWrapper(cursor);
            Pairing voDb = helper.getEntity(cursor);
            assertEquals(Long.valueOf(uri.getLastPathSegment()).longValue(), voDb.id);
            assertEquals(vo.phone, voDb.phone);
            assertEquals(vo.showNotification, voDb.showNotification);
            assertEquals(vo.authorizeType, voDb.authorizeType);
            assertEquals(vo.pairingTime, voDb.pairingTime);
        } finally {
            cursor.close();
        }
    }

    public void testInsertAndSearchForPhone() {
        ContentProvider provider = getProvider();
        // Data
        Pairing vo = new Pairing()//
                .setPhone("0601020304") //
                .setShowNotification(true)//
                .setPairingTime(System.currentTimeMillis())
                .setAuthorizeType(PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS);
        ContentValues values = PairingHelper.getContentValues(vo);
        // Insert
        Uri uri = provider.insert(CONTENT_URI, values);
        // Select
        Uri searchPhoneUri01 = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI_PHONE_FILTER, Uri.encode("+33601020304"));
        Uri searchPhoneUri02 = Uri.withAppendedPath(PairingProvider.Constants.CONTENT_URI_PHONE_FILTER, Uri.encode("0601020304"));
        for (Uri searchPhoneUri : new Uri[] { searchPhoneUri01, searchPhoneUri02 }) {
            Cursor cursor = provider.query(searchPhoneUri, null, null, null, null);
            try {
                assertEquals(1, cursor.getCount());
                cursor.moveToFirst();
                PairingHelper helper = new PairingHelper().initWrapper(cursor);
                Pairing voDb = helper.getEntity(cursor);
                assertEquals(Long.valueOf(uri.getLastPathSegment()).longValue(), voDb.id);
                assertEquals(vo.phone, voDb.phone);
                assertEquals(vo.showNotification, voDb.showNotification);
                assertEquals(vo.pairingTime, voDb.pairingTime);
                assertEquals(vo.authorizeType, voDb.authorizeType);
            } finally {
                cursor.close();
            }
        }
    }

}
