package eu.ttbox.geoping.utils.contact;


import android.graphics.Bitmap;

import eu.ttbox.geoping.domain.model.Person;

public class NotifPersonVo implements INotifPersonVo {

    public final Person person;
    public final String phone;
    public final String contactDisplayName;
    public final Bitmap photo;

    public NotifPersonVo(Person person, String phone, String contactDisplayName, Bitmap photo) {
        this.person = person;
        this.phone = phone;
        this.contactDisplayName = contactDisplayName;
        this.photo = photo;
    }

    public Person getPerson() {
        return person;
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
