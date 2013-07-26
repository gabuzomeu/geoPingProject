package eu.ttbox.geoping.domain.model;

import eu.ttbox.geoping.core.AppConstants;

public class Pairing {

    public long id =  AppConstants.UNSET_ID;
    public String name;
    public String personUuid;
    public String email;
    // Phone
    public String phone;
    public PairingAuthorizeTypeEnum authorizeType;
    public boolean showNotification = false;
    public long pairingTime = AppConstants.UNSET_TIME;
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

    public Pairing setName(String name) {
        this.name = name;
        return this;
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
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append("Pairing [");
        sb.append("id=").append(id)//
                .append(", phone=").append(phone)//
                .append(", name=").append(name)//
                .append(", authorizeType=").append(authorizeType)//
                .append(", showNotification=").append(showNotification)//
                // .append(", time=").append(time) //
                .append(", pairingTime=").append(String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS,%1$tL", pairingTime));

        sb.append("]");
        return sb.toString();
    }

}
