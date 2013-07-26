package eu.ttbox.geoping.service.slave.eventspy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import eu.ttbox.geoping.core.AppConstants;
import eu.ttbox.geoping.domain.pairing.PairingDatabase.PairingColumns;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;

/**
 * {@link http
 * ://stackoverflow.com/questions/10528464/how-to-monitor-sim-state-change}
 * 
 * @author jmorille
 * 
 */
public class SimChangeReceiver extends BroadcastReceiver {

	private static final String TAG = "SimChangeReceiver";

	/**
	 * com.android.internal.telephony.TelephonyIntents.ACTION_SIM_STATE_CHANGED
	 * 
	 * Broadcast Action: The sim card state has changed. The intent will have
	 * the following extra values:</p>
	 * <ul>
	 * <li><em>phoneName</em> - A string version of the phone name.</li>
	 * <li><em>ss</em> - The sim state. One of <code>"ABSENT"</code>
	 * <code>"LOCKED"</code> <code>"READY"</code> <code>"ISMI"</code>
	 * <code>"LOADED"</code></li>
	 * <li><em>reason</em> - The reason while ss is LOCKED, otherwise is null
	 * <code>"PIN"</code> locked on PIN1 <code>"PUK"</code> locked on PUK1
	 * <code>"NETWORK"</code> locked on Network Personalization</li>
	 * </ul>
	 * 
	 * <p class="note">
	 * Requires the READ_PHONE_STATE permission.
	 * 
	 * <p class="note">
	 * This is a protected intent that can only be sent by the system.
	 */
	private static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";

	private static final String EXTRA_SIM_STATE = "ss";
	private static final String SIM_STATE_LOADED = "LOADED";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (ACTION_SIM_STATE_CHANGED.equals(action)) {
			Bundle extras = intent.getExtras();
			printExtras(extras);
			String state = extras.getString(EXTRA_SIM_STATE);
			Log.w(TAG, "SIM Action : " + action + " / State : " + state);
			// Test phoneName = GSM ?
			if (SIM_STATE_LOADED.equals(state)) {
				// Read Phone number
				String phoneNumber = getSystemPhoneNumber(context);
				if (TextUtils.isEmpty(phoneNumber)) {
					Log.e(TAG, "EventSpy : No phone number Readable in TelephonyManager");
				} else {
					// Compare to preference
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
					String prefPhone = prefs.getString(AppConstants.PREFS_EVENT_SPY_SIMCHANGE_PHONENUMBER, null);
					if (TextUtils.isEmpty(prefPhone)) {
						// Save as New Phone
						savePrefsPhoneNumber(prefs, phoneNumber);
						Log.w(TAG, "EventSpy Register for SIM Change Listener Phone Number : " + phoneNumber);
						// TODO Need to notify ?
					} else if (!prefPhone.equals(phoneNumber)) {
						Log.w(TAG, "EventSpy SIM Change for new Phone Number : " + phoneNumber);
						// Send message
						sendSpyNotifSms(context, prefPhone, phoneNumber);
						// Save as New Phone
						// NO savePrefsPhoneNumber(prefs, phoneNumber);
					}
				}
			}

		}
	}

	private void sendSpyNotifSms(Context context, String prefPhone,  String phoneNumber) {
	    String[] phones = SpyNotificationHelper.searchListPhonesForNotif(context, PairingColumns.COL_NOTIF_SIM_CHANGE);
		if (phones != null) {
		    Bundle params = new Bundle();
		    // TODO Manage prefPhone et phoneNumber
			// Send Sms
			SpyNotificationHelper.sendEventSpySmsMessage(context, phones, MessageActionEnum.SPY_SIM_CHANGE, params);
		}
	}

	private void printExtras(Bundle extras) {
		if (extras != null) { 
			for (String key : extras.keySet()) {
				Object value = extras.get(key);
				Log.d(TAG, "EventSpy SIM extras : " + key + " = " + value);
			}
		}
	}

	private static String getSystemPhoneNumber(Context context) {
		// Read Phone number
		TelephonyManager telephoneMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String phoneNumber = telephoneMgr.getLine1Number();
		Log.d(TAG, "EventSpy SIM DeviceId : " + telephoneMgr.getDeviceId()); // Code IMEI
		Log.d(TAG, "EventSpy SIM Network Operator Name : " + telephoneMgr.getNetworkOperatorName());
		Log.d(TAG, "EventSpy SIM Serial Number : " + telephoneMgr.getSimSerialNumber());
		Log.d(TAG, "EventSpy SIM PhoneNumber : " + phoneNumber); // Code IMEI
		return phoneNumber;
	}

	public static String savePrefsPhoneNumber(Context context) {
		String phoneNumber = getSystemPhoneNumber(context);
		if (TextUtils.isEmpty(phoneNumber)) {
		    Log.w(TAG, "No Phone number assciate with this Phone");
			return null;
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String prefPhone = prefs.getString(AppConstants.PREFS_EVENT_SPY_SIMCHANGE_PHONENUMBER, null);
		if (prefPhone == null || !phoneNumber.equals(prefPhone)) {
			savePrefsPhoneNumber(prefs, phoneNumber);
		}
		return phoneNumber;
	}

	public static void savePrefsPhoneNumber(SharedPreferences prefs, String phoneNumber) {
		if (!TextUtils.isEmpty(phoneNumber)) {
			SharedPreferences.Editor prefEditor = prefs.edit();
			prefEditor.putString(AppConstants.PREFS_EVENT_SPY_SIMCHANGE_PHONENUMBER, phoneNumber);
			prefEditor.commit();
			Log.d(TAG, "Register Sim Change Phone : " + phoneNumber);
		} else {
			Log.w(TAG, "No Phone number to save in pref Key : " + AppConstants.PREFS_EVENT_SPY_SIMCHANGE_PHONENUMBER);
		}
	}

}
