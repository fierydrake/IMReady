package com.monstersfromtheid.imready;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.monstersfromtheid.imready.client.ServerAPI;
import com.monstersfromtheid.imready.client.ServerAPI.Action;
import com.monstersfromtheid.imready.client.MessageAPI;
import com.monstersfromtheid.imready.client.ServerAPICallFailedException;
import com.monstersfromtheid.imready.client.Meeting;
import com.monstersfromtheid.imready.client.Participant;

public class ViewMeeting extends ListActivity {
	public static final int ACTIVITY_ADD_PARTICIPANT = 1;
    private ArrayList<HashMap<String, ?>> participants = new ArrayList<HashMap<String, ?>>();
    private String[] from = new String[] { "name", "readiness" };
    private int[] to = new int[] { R.id.meeting_participant_list_item_name,  
            R.id.meeting_participant_list_item_readiness };
    private SimpleAdapter adapter;
    
    private ServerAPI api;
    private int meetingId;
    private String meetingName;
    private boolean myStatus = false;
    View setReadiness;

    private class RefreshMeetingDetailsAction extends Action<Meeting> {
        @Override
        public Meeting action() throws ServerAPICallFailedException {
            return MessageAPI.meeting( api.meeting(meetingId) );
        }
        @Override
        public void success(Meeting meeting) {
            updateMeetingInfo(meeting.getName(), meeting.getId());
            clearParticipants();
            for (Participant participant : meeting.getParticipants()) {
            	if( ! api.getRequestingUserId().equalsIgnoreCase(participant.getUser().getId()) ) {
            		addParticipant(
            				participant.getUser().getDefaultNickname(), 
            				participant.getUser().getId(), 
            				participant.getState() == Participant.STATE_READY
                        	);
            	} else {
            		setMyStatus(participant.getState() == Participant.STATE_READY);
            	}
            }
            adapter.notifyDataSetChanged();
        }
        @Override
        public void failure(ServerAPICallFailedException e) {
            Toast.makeText(ViewMeeting.this, "Failed: " + e, Toast.LENGTH_LONG).show();
        }    	
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setReadiness = getLayoutInflater().inflate(R.layout.view_meeting_set_participant_status_button, null);

        Uri internalMeetingUri = getIntent().getData();

        if (!"content".equals(internalMeetingUri.getScheme())) { return; } // TODO Error handling
        if (!internalMeetingUri.getEncodedPath().startsWith("/meeting")) { return; } // TODO Error handling

        String meetingPath = internalMeetingUri.getEncodedPath();
        Pattern p = Pattern.compile("/meeting/(\\d+)/(.*)");
        Matcher m = p.matcher(meetingPath);
        if (!m.matches()) { return; } // TODO Error handling

        String userNickName = IMReady.getNickName(this);
        final String userName = IMReady.getUserName(this);
        
        api = new ServerAPI(userName);
        meetingId = Integer.parseInt(m.group(1));
        meetingName = Uri.decode(m.group(2));

        adapter = new SimpleAdapter(this, participants, R.layout.meeting_participant_list_item, from, to);
        initialiseActivityFromLocalKnowledge(meetingName, meetingId);
        ServerAPI.performInBackground(new RefreshMeetingDetailsAction());
        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.meeting_participant_list_item_readiness) {
                    ((TextView)view).setTextColor((Boolean)data ? Color.GREEN : Color.RED);
                    ((TextView)view).setText((Boolean)data ? "Ready" : "Not ready");
                    return true;
                }
                return false;
            }
        });

        final TextView userIdText = (TextView) setReadiness.findViewById(R.id.meeting_participant_my_name);
        userIdText.setText(userNickName + "(" + userName + ")");
		setMyStatus(myStatus);

        setReadiness.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		/* We can only set status to ready, so if we already are then we're done. */
				if( myStatus ) {
					return;
				}

				/* Set the status on the server */
				ServerAPI.performInBackground(new Action<Void>() {
        			@Override
        			public Void action() throws ServerAPICallFailedException {
        				api.ready(meetingId, userName);
        				return null;
        			}
        			@Override
        			public void success(Void result) {
        				/* Set the status colour to green */
        				setMyStatus(true);
        			}
        			@Override
        			public void failure(ServerAPICallFailedException e) {
        				Toast.makeText(ViewMeeting.this, "Failed to set user status: " + e, Toast.LENGTH_LONG).show();
        			}
        		});
        	}
        });
        getListView().addHeaderView(setReadiness);
        
        final Button addParticipant = (Button)getLayoutInflater().inflate(R.layout.view_meeting_add_participant_button, null);
        addParticipant.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
            	Uri internalMeetingUri = Uri.parse("content://com.monstersfromtheid.imready/meeting/" + meetingId + "/" + Uri.encode(meetingName)); // TODO hackish
            	startActivityForResult( new Intent(Intent.ACTION_VIEW, internalMeetingUri, ViewMeeting.this, AddParticipant.class), ACTIVITY_ADD_PARTICIPANT);
        	}
        });
        getListView().addFooterView(addParticipant);
        
        setListAdapter(adapter);
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ServerAPI.performInBackground(new RefreshMeetingDetailsAction());
    }

    private void initialiseActivityFromLocalKnowledge(String meetingName, int meetingId) {
        updateMeetingInfo(meetingName, meetingId);
        adapter.notifyDataSetChanged();
    }

    private void updateMeetingInfo(String meetingName, int meetingId) {
        setTitle("Meeting: " + meetingName + " (id=" + meetingId + ")");
    }

    private void clearParticipants() {
        participants.clear();
    }

    private void addParticipant(String nick, String id, boolean readiness) {
        HashMap<String, Object> userItem = new HashMap<String, Object>();
        userItem.put("userId", id);
        userItem.put("name", nick + " (" + id + ")");
        userItem.put("readiness", readiness);
        participants.add(userItem);
    }
    
    private void setMyStatus(boolean status) {
    	myStatus = status;
    	
        TextView setStatus = (TextView) setReadiness.findViewById(R.id.view_meeting_set_participant_status_button);
        TextView readinessText = (TextView) setReadiness.findViewById(R.id.meeting_participant_my_readiness);

        setStatus.setText( myStatus ? R.string.view_meeting_set_participant_status_button_ready : R.string.view_meeting_set_participant_status_button_not_ready );
        readinessText.setTextColor( myStatus ? Color.GREEN : Color.RED );
        readinessText.setText( myStatus ? "Ready" : "Not ready" );
    }
}
