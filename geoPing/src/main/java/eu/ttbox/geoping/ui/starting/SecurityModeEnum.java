package eu.ttbox.geoping.ui.starting;


public enum SecurityModeEnum {

    CUSTOM(0),   //
    PARENT(1),   //
    CHILDREN(2); //

    private final int code;

    SecurityModeEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static SecurityModeEnum getByCode(int code) {
        SecurityModeEnum[] valCodes =  SecurityModeEnum.values();
        if (code<0 || code>=valCodes.length) {
            return null;
        }
        return valCodes[code];
    }
}
