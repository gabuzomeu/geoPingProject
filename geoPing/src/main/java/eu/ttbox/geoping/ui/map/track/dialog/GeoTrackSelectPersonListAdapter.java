package eu.ttbox.geoping.ui.map.track.dialog;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.Map;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.model.Person;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.ui.map.track.GeoTrackOverlay;
import eu.ttbox.geoping.ui.person.PersonColorDrawableHelper;
import eu.ttbox.geoping.ui.person.PhotoEditorView;
import eu.ttbox.geoping.ui.person.PhotoEditorView.EditorListener;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;

public class GeoTrackSelectPersonListAdapter extends android.support.v4.widget.ResourceCursorAdapter {

	private static final String TAG = "GeoTrackSelectPersonListAdapter";

	private Context context;
	private PersonHelper helper;

	private boolean isNotBinding = true;
	private  Map<String, GeoTrackOverlay> geoTrackOverlayByUser;

	// Cache
	PhotoThumbmailCache photoCache;

	private final OnActivatedPersonListener mCallBack;

	public interface OnActivatedPersonListener {
		void onDoRemovePerson(Person person);

		void onDoAddPerson(Person person);
	}

	public GeoTrackSelectPersonListAdapter(Context context, Cursor c, int flags, OnActivatedPersonListener mCallBack,  Map<String, GeoTrackOverlay> geoTrackOverlayByUser) {
		super(context, R.layout.map_geotrack_select_dialog_list_item, c, flags); // if
																					// >10
																					// add
																					// ", flags"
		this.context = context;
		this.geoTrackOverlayByUser = geoTrackOverlayByUser;
		this.mCallBack = mCallBack;
		// Cache
		photoCache = ((GeoPingApplication) context.getApplicationContext()).getPhotoThumbmailCache();

	}

	private void intViewBinding(View view, Context context, Cursor cursor) {
		// Init Cursor
		helper = new PersonHelper().initWrapper(cursor);
		isNotBinding = false;
	}

	@Override
	public void bindView(View view, final Context context, final Cursor cursor) {
		if (isNotBinding) {
			intViewBinding(view, context, cursor);
		}
		ViewHolder holder = (ViewHolder) view.getTag();


		// Bind Value
		helper.setTextPersonName(holder.nameText, cursor)//
				.setTextPersonPhone(holder.phoneText, cursor);
		// Button
		final Person person = helper.getEntity(cursor);
		final String phoneNumber = helper.getPersonPhone(cursor);
		final String contactId = helper.getContactId(cursor);
		// Display Status
		boolean isActif = geoTrackOverlayByUser.containsKey(phoneNumber);
		holder.selectedSelector.setChecked(isActif);
		// Color
		int color = helper.getPersonColor(cursor);
		Drawable stld = PersonColorDrawableHelper.getListBackgroundColor(color);
		view.setBackgroundDrawable(stld);
		// Button
		holder.selectedSelector.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mCallBack != null) {
					CheckBox tb = (CheckBox) v;
					if (tb.isChecked()) {
						mCallBack.onDoAddPerson(person);
					} else {
						mCallBack.onDoRemovePerson(person);
					}
				}

			}
		});

		// Photo
        photoCache.loadPhoto(context, holder.pingButton, contactId, phoneNumber);

		// Action
		holder.pingButton.setEditorListener(new EditorListener() {
			@Override
			public void onRequest(View v, int request) {
				context.startService(Intents.sendSmsGeoPingRequest(context, phoneNumber));
			}
		});
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = super.newView(context, cursor, parent);
		// Then populate the ViewHolder
		ViewHolder holder = new ViewHolder();
		holder.nameText = (TextView) view.findViewById(R.id.person_list_item_name);
		holder.phoneText = (TextView) view.findViewById(R.id.person_list_item_phone);
		holder.pingButton = (PhotoEditorView) view.findViewById(R.id.person_list_item_geoping_button);
		holder.selectedSelector = (CheckBox) view.findViewById(R.id.person_list_item_status_selected);
		// Backgroup Shape

		// normal.getPaint().setShader(shader);
		// normal.setPadding(7, 3, 7, 5);
		// and store it inside the layout.
		view.setTag(holder);
		return view;
	}

	static class ViewHolder {
		PhotoEditorView pingButton;
		TextView nameText;
		TextView phoneText;
		CheckBox selectedSelector;
	//	PhotoLoaderAsyncTask photoLoaderAsyncTask;
	}

	// ===========================================================
	// Listeners
	// ===========================================================


	// ===========================================================
	// Others
	// ===========================================================

}
