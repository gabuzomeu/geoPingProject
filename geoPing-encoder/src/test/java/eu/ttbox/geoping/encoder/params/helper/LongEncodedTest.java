package eu.ttbox.geoping.encoder.params.helper;


import org.junit.Assert;
import org.junit.Test;

public class LongEncodedTest {


    public static long[] VALUES_TESTED = new long[] { //
            -1, -2, -3, -7, -42, -73, 0, 1, 2, 3, 7, 42, 73, //
            Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2//
            , Integer.MIN_VALUE, Integer.MAX_VALUE //
    };

    @Test
    public void testAlphabet() {
        int radix = LongEncoded.MAX_RADIX;
        int i = 0;

        for (char c : IntegerEncoded.ALPHABET) {
            String str = String.valueOf(c);
            int decoded = IntegerEncoded.parseInt(str, radix);
            Assert.assertEquals(i++, decoded);
        }
    }

    @Test
    public void testEncodeMaxRadix() {
        int radix = IntegerEncoded.MAX_RADIX;
        for (int i = 0; i < 2000; i++) {
            doEncodeDecodeTest(i, radix);
        }
        for (long i : VALUES_TESTED) {
            doEncodeDecodeTest(i, radix);
        }
    }

    @Test
    public void testEncodeMulTiRadix() {
        for (int radix : new int[] { 4, 5, 10, 36, LongEncoded.MAX_RADIX }) {
            for (int i = 0; i < 2000; i++) {
                doEncodeDecodeTest(i, radix, false);
            }
            for (long i : VALUES_TESTED) {
                doEncodeDecodeTest(i, radix, false);
            }
        }
    }

    private void doEncodeDecodeTest(long i, int radix) {
        doEncodeDecodeTest(i, radix, true);
    }

    private void doEncodeDecodeTest(long i, int radix, boolean printIt) {
        int fullSize = String.valueOf(i).length();
        String encoded = LongEncoded.toString(i, radix);
        long decoded = LongEncoded.parseLong(encoded, radix);
        Assert.assertEquals(i, decoded);
        if (printIt) {
             System.out.println(String.format("Encoded Message (%s chars /%s) : %s for Long value %s", encoded.length(), fullSize, encoded, i));
        }
    }


}
