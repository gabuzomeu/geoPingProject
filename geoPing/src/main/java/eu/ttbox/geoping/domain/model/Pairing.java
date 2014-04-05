package eu.ttbox.geoping.domain.model;

import eu.ttbox.geoping.core.AppConstants;

public class Pairing {

    public long id =  AppConstants.UNSET_ID;
    public String displayName;
    public String personUuid;
    public String email;
    // Phone
    public String phone;
    public String contactId;

    public PairingAuthorizeTypeEnum authorizeType;
    public boolean showNotification = false;
    public long pairingTime = AppConstants.UNSET_TIME;
    // App Version
    public int appVersion;
    public long appVersionTime= AppConstants.UNSET_TIME;
	// Encryption
	public String encryptionPubKey;
	public String encryptionPrivKey;
	public String encryptionRemotePubKey;
	public long encryptionRemoteTime = AppConstants.UNSET_TIME;
	public String encryptionRemoteWay;
	
    public Pairing setId(long id) {
        this.id = id;
        return this;
    }

    public Pairing setDisplayName(String name) {
        this.displayName = name;
        return this;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public Pairing setPersonUuid(String personUuid) {
        this.personUuid = personUuid;
        return this;
    }

    public Pairing setEmail(String email) {
        this.email = email;
        return this;
    }

    public Pairing setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public Pairing setAuthorizeType(PairingAuthorizeTypeEnum color) {
        this.authorizeType = color;
        return this;
    }

    public Pairing setShowNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }
     

    public Pairing setPairingTime(long pairingTime) {
		this.pairingTime = pairingTime;
		 return this;
	}
    public Pairing setEncryptionRemoteTime(long timeInMs) {
		this.encryptionRemoteTime =timeInMs;
		return this;
	}


    public int getAppVersion() {
        return appVersion;
    }

    public Pairing setAppVersion(int appVersion) {
        this.appVersion = appVersion;
        return this;
    }

    public long getAppVersionTime() {
        return appVersionTime;
    }

    public Pairing setAppVersionTime(long appVersionTime) {
        this.appVersionTime = appVersionTime;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("Pairing [");
        sb.append("id=").append(id)//
                .append(", phone=").append(phone)//
                .append(", displayName=").append(displayName)//
                .append(", authorizeType=").append(authorizeType)//
                .append(", showNotification=").append(showNotification)//
                // .append(", time=").append(time) //
                .append(", pairingTime=").append(String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS,%1$tL", pairingTime));

        sb.append("]");
        return sb.toString();
    }

}
