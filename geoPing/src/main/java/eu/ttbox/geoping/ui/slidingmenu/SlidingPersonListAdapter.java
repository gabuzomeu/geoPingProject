package eu.ttbox.geoping.ui.slidingmenu;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import eu.ttbox.geoping.GeoPingApplication;
import eu.ttbox.geoping.R;
import eu.ttbox.geoping.domain.person.PersonHelper;
import eu.ttbox.geoping.ui.person.PersonColorDrawableHelper;
import eu.ttbox.geoping.ui.person.PhotoEditorView;
import eu.ttbox.geoping.ui.person.PhotoEditorView.EditorListener;
import eu.ttbox.geoping.utils.contact.PhotoThumbmailCache;


public class SlidingPersonListAdapter extends android.support.v4.widget.ResourceCursorAdapter {

    private static final String TAG = "SlidingPersonListAdapter";

    private PersonHelper helper;

    private boolean isNotBinding = true;

    private Context context;
    // Cache
    private PhotoThumbmailCache photoCache;
    
    // Listeners
    private SlidingMenuPersonListItemListener personListItemListener;

    // ===========================================================
    // Constructors
    // ===========================================================

    public SlidingPersonListAdapter(Context context, Cursor c, int flags ) {
        super(context, R.layout.slidingmenu_person_list_item, c, flags);
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
        // Cancel any pending thumbnail task, since this view is now bound to
        // new thumbnail
        //final PhotoLoaderAsyncTask oldTask = holder.photoLoaderAsyncTask;
       // if (oldTask != null) {
        //    oldTask.cancel(false);
        //}

        // Value
        final String phoneNumber = helper.getPersonPhone(cursor);
        final String contactId = helper.getContactId(cursor);
        final long personId = helper.getPersonId(cursor);
        String personName = helper.getPersonDisplayName(cursor);
        int color = helper.getPersonColor(cursor);
        // Bind Value 
        holder.nameText.setText(personName);
        holder.phoneText.setText(phoneNumber);
        // Color
        Drawable stld = PersonColorDrawableHelper.getListBackgroundColor(color);
        //
        // if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.JELLY_BEAN) {
        // view.setBackground(stld);
        // } else
        holder.pingButton.setBackgroundDrawable(stld);
     // Button
        holder.pingButton.setEditorListener(new EditorListener() {
            @Override
            public void onRequest(View v, int request) { 
                if (personListItemListener != null) {
                    personListItemListener.onClickPing(v, personId, phoneNumber);
                }
            }
        });
        // Photo
        photoCache.loadPhoto(context, holder.pingButton, contactId, phoneNumber);
//        if (!TextUtils.isEmpty(contactId)) {
  //          Bitmap cachedResult = photoCache.get(contactId);
    //        if (cachedResult != null) {
      //          holder.pingButton.setValues(cachedResult, false);
        //    } else {
          //      PhotoLoaderAsyncTask newTask = new PhotoLoaderAsyncTask(holder);
            //    holder.photoLoaderAsyncTask = newTask;
              //  newTask.execute(contactId, phoneNumber);
           // }
       // }
 
    }
    
    

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);
        // Then populate the ViewHolder
        ViewHolder holder = new ViewHolder();
        holder.nameText = (TextView) view.findViewById(R.id.person_list_item_name); 
        holder.pingButton = (PhotoEditorView) view.findViewById(R.id.person_list_item_geoping_button);  
        holder.phoneText = (TextView) view.findViewById(R.id.person_list_item_phone); 
        view.setTag(holder);
        return view;

    }

    static class ViewHolder { 
        TextView nameText; 
        TextView phoneText; 
        PhotoEditorView pingButton;
      //  PhotoLoaderAsyncTask photoLoaderAsyncTask;
    }
    


    
    // ===========================================================
    // Listeners
    // ===========================================================

    public void setPersonListItemListener(SlidingMenuPersonListItemListener personListItemListener) {
        this.personListItemListener = personListItemListener;
    }

    public interface SlidingMenuPersonListItemListener {
        
//        public void onClickEditPerson(View v,  long personId, String phoneNumber);
        public void onClickPing(View v,  long personId, String phoneNumber);

         
    }

    
    // ===========================================================
    // Others
    // ===========================================================

}
