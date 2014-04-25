package eu.ttbox.geoping.utils.contact;


import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.core.VersionUtils.AndroidPermissions;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.Person;
import eu.ttbox.geoping.domain.pairing.PairingHelper;
import eu.ttbox.geoping.domain.person.PersonHelper;

/**
 * Woking With Contact <a href="http://www.higherpass.com/Android/Tutorials/Working-With-Android-Contacts/>Working-With-Android-Contacts</a>
 */
public class ContactHelper {

    private static final String TAG = "ContactHelper";

    private static final String PERMISSION_READ_CONTACTS = AndroidPermissions.READ_CONTACTS;


    // ===========================================================
    // Pick Contact
    // ===========================================================
    public static boolean pickContactPhone(Fragment context, int resultCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        // Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
        context.startActivityForResult(intent, resultCode);
        return true;
    }

    public static ContactPickVo loadContactPick(Context context, Uri contactData) {
        Log.d(TAG, "Select contact Uri : " + contactData);
        String selection = null;
        String[] selectionArgs = null;
        ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(contactData, new String[]{ //
                ContactsContract.Data.CONTACT_ID, //
                ContactsContract.Contacts.LOOKUP_KEY, //
                Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER, //
                ContactsContract.CommonDataKinds.Phone.TYPE}, selection, selectionArgs, null);
        // read
        ContactPickVo result = null;
        try {
            // Read value
            if (c != null && c.moveToFirst()) {
                result = new ContactPickVo();
                result.contactId = c.getString(0);
                result.lookupKey = c.getString(1);
                result.name = c.getString(2);
                result.phone = c.getString(3);
                result.phoneType = c.getInt(4);
            }
        } finally {
            c.close();
        }
        return result;
    }

    // ===========================================================
    // Photo
    // ===========================================================


    public static Bitmap openPhotoBitmap(Context context, PhotoThumbmailCache photoCache, String contactId, String phone) {
        Bitmap photo = null;
        boolean isContactId = !TextUtils.isEmpty(contactId);
        boolean isContactPhone = !TextUtils.isEmpty(phone);
        // Search in cache
        if (photo == null && isContactId) {
            photo = photoCache.get(contactId);
        }
        if (photo == null && isContactPhone) {
            photo = photoCache.get(phone);
        }
        // Load Photo
        if (photo == null && isContactId) {
            photo = photoCache.loadPhotoLoaderFromContactId(context.getContentResolver(), contactId);
        }
        if (photo == null && isContactPhone) {
            photo = photoCache.loadPhotoLoaderFromContactPhone(context, phone);
        }
        return photo;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static Bitmap loadPhotoContactHighDef(ContentResolver cr, long contactId ) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Log.d(TAG, "Search Photo for ContactsContract Contact Uri : " + contactUri);
        InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri, true );
        if (is == null) {
            Log.d(TAG, "No Photo found for ContactsContract Contact Uri : " + contactUri);
            return null;
        }
        Bitmap photo = BitmapFactory.decodeStream(is);
        try {
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close Contact Photo Input Stream");
        }
        return photo;
    }

    public static Bitmap loadPhotoContact(ContentResolver cr, long contactId) {
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        Log.d(TAG, "Search Photo for ContactsContract Contact Uri : " + contactUri);
        InputStream is = ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri );
        if (is == null) {
            Log.d(TAG, "No Photo found for ContactsContract Contact Uri : " + contactUri);
            return null;
        }
        Bitmap photo = BitmapFactory.decodeStream(is);
        try {
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close Contact Photo Input Stream");
        }
        return photo;
    }



    // ===========================================================
    // Android Contact
    // ===========================================================

    /**
     * <a href="http://developer.android.com/reference/android/provider/ContactsContract.PhoneLookup.html"> PhoneLookup</a>
     *
     * @param context
     * @param phoneNumber
     * @return
     */
    public static ContactVo searchContactForPhone(Context context, String phoneNumber) {
        String contactName = null;
        long contactId = -1l;
        if (isPermissionReadContact(context)) {
            Log.d(TAG, String.format("Search Contact Name for Phone [%s]", phoneNumber));
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
            final String[] selection = new String[]{ PhoneLookup._ID, PhoneLookup.LOOKUP_KEY, PhoneLookup.DISPLAY_NAME};
            Cursor cur = context.getContentResolver().query(uri, selection, null, null, null);
            try {
                if (cur != null && cur.moveToFirst()) {
                    contactId = cur.getLong(cur.getColumnIndexOrThrow(PhoneLookup._ID));
                    contactName = cur.getString(cur.getColumnIndex(PhoneLookup.DISPLAY_NAME));
                }
            } finally {
                cur.close();
            }
        }
        Log.d(TAG, String.format("Found Contact %s Name for Phone [%s] : %s", contactId, phoneNumber, contactName));
        ContactVo result = null;
        if (contactId != -1l) {
            result = new ContactVo(contactId, contactName);
        }
        return result;
    }

    private static boolean isPermissionReadContact(Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            return PackageManager.PERMISSION_GRANTED == pm.checkPermission(PERMISSION_READ_CONTACTS, context.getPackageName());
        }
        return false;
    }


    // ===========================================================
    // GeoPing Contact
    // ===========================================================

    private static PhotoThumbmailCache getPhotoCache(Context context) {
        PhotoThumbmailCache photoCache = ((GeoPingApplication) context.getApplicationContext()).getPhotoThumbmailCache();
        return photoCache;
    }

    public static NotifPersonVo getNotifPersonVo(Context context, String phone) {
        PhotoThumbmailCache photoCache = getPhotoCache(context);
        return getNotifPersonVo(context, photoCache, phone);
    }

    public static NotifPersonVo getNotifPersonVo(Context context, PhotoThumbmailCache photoCache, String phone) {
        // Contact Name
        Person person = searchPersonForPhone(context, phone);
        String contactDisplayName = phone;
        Bitmap photo = null;
        if (person != null) {
            if (person.displayName != null && person.displayName.length() > 0) {
                contactDisplayName = person.displayName;
            }

            // Load Photos
            photo = ContactHelper.openPhotoBitmap(context, photoCache, person.contactId, phone);
        } else {
            // Search photo in contact database
            ContactVo contactVo = ContactHelper.searchContactForPhone(context, phone);
            if (contactVo != null) {
                contactDisplayName = contactVo.displayName;
                photo = ContactHelper.openPhotoBitmap(context, photoCache, String.valueOf(contactVo.id), phone);
            }
        }
        return new NotifPersonVo(person, phone, contactDisplayName, photo);
    }

    public static NotifPairingVo getNotifPairingVo(Context context, Pairing person) {
        PhotoThumbmailCache photoCache = getPhotoCache(context);
        return getNotifPairingVo(context, photoCache, person);
    }

    public static NotifPairingVo getNotifPairingVo(Context context, String phone) {
        PhotoThumbmailCache photoCache = getPhotoCache(context);
        Pairing person = searchPairingForPhone(context, phone);
        return getNotifPairingVo(context, photoCache, person);
    }

    public static NotifPairingVo getNotifPairingVo(Context context, PhotoThumbmailCache photoCache, Pairing person) {
        // Contact Name
        String phone = person != null ? person.phone : null;
        String contactDisplayName = phone;
        Bitmap photo = null;
        if (person != null) {
            if (person.displayName != null && person.displayName.length() > 0) {
                contactDisplayName = person.displayName;
            }

            photo = ContactHelper.openPhotoBitmap(context, photoCache, person.contactId, phone);
        } else {
            // Search photo in contact database
            ContactVo contactVo = ContactHelper.searchContactForPhone(context, phone);
            if (contactVo != null) {
                contactDisplayName = contactVo.displayName;
                photo = ContactHelper.openPhotoBitmap(context, photoCache, String.valueOf(contactVo.id), phone);
            }
        }
        return new NotifPairingVo(person, phone, contactDisplayName, photo);
    }


    public static Person searchPersonForPhone(Context context, String phoneNumber) {
        Person person = null;
        Log.d(TAG, String.format("Search Person Name for Phone : [%s]", phoneNumber));
        Uri uri = PersonProvider.Constants.getUriPhoneFilter(phoneNumber);
        Cursor cur = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                PersonHelper helper = new PersonHelper().initWrapper(cur);
                person = helper.getEntity(cur);
            } else {
                Log.w(TAG, "Person not found for phone : [" + phoneNumber + "]");
            }
        } finally {
            cur.close();
        }
        Log.d(TAG, String.format("Search Person for Phone [%s] : Found %s", phoneNumber, person));
        return person;
    }

    public static Pairing searchPairingForPhone(Context context, String phoneNumber) {
        Pairing person = null;
        Log.d(TAG, String.format("Search Pairing Name for Phone : [%s]", phoneNumber));
        Uri uri = PairingProvider.Constants.getUriPhoneFilter(phoneNumber);
        Cursor cur = context.getContentResolver().query(uri, null, null, null, null);
        try {
            if (cur != null && cur.moveToFirst()) {
                PairingHelper helper = new PairingHelper().initWrapper(cur);
                person = helper.getEntity(cur);
            } else {
                Log.w(TAG, "Pairing not found for phone : [" + phoneNumber + "]");
            }
        } finally {
            cur.close();
        }
        return person;
    }

}
