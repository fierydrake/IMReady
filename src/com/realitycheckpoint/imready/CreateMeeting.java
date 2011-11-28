package com.realitycheckpoint.imready;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.realitycheckpoint.imready.client.API;
import com.realitycheckpoint.imready.client.APICallFailedException;

public class CreateMeeting extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.create_meeting);
        
        final Button createMeeting = (Button)findViewById(R.id.create_meeting_button);
        final EditText meetingName = (EditText)findViewById(R.id.create_meeting_meeting_name);

        String nickName = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountNickName", "");
        if (!nickName.equalsIgnoreCase("")) {
            TextView welcomeLine = (TextView)findViewById(R.id.create_meeting_nickname);
            welcomeLine.setText("Hello " + nickName);
        }

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
         Toast.makeText(CreateMeeting.this, "TODO", Toast.LENGTH_SHORT).show();
            return true;
        case R.id.menu_blank:
            SharedPreferences.Editor preferences = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).edit();
            preferences.remove("accountDefined");
            preferences.remove("accountUserName");
            preferences.remove("accountNickName");
            preferences.commit();
        
            Toast.makeText(CreateMeeting.this, "Blanked", Toast.LENGTH_SHORT).show();

            setResult(RESULT_CANCELED);
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void createMeeting(final String name) {
        final String creatorId = getSharedPreferences(IMReady.PREFERENCES_NAME, MODE_PRIVATE).getString("accountUserName", "");

        API.performInBackground(new API.Action<Integer>() {
            @Override
            public Integer action() throws APICallFailedException {
                return API.createMeeting(creatorId, name);
            }
            @Override
            public void success(Integer meetingId) {
                Toast.makeText(CreateMeeting.this, "Your meeting '"+name+"' was created with id " + meetingId, Toast.LENGTH_SHORT).show();

                Uri internalMeetingUri = Uri.parse("content://com.realitycheckpoint.imready/meeting/" + meetingId + "/" + Uri.encode(name)); // TODO hackish
                startActivity( new Intent(Intent.ACTION_VIEW, internalMeetingUri, CreateMeeting.this, ViewMeeting.class) );
            }
            @Override
            public void failure(APICallFailedException e) {
                Toast.makeText(CreateMeeting.this, "Failed: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }
}