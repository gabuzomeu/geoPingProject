package eu.ttbox.geoping.service.core;


import android.graphics.Bitmap;

public class NotifPersonVo {
    public final String phone;
    public final String contactDisplayName;
    public final Bitmap photo;

    public NotifPersonVo(String phone, String contactDisplayName, Bitmap photo) {
        this.phone = phone;
        this.contactDisplayName = contactDisplayName;
        this.photo = photo;
    }
}
