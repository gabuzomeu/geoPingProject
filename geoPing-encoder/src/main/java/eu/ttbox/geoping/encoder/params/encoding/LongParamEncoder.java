package eu.ttbox.geoping.encoder.params.encoding;

import eu.ttbox.geoping.encoder.adapter.DecoderAdapter;
import eu.ttbox.geoping.encoder.adapter.EncoderAdapter;
import eu.ttbox.geoping.encoder.params.IParamEncoder;
import eu.ttbox.geoping.encoder.params.MessageParamField;
import eu.ttbox.geoping.encoder.params.helper.LongEncoded;


public class LongParamEncoder implements IParamEncoder {

    public final int radix;


    // ===========================================================
    //   Contructor
    // ===========================================================


    public LongParamEncoder() {
        this(LongEncoded.MAX_RADIX);
    }


    public LongParamEncoder(int radix) {
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
        Long value = null;
        // Try to read value
        Object valueObject = src.get(field.dbFieldName);
        if (valueObject != null) {
            if (valueObject instanceof Long) {
                value = (Long) valueObject;
            } else if (valueObject instanceof String) {
                // Consider that pass a String in radix 10
                value = Long.valueOf((String) valueObject, 10);
            } else if (valueObject instanceof Integer) {
                Integer valueInt = (Integer) valueObject;
                value = Long.valueOf(valueInt.longValue());
            } else {
                throw new RuntimeException("Could not value as Long for " + field + " with value = " + valueObject);
            }
        }
        if (value != null) {
            String valueString = LongEncoded.toString(value, radix);
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
        long decodedValue = LongEncoded.parseLong(encoded, radix);
        dest.putLong(field.dbFieldName, decodedValue);
        return 1;
    }

    // ===========================================================
    //   Overide
    // ===========================================================


    @Override
    public String toString() {
        return "LongParamEncoder{" +
                "radix=" + radix +
                '}';
    }

}
