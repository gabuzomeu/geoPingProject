package eu.ttbox.geoping.ui.widget;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import eu.ttbox.geoping.R;
import eu.ttbox.geoping.core.Intents;
import eu.ttbox.geoping.domain.PersonProvider;
import eu.ttbox.geoping.domain.person.PersonHelper;

/**
 * {link http://www.java2s.com/Code/Android/UI/WeatherListWidget.htm}
 */
@TargetApi(11)
public class PersonWidgetService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
	}

	/**
	 * This is the factory that will provide data to the collection widget.
	 */
	class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
		private Context mContext;
		private Cursor mCursor;
		private int mAppWidgetId;
		private PersonHelper helper;

		public StackRemoteViewsFactory(Context context, Intent intent) {
			mContext = context;
			mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			helper = new PersonHelper();
		}

		public void onCreate() {
			// Since we reload the cursor in onDataSetChanged() which gets
			// called immediately after
			// onCreate(), we do nothing here.
		}

		public void onDestroy() {
			if (mCursor != null) {
				mCursor.close();
			}
		}

		public int getCount() {
			return mCursor.getCount();
		}

		public RemoteViews getViewAt(int position) {
			// Get the data for this position from the content provider
			String displayName = null;
			String phoneNumber = null;
			if (mCursor.moveToPosition(position)) {
				displayName = helper.getPersonDisplayName(mCursor);
				phoneNumber = helper.getPersonPhone(mCursor);
//TODO               PhotoThumbmailCache photoCache = GeoPingApplication.getInstance().getPhotoThumbmailCache();
 				if (TextUtils.isEmpty(displayName)) {
					displayName = phoneNumber;
				}
			}

			RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_person_item);
			rv.setTextViewText(R.id.widget_person_item_displayName, displayName);
 			// Set the click intent so that we can handle it and show a toast
			// message
			final Intent fillInIntent = new Intent() //
					.putExtra(Intents.EXTRA_SMS_PHONE, phoneNumber);
			// final Intent fillInIntent =
			// Intents.sendSmsGeoPingRequest(mContext, phoneNumber);
			rv.setOnClickFillInIntent(R.id.widget_person_item_displayName, fillInIntent);

			return rv;
		}

		public RemoteViews getLoadingView() {
			// We aren't going to return a default loading view in this sample
			return null;
		}

		public int getViewTypeCount() {
			// Technically, we have two types of views (the dark and light
			// background views)
			return 2;
		}

		public long getItemId(int position) {
			return position;
		}

		public boolean hasStableIds() {
			return true;
		}

		public void onDataSetChanged() {
			// Refresh the cursor
			if (mCursor != null) {
				mCursor.close();
			}

			Thread thread = new Thread() {
				public void run() {
					// TODO WidgetProvider.Constants.CONTENT_URI_PERSON
					final ContentResolver cr = mContext.getContentResolver();
					mCursor = cr.query(PersonProvider.Constants.CONTENT_URI, null, null, null, null);
					mCursor.moveToFirst();
					helper.initWrapper(mCursor);
				}
			};
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

	}
}
