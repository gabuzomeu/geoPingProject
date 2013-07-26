package eu.ttbox.geoping.test.service.encoder.param;

import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;
import eu.ttbox.geoping.core.PhoneNumberUtils;
import eu.ttbox.geoping.encoder.model.MessageParamEnum;
import eu.ttbox.geoping.encoder.params.ParamEncoderHelper;
import eu.ttbox.geoping.service.billing.util.Base64;
import eu.ttbox.geoping.service.billing.util.Base64DecoderException;
import eu.ttbox.geoping.service.encoder.MessageEncoderHelper;

public class PhoneNumberEncodedTest extends AndroidTestCase {
    public static final String TAG = "PhoneNumberEncodedTest";

    public void testEncodeInternationnal() throws Base64DecoderException {
       
        String[] phones = new String[] { //
        "+336102030405" //
                , "+336 1 02 03 04 05" //
                , "+336 102 030 405" //
        };
        for (String phoneNumber : phones) {
            String preparePhoneNumber = preparePhoneNumber(phoneNumber);
            // Enocde
            Bundle extras = MessageEncoderHelper.writeToBundle(null, MessageParamEnum.PHONE_NUMBER, preparePhoneNumber);
            StringBuilder dest = new StringBuilder();
            SmsParamEncoderHelper.encodeMessage(extras, dest);
            Log.d(TAG, "PhoneNumber Encode  : " + phoneNumber
                    + " ==> " + dest.toString());
            // Decode
            Bundle decode =   ParamEncoderHelper.decodeMessageAsMap(dest.toString());
            String decodedPhone = MessageEncoderHelper.readString(decode, MessageParamEnum.PHONE_NUMBER);
            String preparedDecodedPhone = new String(Base64.decodeWebSafe(decodedPhone));
            Log.d(TAG, "PhoneNumber Decoded  : " + dest.toString() + " ==> " + preparedDecodedPhone);
            assertEquals(phones[0], preparedDecodedPhone);
        }
    }

    public void testEncodeNationnal() throws Base64DecoderException {
     
        String[] phones = new String[] { //
        "060102030405" //
                , "06 01 02 03 04 05" //
                , "060 102 030 405" //
        };
        for (String phoneNumber : phones) {
            String preparePhoneNumber = preparePhoneNumber(phoneNumber);
            // Enocde
            Bundle extras = MessageEncoderHelper.writeToBundle(null, MessageParamEnum.PHONE_NUMBER, preparePhoneNumber);
            StringBuilder dest = new StringBuilder();
            SmsParamEncoderHelper.encodeMessage(extras, dest);
            Log.d(TAG, "PhoneNumber Encode  : " + preparePhoneNumber + " ==> " + dest.toString());
            // Decode
            MessageEncoderHelper.de
            Bundle decode = SmsParamEncoderHelper.decodeMessageAsMap(dest.toString());
            String decodedPhone = MessageEncoderHelper.readString(decode, MessageParamEnum.PHONE_NUMBER );
            String preparedDecodedPhone = new String(Base64.decodeWebSafe(decodedPhone));
            Log.d(TAG, "PhoneNumber Decoded  : " + dest.toString() + " ==> " + preparedDecodedPhone);
            assertEquals(phones[0], preparedDecodedPhone);
        }
    }

    private String preparePhoneNumber(String phoneNumber) {
        String normailizedPhone = PhoneNumberUtils.normalizeNumber(phoneNumber);
        String preparePhoneNumber = Base64.encodeWebSafe(normailizedPhone.getBytes(), false);
        Log.d(TAG, "Prepare PhoneNumber Encode  : " //
                + phoneNumber + " (" + phoneNumber.length() + ")" //
                + " ==> " + preparePhoneNumber + " (" + preparePhoneNumber.length() + ")" //
        );
        return preparePhoneNumber;
    }
}
