package eu.ttbox.geoping.encoder;


import org.junit.Test;

import java.util.List;

import eu.ttbox.geoping.encoder.adapter.MapEncoderAdpater;

public class SmsDecoderReaderTest {

    @Test
    public void testReadSms() {
        // insert geoTrack UserId [XXX] with Uri : content://eu.ttbox.geoping.GeoTrackerProvider/geoTrackPoint/1660 with WSG84(48874008, 2334862)
        String encoded = "geoPing?fei(d2IHi0,aw,c0,t2IQIc,eQuartier,g3j4ms;9Np4;1k,pg)";
        MapEncoderAdpater decodedMessage = decodeMessage(encoded);
        System.out.println(decodedMessage);
    }

    private MapEncoderAdpater decodeMessage(String encoded) {
        String phone = "0123456789";
        List<MapEncoderAdpater> decodedMessages = SmsEncoderHelper.decodeSmsMessage(new MapEncoderAdpater(), phone, encoded, null);
        MapEncoderAdpater decodedMessage = decodedMessages.get(0);
        return decodedMessage;
    }

}
