package eu.ttbox.geoping.encoder.params.encoding;


import org.junit.Assert;
import org.junit.Test;

import eu.ttbox.geoping.encoder.adapter.MapEncoderAdpater;
import eu.ttbox.geoping.encoder.params.MessageParamField;
import eu.ttbox.geoping.encoder.params.helper.IntegerEncoded;

public class LongParamEncoderTest {

    private LongParamEncoder service = new LongParamEncoder();

    public static long[] VALUES_TESTED = new long[] { //
            -1, -2, -3, -7, -42, -73, 0, 1, 2, 3, 7, 42, 73, //
            Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2//
            , Integer.MIN_VALUE, Integer.MAX_VALUE //
    };

    @Test
    public void testAlphabet() {
        int i = 0;
        for (char c : IntegerEncoded.ALPHABET) {
            String str = String.valueOf(c);
            MapEncoderAdpater dest = new MapEncoderAdpater();
             service.readTo(dest, str, MessageParamField.EMERGENCY_PASSWORD );
            long decoded = dest.getLong(MessageParamField.EMERGENCY_PASSWORD );
                    Assert.assertEquals(i++, decoded);
        }
    }

    @Test
    public void testEncodeMaxRadix() {
        int radix = IntegerEncoded.MAX_RADIX;
        LongParamEncoder service = new LongParamEncoder(radix);
        for (long i = 0; i < 2000; i++) {
            doEncodeDecodeTest(i, service);
        }
        for (long i : VALUES_TESTED) {
            doEncodeDecodeTest(i, service);
        }
    }

    @Test
    public void testEncodeMulTiRadix() {
        for (int radix : new int[] { 4, 5, 10, 36, IntegerEncoded.MAX_RADIX }) {
            LongParamEncoder service = new LongParamEncoder(radix);
            for (long i = 0; i < 2000; i++) {
                doEncodeDecodeTest(i, service, false);
            }
            for (long i : VALUES_TESTED) {
                doEncodeDecodeTest(i, service, false);
            }
        }
    }

    private void doEncodeDecodeTest(long i, LongParamEncoder service) {
        doEncodeDecodeTest(i, service, true);
    }

    private void doEncodeDecodeTest(long i,  LongParamEncoder service , boolean printIt) {

        int fullSize = String.valueOf(i).length();
        MessageParamField field = MessageParamField.EMERGENCY_PASSWORD;
        // Encode
        MapEncoderAdpater src = new MapEncoderAdpater();
        src.putLong(field, i);
        StringBuilder dest = new StringBuilder();
        // Do encode
        service.writeTo(src,  dest,   field, 'y', false);
        String encoded = dest.toString();
        // Decode
        MapEncoderAdpater decodedMap = new MapEncoderAdpater();
        service.readTo(decodedMap, encoded, field);
        long decoded = decodedMap.getLong(field);
        if (printIt) {
            System.out.println(String.format("Encoded Message (%s chars /%s) : %s for Long value %s", encoded.length(), fullSize, encoded, i));

        }
        Assert.assertEquals(i, decoded);
    }


}
