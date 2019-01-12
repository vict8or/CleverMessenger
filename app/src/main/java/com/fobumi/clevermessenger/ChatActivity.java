package com.fobumi.clevermessenger;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fobumi.clevermessenger.data.Contact;
import com.fobumi.clevermessenger.data.Message;
import com.fobumi.clevermessenger.data.MessageAdapter;
import com.fobumi.clevermessenger.utilities.ContactUtils;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity implements SMSReceiver.OnReceiveListener {
    private static final int PERMISSIONS_REQUEST_CODE = 1;

    // EditText fields for contacts search and chat message
    private EditText mContactsInput;
    private EditText mMessageInput;

    // Reference list of contacts for use with suggestions
    private ArrayList<Contact> mContactList;

    // Used to implement suggestions for contacts
    private ListView mContactsListView;
    private ArrayAdapter<Contact> mContactsAdapter;

    // Stores messages in current conversation
    private ArrayList<Message> mMessageList = new ArrayList<>();
    private MessageAdapter mMessageAdapter;

    // Shown when there are no message sent or received yet
    private TextView mEmptyText;

    private SMSReceiver mSMSReceiver;

    // Phone number of current receiver in conversation
    private String mPhoneNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get SMS and contacts permissions
        String[] permissions = {
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_CONTACTS,
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
        }

        // Create and register SMS broadcast receiver, passing in reference 'this'
        // as an onReceiveListener to be notified when a valid message is received
        mSMSReceiver = new SMSReceiver(this);
        IntentFilter smsFilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        this.registerReceiver(mSMSReceiver, smsFilter);

        // If contacts permission is granted, initialize reference contacts list
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            mContactList = ContactUtils.getContactNames(this);
        }

        mContactsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mContactsListView = findViewById(R.id.lv_contacts);
        mContactsListView.setAdapter(mContactsAdapter);
        mContactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // When contact suggestion clicked, update phone number and clear conversation
                Contact contact = mContactsAdapter.getItem(position);
                mContactsInput.setText(contact.getName());
                mPhoneNumber = ContactUtils.getNumberFromContact(ChatActivity.this, contact);
                mSMSReceiver.setPhoneNumber(mPhoneNumber);
                clearThread();

                // Move cursor to chat message text field
                mMessageInput.requestFocus();
            }
        });

        mEmptyText = findViewById(R.id.tv_empty);
        mMessageInput = findViewById(R.id.et_message);

        mMessageAdapter = new MessageAdapter(this, mMessageList);
        RecyclerView messages = findViewById(R.id.rv_messages);
        messages.setAdapter(mMessageAdapter);
        messages.setHasFixedSize(true);
        messages.setLayoutManager(new LinearLayoutManager(this));

        // Listen to changes in contact search to implement suggestions
        mContactsInput = findViewById(R.id.et_contacts);
        mContactsInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Suggest all contacts matching search text
                updateSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mContactsInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // If contact suggestion not used, a phone number will be input,
                    // so update phone number if search text matches format for phone number
                    if (mContactsInput.getText().toString().matches("\\d{10,}")) {
                        mPhoneNumber = mContactsInput.getText().toString();
                        mSMSReceiver.setPhoneNumber(mPhoneNumber);

                        // Clear conversation with old contact/number
                        clearThread();
                    }
                }

                // Clear previous suggestions
                updateSearch("");
            }
        });

        ImageView sendButton = findViewById(R.id.iv_send_btn);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendText();
            }
        });

        // Direct user to appropriate text field when app is opened
        mContactsInput.requestFocus();
    }

    // Update contact suggestions based on search text
    private void updateSearch(String searchText) {
        ArrayList<Contact> names = new ArrayList<>();
        if (!searchText.equals("")) {
            // Regex that matches contact whose first or last name begin with searchText
            String matchPattern = "(?i)(" + searchText + ".*)|(.+\\s+" + searchText + ".*)";
            for (Contact contact : mContactList) {
                if (contact.getName().matches(matchPattern)) {
                    names.add(contact);
                }
            }
        }

        // If no suggestions (empty string passed), set visibility of suggestions accordingly
        mContactsListView.setVisibility(names.size() == 0 ? View.GONE : View.VISIBLE);

        // Update adapter with new suggestions
        mContactsAdapter.clear();
        mContactsAdapter.addAll(names);
    }

    // Called when send button pressed, sends contents of message input to current receiver
    private void sendText() {
        String contents = mMessageInput.getText().toString();
        if (!TextUtils.isEmpty(mPhoneNumber) && !TextUtils.isEmpty(contents)) {
            SmsManager.getDefault().sendTextMessage(
                    mPhoneNumber,
                    null,
                    contents,
                    null,
                    null);
            updateThread(new Message(contents, System.currentTimeMillis(), false));
            mMessageInput.setText("");
        } else {
            Toast.makeText(this, R.string.toast_invalid_input, Toast.LENGTH_SHORT).show();
        }
    }

    // Update RecyclerView with new message
    private void updateThread(Message message) {
        mMessageList.add(message);
        mMessageAdapter.notifyDataSetChanged();
        mEmptyText.setVisibility(View.INVISIBLE);
    }

    // Clear conversations and show empty text
    private void clearThread() {
        mMessageList.clear();
        mMessageAdapter.notifyDataSetChanged();
        mEmptyText.setVisibility(View.VISIBLE);
    }

    // Check if all permissions have been granted
    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Called when SMS broadcast receiver detects a message from current number
    @Override
    public void onReceive(Message message) {
        updateThread(message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Initialize reference contacts list
                mContactList = ContactUtils.getContactNames(this);
                Toast.makeText(this,
                        getResources().getString(R.string.toast_permissions_granted),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        getResources().getString(R.string.toast_permissions_denied),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_auto_reply) {
            // Toggle auto-reply option
            boolean isChecked = item.isChecked();
            item.setChecked(!isChecked);

            // Broadcast receiver must know whether or not to get reply from Cleverbot API
            mSMSReceiver.setAutoReply(!isChecked);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mSMSReceiver);
    }
}
