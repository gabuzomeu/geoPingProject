package eu.ttbox.geoping.encoder.params.encoding;

import eu.ttbox.geoping.encoder.adapter.DecoderAdapter;
import eu.ttbox.geoping.encoder.adapter.EncoderAdapter;
import eu.ttbox.geoping.encoder.params.IParamEncoder;
import eu.ttbox.geoping.encoder.params.MessageParamField;
import eu.ttbox.geoping.encoder.params.helper.IntegerEncoded;

public class IntegerParamEncoder implements IParamEncoder {

    public final int radix;


    // ===========================================================
    //   Contructor
    // ===========================================================

    public IntegerParamEncoder() {
        this(IntegerEncoded.MAX_RADIX);
    }


    public IntegerParamEncoder(int radix) {
        this.radix = radix;
    }

    // ===========================================================
    //   Encoder - Decoder Accessor
    // ===========================================================

    @Override
    public boolean writeTo(EncoderAdapter src, StringBuilder dest, MessageParamField field, char smsFieldName) {
        return writeTo(src, dest, field, smsFieldName, true);
    }

    @Override
    public boolean writeTo(EncoderAdapter src, StringBuilder dest, MessageParamField field, char smsFieldName, boolean isSmsFieldName) {
        boolean isWrite = false;
        Integer value = null;
        // Try to read values
        Object valueObject = src.get(field.dbFieldName);
        if (valueObject != null) {
            if (valueObject instanceof Integer) {
                value = (Integer) valueObject;
            } else if (valueObject instanceof String) {
                // Consider that pass a String in radix 10
                value = Integer.valueOf((String) valueObject, 10);
            } else {
                throw new RuntimeException("Could not value as Integer for " + field + " with value = " + valueObject);
            }
        }
        if (value != null) {
            String valueString = IntegerEncoded.toString(value, radix);
            if (isSmsFieldName) {
                dest.append(smsFieldName);
            }
            dest.append(valueString);
            isWrite = true;
        }
        return isWrite;
    }

    // ===========================================================
    //   Decoder Accessor
    // ===========================================================

    @Override
    public int readTo(DecoderAdapter dest, String encoded, MessageParamField field) {
        int decodedValue = IntegerEncoded.parseInt(encoded, radix);
        dest.putInt(field.dbFieldName, decodedValue);
        return 1;
    }

    // ===========================================================
    //   Overide
    // ===========================================================


    @Override
    public String toString() {
        return "IntegerParamEncoder{" +
                "radix=" + radix +
                '}';
    }
}
