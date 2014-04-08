package eu.ttbox.geoping.utils.contact;


import android.graphics.Bitmap;

import eu.ttbox.geoping.domain.model.Pairing;
import eu.ttbox.geoping.domain.model.Person;

public class NotifPairingVo implements INotifPersonVo {

    public final Pairing pairing;
    public final String phone;
    public final String contactDisplayName;
    public final Bitmap photo;

    public NotifPairingVo(Pairing pairing, String phone, String contactDisplayName, Bitmap photo) {
        this.pairing = pairing;
        this.phone = phone;
        this.contactDisplayName = contactDisplayName;
        this.photo = photo;
    }

    public Pairing getPairing() {
        return pairing;
    }

    public String getPhone() {
        return phone;
    }

    public String getContactDisplayName() {
        return contactDisplayName;
    }

    public Bitmap getPhoto() {
        return photo;
    }
}
