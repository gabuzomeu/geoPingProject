package eu.ttbox.geoping.ui.pairing;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.NotifToasts;
import eu.ttbox.geoping.domain.model.PairingAuthorizeTypeEnum;
import eu.ttbox.geoping.domain.pairing.PairingHelper;
import eu.ttbox.geoping.encoder.model.MessageActionEnum;
import eu.ttbox.geoping.service.slave.GeoPingSlaveLocationService;
import eu.ttbox.geoping.ui.person.PhotoEditorView;
import eu.ttbox.geoping.ui.person.PhotoEditorView.EditorListener;
import eu.ttbox.geoping.ui.person.PhotoThumbmailCache;import eu.ttbox.geoping.ui.person.PhotoThumbmailCache.PhotoLoaderAsyncTask;

import eu.ttbox.geoping.ui.person.PhotoThumbmailCache.PhotoLoaderAsyncTask;

public class PairingListAdapter extends android.support.v4.widget.ResourceCursorAdapter {

	private Context context;
	private PairingHelper helper;

	private boolean isNotBinding = true;

	private String contentDescAuthAlways;
	private String contentDescAuthRequest;
	private String contentDescAuthNever;

	// Cache
	private PhotoThumbmailCache photoCache;

	public PairingListAdapter(Context context, Cursor c, int flags) {
		super(context, R.layout.pairing_list_item, c, flags); // if >10 add
																// ", flags"
		this.context = context;
	}

	private void intViewBinding(View view, Context context, Cursor cursor) {
		// Init Cursor
		helper = new PairingHelper().initWrapper(cursor);
		isNotBinding = false;
		// Image
		Resources r = context.getResources();
		contentDescAuthAlways = r.getString(R.id.pairing_authorize_type_radio_always);
		contentDescAuthNever = r.getString(R.id.pairing_authorize_type_radio_never);
		contentDescAuthRequest = r.getString(R.id.pairing_authorize_type_radio_ask);
		// Cache
		photoCache = ((GeoPingApplication) context.getApplicationContext()).getPhotoThumbmailCache();
	}

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {

		if (isNotBinding) {
			intViewBinding(view, context, cursor);
		}
		ViewHolder holder = (ViewHolder) view.getTag();
		// Cancel any pending thumbnail task, since this view is now bound to
		// new thumbnail
	//	final PhotoLoaderAsyncTask oldTask = holder.photoLoaderAsyncTask;
	//	if (oldTask != null) {
	//		oldTask.cancel(false);
	//	}
		// Bind Value
        final String contactId = helper.getContactId(cursor);
		final String phoneNumber = helper.getPairingPhone(cursor);
		holder.phoneText.setText(phoneNumber);
		helper.setTextPairingName(holder.nameText, cursor);
		// Photo
        photoCache.loadPhoto(context, holder.pingButton, contactId, phoneNumber);
	//	if (!TextUtils.isEmpty(phoneNumber)) {
	//		Bitmap cachedResult = photoCache.get(phoneNumber);
	//		if (cachedResult != null) {
	//			holder.pingButton.setValues(cachedResult, false);
		//	} else {
       //  		PhotoLoaderAsyncTask newTask = photoCache.getPhotoLoaderAsyncTask(context, holder.pingButton);
		//		holder.photoLoaderAsyncTask = newTask;
//				newTask.execute(contactId, phoneNumber);
		//	}
	//	}
		// Button
		holder.pingButton.setEditorListener(new EditorListener() {
			@Override
			public void onRequest(View v, int request) {
				 GeoPingSlaveLocationService.runFindLocationAndSendInService(context , MessageActionEnum.LOC_DECLARATION, new String[] { phoneNumber }, null,  null);
 				// Notif
				NotifToasts.showToastSendGeoPingResponse(context, phoneNumber);
			}
		});

		// Backgroud
		PairingAuthorizeTypeEnum authType = helper.getPairingAuthorizeTypeEnum(cursor);
		if (authType!=null) {
		switch (authType) {
		case AUTHORIZE_ALWAYS:
			// view.setBackgroundResource(R.color.pairing_authorize_type_always);
			// holder.authType.setImageDrawable( drawableAuthAlways);
			holder.authType.setImageResource(R.drawable.ic_cadenas_ouvert_vert);
			holder.authType.setContentDescription(contentDescAuthAlways);
			break;
		case AUTHORIZE_NEVER:
			// view.setBackgroundResource(R.color.pairing_authorize_type_never);
			holder.authType.setImageResource(R.drawable.ic_cadenas_ferme_rouge);
			holder.authType.setContentDescription(contentDescAuthNever);
			break;
		case AUTHORIZE_REQUEST:
			// view.setBackgroundResource(R.color.pairing_authorize_type_request);
			holder.authType.setImageResource(R.drawable.ic_cadenas_entrouvert_jaune);
			holder.authType.setContentDescription(contentDescAuthRequest);
			break;
		default:
			holder.authType.setImageDrawable(null);
			// view.setBackgroundResource(android.R.color.transparent);
			break;
		}
		}  
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);
		// Then populate the ViewHolder
		ViewHolder holder = new ViewHolder();
		holder.nameText = (TextView) view.findViewById(R.id.pairing_list_item_name);
		holder.phoneText = (TextView) view.findViewById(R.id.pairing_list_item_phone);
		holder.pingButton = (PhotoEditorView) view.findViewById(R.id.pairing_list_item_geoping_button);
		holder.pingButton.setFocusable(false);
		holder.pingButton.setFocusableInTouchMode(false);
		holder.authType = (ImageView) view.findViewById(R.id.pairing_list_item_authType);
		// and store it inside the layout.
		view.setTag(holder);
		return view;

	}

	static class ViewHolder {
		PhotoEditorView pingButton;
		TextView nameText;
		TextView phoneText;
		ImageView authType;
	//	PhotoLoaderAsyncTask photoLoaderAsyncTask;
	}

	// ===========================================================
	// Others
	// ===========================================================

}
