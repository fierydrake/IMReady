package com.monstersfromtheid.imready;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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
import com.monstersfromtheid.imready.client.MessageAPIException;
import com.monstersfromtheid.imready.service.CheckMeetingsService;

public class MyMeetings extends ListActivity implements CheckMeetingsService.MeetingsChangeListener {
	public static final int ACTIVITY_CREATE_MEETING = 0;

	private ArrayList<HashMap<String, Object>> meetings = new ArrayList<HashMap<String, Object>>();
    private String[] from = new String[] { "name", "readiness", "recently" };
    private int[] to = new int[] { R.id.meeting_list_item_name,  
            R.id.meeting_list_item_readiness,
            R.id.meeting_list_item_decoration};
    private SimpleAdapter adapter; 
    private Button createMeetingButton; 

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set initial look of the activity
        View loadingSpinner = (View)getLayoutInflater().inflate(R.layout.meetings_loading_spinner, null);
        getListView().addHeaderView(loadingSpinner);

        // Initialize the elements that will be used by the activity
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
					} else if ( ((String)data).equalsIgnoreCase("created") ){
						((ImageView)view).setImageResource(R.drawable.decoration_change);
					}

				default:
					return false;
				}
            	return true;
            }
        });
        createMeetingButton = (Button)getLayoutInflater().inflate(R.layout.meetings_create_meeting_button, null);
        createMeetingButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		startActivityForResult(new Intent(MyMeetings.this, CreateMeeting.class), ACTIVITY_CREATE_MEETING);
        	}
        });
	}
	
    private CheckMeetingsService meetingsService;
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			meetingsService = ((CheckMeetingsService.LocalBinder)service).getService();
			meetingsService.addMeetingsChangeListener(MyMeetings.this);
		}
		public void onServiceDisconnected(ComponentName className) {
			meetingsService.removeMeetingsChangeListener(MyMeetings.this);
			meetingsService = null;
		}		
	};
	
	@Override
	public void onStart() {
		// TODO: bindService returns a boolean - should we be checking it?
        bindService(new Intent(this, CheckMeetingsService.class), serviceConnection, Context.BIND_NOT_FOREGROUND); // guessing on this flag
        
        processMeetingsUpdate();

       	getListView().addFooterView(createMeetingButton);
        getListView().setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parentView, View childView, int position, long id) {
				HashMap<String, ?> info = meetings.get(position);
				int meetingId = (Integer)info.get("id");
				String name = (String)info.get("name");
				Uri internalMeetingUri = Uri.parse("content://com.monstersfromtheid.imready/meeting/" + meetingId + "/" + Uri.encode(name)); // TODO hackish
                startActivity( new Intent(Intent.ACTION_VIEW, internalMeetingUri, MyMeetings.this, ViewMeeting.class) );
			}
        });
        setListAdapter(adapter);
	}
	
	@Override
	public void onStop() {
		unbindService(serviceConnection);
	}
        
	public void processMeetingsUpdate() {
		try {
	        List<Meeting> meetings;
	        List<CheckMeetingsService.MeetingUpdate> updates;
	        synchronized(meetingsService) {
		        // Get the status quo of meetings from the service
		        meetings = meetingsService.getMeetings();
		        // Get the meeting updates since last rollup from the service
		        updates = meetingsService.getMeetingUpdates();
		        // Tell the service to rollup the updates
		        meetingsService.rollupMeetingUpdates();
	        }
	        
	        // Set-up the list model with the meetings and annotations
	        updateView(meetings, updates);
		} catch (MessageAPIException e) {
			// TODO: Handle failure - show error message...?
		}
	}
	
	public void updateView(List<Meeting> meetings, List<CheckMeetingsService.MeetingUpdate> updates) {
		clearMeetings();
		for (Meeting meeting : meetings) {
			addMeeting(meeting.getName(), meeting.getId(), meeting.getState() == Meeting.STATE_READY);
		}
		for (CheckMeetingsService.MeetingUpdate update : updates) {
			decorateMeeting(update.getUpdatedMeeting().getId(), update.getUpdateType());
		}
		adapter.notifyDataSetChanged();
	}
	
	public void onMeetingsChange(CheckMeetingsService.MeetingsChangeEvent e) {
		processMeetingsUpdate();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* If returning after ACTIVITY_GOT_ACCOUNT then just exit this Activity */
        if (requestCode == ACTIVITY_CREATE_MEETING) {
//			  TODO: Replace with appropriate code
//            String userName = IMReady.getUserName(this);
//            
//            final ServerAPI api = new ServerAPI(userName);
//            ServerAPI.performInBackground(new RefreshMeetingsAction(api));

            if(resultCode == RESULT_OK) {
            	int meetingId = data.getIntExtra(IMReady.RETURNS_MEETING_ID, -1);
            	if (meetingId == -1){
            		return;
            	}
            	String name = data.getStringExtra(IMReady.RETURNS_MEETING_NAME);
            	Uri internalMeetingUri = Uri.parse("content://com.monstersfromtheid.imready/meeting/" + meetingId + "/" + Uri.encode(name)); // TODO hackish
            	startActivity( new Intent(Intent.ACTION_VIEW, internalMeetingUri, MyMeetings.this, ViewMeeting.class) );
            }
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
    
    private void decorateMeeting(int meetingId, CheckMeetingsService.MeetingUpdate.UpdateType type) {
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
    			default:
    				// Do nothing
    			}
    		}
    		break;
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
     MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_account_pref, menu);
        return true;
    }
    
//    private void storeMeetingJSON(String s){
//    	IMReady.setUserAwareMeetingsJSON(s, this);
//    	IMReady.setDirtyMeetings(new ArrayList<Integer>(), this);
//    }
    
    @Override
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
