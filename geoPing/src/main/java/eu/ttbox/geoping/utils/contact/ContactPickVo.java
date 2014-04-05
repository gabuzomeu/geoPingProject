package eu.ttbox.geoping.utils.contact;


public class ContactPickVo {

    public String contactId;
    public String name;
    public String phone;
    public int phoneType;

    public String lookupKey;

    @Override
    public String toString() {
        return "ContactPickVo{" +
                "contactId='" + contactId + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", phoneType=" + phoneType +
                ", lookupKey='" + lookupKey + '\'' +
                '}';
    }
}
