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

import com.monstersfromtheid.imready.client.API;
import com.monstersfromtheid.imready.client.API.Action;
import com.monstersfromtheid.imready.client.APICallFailedException;
import com.monstersfromtheid.imready.client.Meeting;
import com.monstersfromtheid.imready.client.Participant;

public class ViewMeeting extends ListActivity {
	public static final int ACTIVITY_ADD_PARTICIPANT = 1;
    private ArrayList<HashMap<String, ?>> participants = new ArrayList<HashMap<String, ?>>();
    private String[] from = new String[] { "name", "readiness" };
    private int[] to = new int[] { R.id.meeting_participant_list_item_name,  
            R.id.meeting_participant_list_item_readiness };
    private SimpleAdapter adapter;
    
    private API api;
    private int meetingId;
    private String meetingName;
    
    private class RefreshMeetingDetailsAction extends Action<Meeting> {
        @Override
        public Meeting action() throws APICallFailedException {
            return api.meeting(meetingId);
        }
        @Override
        public void success(Meeting meeting) {
            updateMeetingInfo(meeting.getName(), meeting.getId());
            clearParticipants();
            for (Participant participant : meeting.getParticipants()) {
                addParticipant(
                        participant.getUser().getDefaultNickname(), 
                        participant.getUser().getId(), 
                        participant.getState() == Participant.STATE_READY
                        );
            }
            adapter.notifyDataSetChanged();
        }
        @Override
        public void failure(APICallFailedException e) {
            Toast.makeText(ViewMeeting.this, "Failed: " + e, Toast.LENGTH_LONG).show();
        }    	
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri internalMeetingUri = getIntent().getData();

        if (!"content".equals(internalMeetingUri.getScheme())) { return; } // TODO Error handling
        if (!internalMeetingUri.getEncodedPath().startsWith("/meeting")) { return; } // TODO Error handling

        String meetingPath = internalMeetingUri.getEncodedPath();
        Pattern p = Pattern.compile("/meeting/(\\d+)/(.*)");
        Matcher m = p.matcher(meetingPath);
        if (!m.matches()) { return; } // TODO Error handling

        String userNickName = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountNickName", "");
        String userName = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountUserName", "");
        
        api = new API(userName);
        meetingId = Integer.parseInt(m.group(1));
        meetingName = Uri.decode(m.group(2));

        adapter = new SimpleAdapter(this, participants, R.layout.meeting_participant_list_item, from, to);
        initialiseActivityFromLocalKnowledge(meetingName, meetingId, userNickName, userName);
        API.performInBackground(new RefreshMeetingDetailsAction());
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
        API.performInBackground(new RefreshMeetingDetailsAction());
    }

    private void initialiseActivityFromLocalKnowledge(String meetingName, int meetingId, String creatorNick, String creatorUserId) {
        updateMeetingInfo(meetingName, meetingId);
        addParticipant(creatorNick, creatorUserId, false);
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
        userItem.put("name", nick + " (" + id + ")");
        userItem.put("readiness", readiness);
        participants.add(userItem);
    }
}
