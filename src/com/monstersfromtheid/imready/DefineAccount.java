package com.monstersfromtheid.imready;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.monstersfromtheid.imready.client.ServerAPI;
import com.monstersfromtheid.imready.client.ServerAPI.Action;
import com.monstersfromtheid.imready.client.ServerAPICallFailedException;

public class DefineAccount extends Activity {

    public static final int NEW_ACCOUNT = 0;
    public static final int EXISTING_ACCOUNT = 1;
    public static final int ACTIVITY_GOT_ACCOUNT = 0;
    public static final String USERID_PATTERN = "[A-Za-z_0-9]+";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        boolean changeAccount = false;
        // TODO  - something to guarantee the alarm manager is always going for us.
        // Does this method always get called?
        IMReady.setNextAlarm(this); // remove this in the long run.

	    Uri accountDetailsAction = getIntent().getData();
        if ( accountDetailsAction != null &&
        	 "content".equals( accountDetailsAction.getScheme() ) &&
             accountDetailsAction.getEncodedPath().startsWith("/util")) {

        	String accountAction = accountDetailsAction.getEncodedPath();
        	Pattern p = Pattern.compile("/util/(.*)");
        	Matcher m = p.matcher(accountAction);
        	if (m.matches()) {

        		String actionName = Uri.decode(m.group(1));

        		/* If we're launched to change the account details */
        		if ( actionName.equalsIgnoreCase(IMReady.ACTIONS_ACOUNT_CHANGE_DETAILS) ) {
        			changeAccount = true;
        		}
        	}
        }
        
        /* If we're launched from new.  If an account is already defined, move to meeting list. */
        if ( !changeAccount && IMReady.isAccountDefined(this) ) {
            startActivityForResult(new Intent(DefineAccount.this, MyMeetings.class), ACTIVITY_GOT_ACCOUNT);
            /* Otherwise, create an account */
        } else {
            setContentView(R.layout.define_account);

            final Button newAccount = (Button) findViewById(R.id.define_account_new_button);
            final Button existingAccount = (Button) findViewById(R.id.define_account_existing_button);
            final EditText userName = (EditText) findViewById(R.id.define_account_username);
            final EditText nickName = (EditText) findViewById(R.id.define_account_nickname);
            
            if ( changeAccount && IMReady.isAccountDefined(this) ){
            	userName.setText( IMReady.getUserName(this) );
            	nickName.setText( IMReady.getNickName(this) );
            }

            newAccount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String userId = userName.getText().toString().trim();
                    if (userId.matches(USERID_PATTERN)) {
                        createAccount(NEW_ACCOUNT, userId, nickName.getText().toString());
                    } else {
                        Toast.makeText(DefineAccount.this, "The username you supplied is invalid, it may only include letters, digits and underscores", Toast.LENGTH_LONG).show();
                    }
                }
            });

            existingAccount.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    String userId = userName.getText().toString().trim();
                    if (userId.matches(USERID_PATTERN)) {
                        createAccount(EXISTING_ACCOUNT, userId, nickName.getText().toString());
                    } else {
                        Toast.makeText(DefineAccount.this, "The username you supplied is invalid, it may only include letters, digits and underscores", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /* If returning after ACTIVITY_GOT_ACCOUNT then just exit this Activity */
        if (requestCode == ACTIVITY_GOT_ACCOUNT) {
            finish();
        }
    }

    private void createAccount(final int accountType, final String username, final String nickname) {
        /* Sanity check the user and nick name */
        /* TODO */
    	final ServerAPI api = new ServerAPI(username);

    	IMReady.setUserAndNickName(username, nickname, this);

        ServerAPI.performInBackground(new Action<Void>() {
            @Override
            public Void action() throws ServerAPICallFailedException {
                if (accountType == NEW_ACCOUNT) {
                    api.createUser(username, nickname);
                } else if (accountType == EXISTING_ACCOUNT) {
                    api.user(username);
                }
                return null;
            }

            @Override
            public void success(Void result) {
                IMReady.setAccountDefined(true, DefineAccount.this);

                /* Now we have an account, we can go to create a meeting */
                startActivityForResult(new Intent(DefineAccount.this, MyMeetings.class), ACTIVITY_GOT_ACCOUNT);
            }

            @Override
            public void failure(ServerAPICallFailedException e) {
                Toast.makeText(DefineAccount.this, "Failed: " + e, Toast.LENGTH_LONG).show();
            }
        });
    }
}
