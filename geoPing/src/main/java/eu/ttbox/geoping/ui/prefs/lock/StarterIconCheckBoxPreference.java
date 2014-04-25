package eu.ttbox.geoping.ui.prefs.lock;


import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.domain.PairingProvider;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.pairing.PairingDatabase;
import eu.ttbox.geoping.ui.billing.ExtraFeatureHelper;
import eu.ttbox.geoping.ui.pairing.PairingListActivity;
import eu.ttbox.geoping.ui.prefs.comp.componentname.AbstractComponentNameCheckBoxPreference;

public class StarterIconCheckBoxPreference extends AbstractComponentNameCheckBoxPreference {

    private static final String TAG = "StarterIconCheckBoxPreference";


    public StarterIconCheckBoxPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public StarterIconCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StarterIconCheckBoxPreference(Context context) {
        super(context);
    }


    @Override
    public ComponentName getComponentNameBind() {
        return ExtraFeatureHelper.getComponentNameLaucherIcon(getContext());
    }

    @Override
    protected boolean isChangeValueValid(boolean newValueBoolean) {
        boolean isChangeValid = true;
        if (!newValueBoolean) {
            // TODO Check Valid condition for Hide Application
            isChangeValid = checkValidPairingUsers(getContext());
            Log.d(TAG, "### Check callChangeListener for newValueBoolean : " + newValueBoolean + " ==> isChangeValidForAccount : " + isChangeValid);
            // Not Valid Open the correct Intent
            if (!isChangeValid) {
                startIntentListPairing();
                Toast.makeText(getContext(), R.string.prefs_launcher_icon_error_missing_pairing_command_authorize, Toast.LENGTH_LONG).show();
            }
        }
        return isChangeValid;
    }


    private void startIntentListPairing() {
        Intent intent =  new Intent(getContext(), PairingListActivity.class) //
                .setAction(Intent.ACTION_VIEW);
        getContext().startActivity(intent);
    }

    private static boolean checkValidPairingUsers(Context context) {
        boolean result = false;
        ContentResolver cr = context.getContentResolver();
        String[] projection = new String[]{PairingDatabase.PairingColumns.COL_ID};
        String selection = String.format(Locale.getDefault(), "%s = ?", PairingDatabase.PairingColumns.COL_AUTHORIZE_TYPE);
        String[] selectionArgs = new String[]{PairingAuthorizeTypeEnum.AUTHORIZE_ALWAYS.getCodeAsString()};
        Cursor cursor = cr.query(PairingProvider.Constants.CONTENT_URI, projection, selection, selectionArgs, null);
        try {
            //PairingHelper helper = new PairingHelper().initWrapper(cursor);
            while (cursor.moveToNext()) {
               //  long id = helper.getPairingId(cursor);
                result  =true;
                return result;
            }
        } finally {
            cursor.close();
        }

        return result;
    }

}
