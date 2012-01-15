package com.monstersfromtheid.imready;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.monstersfromtheid.imready.client.MessageAPI;
import com.monstersfromtheid.imready.client.MessageAPIException;
import com.monstersfromtheid.imready.client.ServerAPI;
import com.monstersfromtheid.imready.client.ServerAPI.Action;
import com.monstersfromtheid.imready.client.ServerAPICallFailedException;
import com.monstersfromtheid.imready.client.Meeting;

public class MyMeetings extends ListActivity {
	public static final int ACTIVITY_CREATE_MEETING = 0;

	private ArrayList<HashMap<String, ?>> meetings = new ArrayList<HashMap<String, ?>>();
    private String[] from = new String[] { "name", "readiness" };
    private int[] to = new int[] { R.id.meeting_list_item_name,  
            R.id.meeting_list_item_readiness };
    private SimpleAdapter adapter; 

    private class RefreshMeetingsAction extends Action<List<Meeting>> {
    	private ServerAPI api;
    	public RefreshMeetingsAction(ServerAPI api) {
    		this.api = api;
    	}
        @Override
        public List<Meeting> action() throws ServerAPICallFailedException {
        	try {
        		return MessageAPI.userMeetings( api.userMeetings(api.getRequestingUserId()) );
        	} catch (MessageAPIException e) {
        		// Just a quick hack to get me beyond this. 
				throw new ServerAPICallFailedException(e.getMessage(), e);
			}
        }
        @Override
        public void success(List<Meeting> meetings) {
            clearMeetings();
            for (Meeting meeting : meetings) {
                addMeeting(
                		meeting.getName(),
                		meeting.getId(),
                		meeting.getState() == Meeting.STATE_READY
                		);
            }
            adapter.notifyDataSetChanged();
        }
        @Override
        public void failure(ServerAPICallFailedException e) {
            Toast.makeText(MyMeetings.this, "Failed: " + e, Toast.LENGTH_LONG).show();
        }    	
    }

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String userName = IMReady.getUserName(this);
        
        final ServerAPI api = new ServerAPI(userName);

        adapter = new SimpleAdapter(this, meetings, R.layout.meeting_list_item, from, to);
//        initialiseActivityFromLocalKnowledge(meetingName, meetingId, userNickName, userName);
        ServerAPI.performInBackground(new RefreshMeetingsAction(api));
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.meeting_list_item_readiness) {
                    ((TextView)view).setTextColor((Boolean)data ? Color.GREEN : Color.RED);
                    ((TextView)view).setText((Boolean)data ? "Ready" : "Not ready");
                    return true;
                }
                return false;
            }
        });

        final Button createMeeting = (Button)getLayoutInflater().inflate(R.layout.meetings_create_meeting_button, null);
        createMeeting.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		startActivityForResult(new Intent(MyMeetings.this, CreateMeeting.class), ACTIVITY_CREATE_MEETING);
        	}
        });
        getListView().addFooterView(createMeeting);
        
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

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* If returning after ACTIVITY_GOT_ACCOUNT then just exit this Activity */
        if (requestCode == ACTIVITY_CREATE_MEETING) {
        	String userName = IMReady.getUserName(this);
            
            final ServerAPI api = new ServerAPI(userName);
            ServerAPI.performInBackground(new RefreshMeetingsAction(api));

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
        meetings.add(userItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
     MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_account:
            // Open the accounts page and blank the "activity history"
            Uri resetAccountDetails = Uri.parse("content://com.monstersfromtheid.imready/util/" + IMReady.ACTIONS_ACOUNT_CHANGE_DETAILS);
            startActivity(new Intent(Intent.ACTION_VIEW, resetAccountDetails, MyMeetings.this, DefineAccount.class));
            return true;
        case R.id.menu_blank: // TODO This should be removed eventually.
            SharedPreferences.Editor preferences = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).edit();
            preferences.remove("accountDefined");
            preferences.remove("accountUserName");
            preferences.remove("accountNickName");
            preferences.commit();
        
            Toast.makeText(MyMeetings.this, "Blanked", Toast.LENGTH_SHORT).show();

            setResult(RESULT_CANCELED);
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
