package eu.ttbox.geoping.domain.model;

import android.net.Uri;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.domain.PersonProvider;

public class Person {

    public long id = AppConstants.UNSET_ID;
    public String displayName;
    public String personUuid;
    public String email;
    // Phone
    public String phone;
    public String contactId;

    public long pairingTime;
    // Config
    public int color;
    // App Version
    public int appVersion;
    public long appVersionTime = AppConstants.UNSET_TIME;

    // Encryption
    public String encryptionPubKey;
    public String encryptionPrivKey;
    public String encryptionRemotePubKey;
    public long encryptionRemoteTime;
    public String encryptionRemoteWay;

    public Person setId(long id) {
        this.id = id;
        return this;
    }

    public Person setDisplayName(String name) {
        this.displayName = name;
        return this;
    }


    public Person setPersonUuid(String personUuid) {
        this.personUuid = personUuid;
        return this;
    }

    public Person setEmail(String email) {
        this.email = email;
        return this;
    }

    public Person setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public Person setColor(int color) {
        this.color = color;
        return this;
    }

    public Person setContactId(String contactId) {
        this.contactId = contactId;
        return this;
    }


    public Person setPairingTime(long pairingTime) {
        this.pairingTime = pairingTime;
        return this;
    }

    public Person setEncryptionRemoteTime(long timeInMs) {
        this.encryptionRemoteTime = timeInMs;
        return this;
    }

    public int getAppVersion() {
        return appVersion;
    }

    public Person setAppVersion(int appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public long getAppVersionTime() {
        return appVersionTime;
    }

    public Person setAppVersionTime(long appVersionTime) {
        this.appVersionTime = appVersionTime;
        return this;
    }

    public Uri getPersonUri() {
        Uri entityUri = null;
        if ( this.id != AppConstants.UNSET_ID) {
            entityUri = Uri.withAppendedPath(PersonProvider.Constants.CONTENT_URI, String.valueOf(this.id));
        }
        return entityUri;
    }

    @Override
    public String toString() {
        return "Person [id=" + id + ", displayName=" + displayName + ", phone=" + phone + "]";
    }


}
