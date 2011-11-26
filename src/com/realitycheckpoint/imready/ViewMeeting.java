package com.realitycheckpoint.imready;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ListActivity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class ViewMeeting extends ListActivity {
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

        String meetingId = m.group(1);
        String meetingName = Uri.decode(m.group(2));

        String userNickName = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountNickName", "");
        String userName = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountUserName", "");

        setTitle("Meeting: " + meetingName + " (id=" + meetingId + ")");
        
        ArrayList<HashMap<String, ?>> participants = new ArrayList<HashMap<String, ?>>();
        HashMap<String, Object> userItem = new HashMap<String, Object>();
        userItem.put("name", userNickName + " (" + userName + ")");
        userItem.put("readiness", false);
        participants.add(userItem);
        String[] from = new String[] { "name", "readiness" };
        int[] to = new int[] { R.id.meeting_participant_list_item_name,  
        					   R.id.meeting_participant_list_item_readiness };
        SimpleAdapter adapter = new SimpleAdapter(this, participants, R.layout.meeting_participant_list_item, from, to);
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
        setListAdapter(adapter);
    }
}
