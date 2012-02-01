package com.monstersfromtheid.imready;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class Preferences extends Activity {

	private Spinner notificationSpinner;
	private Spinner pollingSpinner;

	public class OnPollingItemSelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
	    	
	    	if( parent == Preferences.this.notificationSpinner ){
		    	IMReady.setNotificationLevel(pos, Preferences.this);

		    	// If we just set the notification to "never", cancel the cron job and disable the polling interval
		    	// Do we need to cancel the alarm?  If the app is running, what's the state of the alarm?
		    	IMReady.setNextAlarm(Preferences.this);

		    	pollingSpinner.setEnabled( pos != 0 );

	    	} else if (parent == Preferences.this.pollingSpinner) {
	    		IMReady.setPollingInterval(pos, Preferences.this);
			} else {
				// Unknown sender, so ignore.
			}
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.preferences);
        setTitle("Preferences");

        notificationSpinner = (Spinner) findViewById(R.id.preferences_notification_level_spinner);
        ArrayAdapter<CharSequence> notificationAdapter = ArrayAdapter.createFromResource(this, R.array.preferences_notification_level_values, android.R.layout.simple_spinner_item);
        notificationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notificationSpinner.setAdapter(notificationAdapter);
        notificationSpinner.setOnItemSelectedListener(new OnPollingItemSelectedListener());
        notificationSpinner.setSelection(IMReady.getNotificationLevel(this));

        pollingSpinner = (Spinner) findViewById(R.id.preferences_polling_interval_spinner);
        ArrayAdapter<CharSequence> pollingAdapter = ArrayAdapter.createFromResource(this, R.array.preferences_polling_interval_values, android.R.layout.simple_spinner_item);
        pollingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pollingSpinner.setAdapter(pollingAdapter);
        pollingSpinner.setOnItemSelectedListener(new OnPollingItemSelectedListener());
        pollingSpinner.setSelection(IMReady.getPollingInterval(this));
        pollingSpinner.setEnabled( IMReady.getNotificationLevel(this) != 0 );
	}
}
