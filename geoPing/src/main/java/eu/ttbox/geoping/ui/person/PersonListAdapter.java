package eu.ttbox.geoping.ui.person;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.ui.person.PhotoEditorView.EditorListener;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;

public class PersonListAdapter extends android.support.v4.widget.ResourceCursorAdapter {

	private static final String TAG = "PersonListAdapter";

	private PersonHelper helper;

	private boolean isNotBinding = true;

	private Context context;
	// Cache
	private PhotoThumbmailCache photoCache;
	
	// Listeners
	private PersonListItemListener personListItemListener;
	
	// ===========================================================
	// Constructors
	// ===========================================================

	public PersonListAdapter(Context context, Cursor c, int flags ) {
		super(context, R.layout.track_person_list_item, c, flags);
		this.context = context; 
		// Cache
		photoCache = ((GeoPingApplication) context.getApplicationContext()).getPhotoThumbmailCache();

	}

	private void intViewBinding(View view, Context context, Cursor cursor) {
		// Init Cursor
		helper = new PersonHelper().initWrapper(cursor);
		isNotBinding = false;
	}

	@Override
	public void bindView(final View view, final Context context, Cursor cursor) {

		if (isNotBinding) {
			intViewBinding(view, context, cursor);
		}
		final ViewHolder holder = (ViewHolder) view.getTag();


		// Value
		final String phoneNumber = helper.getPersonPhone(cursor);
		final String contactId = helper.getContactId(cursor);
		final long personId = helper.getPersonId(cursor);
		String personName = helper.getPersonDisplayName(cursor);
		int color = helper.getPersonColor(cursor);
		// Bind Value
		holder.phoneText.setText(phoneNumber);
		holder.nameText.setText(personName);
		// Color
		Drawable stld = PersonColorDrawableHelper.getListBackgroundColor(color);

//		 if (VersionUtils.isJb16 ) {
//		    view.setBackground(stld);
//		 } else {
  		   view.setBackgroundDrawable(stld);
//         }

		// Photo
        photoCache.loadPhoto(context, holder.pingButton, contactId, phoneNumber);

		// Button
		holder.pingButton.setEditorListener(new EditorListener() {
			@Override
			public void onRequest(View v, int request) { 
				if (personListItemListener != null) {
					personListItemListener.onClickPing(v, personId, phoneNumber);
 				}
			}
		});
		holder.editButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (personListItemListener != null) {
                    personListItemListener.onClickEditPerson(v, personId, phoneNumber);
                }

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
		holder.editButton = (ImageButton) view.findViewById(R.id.person_list_item_editButton);
		// Do not work if set in the xml laytou file
		holder.editButton.setFocusable(false);
        holder.editButton.setFocusableInTouchMode(false);
		view.setTag(holder);
		return view;

	}

	static class ViewHolder {
		ImageButton editButton;
		TextView nameText;
		TextView phoneText;
		PhotoEditorView pingButton;
	//	PhotoLoaderAsyncTask photoLoaderAsyncTask;
	}

	// ===========================================================
	// Listeners
	// ===========================================================
	 
	
	public void setPersonListItemListener(PersonListItemListener personListItemListener) {
		this.personListItemListener = personListItemListener;
	}

	public interface PersonListItemListener {
		
		public void onClickEditPerson(View v, long personId, String phoneNumber);
		public void onClickPing(View v,  long personId, String phoneNumber);

		 
	}

	
	// ===========================================================
	// Others
	// ===========================================================

}
