package eu.ttbox.geoping.utils;


import android.content.Context;
import android.os.Bundle;

import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.slave.GeoPingSlaveLocationService;

public class GeoPingCommandHelper {


    public static void sendGeopingLocationCheckinDeclaration(Context context, String phoneNumber, Bundle params) {
        String[] phones = new String[]{phoneNumber};
        MessageActionEnum smsAction = MessageActionEnum.LOC_DECLARATION;
        // Send to service
        sendGeopingLocationCheckin(context, phones, smsAction, params);
    }

    public static void sendGeopingLocationCheckin(Context context,  String[] phones,  MessageActionEnum smsAction, Bundle params) {
        GeoPingSlaveLocationService.runFindLocationAndSendInService(context, smsAction, phones, params, null);
       // LocationChangeReceiver.requestSingleUpdate(context, phones, smsAction, params);

    }

}
