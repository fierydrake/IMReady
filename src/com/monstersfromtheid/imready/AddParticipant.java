package com.monstersfromtheid.imready;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.monstersfromtheid.imready.client.API;
import com.monstersfromtheid.imready.client.APICallFailedException;

public class AddParticipant extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_participant);

        // DRY up (pasta'd from ViewMeeting)
        Uri internalMeetingUri = getIntent().getData();

        if (!"content".equals(internalMeetingUri.getScheme())) { return; } // TODO Error handling
        if (!internalMeetingUri.getEncodedPath().startsWith("/meeting")) { return; } // TODO Error handling

        String meetingPath = internalMeetingUri.getEncodedPath();
        Pattern p = Pattern.compile("/meeting/(\\d+)/(.*)");
        Matcher m = p.matcher(meetingPath);
        if (!m.matches()) { return; } // TODO Error handling
        // DRY up end

        final int meetingId = Integer.parseInt(m.group(1));
//        String meetingName = Uri.decode(m.group(2));

        final Button addParticipant = (Button)findViewById(R.id.add_participant_button);
        final EditText username = (EditText)findViewById(R.id.add_participant_user_name);

        addParticipant.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                addParticipant(meetingId, username.getText().toString());
            }
        });
        
        username.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent event) {
             // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                 addParticipant(meetingId, username.getText().toString());
                 return true;
                }
                return false;
            }
        });
    }

    private void addParticipant(final int meetingId, final String userId) {
        final String creatorId = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountUserName", "");
        final API api = new API(creatorId);

        API.performInBackground(new API.Action<Void>() {
            @Override
            public Void action() throws APICallFailedException {
                api.addMeetingParticipant(meetingId, userId);
                return null;
            }
            @Override
            public void success(Void _) {
                Toast.makeText(AddParticipant.this, "Added '" + userId + "' to meeting with id " + meetingId, Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void failure(APICallFailedException e) {
                Toast.makeText(AddParticipant.this, "Failed: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }
}