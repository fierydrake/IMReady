package com.monstersfromtheid.imready;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.monstersfromtheid.imready.client.ServerAPI;
import com.monstersfromtheid.imready.client.ServerAPICallFailedException;

public class CreateMeeting extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_meeting);
        
        final Button createMeeting = (Button)findViewById(R.id.create_meeting_button);
        final EditText meetingName = (EditText)findViewById(R.id.create_meeting_meeting_name);

        createMeeting.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                createMeeting(meetingName.getText().toString());
            }
        });
        
        meetingName.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent event) {
             // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                 createMeeting(meetingName.getText().toString());
                 return true;
                }
                return false;
            }
        });
    }

    private void createMeeting(final String name) {
        final String creatorId = IMReady.getUserName(this);
        final ServerAPI api = new ServerAPI(creatorId);

        ServerAPI.performInBackground(new ServerAPI.Action<Integer>() {
            @Override
            public Integer action() throws ServerAPICallFailedException {
                return api.createMeeting(creatorId, name);
            }
            @Override
            public void success(Integer meetingId) {
                //Toast.makeText(CreateMeeting.this, "Your meeting '"+name+"' was created with id " + meetingId, Toast.LENGTH_SHORT).show();

                final Intent i = new Intent();
                i.putExtra(IMReady.RETURNS_MEETING_ID, meetingId);
                i.putExtra(IMReady.RETURNS_MEETING_NAME, name);
                setResult(RESULT_OK, i);
                finish();
            }
            @Override
            public void failure(ServerAPICallFailedException e) {
                Toast.makeText(CreateMeeting.this, "Failed: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }
}