package eu.ttbox.geoping.encoder.params.encoding;


public class AppVersionParamEncoder extends IntegerParamEncoder {

    public final static int ZERO_VALUE = 62;

    public AppVersionParamEncoder() {
        super();
    }


    @Override
    public boolean writeTo(Integer value, StringBuilder dest, char smsFieldName, boolean isSmsFieldName) {
        Integer valueTranslated = value;
        if (valueTranslated!=null) {
            valueTranslated = Integer.valueOf(valueTranslated.intValue() - ZERO_VALUE);
        }
       return super.writeTo(valueTranslated, dest, smsFieldName, isSmsFieldName);
    }

    @Override
    public int parseEncodedValue(String encoded) {
        int value = super.parseEncodedValue(encoded);
        int decodedValue = value + ZERO_VALUE;
        return decodedValue;
    }

}
