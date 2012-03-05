package com.monstersfromtheid.imready;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.monstersfromtheid.imready.client.Meeting;
import com.monstersfromtheid.imready.client.MessageAPI;
import com.monstersfromtheid.imready.client.MessageAPIException;

public class MyMeetings extends ListActivity{
	public static final int ACTIVITY_CREATE_MEETING = 0;
	public static final int ACTIVITY_VIEW_MEETING   = 1;

	private ArrayList<HashMap<String, Object>> meetings = new ArrayList<HashMap<String, Object>>();
	private String[] from = new String[] { "name", "readiness", "recently" };
	private int[] to = new int[] { R.id.meeting_list_item_name,  
			R.id.meeting_list_item_readiness,
			R.id.meeting_list_item_decoration};
	private SimpleAdapter adapter; 
	private Button createMeetingButton;
	private ResponseReceiver receiver;

	public static class MeetingUpdate {
		public enum UpdateType { NEW, CHANGE, VIEWED };

		private Meeting updatedMeeting;
		private UpdateType updateType;

		public MeetingUpdate(Meeting updatedMeeting, UpdateType updateType) {
			this.updatedMeeting = updatedMeeting;
			this.updateType = updateType;
		}

		public UpdateType getUpdateType() { return updateType; }
		public Meeting getUpdatedMeeting() { return updatedMeeting; }
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Initialize the elements that will be used by the activity

		// Define an adapter to convert from a dataset to a list
		adapter = new SimpleAdapter(this, meetings, R.layout.meeting_list_item, from, to);
		adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				switch (view.getId()) {
				case R.id.meeting_list_item_readiness:
					((TextView)view).setTextColor((Boolean)data ? Color.GREEN : Color.RED);
					((TextView)view).setText((Boolean)data ? "Ready" : "Not ready");
					break;

				case R.id.meeting_list_item_decoration:
					if( ((String)data).equalsIgnoreCase("changed") ) {
						((ImageView)view).setImageResource(R.drawable.decoration_change);
					} else {
						((ImageView)view).setImageResource(R.drawable.decoration_none);
					}
					break;

				default:
					return false;
				}
				return true;
			}
		});

	   	// Set each list item to launch ViewMeeting
		getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
				HashMap<String, ?> info = meetings.get(position);
				int meetingId = (Integer)info.get("id");
				String name = (String)info.get("name");
				Uri internalMeetingUri = Uri.parse("content://com.monstersfromtheid.imready/meeting/" + meetingId + "/" + Uri.encode(name));
				startActivityForResult( new Intent(Intent.ACTION_VIEW, internalMeetingUri, MyMeetings.this, ViewMeeting.class), ACTIVITY_VIEW_MEETING );
			}
		});

		// Add a button to the list footer for creating a meeting
		createMeetingButton = (Button)getLayoutInflater().inflate(R.layout.meetings_create_meeting_button, null);
		createMeetingButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivityForResult(new Intent(MyMeetings.this, CreateMeeting.class), ACTIVITY_CREATE_MEETING);
			}
		});
	   	getListView().addFooterView(createMeetingButton);

	   	// Prime the list with meetings we already know about
	   	List<Meeting> meetings;
	   	List<MeetingUpdate> updates = new ArrayList<MeetingUpdate>();
		try {
			meetings = MessageAPI.userMeetings(IMReady.getLastSeenMeetingsJSON(this));
			// TODO Get previously decorated meetings
			// Create update list from recorded decorated list
			updateView(meetings, updates);
			
		} catch (MessageAPIException e) {
		}

		setListAdapter(adapter);
	}

	// The CheckMeetingsService broadcasts when it sees a change to it's list of meetings.
	// NB We need to use a BroadcastReceiver as the UI thread is the only one that can modifying the View. 
	public class ResponseReceiver extends BroadcastReceiver {
		public static final String ACTION_RESP = "com.monstersfromtheid.imready.MEETING_CHANGES";

		public void onReceive(Context context, Intent intent) {
			processMeetingsUpdate();
		}
	}

	public void onStart() {
		super.onStart();

		// Register the broadcast receiver to catch change notifications
		IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
		filter.addCategory(Intent.CATEGORY_DEFAULT);
		receiver = new ResponseReceiver();
		registerReceiver(receiver, filter);

		// Sort alarm as quick
		IMReady.setNextAlarm(this, true);
		// Use a ScheduledThreadPoolExecutor to periodically call the mother-ship?
	}

	public void onStop() {
		super.onStop();

		// Cancel whatever ScheduledThreadPoolExecutor we were using?

		// Sort alarm as slow
		IMReady.setNextAlarm(this);

		unregisterReceiver(receiver);
		receiver = null;
	}

	// We've been notified of a change, so go get the latest information and handle it
	public void processMeetingsUpdate() {
		try {
			List<Meeting> meetings;
			List<MeetingUpdate> updates;
			meetings = MessageAPI.userMeetings(IMReady.getLastSeenMeetingsJSON(this));

			{
				List<Meeting> userAwareMeetings = MessageAPI.userMeetings(IMReady.getUserAwareMeetingsJSON(this));
				List<Meeting> lastSeenMeetings = MessageAPI.userMeetings(IMReady.getLastSeenMeetingsJSON(this));
				
				List<Meeting> newMeetings = IMReady.newMeetings(userAwareMeetings, lastSeenMeetings);
				List<Meeting> changedMeetings = IMReady.changedMeetings(userAwareMeetings, lastSeenMeetings);
				updates = new ArrayList<MeetingUpdate>(newMeetings.size() + changedMeetings.size());
				
				for (Meeting newMeeting : newMeetings) { updates.add(new MeetingUpdate(newMeeting, MeetingUpdate.UpdateType.NEW)); }
				for (Meeting changedMeeting : changedMeetings) { updates.add(new MeetingUpdate(changedMeeting, MeetingUpdate.UpdateType.CHANGE)); }
				//IMReady.readyMeetings(meetings); // ? Surely we only want meetings that changed to ready since we last looked?
			}

			// Roll up lastSeen in to userAware (and blank the dirty list?)
			IMReady.setUserAwareMeetingsJSON(IMReady.getLastSeenMeetingsJSON(this), this);
			//IMReady.setDirtyMeetings(new ArrayList<Integer>(), this);

			// Set-up the list model with the meetings and annotations
			updateView(meetings, updates);
		} catch (MessageAPIException e) {
			// TODO: Handle failure - show error message...?
		}
	}

	// Take the meeting list and the update list and display them
	public void updateView(List<Meeting> meetings, List<MeetingUpdate> updates) {
		clearMeetings();
		for (Meeting meeting : meetings) {
			addMeeting(meeting.getName(), meeting.getId(), meeting.getState() == Meeting.STATE_READY);
		}
		for (MeetingUpdate update : updates) {
			decorateMeeting(update.getUpdatedMeeting().getId(), update.getUpdateType());
		}
		adapter.notifyDataSetChanged();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		switch (requestCode) {
		case ACTIVITY_CREATE_MEETING:
			
//			  TODO: Replace with appropriate code
//			String userName = IMReady.getUserName(this);
//			
//			final ServerAPI api = new ServerAPI(userName);
//			ServerAPI.performInBackground(new RefreshMeetingsAction(api));

			//addMeeting();

			if(resultCode == RESULT_OK) {
				int meetingId = data.getIntExtra(IMReady.RETURNS_MEETING_ID, -1);
				if (meetingId == -1){
					return;
				}
				String name = data.getStringExtra(IMReady.RETURNS_MEETING_NAME);
				Uri internalMeetingUri = Uri.parse("content://com.monstersfromtheid.imready/meeting/" + meetingId + "/" + Uri.encode(name)); // TODO hackish
				startActivity( new Intent(Intent.ACTION_VIEW, internalMeetingUri, MyMeetings.this, ViewMeeting.class) );
			}
			break;
			
		case ACTIVITY_VIEW_MEETING:
			if(resultCode == RESULT_OK) {
				try{
					decorateMeeting(data.getIntExtra(IMReady.RETURNS_MEETING_ID, 0),MeetingUpdate.UpdateType.VIEWED);
					adapter.notifyDataSetChanged();
				} catch (IndexOutOfBoundsException e) {
					// Meeting view returned something odd - do nothing
				}
			}

		default:
			/* If returning after ACTIVITY_GOT_ACCOUNT then just exit this Activity */
			break;
		}
	}

	private void clearMeetings() {
		meetings.clear();
	}

	private void addMeeting(String name, int meetingId, boolean readiness) {
		HashMap<String, Object> userItem = new HashMap<String, Object>();
		userItem.put("id", meetingId);
		userItem.put("name", name);
		userItem.put("readiness", readiness);
		userItem.put("recently", "none");
		meetings.add(userItem);
	}

	private void decorateMeeting(int meetingId, MeetingUpdate.UpdateType type) {
		Iterator<HashMap<String, Object>> iterMeetings = meetings.iterator();
		while(iterMeetings.hasNext()){
			HashMap<String, Object> meetingItem = iterMeetings.next();
			if( (Integer)meetingItem.get("id") == meetingId ){
				switch (type) {
				case NEW:
					meetingItem.put("recently", "created");
					break;
				case CHANGE:
					meetingItem.put("recently", "changed");
					break;
				case VIEWED:
					meetingItem.put("recently", "none");
					break;
				default:
					// Do nothing
				}
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_account_pref, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_account:
			// Open the accounts page
			Uri resetAccountDetails = Uri.parse("content://com.monstersfromtheid.imready/util/" + IMReady.ACTIONS_ACOUNT_CHANGE_DETAILS);
			startActivity(new Intent(Intent.ACTION_VIEW, resetAccountDetails, MyMeetings.this, DefineAccount.class));
			return true;
		case R.id.menu_preferences:
			// Open the preferences page
			startActivity(new Intent( MyMeetings.this, Preferences.class ));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
